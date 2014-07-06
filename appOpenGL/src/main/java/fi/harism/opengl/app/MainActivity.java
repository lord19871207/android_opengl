package fi.harism.opengl.app;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLES30;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.egl.EglSurface;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.util.GlRunnable;
import fi.harism.opengl.lib.view.GlTextureView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private GlTextureView mGlTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new TestAdapter());
        setContentView(recyclerView);
    }

    private class TestAdapter extends RecyclerView.Adapter<TestHolder> {

        private final int mColors[] = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFFFFFFFF};

        @Override
        public TestHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            GlTextureView glTextureView = new GlTextureView(MainActivity.this);
            glTextureView.setEglVersion(EglCore.VERSION_GLES3);
            CardView cardView = new CardView(MainActivity.this);
            cardView.setRadius(50);
            cardView.setAlpha(0.6f);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(700, 400);
            cardView.setLayoutParams(layoutParams);
            cardView.addView(glTextureView);
            return new TestHolder(cardView, glTextureView);
        }

        @Override
        public void onBindViewHolder(TestHolder testHolder, int position) {
            testHolder.getGlTextureView().setGlRenderer(new TestGlRenderer(mColors[position]));
        }

        @Override
        public int getItemCount() {
            return mColors.length;
        }
    }

    private class TestHolder extends RecyclerView.ViewHolder {

        private final GlTextureView mGlTextureView;

        private TestHolder(View parent, GlTextureView glTextureView) {
            super(parent);
            mGlTextureView = glTextureView;
        }

        public GlTextureView getGlTextureView() {
            return mGlTextureView;
        }

    }

    private class TestGlRenderer implements GlRenderer {

        private final int mColor;

        public TestGlRenderer(int color) {
            mColor = color;
        }

        @Override
        public void onCreate() {
            Log.d(TAG, "onCreate");
        }

        @Override
        public void onRelease() {
            Log.d(TAG, "onRelease");
        }

        @Override
        public void onSizeChanged(int width, int height) {
            Log.d(TAG, "onSizeChanged " + width + "x" + height);
        }

        @Override
        public void onRender() {
            Log.d(TAG, "onRender");
            float val = (float) Math.random();
            float inv = 1.0f / 255.0f;
            GLES30.glClearColor(Color.red(mColor) * val * inv, Color.green(mColor) * val * inv, Color.blue(mColor) * val * inv, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        }
    }

}
