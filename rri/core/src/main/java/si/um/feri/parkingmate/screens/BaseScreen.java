package si.um.feri.parkingmate.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;

public abstract class BaseScreen implements Screen {

    protected OrthographicCamera camera;

    public BaseScreen() {
        camera = new OrthographicCamera();
    }

    @Override public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
