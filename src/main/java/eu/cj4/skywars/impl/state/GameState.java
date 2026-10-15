package eu.cj4.skywars.impl.state;

import eu.cj4.skywars.impl.SkywarsInstance;

public abstract class GameState {
    protected final SkywarsInstance instance;

    public GameState(SkywarsInstance instance) {
        this.instance = instance;
    }

    public abstract void tick();

    public void teamDeath(int team) {
    }

    public boolean allowTeamDeath() {
        return false;
    }
}
