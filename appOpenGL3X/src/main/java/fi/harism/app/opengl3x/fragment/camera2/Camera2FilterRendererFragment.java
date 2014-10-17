package fi.harism.app.opengl3x.fragment.camera2;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.os.SystemClock;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.fragment.RendererFragment;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.utils.MersenneTwisterFast;

public class Camera2FilterRendererFragment extends RendererFragment implements SurfaceTexture.OnFrameAvailableListener {

    private static final String RENDERER_ID = "renderer.camera2.filter";
    private static final String PREFERENCE_HUE = "renderer.camera2.filter.hue";
    private static final String PREFERENCE_SATURATION = "renderer.camera2.filter.saturation";
    private static final String PREFERENCE_VALUE = "renderer.camera2.filter.value";
    private static final String PREFERENCE_FILTER = "renderer.camera2.filter.filter";
    private static final String PREFERENCE_EFFECT = "renderer.camera2.filter.effect";
    private static final int DEFAULT_HUE = 10;
    private static final int DEFAULT_SATURATION = 5;
    private static final int DEFAULT_VALUE = 5;
    private static final int DEFAULT_FILTER = 0;
    private static final int DEFAULT_EFFECT = 0;

    private static final int IN_POSITION = 0;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private HandlerThread cameraHandlerThread;
    private Handler cameraHandler;

    private GlTexture glTextureCameraIn;
    private GlTexture glTextureCamera;
    private GlTexture glTextureHsv;
    private GlTexture glTextureFilter;
    private GlTexture glTextureNoise;
    private GlFramebuffer glFramebufferCamera;
    private GlFramebuffer glFramebufferHsv;
    private GlFramebuffer glFramebufferFilter;
    private GlSampler glSamplerNearest;
    private GlSampler glSamplerLinear;
    private GlProgram glProgramCameraIn;
    private GlProgram glProgramCopy;
    private GlProgram glProgramHsv;
    private GlProgram glProgramFilterSepia;
    private GlProgram glProgramFilterBlackAndWhite;
    private GlProgram glProgramEffectRadialDots;
    private GlProgram glProgramEffectEdgeDetection;
    private GlProgram glProgramEffectVoronoi;
    private GlProgram glProgramEffectOilPainting;

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

    private int filterIndex;
    private int effectIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        cameraHandlerThread = new HandlerThread("CameraHandler");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());
        frameAvailable = false;

        SharedPreferences prefs =
                getActivity().getPreferences(Context.MODE_PRIVATE);
        hsvVector[0] = prefs.getInt(PREFERENCE_HUE, DEFAULT_HUE) / 20f;
        hsvVector[1] = prefs.getInt(PREFERENCE_SATURATION, DEFAULT_SATURATION) / 10f;
        hsvVector[2] = prefs.getInt(PREFERENCE_VALUE, DEFAULT_VALUE) / 10f;
        filterIndex = prefs.getInt(PREFERENCE_FILTER, DEFAULT_FILTER);
        effectIndex = prefs.getInt(PREFERENCE_EFFECT, DEFAULT_EFFECT);
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
        glTextureNoise = new GlTexture();

        MersenneTwisterFast rand = new MersenneTwisterFast(8349);
        FloatBuffer randBuffer = ByteBuffer
                .allocateDirect(4 * 4 * 256 * 256)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        for (int i = 0; i < 4 * 256 * 256; ++i) {
            randBuffer.put(rand.nextFloat(true, true));
        }
        glTextureNoise
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, 256, 256, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, randBuffer.position(0));

        glFramebufferCamera = new GlFramebuffer();
        glFramebufferHsv = new GlFramebuffer();
        glFramebufferFilter = new GlFramebuffer();

        glSamplerNearest = new GlSampler();
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSamplerNearest.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        glSamplerLinear = new GlSampler();
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(glTextureCameraIn.name());
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);

        final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
        verticesQuad = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
        verticesQuad.put(VERTICES).position(0);

        try {
            glProgramCameraIn = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/camera_in_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/camera_in_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramCameraIn.getUniformLocation("sTextureOES"), 0);

            glProgramCopy = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/copy_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/copy_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramCopy.getUniformLocation("sTexture"), 0);

            glProgramHsv = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/hsv_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/hsv_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramHsv.getUniformLocation("sTexture"), 0);

            glProgramFilterSepia = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/filter_sepia_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/filter_sepia_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramFilterSepia.getUniformLocation("sTexture"), 0);

            glProgramFilterBlackAndWhite = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/filter_black_and_white_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/filter_black_and_white_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramFilterBlackAndWhite.getUniformLocation("sTexture"), 0);

            glProgramEffectRadialDots = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_radial_dots_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_radial_dots_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramEffectRadialDots.getUniformLocation("sTexture"), 0);

            glProgramEffectEdgeDetection = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_edge_detection_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_edge_detection_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramEffectEdgeDetection.getUniformLocation("sTexture"), 0);

            glProgramEffectVoronoi = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_voronoi_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_voronoi_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramEffectVoronoi.getUniformLocation("sTexture"), 0);
            GLES30.glUniform1i(glProgramEffectVoronoi.getUniformLocation("sTextureNoise"), 1);

            glProgramEffectOilPainting = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_oil_painting_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/camera2/filter/effect_oil_painting_fs.txt"),
                    null)
                    .useProgram();
            GLES30.glUniform1i(glProgramEffectOilPainting.getUniformLocation("sTexture"), 0);

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

        switch (filterIndex) {
            case 1:
                glProgramFilterBlackAndWhite.useProgram();
                break;
            case 2:
                glProgramFilterSepia.useProgram();
                break;
            default:
                glProgramCopy.useProgram();
                break;
        }

        glFramebufferFilter.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureCamera.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(IN_POSITION);

        glProgramHsv.useProgram();
        glFramebufferHsv.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glUniform3fv(glProgramHsv.getUniformLocation("uHsv"), 1, hsvVector, 0);
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureFilter.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(IN_POSITION);

        switch (effectIndex) {
            case 1:
                glProgramEffectRadialDots.useProgram();
                GLES30.glUniform2f(
                        glProgramEffectRadialDots.getUniformLocation("uAspectRatio"),
                        1f, (float) surfaceSize.getHeight() / surfaceSize.getWidth());
                break;
            case 2:
                glProgramEffectEdgeDetection.useProgram();
                GLES30.glUniform2f(
                        glProgramEffectEdgeDetection.getUniformLocation("uPixelSize"),
                        1f / surfaceSize.getWidth(), 1f / surfaceSize.getHeight());
                break;
            case 3:
                float time = SystemClock.uptimeMillis() % 31416 / 1000f;
                glProgramEffectVoronoi.useProgram();
                GLES30.glUniform1f(
                        glProgramEffectVoronoi.getUniformLocation("uGlobalTime"), time);
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                glTextureNoise.bind(GLES30.GL_TEXTURE_2D);
                glSamplerLinear.bind(1);
                break;
            case 4:
                glProgramEffectOilPainting.useProgram();
                GLES30.glUniform2f(
                        glProgramEffectOilPainting.getUniformLocation("uSampleOffset"),
                        2f / surfaceSize.getWidth(),
                        2f / surfaceSize.getHeight());
                break;
            default:
                glProgramCopy.useProgram();
                break;
        }
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
        GLES30.glEnableVertexAttribArray(IN_POSITION);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureHsv.bind(GLES30.GL_TEXTURE_2D);
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

    public void onEvent(SetFilterValuesEvent event) {
        hsvVector[0] = event.getHue() / 20f;
        hsvVector[1] = event.getSaturation() / 10f;
        hsvVector[2] = event.getValue() / 10f;
        filterIndex = event.getFilter();
        effectIndex = event.getEffect();
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                postFilterValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveFilterValues();
            }
        };

        private final Spinner.OnItemSelectedListener
                spinnerSelectedListener = new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                postFilterValues();
                saveFilterValues();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };

        private SeekBar seekBarHue;
        private SeekBar seekBarSaturation;
        private SeekBar seekBarValue;
        private Spinner spinnerFilter;
        private Spinner spinnerEffect;

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

            spinnerFilter = (Spinner) view.findViewById(R.id.spinner_filter);
            ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.camera2_filter_filters, R.layout.container_spinner_selected_item);
            filterAdapter.setDropDownViewResource(R.layout.container_spinner_dropdown_item);
            spinnerFilter.setAdapter(filterAdapter);

            spinnerEffect = (Spinner) view.findViewById(R.id.spinner_effect);
            ArrayAdapter<CharSequence> effectAdapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.camera2_filter_effects, R.layout.container_spinner_selected_item);
            effectAdapter.setDropDownViewResource(R.layout.container_spinner_dropdown_item);
            spinnerEffect.setAdapter(effectAdapter);

            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            seekBarHue.setProgress(prefs.getInt(PREFERENCE_HUE, DEFAULT_HUE));
            seekBarSaturation.setProgress(prefs.getInt(PREFERENCE_SATURATION, DEFAULT_SATURATION));
            seekBarValue.setProgress(prefs.getInt(PREFERENCE_VALUE, DEFAULT_VALUE));
            spinnerFilter.setSelection(prefs.getInt(PREFERENCE_FILTER, DEFAULT_FILTER));
            spinnerEffect.setSelection(prefs.getInt(PREFERENCE_EFFECT, DEFAULT_EFFECT));

            seekBarHue.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarSaturation.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarValue.setOnSeekBarChangeListener(seekBarChangeListener);
            spinnerFilter.setOnItemSelectedListener(spinnerSelectedListener);
            spinnerEffect.setOnItemSelectedListener(spinnerSelectedListener);

            return view;
        }

        private void postFilterValues() {
            int hue = seekBarHue.getProgress();
            int saturation = seekBarSaturation.getProgress();
            int value = seekBarValue.getProgress();
            int filter = spinnerFilter.getSelectedItemPosition();
            int effect = spinnerEffect.getSelectedItemPosition();
            EventBus.getDefault().post(
                    new SetFilterValuesEvent(hue, saturation, value, filter, effect));
        }

        private void saveFilterValues() {
            SharedPreferences.Editor editor =
                    getActivity().getPreferences(Context.MODE_PRIVATE).edit();
            editor.putInt(PREFERENCE_HUE, seekBarHue.getProgress());
            editor.putInt(PREFERENCE_SATURATION, seekBarSaturation.getProgress());
            editor.putInt(PREFERENCE_VALUE, seekBarValue.getProgress());
            editor.putInt(PREFERENCE_FILTER, spinnerFilter.getSelectedItemPosition());
            editor.putInt(PREFERENCE_EFFECT, spinnerEffect.getSelectedItemPosition());
            editor.commit();
        }

    }

    public static class SetFilterValuesEvent {

        private int hue;
        private int saturation;
        private int value;
        private int filter;
        private int effect;

        public SetFilterValuesEvent(int hue, int saturation, int value,
                                    int filter, int effect) {
            this.hue = hue;
            this.saturation = saturation;
            this.value = value;
            this.filter = filter;
            this.effect = effect;
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

        public int getFilter() {
            return filter;
        }

        public int getEffect() {
            return effect;
        }

    }

}
