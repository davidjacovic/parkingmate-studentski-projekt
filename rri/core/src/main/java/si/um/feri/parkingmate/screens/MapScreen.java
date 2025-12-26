package si.um.feri.parkingmate.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import si.um.feri.parkingmate.ParkingMate;

public class MapScreen extends BaseScreen {

    private final ParkingMate game;

    public MapScreen(ParkingMate game) {
        this.game = game;
    }

    @Override
    public void show() {
        // inicijalizacija mape ide kasnije (Subtask 2.1.2)
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // za sada samo prazni render
    }

    @Override
    public void dispose() {
        // kasnije čišćenje resursa
    }
}

