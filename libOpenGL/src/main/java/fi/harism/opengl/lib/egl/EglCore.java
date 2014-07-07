package fi.harism.opengl.lib.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;

public class EglCore {

    private static final String TAG = "EglCore";

    public static final int FLAG_RECORDABLE = 0x01;

    public static final int VERSION_GLES2 = 0x00020000;
    public static final int VERSION_GLES3 = 0x00030000;
    public static final int VERSION_GLES31 = 0x00030001;

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;

    public EglCore(int version, int flags) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        final int eglVersion[] = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, eglVersion, 0, eglVersion, 1)) {
            release();
            throw new RuntimeException("eglInitialize failed");
        }

        EGLConfig config = getConfig(version, flags);
        if (config != null) {
            final int attribs[] = {
                    EGLExt.EGL_CONTEXT_MAJOR_VERSION_KHR, version >> 16,
                    EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR, version & 0xFFFF,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, EGL14.EGL_NO_CONTEXT, attribs, 0);
            if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                mEGLContext = context;
                mEGLConfig = config;
            } else {
                release();
                throw new RuntimeException("eglCreateContext failed");
            }
        } else {
            release();
            throw new RuntimeException("getConfig failed");
        }
    }

    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (mEGLContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            }
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY || mEGLContext != EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "finalize called without release");
        }
        super.finalize();
    }

    public void releaseSurface(EGLSurface eglSurface) {
        EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
    }

    public EGLSurface createWindowSurface(Object surface) {
        int[] surfaceAttribs = {EGL14.EGL_NONE};
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface,
                surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }

    public void makeCurrent(EGLSurface eglSurface) {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }

    public void setPresentationTime(EGLSurface eglSurface, long frameTimeNanos) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, frameTimeNanos);
    }

    public int querySurface(EGLSurface eglSurface, int key) {
        final int value[] = new int[1];
        EGL14.eglQuerySurface(mEGLDisplay, eglSurface, key, value, 0);
        return value[0];
    }

    private EGLConfig getConfig(int version, int flags) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >> 16 >= 3) {
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / " + Integer.toHexString(version) + " EGLConfig");
            return null;
        }
        return configs[0];
    }

    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

}
