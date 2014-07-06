package fi.harism.opengl.lib.util;

import android.util.Log;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.egl.EglSurface;

public class GlRunnable implements Runnable {

    private static final String TAG = "GlRunnable";

    private final int mEglVersion;
    private final int mEglFlags;
    private Object mSurface = null;

    private boolean mRunning = true;

    private final Object mLock = new Object();

    private GlRenderer mRenderer = null;
    private GlRenderer mRendererNew = null;

    public GlRunnable(Object surface, int eglVersion, int eglFlags) {
        mSurface = surface;
        mEglVersion = eglVersion;
        mEglFlags = eglFlags;
    }

    public void setRenderer(GlRenderer renderer) {
        synchronized (mLock) {
            mRendererNew = renderer;
            mLock.notifyAll();
        }
    }

    public void stop() {
        synchronized (mLock) {
            mRunning = false;
            mLock.notifyAll();
        }
    }

    @Override
    public void run() {
        EglCore eglCore = null;
        EglSurface eglSurface = null;
        int surfaceWidth = -1;
        int surfaceHeight = -1;

        try {
            eglCore = new EglCore(mEglVersion, mEglFlags);
            eglSurface = new EglSurface(eglCore, mSurface);

            while (mRunning) {
                while (mRenderer == null && mRendererNew == null) {
                    synchronized (mLock) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException ex) {
                            Log.w(TAG, "mRenderer==null wait interrupted");
                        }
                    }
                }

                eglSurface.makeCurrent();

                if (mRendererNew != null) {
                    synchronized (mLock) {
                        if (mRenderer != null) {
                            mRenderer.onRelease();
                        }
                        mRenderer = mRendererNew;
                        mRendererNew = null;

                        mRenderer.onCreate();
                        surfaceWidth = surfaceHeight = -1;
                    }
                }

                int currentWidth = eglSurface.getWidth();
                int currentHeight = eglSurface.getHeight();
                if (surfaceWidth != currentWidth || surfaceHeight != currentHeight) {
                    surfaceWidth = currentWidth;
                    surfaceHeight = currentHeight;
                    mRenderer.onSizeChanged(surfaceWidth, surfaceHeight);
                }

                mRenderer.onRender();

                eglSurface.swapBuffers();
            }

        } finally {
            if (eglSurface != null) {
                eglSurface.release();
            }
            if (eglCore != null) {
                eglSurface.release();
            }
        }
    }

}
