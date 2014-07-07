package fi.harism.opengl.lib.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import fi.harism.opengl.lib.util.GlRenderThread;
import fi.harism.opengl.lib.util.GlRenderer;

public class GlTextureView extends TextureView {

    private static final String TAG = "GlTextureView";

    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, int width, int height) {
            Log.d(TAG, "surfaceAvailable");
            mGlRenderThread.getGlRenderHandler().postCreateEglSurface(surfaceTexture);
            mGlRenderThread.getGlRenderHandler().postRenderFrame();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            // Do nothing
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            Log.d(TAG, "surfaceDestroyed");
            mGlRenderThread.getGlRenderHandler().postReleaseEglSurfaceAndWait();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            // Do nothing
        }

    };
    private GlRenderThread mGlRenderThread;

    public GlTextureView(Context context) {
        this(context, null);
    }

    public GlTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GlTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mGlRenderThread = new GlRenderThread();
        mGlRenderThread.startAndWaitUntilReady();
        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    public void setEglContext(int eglVersion, int eglFlags) {
        mGlRenderThread.getGlRenderHandler().postCreateEglContext(eglVersion, eglFlags);
    }

    public void setGlRenderer(GlRenderer glRenderer) {
        mGlRenderThread.getGlRenderHandler().postSetRenderer(glRenderer);
    }

    public void renderFrame() {
        mGlRenderThread.getGlRenderHandler().postRenderFrame();
    }

    public void renderFrame(long frameTimeNanos) {
        mGlRenderThread.getGlRenderHandler().postRenderFrame(frameTimeNanos);
    }

    public void onDestroy() {
        mGlRenderThread.stopAndWaitUntilReady();
        mGlRenderThread = null;
    }

}
