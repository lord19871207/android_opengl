package fi.harism.app.opengl3x.fragment.camera2;

import android.app.Activity;
import android.app.Fragment;
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
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.fragment.RendererFragment;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;

public class Camera2FilterRendererFragment extends RendererFragment implements SurfaceTexture.OnFrameAvailableListener {

    private static final String RENDERER_ID = "renderer.camera2.filter";
    private static final int IN_POSITION = 0;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private HandlerThread cameraHandlerThread;
    private Handler cameraHandler;

    private GlTexture glTextureCameraIn;
    private GlTexture glTextureCamera;
    private GlTexture glTextureHsv;
    private GlTexture glTextureFilter;
    private GlFramebuffer glFramebufferCamera;
    private GlFramebuffer glFramebufferHsv;
    private GlFramebuffer glFramebufferFilter;
    private GlSampler glSamplerNearest;
    private GlProgram glProgramCameraIn;
    private GlProgram glProgramHsv;
    private GlProgram glProgramCopy;

    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private Size surfaceSize;
    private Size previewSize;
    private boolean frameAvailable;

    private ByteBuffer verticesQuad;

    private int cameraOrientation;
    private final float[] frameMatrix = new float[16];
    private final float[] orientationMatrix = new float[16];
    private final float[] hsvVector = new float[3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        cameraHandlerThread = new HandlerThread("CameraHandler");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());
        frameAvailable = false;
        setManualRendering(true);
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
        return R.string.renderer_camera2_filter_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_camera2_filter_caption;
    }

    @Override
    public Fragment getSettingsFragment() {
        return new SettingsFragment();
    }

    @Override
    public void onSurfaceCreated() {
        glTextureCameraIn = new GlTexture();
        glTextureCamera = new GlTexture();
        glTextureHsv = new GlTexture();
        glTextureFilter = new GlTexture();
        glFramebufferCamera = new GlFramebuffer();
        glFramebufferHsv = new GlFramebuffer();
        glFramebufferFilter = new GlFramebuffer();

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
            final String CAMERA_IN_VS = GlUtils.loadString(getActivity(), "shaders/camera2/filter/camera_in_vs.txt");
            final String CAMERA_IN_FS = GlUtils.loadString(getActivity(), "shaders/camera2/filter/camera_in_fs.txt");
            glProgramCameraIn = new GlProgram(CAMERA_IN_VS, CAMERA_IN_FS, null).useProgram();
            GLES30.glUniform1i(glProgramCameraIn.getUniformLocation("sTextureOES"), 0);

            final String COPY_VS = GlUtils.loadString(getActivity(), "shaders/camera2/filter/copy_vs.txt");
            final String COPY_FS = GlUtils.loadString(getActivity(), "shaders/camera2/filter/copy_fs.txt");
            glProgramCopy = new GlProgram(COPY_VS, COPY_FS, null).useProgram();
            GLES30.glUniform1i(glProgramCopy.getUniformLocation("sTexture"), 0);

            final String HSB_VS = GlUtils.loadString(getActivity(), "shaders/camera2/filter/hsv_vs.txt");
            final String HSB_FS = GlUtils.loadString(getActivity(), "shaders/camera2/filter/hsv_fs.txt");
            glProgramHsv = new GlProgram(HSB_VS, HSB_FS, null).useProgram();
            GLES30.glUniform1i(glProgramHsv.getUniformLocation("sTexture"), 0);

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
        glTextureHsv
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureFilter
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);

        glFramebufferCamera
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureCamera.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0);
        glFramebufferHsv
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureHsv.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0);
        glFramebufferFilter
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureFilter.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);
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

        glProgramHsv.useProgram();
        glFramebufferHsv.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glUniform3fv(glProgramHsv.getUniformLocation("uHsv"), 1, hsvVector, 0);
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureCamera.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(IN_POSITION);
        glTextureCamera.unbind(GLES30.GL_TEXTURE_2D);

        glProgramCopy.useProgram();
        glFramebufferFilter.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureHsv.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(IN_POSITION);
        glTextureHsv.unbind(GLES30.GL_TEXTURE_2D);

        glProgramCopy.useProgram();
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureFilter.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(IN_POSITION);
        glTextureFilter.unbind(GLES30.GL_TEXTURE_2D);
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
                    cameraManager.openCamera(cameraId, new CameraDevice.StateListener() {
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
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateListener() {
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

    public void onEvent(SetFilterValuesEvent event) {
        hsvVector[0] = event.getHue() / 255f;
        hsvVector[1] = event.getSaturation() / 255f;
        hsvVector[2] = event.getValue() / 255f;
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int hue = seekBarHue.getProgress();
                int saturation = seekBarSaturation.getProgress();
                int value = seekBarValue.getProgress();
                EventBus.getDefault().post(new SetFilterValuesEvent(hue, saturation, value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //int color = Color.rgb(
                //        seekBarRed.getProgress(),
                //        seekBarGreen.getProgress(),
                //        seekBarBlue.getProgress());
                //SharedPreferences.Editor editor =
                //        getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                //editor.putInt(PREFERENCE_COLOR, color);
                //editor.commit();
            }
        };

        private SeekBar seekBarHue;
        private SeekBar seekBarSaturation;
        private SeekBar seekBarValue;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_camera2_filter, container, false);

            seekBarHue = (SeekBar) view.findViewById(R.id.seekbar_hue);
            seekBarSaturation = (SeekBar) view.findViewById(R.id.seekbar_saturation);
            seekBarValue = (SeekBar) view.findViewById(R.id.seekbar_value);
            seekBarHue.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarSaturation.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarValue.setOnSeekBarChangeListener(seekBarChangeListener);

            //int color = getActivity().getPreferences(Context.MODE_PRIVATE)
            //        .getInt(PREFERENCE_COLOR, DEFAULT_COLOR);
            //seekBarRed.setProgress(Color.red(color));
            //seekBarGreen.setProgress(Color.green(color));
            //seekBarBlue.setProgress(Color.blue(color));

            return view;
        }

    }

    public static class SetFilterValuesEvent {

        private int hue;
        private int saturation;
        private int value;

        public SetFilterValuesEvent(int hue, int saturation, int value) {
            this.hue = hue;
            this.saturation = saturation;
            this.value = value;
        }

        public int getHue() {
            return hue;
        }

        public int getSaturation() {
            return saturation;
        }

        public int getValue() {
            return value;
        }

    }

}
