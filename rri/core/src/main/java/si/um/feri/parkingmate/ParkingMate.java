package si.um.feri.parkingmate;

import com.badlogic.gdx.Game;
import si.um.feri.parkingmate.screens.MapScreen;
import si.um.feri.parkingmate.simulation.SimulationScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */

public class ParkingMate extends Game {

    @Override
    public void create() {
        setScreen(new MapScreen(this));
    }
}
