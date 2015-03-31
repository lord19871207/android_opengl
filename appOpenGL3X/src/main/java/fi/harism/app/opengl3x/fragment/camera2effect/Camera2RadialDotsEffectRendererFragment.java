package fi.harism.app.opengl3x.fragment.camera2effect;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.fragment.RendererFragment;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;

public class Camera2RadialDotsEffectRendererFragment extends RendererFragment implements SurfaceTexture.OnFrameAvailableListener {

    private static final String RENDERER_ID = "renderer.camera2effect.radial.dots";

    private static final int IN_POSITION = 0;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private HandlerThread cameraHandlerThread;
    private Handler cameraHandler;

    private GlTexture glTextureCameraIn;
    private GlTexture glTextureCamera;
    private GlFramebuffer glFramebufferCamera;
    private GlSampler glSamplerNearest;
    private GlProgram glProgramCameraIn;
    private GlProgram glProgramEffectRadialDots;

    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private Size surfaceSize;
    private Size previewSize;
    private boolean frameAvailable;

    private ByteBuffer verticesQuad;

    private int cameraOrientation;
    private final float[] frameMatrix = new float[16];
    private final float[] orientationMatrix = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        cameraHandlerThread = new HandlerThread("CameraHandler");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());
        frameAvailable = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraHandlerThread.quit();
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_camera2effect_radial_dots_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_camera2effect_radial_dots_caption;
    }

    @Override
    public void onSurfaceCreated() {
        glTextureCameraIn = new GlTexture();
        glTextureCamera = new GlTexture();

        glFramebufferCamera = new GlFramebuffer();

        glSamplerNearest = new GlSampler();
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(glTextureCameraIn.name());
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);

        final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
        verticesQuad = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
        verticesQuad.put(VERTICES).position(0);

        try {
            glProgramCameraIn = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2effect/camera_in_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2effect/camera_in_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramCameraIn.getUniformLocation("sTextureOES"), 0);

            glProgramEffectRadialDots = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2effect/effect_radial_dots_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2effect/effect_radial_dots_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramEffectRadialDots.getUniformLocation("sTexture"), 0);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            });
        } catch (final Exception ex) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceSize = new Size(width, height);

        glTextureCamera
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferCamera
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureCamera.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0);
    }

    @Override
    public void onRenderFrame() {
        if (frameAvailable) {
            frameAvailable = false;
            surfaceTexture.getTransformMatrix(frameMatrix);
            surfaceTexture.updateTexImage();

            float aspectSurface = (float) surfaceSize.getWidth() / surfaceSize.getHeight();
            float aspectPreview = (float) previewSize.getWidth() / previewSize.getHeight();
            aspectPreview = cameraOrientation % 180 != 0 ? aspectPreview : 1.0f / aspectPreview;
            float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
            float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

            glFramebufferCamera.bind(GLES30.GL_DRAW_FRAMEBUFFER);
            GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());

            glProgramCameraIn.useProgram();
            GLES30.glUniform2f(glProgramCameraIn.getUniformLocation("uAspectRatio"), aspectX, aspectY);
            GLES30.glUniformMatrix4fv(glProgramCameraIn.getUniformLocation("uFrameMatrix"), 1, false, frameMatrix, 0);
            GLES30.glUniformMatrix4fv(glProgramCameraIn.getUniformLocation("uOrientationMatrix"), 1, false, orientationMatrix, 0);
            GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
            GLES30.glEnableVertexAttribArray(IN_POSITION);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            glTextureCameraIn.bind(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            glSamplerNearest.bind(0);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(IN_POSITION);
            glTextureCameraIn.unbind(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        }

        glProgramEffectRadialDots.useProgram();
        GLES30.glUniform2f(
                glProgramEffectRadialDots.getUniformLocation("uAspectRatio"),
                1f, (float) surfaceSize.getHeight() / surfaceSize.getWidth());

        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureCamera.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(IN_POSITION);
    }

    @Override
    public void onSurfaceReleased() {
        surface.release();
        surfaceTexture.release();
        surface = null;
        surfaceTexture = null;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        frameAvailable = true;
        requestRender();
    }

    private void setOrientation(int orientation) {
        Display d = ((WindowManager) getActivity()
                .getSystemService(Activity.WINDOW_SERVICE))
                .getDefaultDisplay();
        cameraOrientation = d.getRotation() * 90;
        Matrix.setRotateM(orientationMatrix, 0, cameraOrientation, 0, 0, 1);
    }

    public void setSurfaceTextureSize(Size previewSize) {
        this.previewSize = previewSize;
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
    }

    private void openCamera() {
        try {
            String cameraIds[] = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                int lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(CameraDevice device) {
                            cameraDevice = device;
                            startPreview();
                        }

                        @Override
                        public void onDisconnected(CameraDevice cameraDevice) {
                        }

                        @Override
                        public void onError(CameraDevice cameraDevice, int i) {
                        }
                    }, cameraHandler);
                    break;
                }
            }
        } catch (CameraAccessException ex) {
            ex.printStackTrace();
        }
    }

    private void startPreview() {
        if (cameraDevice == null || surface == null) {
            return;
        }
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSizes[] = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
            setOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            setSurfaceTextureSize(previewSizes[0]);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        captureRequestBuilder.addTarget(surface);
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler);
                    } catch (CameraAccessException ex) {
                        ex.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                }
            }, cameraHandler);
        } catch (CameraAccessException ex) {
            ex.printStackTrace();
        }
    }

}
