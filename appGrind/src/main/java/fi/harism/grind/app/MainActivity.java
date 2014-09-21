package fi.harism.grind.app;

import android.animation.Animator;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
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
import fi.harism.opengl.lib.gl.GlRenderbuffer;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.model.GlCamera;
import fi.harism.opengl.lib.model.GlCameraAnimator;
import fi.harism.opengl.lib.model.GlObject;
import fi.harism.opengl.lib.model.GlObjectLoader;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.view.GlTextureView;
import fi.harism.utils.lib.MersenneTwisterFast;

public class MainActivity extends Activity {

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            glTextureView.renderFrame(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    private MediaPlayer mediaPlayer;
    private Choreographer choreographer;
    private View progressLayout;
    private ProgressBar progressBar;
    private GlTextureView glTextureView;

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

        glTextureView = (GlTextureView) findViewById(R.id.texture_view);
        glTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        glTextureView.setGlRenderer(new MainRenderer());
        glTextureView.setAlpha(0f);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        mediaPlayer = null;

        choreographer.removeFrameCallback(frameCallback);

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        glTextureView.onDestroy();
        glTextureView = null;
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

    private class MainRenderer implements GlRenderer {
        private GlFramebuffer glFramebufferOut;
        private GlTexture glTextureOut;
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

        private RendererOut rendererOut;
        private RendererScene rendererScene;

        private long mPrevTime;

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
                rendererOut = new RendererOut(MainActivity.this, mBufferQuad, glTextureOut, glTextureRand);
                rendererScene = new RendererScene(MainActivity.this, mBufferQuad, glCamera, glFramebufferOut);

                setProgress(9, 1);
                glObjectAnd = GlObjectLoader.loadDat(MainActivity.this, "models/and.dat");
                setProgress(9, 2);
                glObjectBy = GlObjectLoader.loadDat(MainActivity.this, "models/by.dat");
                setProgress(9, 3);
                glObjectCoding = GlObjectLoader.loadDat(MainActivity.this, "models/coding.dat");
                setProgress(9, 4);
                glObjectDoctrnal = GlObjectLoader.loadDat(MainActivity.this, "models/doctrnal.dat");
                setProgress(9, 5);
                glObjectGrind = GlObjectLoader.loadDat(MainActivity.this, "models/grind.dat");
                setProgress(9, 6);
                glObjectHarism = GlObjectLoader.loadDat(MainActivity.this, "models/harism.dat");
                setProgress(9, 7);
                glObjectMusic = GlObjectLoader.loadDat(MainActivity.this, "models/music.dat");
                setProgress(9, 8);
                glObjectScottXylo = GlObjectLoader.loadDat(MainActivity.this, "models/scott_xylo.dat");
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
            glCamera.setPerspectiveM(width, height, 60f, 1f, 100f);
            glCamera.setApertureDiameter(3f);
            glCamera.setPlaneInFocus(10f);

            glTextureOut.bind(GLES30.GL_TEXTURE_2D);
            glTextureOut.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
            glTextureOut.unbind(GLES30.GL_TEXTURE_2D);

            glRenderbufferDepth.bind();
            glRenderbufferDepth.storage(GLES30.GL_DEPTH_COMPONENT32F, width, height);
            glRenderbufferDepth.unbind();

            glFramebufferOut.bind(GLES30.GL_DRAW_FRAMEBUFFER);
            glFramebufferOut.renderbuffer(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, glRenderbufferDepth.name());
            glFramebufferOut.texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureOut.name(), 0);
            final int[] ATTACHMENTS = {GLES30.GL_COLOR_ATTACHMENT0};
            GLES30.glDrawBuffers(1, ATTACHMENTS, 0);
            glFramebufferOut.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

            rendererScene.onSurfaceChanged(width, height);
            rendererOut.onSurfaceChanged(width, height);
        }

        @Override
        public void onRenderFrame() {
            long time = SystemClock.uptimeMillis();
            Log.d("FPS", "fps = " + (1f / ((time - mPrevTime) / 1000f)));
            mPrevTime = time;

            double angle = SystemClock.uptimeMillis() % 10000 / 5000.0 * Math.PI;
            float rx = (float) (Math.sin(angle) * 5);
            float rz = (float) (Math.cos(angle) * 5);
            glCamera.setLookAtM(rx, 0, 5, rx, 0, 0, 0, 1, 0);

            rendererScene.onRenderFrame();
            rendererOut.onRenderFrame();
        }

        @Override
        public void onSurfaceReleased() {
            rendererScene.onSurfaceReleased();
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
