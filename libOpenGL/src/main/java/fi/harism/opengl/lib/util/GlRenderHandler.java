package fi.harism.opengl.lib.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.egl.EglSurface;

public class GlRenderHandler extends Handler {

    private static final String TAG = "GlRenderHandler";

    public static final int MSG_RELEASE = 1;
    public static final int MSG_CREATE_EGLCONTEXT = 2;
    public static final int MSG_CREATE_EGLSURFACE = 3;
    public static final int MSG_RELEASE_EGLCONTEXT = 4;
    public static final int MSG_RELEASE_EGLSURFACE = 5;
    public static final int MSG_SET_RENDERER = 6;
    public static final int MSG_RENDER_FRAME = 7;

    private EglCore mEglCore = null;
    private EglSurface mEglSurface = null;
    private GlRenderer mGlRenderer = null;

    private final Object mLock = new Object();

    public GlRenderHandler() {
    }

    public void postRelease() {
        removeMessages(MSG_RELEASE);
        sendMessage(obtainMessage(MSG_RELEASE));
    }

    public void postCreateEglContext(int eglVersion, int eglFlags) {
        removeMessages(MSG_CREATE_EGLCONTEXT);
        sendMessage(obtainMessage(MSG_CREATE_EGLCONTEXT, eglVersion, eglFlags));
    }

    public void postCreateEglSurface(Object surface) {
        removeMessages(MSG_CREATE_EGLSURFACE);
        sendMessage(obtainMessage(MSG_CREATE_EGLSURFACE, surface));
    }

    public void postReleaseEglContext() {
        removeMessages(MSG_RELEASE_EGLCONTEXT);
        sendMessage(obtainMessage(MSG_RELEASE_EGLCONTEXT));
    }

    public void postReleaseEglSurface() {
        removeMessages(MSG_RELEASE_EGLSURFACE);
        sendMessage(obtainMessage(MSG_RELEASE_EGLSURFACE));
    }

    public void postReleaseEglSurfaceAndWait() {
        synchronized (mLock) {
            postReleaseEglSurface();
            while (mEglSurface != null) {
                try {
                    mLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void postSetRenderer(GlRenderer glRenderer) {
        removeMessages(MSG_SET_RENDERER);
        sendMessage(obtainMessage(MSG_SET_RENDERER, glRenderer));
    }

    public void postRenderFrame() {
        removeMessages(MSG_RENDER_FRAME);
        sendMessage(obtainMessage(MSG_RENDER_FRAME, 0, 0));
    }

    public void postRenderFrame(long time) {
        removeMessages(MSG_RENDER_FRAME);
        sendMessage(obtainMessage(MSG_RENDER_FRAME, (int)(time >> 32), (int)time));
    }

    private void onRelease() {
        onReleaseEglSurface();
        onReleaseEglContext();
        Looper.myLooper().quitSafely();
    }

    private void onCreateEglContext(int eglVersion, int eglFlags) {
        onReleaseEglSurface();
        onReleaseEglContext();
        mEglCore = new EglCore(eglVersion, eglFlags);

        if (mGlRenderer != null) {
            mGlRenderer.onContextCreated();
        }
    }

    private void onCreateEglSurface(Object surface) {
        onReleaseEglSurface();
        mEglSurface = new EglSurface(mEglCore, surface);
        mEglSurface.makeCurrent();

        if (mGlRenderer != null) {
            mGlRenderer.onSurfaceChanged(mEglSurface.getWidth(), mEglSurface.getHeight());
        }
    }

    private void onReleaseEglContext() {
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    private void onReleaseEglSurface() {
        if (mEglSurface != null) {
            synchronized (mLock) {
                mEglSurface.release();
                mEglSurface = null;
                mLock.notifyAll();
            }
        }
    }

    private void onSetRenderer(GlRenderer glRenderer) {
        if (mGlRenderer != null) {
            mGlRenderer.onRelease();
        }
        mGlRenderer = glRenderer;

        if (mEglCore != null) {
            mGlRenderer.onContextCreated();
        }
        if (mEglSurface != null) {
            mGlRenderer.onSurfaceChanged(mEglSurface.getWidth(), mEglSurface.getHeight());
        }
    }

    private void onRenderFrame(long frameTimeNanos) {
        if (mGlRenderer != null) {
            mGlRenderer.onRenderFrame();
        }
        if (mEglSurface != null) {
            mEglSurface.setPresentationTime(frameTimeNanos);
            mEglSurface.swapBuffers();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_RELEASE:
                onRelease();
                break;
            case MSG_CREATE_EGLCONTEXT:
                onCreateEglContext(msg.arg1, msg.arg2);
                break;
            case MSG_CREATE_EGLSURFACE:
                onCreateEglSurface(msg.obj);
                break;
            case MSG_RELEASE_EGLCONTEXT:
                onReleaseEglContext();
                break;
            case MSG_RELEASE_EGLSURFACE:
                onReleaseEglSurface();
                break;
            case MSG_SET_RENDERER:
                onSetRenderer((GlRenderer) msg.obj);
                break;
            case MSG_RENDER_FRAME:
                onRenderFrame((long)msg.arg1 << 32 | msg.arg2 & 0xFFFFFFFFL);
                break;
            default:
                Log.d(TAG, "Unknown msg type = " + msg.what);
        }
    }

}
