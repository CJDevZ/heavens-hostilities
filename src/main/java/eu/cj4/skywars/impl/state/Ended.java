package eu.cj4.skywars.impl.state;

import eu.cj4.skywars.impl.SkywarsInstance;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;

public class Ended extends GameState {
    private int countdown;

    public Ended(SkywarsInstance instance, int countdown) {
        super(instance);
        this.countdown = countdown;
    }

    @Override
    public void tick() {
        if (--this.countdown <= 0) {
            this.instance.gameSpace().close(GameCloseReason.FINISHED);
        }
    }

    @Override
    public boolean allowTeamDeath() {
        return true;
    }
}
