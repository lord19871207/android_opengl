package fi.harism.opengl.app.test;

import android.graphics.Color;
import android.opengl.GLES30;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;

import fi.harism.opengl.app.R;
import fi.harism.opengl.app.RenderActivity;
import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.view.GlTextureView;

public class TestRenderActivity extends RenderActivity {

    private static final String TAG = "TestRenderFragment";

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            mGlTextureView.renderFrame(frameTimeNanos);
            mChoreographer.postFrameCallback(this);
        }
    };

    private GlTextureView mGlTextureView;
    private Choreographer mChoreographer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChoreographer = Choreographer.getInstance();

        mGlTextureView = new GlTextureView(this);
        mGlTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        mGlTextureView.setGlRenderer(new TestRenderer(0xFF3366FF));
        setContentView(mGlTextureView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mChoreographer.postFrameCallback(mFrameCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        mChoreographer.removeFrameCallback(mFrameCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGlTextureView.onDestroy();
        mGlTextureView = null;
    }

    @Override
    public int getRendererTitleId() {
        return R.string.test_renderer_title;
    }

    @Override
    public int getRendererCaptionId() {
        return R.string.test_renderer_caption;
    }

    private class TestRenderer implements GlRenderer {

        private final int mColor;

        public TestRenderer(int color) {
            mColor = color;
        }

        @Override
        public void onSurfaceCreated() {
            Log.d(TAG, "onSurfaceCreated");
        }

        @Override
        public void onSurfaceReleased() {
            Log.d(TAG, "onSurfaceReleased");
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
        }

        @Override
        public void onRenderFrame() {
            //Log.d(TAG, "onRender");
            float val = (float) Math.random();
            float inv = 1.0f / 255.0f;
            GLES30.glClearColor(Color.red(mColor) * val * inv, Color.green(mColor) * val * inv, Color.blue(mColor) * val * inv, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        }
    }

}
