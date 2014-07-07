package fi.harism.opengl.lib.egl;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

public class EglSurface {

    private static final String TAG = "EglSurface";

    private EglCore mEglCore;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

    public EglSurface(EglCore eglCore, Object surface) {
        mEglCore = eglCore;
        mEGLSurface = eglCore.createWindowSurface(surface);
    }

    public void release() {
        mEglCore.releaseSurface(mEGLSurface);
        mEGLSurface = EGL14.EGL_NO_SURFACE;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "finalize called without release");
        }
        super.finalize();
    }

    public void makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface);
    }

    public boolean swapBuffers() {
        boolean result = mEglCore.swapBuffers(mEGLSurface);
        if (!result) {
            Log.d(TAG, "swapBuffers failed");
        }
        return result;
    }

    public void setPresentationTime(long frameTimeNanos) {
        mEglCore.setPresentationTime(mEGLSurface, frameTimeNanos);
    }

    public int getWidth() {
        return mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
    }

    public int getHeight() {
        return mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
    }

}
