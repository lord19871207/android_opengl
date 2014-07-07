package fi.harism.opengl.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.opengl.GLES30;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.view.GlTextureView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            GlTextureView glTextureView = (GlTextureView) findViewById(R.id.gltextureview);
            glTextureView.renderFrame(frameTimeNanos);
            mChoreographer.postFrameCallback(this);
        }
    };

    private GlTextureView mGlTextureView;
    private Choreographer mChoreographer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View actionButton = findViewById(R.id.button_action);
        int actionButtonWidth = actionButton.getLayoutParams().width;
        int actionButtonHeight = actionButton.getLayoutParams().height;

        Outline actionButtonOutline = new Outline();
        actionButtonOutline.setOval(0, 0, actionButtonWidth, actionButtonHeight);

        actionButton.setOutline(actionButtonOutline);
        actionButton.setClipToOutline(true);

        mGlTextureView = (GlTextureView) findViewById(R.id.gltextureview);
        mGlTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        mGlTextureView.setGlRenderer(new TestRenderer(0xFF808080));

        actionButton.setAlpha(0f);
        actionButton.animate().alpha(1.0f).setDuration(500).start();
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.animate().alpha(0f).setDuration(500).start();
            }
        });

        mChoreographer = Choreographer.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChoreographer.postFrameCallback(mFrameCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mChoreographer.removeFrameCallback(mFrameCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGlTextureView.onDestroy();
    }

    private class TestRenderer implements GlRenderer {

        private final int mColor;

        public TestRenderer(int color) {
            mColor = color;
        }

        @Override
        public void onContextCreated() {
            Log.d(TAG, "onContextCreated");
        }

        @Override
        public void onRelease() {
            Log.d(TAG, "onRelease");
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
