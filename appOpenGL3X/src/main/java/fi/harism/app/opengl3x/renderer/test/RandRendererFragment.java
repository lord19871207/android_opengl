package fi.harism.app.opengl3x.renderer.test;

import android.opengl.GLES30;

import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.renderer.RendererFragment;
import fi.harism.lib.opengl.util.GlRenderer;

public class RandRendererFragment extends RendererFragment {

    @Override
    public int getTitleStringId() {
        return R.string.renderer_tests_clear_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_tests_clear_caption;
    }

    @Override
    public void onSurfaceCreated() {
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
    }

    @Override
    public void onRenderFrame() {
        float rand = (float) Math.random();
        GLES30.glClearColor(rand, rand, rand, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void onSurfaceReleased() {
    }
}
