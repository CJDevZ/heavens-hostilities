package eu.cj4.skywars.impl;

import com.mojang.logging.LogUtils;
import eu.cj4.minigame.impl.MiniGame;
import eu.cj4.serverlib.api.event.PlayerEvents;
import eu.cj4.minigame.api.FantasyEvents;
import eu.cj4.serverlib.impl.ServerLib;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.*;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameTypes;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.plasmid.api.game.player.GamePlayerJoiner;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;

import java.util.*;
import java.util.function.Predicate;

public class Skywars implements ModInitializer {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<Registry<SkywarsMap>> SKYWARS_MAP_REGISTRY;
    public static final SkywarsConfig CONFIG = ServerLib.readConfig("skywars.toml", SkywarsConfig.class);

    @Override
    public void onInitialize() {
        DynamicRegistries.register(SKYWARS_MAP_REGISTRY, SkywarsMap.CODEC);

        //PlayerEvents.GAMEMODE_JOIN.register((player, mapId) -> player.teleport(joinNewGame(player, mapId)));
        //FantasyEvents.KICKED_FROM_LEVEL.register(player -> joinNewGame(player, null));

        ServerPlayerEvents.JOIN.register(this::updateGameInfo);
        ServerPlayerEvents.LEAVE.register(this::updateGameInfo);
        GameTypes.register(Identifier.parse("skywars:skywars"), RegistryCodecs.holder(SKYWARS_MAP_REGISTRY).fieldOf("map"), SkywarsInstance::open);
    }

    public void updateGameInfo(ServerPlayer proxy) {
        var server = proxy.level().getServer();
        var maxPlayers = server.getMaxPlayers();

        int freeSlots = 0;

        if (!MiniGame.noFreeLobbies()) {
            freeSlots = MiniGame.getFreeLobbySlots();
        }
        if (freeSlots == 0) freeSlots = Math.min(CONFIG.playersPerTeam * CONFIG.teamsPerMap, maxPlayers - server.getPlayerCount());

        // TODO: Sync Free Lobby Data via Redis
        //proxy.connection.send(new ClientboundCustomPayloadPacket(new UpdateGameInfoPayload(new GameInfo(freeSlots))));
    }

//    public static TeleportTransition joinNewGame(ServerPlayer serverPlayer, Identifier mapId) {
//        serverPlayer.minigame$resetPlayer();
//        serverPlayer.setGameMode(GameType.ADVENTURE);
//
//        MinecraftServer server = serverPlayer.level().getServer();
//        RandomSource random = serverPlayer.getRandom();
//        Registry<SkywarsMap> registry = serverPlayer.registryAccess().lookupOrThrow(SKYWARS_MAP_REGISTRY);
//        SkywarsMap chosenMap = getMapById(registry, mapId);
//
//        Predicate<SkywarsInstance> predicate = chosenMap != null
//                ? i -> i.freeSlots() > 0 && i.map() == chosenMap
//                : i -> i.freeSlots() > 0;
//
//        new GameOpenContext<>(server, Holder.direct(new GameConfig<>(Util.getRandomSafe(registry.listElements().toList(), random)
//                .orElseThrow(() -> new IllegalStateException("No Skywars map registered!"))
//                .value())));
//        SkywarsInstance instance = MiniGame.getRandomLobby(random, predicate).orElseGet(() ->
//                SkywarsInstance.open());
//
//        GamePlayerJoiner.tryJoin(serverPlayer, instance.gameSpace, JoinIntent.PLAY);
//        return new TeleportTransition(instance.level, serverPlayer.position(), Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING);
//    }

    public static @Nullable SkywarsMap getMapById(HolderLookup.RegistryLookup<SkywarsMap> lookup, Identifier mapId) {
        return lookup.get(ResourceKey.create(SKYWARS_MAP_REGISTRY, mapId)).map(Holder.Reference::value).orElse(null);
    }

    static {
        SKYWARS_MAP_REGISTRY = ResourceKey.createRegistryKey(Identifier.parse("skywars:map"));
    }

}
