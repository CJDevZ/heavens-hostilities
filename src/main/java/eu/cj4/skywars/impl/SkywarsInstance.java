package eu.cj4.skywars.impl;

import eu.cj4.minigame.api.TeamManager;
import eu.cj4.skywars.impl.state.GameState;
import eu.cj4.skywars.impl.state.InProgress;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.*;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.*;

import static eu.cj4.skywars.impl.Skywars.CONFIG;

public class SkywarsInstance {
    protected final SkywarsMap skywarsMap;
    protected final GameSpace gameSpace;
    protected final ServerLevel level;

    protected final TeamManager<SkywarsTeam> teamManager;

    protected GameState gameState;
    protected int timer;

    public static final String INCREMENT_WINS_STAT = "skywars:wins";

    public SkywarsInstance(SkywarsMap skywarsMap, GameSpace gameSpace, ServerLevel level) {
        this.skywarsMap = skywarsMap;
        this.gameSpace = gameSpace;
        this.level = level;

        this.timer = 0;
        this.gameState = null;

        BlockPos offset = mapOffset();
        List<BlockPos> spawnLocations = skywarsMap.spawnLocations();
        int teamCount = spawnLocations.size();
        this.teamManager = TeamManager.create(new SkywarsTeam[teamCount], team -> {
            BlockPos spawnLocation = spawnLocations.get(team).offset(offset);
            AABB spawnAABB = AABB.ofSize(Vec3.atCenterOf(spawnLocation), 5, 5, 5);
            return new SkywarsTeam(spawnLocation, spawnAABB);
        }, this::teamDeath);
    }

    public static GameOpenProcedure open(GameOpenContext<Holder<SkywarsMap>> context) {
        SkywarsMap skywarsMap = context.config().value();
        RuntimeLevelConfig worldConfig = new RuntimeLevelConfig()
                .setGenerator(new VoidChunkGenerator(context.server(), skywarsMap.map().biome()))
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setGameRule(GameRules.SPAWN_MONSTERS, false)
                .setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
                .setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 200)
                .setGameRule(GameRules.SPAWN_PHANTOMS, false)
                .setGameRule(GameRules.LOCATOR_BAR, false)
                .setDifficulty(Difficulty.NORMAL);

        return context.openWithLevel(worldConfig, (activity, serverLevel) -> {
            SkywarsInstance currentGame = new SkywarsInstance(skywarsMap, activity.getGameSpace(), serverLevel);

            skywarsMap.placeMap(serverLevel);
            serverLevel.getWorldBorder().setCenter(skywarsMap.centerPos().x, skywarsMap.centerPos().z);
            serverLevel.getWorldBorder().setSize(skywarsMap.borderWidth());

            GameWaitingLobby.addTo(activity, new WaitingLobbyConfig(2, CONFIG.teamsPerMap * CONFIG.playersPerTeam));

            currentGame.addCommonActivity(activity);
            activity.listen(GamePlayerEvents.ADD, currentGame::addPlayer);
            activity.listen(GamePlayerEvents.ACCEPT, acceptor -> acceptor.teleport(serverLevel, currentGame.getCenterPos()));
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptParticipants);
            activity.listen(GameActivityEvents.REQUEST_START, currentGame::onStart);
        });
    }

    public TeamManager<SkywarsTeam> teamManager() {
        return this.teamManager;
    }

    public void setGameState(@Nullable GameState gameState) {
        this.gameState = gameState;
    }

    public void addCommonActivity(GameActivity activity) {
        activity.listen(PlayerDeathEvent.EVENT, this::onPlayerDeath);
    }

    public void addInGameActivity(GameActivity activity) {
        this.addCommonActivity(activity);
        //activity.listen(GamePlayerEvents.OFFER, _ -> new JoinOfferResult.Reject());
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    public GameResult onStart() {
        var AIR = Blocks.AIR.defaultBlockState();
        for (SkywarsTeam teamData : this.teamManager.dataArray()) {
            BlockPos.betweenClosedStream(teamData.glassCase()).forEach(blockPos -> {
                this.level.setBlock(blockPos, AIR, Block.UPDATE_CLIENTS);
            });
        }
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            player.setGameMode(GameType.SURVIVAL);
            player.setIgnoreFallDamageFromCurrentImpulse(true, player.position().with(Direction.Axis.Y, -Double.MAX_VALUE));
        }

        this.gameState = new InProgress(this);
        this.gameSpace.setActivity(this::addInGameActivity);
        return GameResult.ok();
    }

    public void onTick() {
        this.gameState.tick();
    }

    public EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        if (this.gameState.allowTeamDeath()) {
            this.teamManager.removePlayerFromTeam(player);
            player.setGameMode(GameType.SPECTATOR);
        }
        Vec3 location = getRespawnLocation(player);
        player.teleportTo(this.level, location.x, location.y, location.z, Relative.ROTATION, 0, 0, false);
        return EventResult.DENY;
    }

    public void removePlayer(ServerPlayer player) {
        if (this.gameState.allowTeamDeath()) {
            this.teamManager.removePlayerFromTeam(player);
        }
    }

    public void addPlayer(ServerPlayer player) {
        int smallestTeam = this.teamManager.smallestTeamIndex();
        SkywarsTeam teamData = this.teamManager.getData(smallestTeam);
        this.teamManager.addPlayerToTeam(player, smallestTeam);
        BlockPos freeLocation = teamData.spawnPoint();

        this.gameSpace.getPlayers().sendMessage(Component.literal("ADDED PLAYER"));
        this.gameSpace.getPlayers().sendMessage(Component.literal("%s/%s Players.".formatted(gameSpace.getPlayers().size(), CONFIG.teamsPerMap * CONFIG.playersPerTeam)));
        player.teleport(new TeleportTransition(this.level, Vec3.atBottomCenterOf(freeLocation), Vec3.ZERO, 0f, 0f, entity -> {
            entity.lookAt(EntityAnchorArgument.Anchor.FEET, getCenterPos());
        }));
    }

    public void teamDeath(int team) {
        if (this.gameState != null) {
            this.gameState.teamDeath(team);
        }
    }

    public Level level() {
        return this.level;
    }

    public GameSpace gameSpace() {
        return this.gameSpace;
    }

    public Vec3 getCenterPos() {
        return new Vec3(this.mapOffset()).add(skywarsMap.centerPos());
    }

    public Vec3 getRespawnLocation(ServerPlayer player) {
        if (this.gameState.allowTeamDeath()) {
            return Vec3.atBottomCenterOf(teamManager.getData(player.minigame$getTeam()).spawnPoint());
        } else {
            return getCenterPos();
        }
    }

    public BlockPos mapOffset() {
        return skywarsMap.offset();
    }

    public int freeSlots() {
        return CONFIG.teamsPerMap * CONFIG.playersPerTeam - gameSpace.getPlayers().size();
    }
}
