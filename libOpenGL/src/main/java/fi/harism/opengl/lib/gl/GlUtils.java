package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;
import android.util.Log;

public class GlUtils {

    public static void checkGLErrors() {
        int error;
        while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
            String stackTrace = Thread.currentThread().getStackTrace()[3].toString();
            Log.d("OpenGL", stackTrace + "  error=0x" + Integer.toHexString(error));
        }
    }

}
