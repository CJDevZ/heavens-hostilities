package eu.cj4.skywars.impl.state;

import eu.cj4.skywars.impl.Skywars;
import eu.cj4.skywars.impl.SkywarsInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static eu.cj4.skywars.impl.Skywars.CONFIG;
import static eu.cj4.skywars.impl.SkywarsInstance.INCREMENT_WINS_STAT;

public class InProgress extends GameState {
//    private int timer;

    public InProgress(SkywarsInstance instance) {
        super(instance);
    }

    @Override
    public void tick() {
//        ++this.timer;
    }

    @Override
    public void teamDeath(int team) {
        var teamManager = this.instance.teamManager();
        if (teamManager.teamsAlive() > 1) {
            return;
        }
        int winnerTeam = teamManager.smallestAliveTeamIndex();
        if (winnerTeam != -1) {
            this.teamWon(winnerTeam);
        }
        this.instance.setGameState(new Ended(this.instance, 200));
    }

    public void teamWon(int team) {
        List<ServerPlayer> winners = new ArrayList<>(CONFIG.playersPerTeam);
        List<Component> winnerComponents = new ArrayList<>(CONFIG.playersPerTeam);
        for (ServerPlayer serverPlayer : this.instance.gameSpace().getPlayers()) {
            Integer playerTeam = serverPlayer.minigame$getTeam();
            if (playerTeam != null && playerTeam == team) {
                winners.add(serverPlayer);
                winnerComponents.add(serverPlayer.getDisplayName());
                serverPlayer.serverlib$incrementStat(INCREMENT_WINS_STAT);
            }
        }
        Component separator = Component.literal(", ").withStyle(ChatFormatting.GRAY);
        Component winnerText = ComponentUtils.formatList(winnerComponents, _ -> separator);

        Skywars.LOGGER.info("Team ({}) won!", winnerText);
        //player.setGameMode(GameType.ADVENTURE);
        this.instance.gameSpace().getPlayers().sendMessage(Component.translatable("skywars.won", winnerText).withStyle(ChatFormatting.LIGHT_PURPLE));
        // TODO: maybe particles
    }

    @Override
    public boolean allowTeamDeath() {
        return true;
    }
}
