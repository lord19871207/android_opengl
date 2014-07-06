package fi.harism.opengl.lib.util;

import android.os.Looper;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.egl.EglSurface;

public class GlRenderThread extends Thread {

    private static final String TAG = "GlRenderThread";

    private static int MSG_DRAW_FRAME = 1;

    private final int mEglVersion;
    private final int mEglFlags;
    private final Object mSurface;

    private boolean mStartReady = false;
    private final Object mStartLock = new Object();

    private GlRenderer mRenderer = null;
    private GlRenderer mRendererNew = null;

    private GlRenderHandler mHandler;

    public GlRenderThread(Object surface, int eglVersion, int eglFlags) {
        mSurface = surface;
        mEglVersion = eglVersion;
        mEglFlags = eglFlags;
    }

    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mStartReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException ex) {
                }
            }

        }
    }

    @Override
    public void run() {
        EglCore eglCore = null;
        EglSurface eglSurface = null;
        try {
            eglCore = new EglCore(mEglVersion, mEglFlags);
            eglSurface = new EglSurface(eglCore, mSurface);

            Looper.prepare();
            mHandler = new GlRenderHandler();
            synchronized (mStartLock) {
                mStartReady = true;
                mStartLock.notifyAll();
            }
            Looper.loop();

        } finally {
            //if ()
        }

    }

}
