package fi.harism.app.opengl3x.fragment.test;

import android.graphics.Color;
import android.opengl.GLES30;

import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.fragment.RendererFragment;

public class ClearRendererFragment extends RendererFragment {

    private static final String RENDERER_ID = "renderer.test.clear";

    private static final int clearColor = Color.WHITE;

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_test_clear_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_test_clear_caption;
    }

    @Override
    public void onSurfaceCreated() {
        setContinuousRendering(true);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
    }

    @Override
    public void onRenderFrame() {
        float rand = (float) Math.random();
        float r = rand * (Color.red(clearColor) / 255f);
        float g = rand * (Color.green(clearColor) / 255f);
        float b = rand * (Color.blue(clearColor) / 255f);
        GLES30.glClearColor(r, g, b, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void onSurfaceReleased() {
    }

}
