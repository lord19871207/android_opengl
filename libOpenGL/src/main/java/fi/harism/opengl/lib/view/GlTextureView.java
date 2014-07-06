package fi.harism.opengl.lib.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.util.GlRunnable;

public class GlTextureView extends TextureView {

    private static final String TAG = "GlTextureView";

    private int mEglVersion = EglCore.VERSION_GLES2;
    private int mEglFlags = 0;

    private GlRunnable mGlRunnable = null;
    private GlRenderer mGlRenderer = null;

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
        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    public void setEglVersion(int eglVersion) {
        mEglVersion = eglVersion;
    }

    public void setEglFlags(int eglFlags) {
        mEglFlags = eglFlags;
    }

    public void setGlRenderer(GlRenderer glRenderer) {
        mGlRenderer = glRenderer;
        if (mGlRunnable != null) {
            mGlRunnable.setRenderer(glRenderer);
        }
    }

    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, int width, int height) {
            mGlRunnable = new GlRunnable(surfaceTexture, mEglVersion, mEglFlags);
            mGlRunnable.setRenderer(mGlRenderer);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGlRunnable.run();
                    surfaceTexture.release();
                }
            }).start();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            // Do nothing
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            mGlRunnable.stop();
            mGlRunnable = null;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            // Do nothing
        }

    };

}
