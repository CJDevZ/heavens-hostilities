package eu.cj4.skywars.impl.state;

import eu.cj4.minigame.impl.MiniGame;
import eu.cj4.skywars.impl.SkywarsInstance;
import eu.cj4.skywars.impl.SkywarsTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static eu.cj4.skywars.impl.Skywars.CONFIG;

public class WaitingForPlayers extends GameState {
    private int countdown;

    public WaitingForPlayers(SkywarsInstance instance) {
        super(instance);
    }

    @Override
    public void tick() {
        int teamCount = this.instance.teamManager().teamsAlive();
        int requiredPlayers = (CONFIG.teamsPerMap * CONFIG.startPercentage + 99) / 100;

        if (teamCount < 2 || teamCount < requiredPlayers) {
            if (this.countdown != 0) {
                this.instance.gameSpace().getPlayers().sendMessage(Component.literal("Start cancelled").withStyle(ChatFormatting.RED));
            }
            this.countdown = 0;
            return;
        }

        if (this.countdown == 0) {
            this.countdown = 600;
            this.instance.gameSpace().getPlayers().sendMessage(Component.literal("Starting Game in 30").withStyle(ChatFormatting.YELLOW));
            return;
        }

        boolean enoughTeams = teamCount >= CONFIG.teamsPerMap;

        --this.countdown;
        if (enoughTeams && this.countdown > 200) {
            this.countdown = 200;
        } else if (this.countdown == 0) {
            this.instance.gameSpace().getPlayers().sendMessage(Component.literal("Starting Game").withStyle(ChatFormatting.GREEN));
            this.startGame();
        } else if (this.countdown % 100 == 0 || (this.countdown < 100 && this.countdown % 20 == 0)) {
            int secondsRemaining = this.countdown / 20;
            this.instance.gameSpace().getPlayers().sendMessage(Component.literal("Starting Game in ").append(String.valueOf(secondsRemaining)).withStyle(ChatFormatting.YELLOW));
        }
    }

    private void startGame() {
        MiniGame.FREE_LOBBIES.remove(this.instance);

        var AIR = Blocks.AIR.defaultBlockState();
        for (SkywarsTeam teamData : this.instance.teamManager().dataArray()) {
            BlockPos.betweenClosedStream(teamData.glassCase()).forEach(blockPos -> {
                this.instance.level().setBlock(blockPos, AIR, Block.UPDATE_CLIENTS);
            });
        }
        for (ServerPlayer player : this.instance.gameSpace().getPlayers()) {
            player.setGameMode(GameType.SURVIVAL);
            player.setIgnoreFallDamageFromCurrentImpulse(true, player.position().with(Direction.Axis.Y, -Double.MAX_VALUE));
        }

        this.instance.setGameState(new InProgress(this.instance));
    }
}
