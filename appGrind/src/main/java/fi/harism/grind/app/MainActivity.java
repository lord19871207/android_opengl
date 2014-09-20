package fi.harism.grind.app;

import android.animation.Animator;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.opengl.GLES30;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
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
import fi.harism.opengl.lib.model.GlCameraAnimator;
import fi.harism.opengl.lib.model.GlObject;
import fi.harism.opengl.lib.model.GlObjectLoader;
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
    private Choreographer mChoreographer;
    private View mProgressLayout;
    private ProgressBar mProgressBar;
    private GlTextureView mGlTextureView;
    private MainRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChoreographer = Choreographer.getInstance();

        setContentView(R.layout.activity_main);

        mProgressLayout = findViewById(R.id.layout_progress);
        mProgressLayout.setAlpha(0f);
        mProgressLayout.animate().setStartDelay(500).setDuration(500).alpha(1f);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setMax(10);
        mProgressBar.setProgress(0);

        mRenderer = new MainRenderer();
        mGlTextureView = (GlTextureView) findViewById(R.id.texture_view);
        mGlTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        mGlTextureView.setGlRenderer(mRenderer);
        mGlTextureView.setAlpha(0f);

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
            AssetFileDescriptor desc = getAssets().openFd("music/music.mp3");
            mMediaPlayer.setDataSource(desc.getFileDescriptor(), desc.getStartOffset(), desc.getLength());
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
            mGlTextureView.animate().alpha(1f).setStartDelay(300).setDuration(2000);

            mProgressLayout.animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    mProgressLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            mChoreographer.postFrameCallback(mFrameCallback);
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Unable to start audio playback", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private class MainRenderer implements GlRenderer {

        private final Point mSurfaceSize = new Point();

        private GlFramebuffer mFramebuffer;
        private GlTexture mTexture;
        private GlTexture mTextureRand;
        private GlSampler mSampler;
        private ByteBuffer mBufferQuad;
        private GlRenderbuffer mRenderbufferDepth;

        private GlCamera mCamera;
        private GlCameraAnimator mCameraAnimator;
        private MersenneTwisterFast mMersenneTwisterFast;

        private GlObject mObjectAnd;
        private GlObject mObjectBy;
        private GlObject mObjectCoding;
        private GlObject mObjectDoctrnal;
        private GlObject mObjectGrind;
        private GlObject mObjectHarism;
        private GlObject mObjectMusic;
        private GlObject mObjectScottXylo;

        private OutRenderer mOutRenderer;
        private SceneRenderer mSceneRenderer;

        @Override
        public void onSurfaceCreated() {
            mCamera = new GlCamera();
            mCameraAnimator = new GlCameraAnimator();
            mCameraAnimator.addPosition(0, 0, 10, 0, 0, 0, -10);
            mCameraAnimator.addPosition(0, 0, 20, 0, 0, 0, 0);
            mCameraAnimator.addPosition(0, 0, 10, 0, 0, 0, 10);
            mCameraAnimator.addPosition(0, 0, 20, 0, 0, 0, 20);
            mCameraAnimator.addPosition(0, 0, 10, -5, 0, 0, 160);
            mCameraAnimator.addPosition(0, 0, 20, 0, 0, 0, 300);
            mCameraAnimator.prepare();
            mMersenneTwisterFast = new MersenneTwisterFast(8473);

            mSampler = new GlSampler();
            mSampler.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            mSampler.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            mSampler.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            mSampler.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

            mFramebuffer = new GlFramebuffer();
            mTexture = new GlTexture();

            mTextureRand = new GlTexture().bind(GLES30.GL_TEXTURE_2D);
            final FloatBuffer bufferRand = ByteBuffer.allocateDirect(4 * 4 * 256 * 256).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int index = 0; index < 4 * 256 * 256; ++index) {
                bufferRand.put(mMersenneTwisterFast.nextFloat(true, true));
            }
            bufferRand.position(0);
            mTextureRand.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, 256, 256, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, bufferRand);
            mTextureRand.unbind(GLES30.GL_TEXTURE_2D);

            mRenderbufferDepth = new GlRenderbuffer();

            final byte[] VERTICES_QUAD = {-1, 1, -1, -1, 1, 1, 1, -1};
            mBufferQuad = ByteBuffer.allocateDirect(VERTICES_QUAD.length).order(ByteOrder.nativeOrder());
            mBufferQuad.put(VERTICES_QUAD).position(0);

            try {
                mOutRenderer = new OutRenderer(MainActivity.this, mBufferQuad, mTexture, mTextureRand);
                mSceneRenderer = new SceneRenderer(MainActivity.this, mBufferQuad, mCamera, mFramebuffer);

                setProgress(9, 1);
                mObjectAnd = GlObjectLoader.loadDat(MainActivity.this, "models/and.dat");
                setProgress(9, 2);
                mObjectBy = GlObjectLoader.loadDat(MainActivity.this, "models/by.dat");
                setProgress(9, 3);
                mObjectCoding = GlObjectLoader.loadDat(MainActivity.this, "models/coding.dat");
                setProgress(9, 4);
                mObjectDoctrnal = GlObjectLoader.loadDat(MainActivity.this, "models/doctrnal.dat");
                setProgress(9, 5);
                mObjectGrind = GlObjectLoader.loadDat(MainActivity.this, "models/grind.dat");
                setProgress(9, 6);
                mObjectHarism = GlObjectLoader.loadDat(MainActivity.this, "models/harism.dat");
                setProgress(9, 7);
                mObjectMusic = GlObjectLoader.loadDat(MainActivity.this, "models/music.dat");
                setProgress(9, 8);
                mObjectScottXylo = GlObjectLoader.loadDat(MainActivity.this, "models/scott_xylo.dat");
                setProgress(9, 9);

                mSceneRenderer.setObject(mObjectGrind);

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
            mTexture.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
            mTexture.unbind(GLES30.GL_TEXTURE_2D);

            mRenderbufferDepth.bind();
            mRenderbufferDepth.storage(GLES30.GL_DEPTH_COMPONENT32F, width, height);
            mRenderbufferDepth.unbind();

            mFramebuffer.bind(GLES30.GL_DRAW_FRAMEBUFFER);
            mFramebuffer.renderbuffer(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, mRenderbufferDepth.getRenderbuffer());
            mFramebuffer.texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mTexture.getTexture(), 0);
            final int[] ATTACHMENTS = {GLES30.GL_COLOR_ATTACHMENT0};
            GLES30.glDrawBuffers(1, ATTACHMENTS, 0);
            mFramebuffer.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

            mSceneRenderer.onSurfaceChanged(width, height);
            mOutRenderer.onSurfaceChanged(width, height);
        }

        @Override
        public void onRenderFrame() {
            double angle = SystemClock.uptimeMillis() % 10000 / 5000.0 * Math.PI;
            float rx = (float) (Math.sin(angle) * 5);
            float rz = (float) (Math.cos(angle) * 5);
            mCamera.setLookAtM(rx, 0, 5, rx, 0, 0, 0, 1, 0);

            mSceneRenderer.onRenderFrame();
            mOutRenderer.onRenderFrame();
        }

        @Override
        public void onSurfaceReleased() {
            mSceneRenderer.onSurfaceReleased();
            mOutRenderer.onSurfaceReleased();
        }

        private void setProgress(final int max, final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setMax(max);
                    mProgressBar.setProgress(progress);
                }
            });
        }

    }

}
