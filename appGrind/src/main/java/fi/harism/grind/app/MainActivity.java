package fi.harism.grind.app;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.gl.GlFramebuffer;
import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlRenderbuffer;
import fi.harism.opengl.lib.gl.GlSampler;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.gl.GlUtils;
import fi.harism.opengl.lib.model.GlCamera;
import fi.harism.opengl.lib.model.GlLoaderObj;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.view.GlTextureView;
import fi.harism.utils.lib.MersenneTwisterFast;

public class MainActivity extends Activity {

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            mGlTextureView.renderFrame(frameTimeNanos);
            mChoreographer.postFrameCallback(this);
        }
    };

    private MediaPlayer mMediaPlayer;
    private GlTextureView mGlTextureView;
    private MainRenderer mRenderer;
    private Choreographer mChoreographer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChoreographer = Choreographer.getInstance();

        mRenderer = new MainRenderer();
        mGlTextureView = new GlTextureView(this);
        mGlTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        mGlTextureView.setGlRenderer(mRenderer);
        mGlTextureView.setAlpha(0f);
        setContentView(mGlTextureView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }
        mMediaPlayer = null;

        mChoreographer.removeFrameCallback(mFrameCallback);

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGlTextureView.onDestroy();
        mGlTextureView = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void startDemo() {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getAssets().openFd("music/music.mp3").getFileDescriptor());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mGlTextureView.animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            finish();
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });
                }
            });
            mGlTextureView.animate().alpha(1f).setDuration(6395);
            mChoreographer.postFrameCallback(mFrameCallback);
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "IOException..", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private class MainRenderer implements GlRenderer {

        private final Point mSurfaceSize = new Point();

        private GlFramebuffer mFramebuffer;
        private GlTexture mTexture;
        private GlTexture mTextureRand;
        private GlTexture mTextureSky;
        private GlSampler mSampler;
        private GlProgram mProgramOut;
        private GlProgram mProgramDots;
        private GlProgram mProgramCube;
        private ByteBuffer mBufferQuad;
        private FloatBuffer mBufferDots;
        private ByteBuffer mBufferCubeVertices;
        private ByteBuffer mBufferCubeNormals;
        private GlRenderbuffer mRenderbufferDepth;

        private GlCamera mCamera;
        private MersenneTwisterFast mMersenneTwisterFast;

        private GlLoaderObj mLoaderObjHarism;
        private GlLoaderObj mLoaderObjUntitled;

        @Override
        public void onSurfaceCreated() {
            mCamera = new GlCamera();
            mMersenneTwisterFast = new MersenneTwisterFast(8473);

            mSampler = new GlSampler();
            mSampler.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            mSampler.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            mSampler.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            mSampler.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

            mFramebuffer = new GlFramebuffer();
            mTexture = new GlTexture();

            mTextureRand = new GlTexture().bind(GLES30.GL_TEXTURE_2D);
            final FloatBuffer bufferRand = ByteBuffer.allocateDirect(4 * 256 * 256).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int index = 0; index < 256 * 256; ++index) {
                bufferRand.put(mMersenneTwisterFast.nextFloat(true, true));
            }
            bufferRand.position(0);
            mTextureRand.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R32F, 256, 256, 0, GLES30.GL_RED, GLES30.GL_FLOAT, bufferRand);
            mTextureRand.unbind(GLES30.GL_TEXTURE_2D);

            mTextureSky = new GlTexture().bind(GLES30.GL_TEXTURE_CUBE_MAP);
            final FloatBuffer bufferSkyColors = ByteBuffer.allocateDirect(4 * 4 * 256 * 256).order(ByteOrder.nativeOrder()).asFloatBuffer();
            final int[] SKY_MAPS = {GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
                    GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                    GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
            };
            final float[][] SKY_DATA = {{0.65f, 0.75f, 0.90f, 1f}, {0.57f, 0.90f, 0.73f, 1f},
                    {0.86f, 0.58f, 0.74f, 1f}, {0.83f, 0.63f, 0.69f, 1f},
                    {0.58f, 0.85f, 0.71f, 1f}, {0.65f, 0.92f, 0.68f, 1f}};
            final float[] SKY_BG = {0.65f, 0.75f, 0.90f, 1f};
            for (int index = 0; index < SKY_MAPS.length; ++index) {
                bufferSkyColors.position(0);
                for (int x = 0; x < 256; ++x) {
                    for (int y = 0; y < 256; ++y) {
                        float xf = x / 255f;
                        float yf = y / 255f;
                        xf = xf * 2 - 1;
                        yf = yf * 2 - 1;
                        final float[] c = Math.sqrt(xf * xf + yf * yf) < 0.65 ? SKY_DATA[index] : SKY_BG;
                        bufferSkyColors.put(c);
                    }
                }
                bufferSkyColors.position(0);
                mTextureSky.texImage2D(SKY_MAPS[index], 0, GLES30.GL_RGBA16F, 256, 256, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, bufferSkyColors);
            }
            mTextureSky.unbind(GLES30.GL_TEXTURE_CUBE_MAP);

            mRenderbufferDepth = new GlRenderbuffer();

            final byte[] VERTICES_QUAD = {-1, 1, -1, -1, 1, 1, 1, -1};
            mBufferQuad = ByteBuffer.allocateDirect(VERTICES_QUAD.length).order(ByteOrder.nativeOrder());
            mBufferQuad.put(VERTICES_QUAD).position(0);

            mBufferDots = ByteBuffer.allocateDirect(4 * 3 * 1000).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int i = 0; i < 3 * 1000; ++i) {
                mBufferDots.put(mMersenneTwisterFast.nextFloat(true, true) * 100 - 50);
            }
            mBufferDots.position(0);

            final byte VERTICES_CUBE[][] = {{-1, 1, 1}, {-1, -1, 1}, {1, 1, 1},
                    {1, -1, 1}, {-1, 1, -1}, {-1, -1, -1}, {1, 1, -1},
                    {1, -1, -1}};
            final byte NORMALS_CUBE[][] = {{0, 0, 1}, {0, 0, -1}, {-1, 0, 0},
                    {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
            final int INDICES_CUBE[][][] = {{{0, 1, 2, 1, 3, 2}, {0}},
                    {{5, 4, 7, 4, 6, 7}, {1}},
                    {{0, 4, 1, 4, 5, 1}, {2}},
                    {{3, 7, 2, 7, 6, 2}, {3}},
                    {{2, 6, 0, 6, 4, 0}, {4}},
                    {{1, 5, 3, 5, 7, 3}, {5}}};
            mBufferCubeVertices = ByteBuffer.allocateDirect(3 * 6 * 6);
            mBufferCubeNormals = ByteBuffer.allocateDirect(3 * 6 * 6);
            for (int[][] indices : INDICES_CUBE) {
                for (int i = 0; i < 6; ++i) {
                    mBufferCubeVertices.put(VERTICES_CUBE[indices[0][i]]);
                    mBufferCubeNormals.put(NORMALS_CUBE[indices[1][0]]);
                }
            }
            mBufferCubeVertices.position(0);
            mBufferCubeNormals.position(0);

            try {
                final String OUT_VS = GlUtils.loadString(MainActivity.this, "shaders/out_vs.txt");
                final String OUT_FS = GlUtils.loadString(MainActivity.this, "shaders/out_fs.txt");
                mProgramOut = new GlProgram(OUT_VS, OUT_FS, null).useProgram();
                GLES30.glUniform1i(mProgramOut.getUniformLocation("sTexture"), 0);
                GLES30.glUniform1i(mProgramOut.getUniformLocation("sTextureRand"), 1);

                final String DOT_VS = GlUtils.loadString(MainActivity.this, "shaders/dots_vs.txt");
                final String DOT_FS = GlUtils.loadString(MainActivity.this, "shaders/dots_fs.txt");
                mProgramDots = new GlProgram(DOT_VS, DOT_FS, null);

                final String CUBE_VS = GlUtils.loadString(MainActivity.this, "shaders/cube_vs.txt");
                final String CUBE_FS = GlUtils.loadString(MainActivity.this, "shaders/cube_fs.txt");
                mProgramCube = new GlProgram(CUBE_VS, CUBE_FS, null).useProgram();
                GLES30.glUniform1i(mProgramCube.getUniformLocation("sTextureSky"), 0);

                mLoaderObjHarism = new GlLoaderObj(MainActivity.this, "models/harism.obj");
                mLoaderObjUntitled = new GlLoaderObj(MainActivity.this, "models/untitled.obj");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            startDemo();
                        }
                    }
                });
            } catch (final Exception ex) {
                ex.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            mSurfaceSize.x = width;
            mSurfaceSize.y = height;

            mCamera.setPerspectiveM(width, height, 60f, 1f, 100f);

            mTexture.bind(GLES30.GL_TEXTURE_2D);
            mTexture.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, height, width, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
            mTexture.unbind(GLES30.GL_TEXTURE_2D);

            mRenderbufferDepth.bind();
            mRenderbufferDepth.storage(GLES30.GL_DEPTH_COMPONENT32F, height, width);
            mRenderbufferDepth.unbind();

            mFramebuffer.bind(GLES30.GL_DRAW_FRAMEBUFFER);
            mFramebuffer.renderbuffer(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, mRenderbufferDepth.getRenderbuffer());
            mFramebuffer.texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mTexture.getTexture(), 0);
            final int[] ATTACHMENTS = {GLES30.GL_COLOR_ATTACHMENT0};
            GLES30.glDrawBuffers(1, ATTACHMENTS, 0);
            mFramebuffer.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        }

        @Override
        public void onRenderFrame() {
            mFramebuffer.bind(GLES30.GL_DRAW_FRAMEBUFFER);
            GLES30.glViewport(0, 0, mSurfaceSize.y, mSurfaceSize.x);
            GLES30.glClearColor(0.75f, 0.85f, 1.00f, 1.00f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

            //float z = SystemClock.uptimeMillis() % 10000 / 500f - 10f;
            //mCamera.setLookAtM(10, 0, z, 0, 0, 0, 0, 1, 0);
            //mCamera.setLookAtM(0, -10, 0.001f, 0, 0, 0, 0, 1, 0);

            double angle = SystemClock.uptimeMillis() % 10000 / 5000.0 * Math.PI;
            float rx = (float) (Math.sin(angle) * 5);
            float rz = (float) (Math.cos(angle) * 5);
            mCamera.setLookAtM(rx, 5, rz, 0, 0, 0, 0, 1, 0);


            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            //renderCubeTest();
            renderObjTest(mLoaderObjUntitled);
            renderDotsTest();
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);

            mFramebuffer.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

            mProgramOut.useProgram();
            GLES30.glViewport(0, 0, mSurfaceSize.x, mSurfaceSize.y);
            GLES30.glUniform2f(mProgramOut.getUniformLocation("uTextureRandScale"), mSurfaceSize.x / 1024f, mSurfaceSize.y / 1024f);
            GLES30.glUniform2f(mProgramOut.getUniformLocation("uTextureRandDiff"), (float) (Math.random() * 2), (float) (Math.random() * 2));
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, mBufferQuad);
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            mTexture.bind(GLES30.GL_TEXTURE_2D);
            mSampler.bind(0);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            mTextureRand.bind(GLES30.GL_TEXTURE_2D);
            mSampler.bind(1);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(0);
            mTexture.unbind(GLES30.GL_TEXTURE_2D);
            mTextureRand.unbind(GLES30.GL_TEXTURE_2D);
        }

        @Override
        public void onSurfaceReleased() {

        }

        private void renderObjTest(GlLoaderObj loaderObj) {
            mProgramCube.useProgram();
            final float angle = 0; //SystemClock.uptimeMillis() % 3600 / 10f;
            final float[] modelViewProjM = new float[16];
            Matrix.setRotateM(modelViewProjM, 0, angle, 1, 0, 0);
            GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uViewM"), 1, false, mCamera.getLookAtM(), 0);
            Matrix.multiplyMM(modelViewProjM, 0, mCamera.getLookAtM(), 0, modelViewProjM, 0);
            GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uModelViewM"), 1, false, modelViewProjM, 0);
            Matrix.multiplyMM(modelViewProjM, 0, mCamera.getPerspectiveM(), 0, modelViewProjM, 0);
            GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uModelViewProjM"), 1, false, modelViewProjM, 0);
            GLES30.glUniform3fv(mProgramCube.getUniformLocation("uEyePositionW"), 1, mCamera.getLookAtV(), 0);
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, loaderObj.getBufferVertices());
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, loaderObj.getBufferNormals());
            GLES30.glEnableVertexAttribArray(1);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            mTextureSky.bind(GLES30.GL_TEXTURE_CUBE_MAP);
            mSampler.bind(0);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, loaderObj.getVertexCount());
            GLES30.glDisableVertexAttribArray(0);
            GLES30.glDisableVertexAttribArray(1);
            mTextureSky.unbind(GLES30.GL_TEXTURE_CUBE_MAP);
        }

        private void renderCubeTest() {
            mProgramCube.useProgram();
            final float angle = SystemClock.uptimeMillis() % 3600 / 10f;
            final float[] modelViewProjM = new float[16];
            Matrix.setRotateM(modelViewProjM, 0, angle, 1, 0, 0);
            GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uViewM"), 1, false, mCamera.getLookAtM(), 0);
            Matrix.multiplyMM(modelViewProjM, 0, mCamera.getLookAtM(), 0, modelViewProjM, 0);
            GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uModelViewM"), 1, false, modelViewProjM, 0);
            Matrix.multiplyMM(modelViewProjM, 0, mCamera.getPerspectiveM(), 0, modelViewProjM, 0);
            GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uModelViewProjM"), 1, false, modelViewProjM, 0);
            GLES30.glUniform3fv(mProgramCube.getUniformLocation("uEyePositionW"), 1, mCamera.getLookAtV(), 0);
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_BYTE, false, 0, mBufferCubeVertices);
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_BYTE, false, 0, mBufferCubeNormals);
            GLES30.glEnableVertexAttribArray(1);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            mTextureSky.bind(GLES30.GL_TEXTURE_CUBE_MAP);
            mSampler.bind(0);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
            GLES30.glDisableVertexAttribArray(0);
            GLES30.glDisableVertexAttribArray(1);
            mTextureSky.unbind(GLES30.GL_TEXTURE_CUBE_MAP);
        }

        private void renderDotsTest() {
            mProgramDots.useProgram();
            GLES30.glUniformMatrix4fv(mProgramDots.getUniformLocation("uProjM"), 1, false, mCamera.getPerspectiveM(), 0);
            GLES30.glUniformMatrix4fv(mProgramDots.getUniformLocation("uViewM"), 1, false, mCamera.getLookAtM(), 0);
            GLES30.glDepthMask(false);
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, mBufferDots);
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribDivisor(0, 1);
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_BYTE, false, 0, mBufferQuad);
            GLES30.glEnableVertexAttribArray(1);
            GLES30.glVertexAttribDivisor(1, 0);
            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, 1000);
            GLES30.glDisableVertexAttribArray(0);
            GLES30.glDisableVertexAttribArray(1);
            GLES30.glVertexAttribDivisor(0, 0);
            GLES30.glDisable(GLES30.GL_BLEND);
            GLES30.glDepthMask(true);
        }

    }

}
