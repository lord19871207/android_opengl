package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlProgram {

    private int mProgram;

    public GlProgram(String vertSource, String fragSource, String compSource) throws Exception {
        int vertShader = 0, fragShader = 0, compShader = 0;
        try {
            vertShader = vertSource != null ? compileShader(vertSource, GLES31.GL_VERTEX_SHADER) : 0;
            fragShader = fragSource != null ? compileShader(fragSource, GLES31.GL_FRAGMENT_SHADER) : 0;
            compShader = compSource != null ? compileShader(compSource, GLES31.GL_COMPUTE_SHADER) : 0;

            mProgram = GLES31.glCreateProgram();
            if (mProgram != 0 &&
                    (vertSource == null || vertShader != 0) &&
                    (fragSource == null || fragShader != 0) &&
                    (compSource == null || compShader != 0)) {
                final int[] linkStatus = {GLES31.GL_FALSE};
                if (vertShader != 0) GLES31.glAttachShader(mProgram, vertShader);
                if (fragShader != 0) GLES31.glAttachShader(mProgram, fragShader);
                if (compShader != 0) GLES31.glAttachShader(mProgram, compShader);
                GLES31.glLinkProgram(mProgram);
                GLES31.glGetProgramiv(mProgram, GLES31.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES31.GL_TRUE) {
                    String infoLog = GLES31.glGetProgramInfoLog(mProgram);
                    throw new Exception(infoLog.isEmpty() ? "Link program failed empty." : infoLog);
                }
            }
        } catch (Exception ex) {
            GLES31.glDeleteProgram(mProgram);
            mProgram = 0;
            throw ex;
        } finally {
            GLES31.glDeleteShader(vertShader);
            GLES31.glDeleteShader(fragShader);
            GLES31.glDeleteShader(compShader);
            GlUtils.checkGLErrors();
        }
    }

    public GlProgram useProgram() {
        GLES31.glUseProgram(mProgram);
        GlUtils.checkGLErrors();
        return this;
    }

    public int getAttribLocation(String name) {
        int location = GLES31.glGetAttribLocation(mProgram, name);
        GlUtils.checkGLErrors();
        return location;
    }

    public int getUniformLocation(String name) {
        int location = GLES31.glGetUniformLocation(mProgram, name);
        GlUtils.checkGLErrors();
        return location;
    }

    private int compileShader(String shaderSource, int shaderType) throws Exception {
        int shader = GLES31.glCreateShader(shaderType);
        try {
            if (shader != 0) {
                final int[] compileStatus = {GLES31.GL_FALSE};
                GLES31.glShaderSource(shader, shaderSource);
                GLES31.glCompileShader(shader);
                GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compileStatus, 0);
                if (compileStatus[0] != GLES31.GL_TRUE) {
                    String infoLog = GLES31.glGetShaderInfoLog(shader);
                    throw new Exception(infoLog.isEmpty() ? "compileShader failed empty." : infoLog);
                }
            }
            return shader;
        } catch (Exception ex) {
            GLES31.glDeleteShader(shader);
            throw ex;
        } finally {
            GlUtils.checkGLErrors();
        }
    }

}
