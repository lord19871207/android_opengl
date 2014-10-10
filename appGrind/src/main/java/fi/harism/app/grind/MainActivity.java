package fi.harism.app.grind;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.opengl.GLES30;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlRenderbuffer;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.model.GlCamera;
import fi.harism.lib.opengl.model.GlCameraAnimator;
import fi.harism.lib.opengl.model.GlObject;
import fi.harism.lib.opengl.model.GlObjectData;
import fi.harism.lib.opengl.util.GlRenderer;
import fi.harism.lib.opengl.view.GlTextureView;
import fi.harism.lib.utils.MersenneTwisterFast;

public class MainActivity extends Activity {

    public final static String EXTRA_RESOLUTION_WIDTH = "EXTRA_RESOLUTION_WIDTH";
    public final static String EXTRA_RESOLUTION_HEIGHT = "EXTRA_RESOLUTION_HEIGHT";
    public final static String EXTRA_SHOW_FPS = "EXTRA_SHOW_FPS";

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            glTextureView.renderFrame(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    private final Runnable fpsRunnable = new Runnable() {
        @Override
        public void run() {
            textViewFps.setText("FPS " + Math.round(fps));
        }
    };

    private MediaPlayer mediaPlayer;
    private Choreographer choreographer;
    private View progressLayout;
    private ProgressBar progressBar;
    private TextView textViewFps;
    private GlTextureView glTextureView;
    private int resolutionWidth;
    private int resolutionHeight;
    private float fps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        choreographer = Choreographer.getInstance();

        setContentView(R.layout.activity_main);

        progressLayout = findViewById(R.id.layout_progress);
        progressLayout.setAlpha(0f);
        progressLayout.animate().setStartDelay(500).setDuration(500).alpha(1f);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setMax(10);
        progressBar.setProgress(0);

        textViewFps = (TextView) findViewById(R.id.textview_fps);
        textViewFps.setAlpha(0f);

        glTextureView = (GlTextureView) findViewById(R.id.texture_view);
        glTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        glTextureView.setGlRenderer(new MainRenderer());
        glTextureView.setAlpha(0f);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        if (!intent.getBooleanExtra(EXTRA_SHOW_FPS, true)) {
            textViewFps.setVisibility(View.GONE);
        }
        resolutionWidth = intent.getIntExtra(EXTRA_RESOLUTION_WIDTH, 1920);
        resolutionHeight = intent.getIntExtra(EXTRA_RESOLUTION_HEIGHT, 1080);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDemo();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDemo();
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
            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor desc = getAssets().openFd("music/music.mp3");
            mediaPlayer.setDataSource(desc.getFileDescriptor(), desc.getStartOffset(), desc.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    glTextureView.animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
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
            glTextureView.animate().alpha(1f).setStartDelay(500).setDuration(2000);
            textViewFps.animate().alpha(1f).setStartDelay(500).setDuration(2000);

            progressLayout.animate().alpha(0f).setDuration(400).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    progressLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            choreographer.postFrameCallback(frameCallback);
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Unable to start audio playback", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void stopDemo() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (glTextureView != null) {
            glTextureView.onDestroy();
            glTextureView = null;
        }

        choreographer.removeFrameCallback(frameCallback);
    }

    private class MainRenderer implements GlRenderer {
        private GlFramebuffer glFramebufferOut;
        private GlTexture glTextureOut;
        private GlTexture glTextureHalf1;
        private GlTexture glTextureHalf2;
        private GlTexture glTextureHalf3;
        private GlTexture glTextureRand;
        private ByteBuffer mBufferQuad;
        private GlRenderbuffer glRenderbufferDepth;

        private GlCamera glCamera;
        private GlCameraAnimator glCameraAnimator;

        private GlObject glObjectAnd;
        private GlObject glObjectBy;
        private GlObject glObjectCoding;
        private GlObject glObjectDoctrnal;
        private GlObject glObjectGrind;
        private GlObject glObjectHarism;
        private GlObject glObjectMusic;
        private GlObject glObjectScottXylo;

        private RendererScene rendererScene;
        private RendererDof rendererDof;
        private RendererOut rendererOut;

        private float fpsSum;
        private int fpsCount;
        private long timeStart;
        private long timePrev;

        @Override
        public void onSurfaceCreated() {
            glCamera = new GlCamera();
            glCameraAnimator = new GlCameraAnimator();
            glCameraAnimator.addPosition(0, 0, 10, 0, 0, 0, -10);
            glCameraAnimator.addPosition(0, 0, 20, 0, 0, 0, 0);
            glCameraAnimator.addPosition(0, 0, 10, 0, 0, 0, 10);
            glCameraAnimator.addPosition(0, 0, 20, 0, 0, 0, 20);
            glCameraAnimator.addPosition(0, 0, 10, -5, 0, 0, 160);
            glCameraAnimator.addPosition(0, 0, 20, 0, 0, 0, 300);
            glCameraAnimator.prepare();

            glFramebufferOut = new GlFramebuffer();
            glTextureOut = new GlTexture();
            glTextureHalf1 = new GlTexture();
            glTextureHalf2 = new GlTexture();
            glTextureHalf3 = new GlTexture();

            MersenneTwisterFast rand = new MersenneTwisterFast(8347);
            glTextureRand = new GlTexture().bind(GLES30.GL_TEXTURE_2D);
            final FloatBuffer bufferRand = ByteBuffer.allocateDirect(4 * 4 * 256 * 256).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int index = 0; index < 4 * 256 * 256; ++index) {
                bufferRand.put(rand.nextFloat(true, true));
            }
            bufferRand.position(0);
            glTextureRand.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, 256, 256, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, bufferRand);
            glTextureRand.unbind(GLES30.GL_TEXTURE_2D);

            glRenderbufferDepth = new GlRenderbuffer();

            final byte[] VERTICES_QUAD = {-1, 1, -1, -1, 1, 1, 1, -1};
            mBufferQuad = ByteBuffer.allocateDirect(VERTICES_QUAD.length).order(ByteOrder.nativeOrder());
            mBufferQuad.put(VERTICES_QUAD).position(0);

            try {
                rendererScene = new RendererScene(MainActivity.this, mBufferQuad, glCamera, glFramebufferOut);
                rendererDof = new RendererDof(MainActivity.this, mBufferQuad, glCamera,
                        glFramebufferOut, glTextureOut, glTextureHalf1, glTextureHalf2, glTextureHalf3);
                rendererOut = new RendererOut(MainActivity.this, mBufferQuad, glTextureOut, glTextureRand);

                setProgress(9, 1);
                glObjectAnd = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/and.dat"));
                setProgress(9, 2);
                glObjectBy = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/by.dat"));
                setProgress(9, 3);
                glObjectCoding = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/coding.dat"));
                setProgress(9, 4);
                glObjectDoctrnal = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/doctrnal.dat"));
                setProgress(9, 5);
                glObjectGrind = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/grind.dat"));
                setProgress(9, 6);
                glObjectHarism = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/harism.dat"));
                setProgress(9, 7);
                glObjectMusic = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/music.dat"));
                setProgress(9, 8);
                glObjectScottXylo = new GlObject(GlObjectData.loadDat(MainActivity.this, "models/scott_xylo.dat"));
                setProgress(9, 9);

                rendererScene.setObject(glObjectScottXylo);

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
            glCamera.setPerspective(resolutionWidth, resolutionHeight, 60f, 1f, 100f);
            glCamera.setApertureDiameter(4.8f);
            glCamera.setPlaneInFocus(10f);

            glTextureOut.bind(GLES30.GL_TEXTURE_2D)
                    .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, resolutionWidth, resolutionHeight, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                    .unbind(GLES30.GL_TEXTURE_2D);

            glRenderbufferDepth.bind(GLES30.GL_RENDERBUFFER)
                    .storage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT32F, resolutionWidth, resolutionHeight)
                    .unbind(GLES30.GL_RENDERBUFFER);

            glFramebufferOut.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                    .renderbuffer(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, glRenderbufferDepth.name())
                    .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureOut.name(), 0);
            GLES30.glDrawBuffers(1, new int[]{GLES30.GL_COLOR_ATTACHMENT0}, 0);
            glFramebufferOut.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

            glTextureHalf1.bind(GLES30.GL_TEXTURE_2D)
                    .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, resolutionWidth / 2, resolutionHeight / 2, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                    .unbind(GLES30.GL_TEXTURE_2D);

            glTextureHalf2.bind(GLES30.GL_TEXTURE_2D)
                    .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, resolutionWidth / 2, resolutionHeight / 2, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                    .unbind(GLES30.GL_TEXTURE_2D);

            glTextureHalf3.bind(GLES30.GL_TEXTURE_2D)
                    .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, resolutionWidth / 2, resolutionHeight / 2, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                    .unbind(GLES30.GL_TEXTURE_2D);

            rendererScene.onSurfaceChanged(resolutionWidth, resolutionHeight);
            rendererDof.onSurfaceChanged(resolutionWidth, resolutionHeight);
            rendererOut.onSurfaceChanged(width, height);

            float aspectOffscreen = (float) resolutionWidth / resolutionHeight;
            float aspectSurface = (float) width / height;
            if (aspectOffscreen >= aspectSurface) {
                rendererOut.setAspectRatio(aspectSurface / aspectOffscreen, 1.0f);
            } else {
                rendererOut.setAspectRatio(1.0f, aspectOffscreen / aspectSurface);
            }
        }

        @Override
        public void onRenderFrame() {
            long time = SystemClock.uptimeMillis();
            fpsSum += 1f / ((time - timePrev) / 1000f);
            fpsCount += 1;
            timePrev = time;
            if (time > timeStart + 1000) {
                timeStart = time;
                fps = fpsSum / fpsCount;
                fpsSum = 0;
                fpsCount = 0;
                runOnUiThread(fpsRunnable);
            }

            double angle = SystemClock.uptimeMillis() % 10000 / 5000.0 * Math.PI;
            float rx = (float) (Math.sin(angle) * 5);
            float rz = (float) (Math.cos(angle) * 10) + 12;
            glCamera.setPos(new float[]{0, 0, rz});

            rendererScene.onRenderFrame();
            rendererDof.onRenderFrame();
            rendererOut.onRenderFrame();
        }

        @Override
        public void onSurfaceReleased() {
            rendererScene.onSurfaceReleased();
            rendererDof.onSurfaceReleased();
            rendererOut.onSurfaceReleased();
        }

        private void setProgress(final int max, final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setMax(max);
                    progressBar.setProgress(progress);
                }
            });
        }

    }

}
