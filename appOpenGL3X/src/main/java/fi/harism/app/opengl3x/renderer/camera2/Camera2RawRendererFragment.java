package fi.harism.app.opengl3x.renderer.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.LensShadingMap;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.renderer.RendererFragment;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;

public class Camera2RawRendererFragment extends RendererFragment {

    private static final String RENDERER_ID = "renderer.camera2.basic";
    private static final int IN_POSITION = 0;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;

    private GlTexture glTextureRaw;
    private GlTexture glTextureRgb;
    private GlTexture glTextureLensMap;
    private GlTexture glTextureTonemapCurve;
    private GlFramebuffer glFramebufferRgb;
    private GlSampler glSamplerNearest;
    private GlSampler glSamplerLinear;
    private GlProgram glProgramConvert;
    private GlProgram glProgramCopy;
    private ByteBuffer verticesQuad;

    private int rawMaxValue;
    private int cameraOrientation;
    private Size surfaceSize;
    private Size previewSize;

    private final float[] bufferLensMapArray = new float[4 * 64 * 64];
    private final FloatBuffer bufferLensMap = FloatBuffer.wrap(bufferLensMapArray);
    private int bufferLensMapColumns;
    private int bufferLensMapRows;
    private float[] bufferTonemapCurveArray;
    private FloatBuffer bufferTonemapCurve;
    private int bufferTonemapCurveMax;

    private final float[] frameMatrix = new float[16];
    private final float[] orientationMatrix = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
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
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_camera2_raw_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_camera2_raw_caption;
    }

    @Override
    public void onSurfaceCreated() {
        glTextureRaw = new GlTexture();
        glTextureRgb = new GlTexture();
        glTextureLensMap = new GlTexture();
        glTextureTonemapCurve = new GlTexture();

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

        glFramebufferRgb = new GlFramebuffer();

        final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
        verticesQuad = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
        verticesQuad.put(VERTICES).position(0);

        try {
            final String CONVERT_VS = GlUtils.loadString(getActivity(), "shaders/camera2/raw/convert_vs.txt");
            final String CONVERT_FS = GlUtils.loadString(getActivity(), "shaders/camera2/raw/convert_fs.txt");
            glProgramConvert = new GlProgram(CONVERT_VS, CONVERT_FS, null).useProgram();
            GLES30.glUniform1i(glProgramConvert.getUniformLocation("sTextureRaw"), 0);
            GLES30.glUniform1i(glProgramConvert.getUniformLocation("sTextureLensMap"), 1);

            final String COPY_VS = GlUtils.loadString(getActivity(), "shaders/camera2/raw/copy_vs.txt");
            final String COPY_FS = GlUtils.loadString(getActivity(), "shaders/camera2/raw/copy_fs.txt");
            glProgramCopy = new GlProgram(COPY_VS, COPY_FS, null).useProgram();
            GLES30.glUniform1i(glProgramCopy.getUniformLocation("sTextureRgb"), 0);
            GLES30.glUniform1i(glProgramCopy.getUniformLocation("sTextureTonemapCurve"), 1);

            startPreview();
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
    }

    @Override
    public void onRenderFrame() {
        if (imageReader != null) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                glTextureRaw
                        .bind(GLES30.GL_TEXTURE_2D)
                        .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, imageWidth, imageHeight, 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, image.getPlanes()[0].getBuffer())
                        .unbind(GLES30.GL_TEXTURE_2D);
                glTextureRgb
                        .bind(GLES30.GL_TEXTURE_2D)
                        .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, imageWidth, imageHeight, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                        .unbind(GLES30.GL_TEXTURE_2D);

                image.close();

                bufferLensMap.position(0);
                glTextureLensMap
                        .bind(GLES30.GL_TEXTURE_2D)
                        .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, bufferLensMapColumns, bufferLensMapRows, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, bufferLensMap)
                        .unbind(GLES30.GL_TEXTURE_2D);

                bufferTonemapCurve.position(0);
                glTextureTonemapCurve
                        .bind(GLES30.GL_TEXTURE_2D)
                        .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG16F, bufferTonemapCurveMax, 3, 0, GLES30.GL_RG, GLES30.GL_FLOAT, bufferTonemapCurve)
                        .unbind(GLES30.GL_TEXTURE_2D);

                glFramebufferRgb
                        .bind(GLES30.GL_FRAMEBUFFER)
                        .texture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureRgb.name(), 0)
                        .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0);

                GLES30.glViewport(0, 0, imageWidth, imageHeight);
                glProgramConvert.useProgram();
                GLES30.glUniform2f(glProgramConvert.getUniformLocation("uTextureRawSize"), imageWidth, imageHeight);
                GLES30.glUniform1f(glProgramConvert.getUniformLocation("uRawMaxValue"), rawMaxValue);
                GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
                GLES30.glEnableVertexAttribArray(IN_POSITION);
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                glTextureRaw.bind(GLES30.GL_TEXTURE_2D);
                glSamplerNearest.bind(0);
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                glTextureLensMap.bind(GLES30.GL_TEXTURE_2D);
                glSamplerLinear.bind(1);
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
                GLES30.glDisableVertexAttribArray(IN_POSITION);
                glTextureRaw.unbind(GLES30.GL_TEXTURE_2D);
                glFramebufferRgb.unbind(GLES30.GL_FRAMEBUFFER);
            }

            GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());

            float aspectSurface = (float) surfaceSize.getWidth() / surfaceSize.getHeight();
            float aspectPreview = (float) previewSize.getWidth() / previewSize.getHeight();
            aspectPreview = cameraOrientation % 180 == 0 ? aspectPreview : 1.0f / aspectPreview;
            float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
            float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

            glProgramCopy.useProgram();
            GLES30.glUniform2f(glProgramCopy.getUniformLocation("uAspectRatio"), aspectX, aspectY);
            GLES30.glUniformMatrix4fv(glProgramCopy.getUniformLocation("uFrameMatrix"), 1, false, frameMatrix, 0);
            GLES30.glUniformMatrix4fv(glProgramCopy.getUniformLocation("uOrientationMatrix"), 1, false, orientationMatrix, 0);
            GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
            GLES30.glEnableVertexAttribArray(IN_POSITION);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            glTextureRgb.bind(GLES30.GL_TEXTURE_2D);
            glSamplerLinear.bind(0);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            glTextureTonemapCurve.bind(GLES30.GL_TEXTURE_2D);
            glSamplerLinear.bind(1);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(IN_POSITION);
            glTextureRgb.unbind(GLES30.GL_TEXTURE_2D);
            glTextureLensMap.unbind(GLES30.GL_TEXTURE_2D);
            glTextureTonemapCurve.unbind(GLES30.GL_TEXTURE_2D);
        }
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void setOrientation(int orientation) {
        cameraOrientation = orientation;
        Matrix.setRotateM(orientationMatrix, 0, orientation, 0, 0, -1);
    }

    public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
    }

    public void setLensShadingMap(LensShadingMap lensShadingMap) {
        bufferLensMapColumns = lensShadingMap.getColumnCount();
        bufferLensMapRows = lensShadingMap.getRowCount();
        lensShadingMap.copyGainFactors(bufferLensMapArray, 0);
    }

    public void setTonemapCurveMax(int max) {
        bufferTonemapCurveMax = max;
        bufferTonemapCurveArray = new float[2 * 3 * max];
        bufferTonemapCurve = FloatBuffer.wrap(bufferTonemapCurveArray);
    }

    public void setTonemapCurve(TonemapCurve tonemapCurve) {
        tonemapCurve.copyColorCurve(TonemapCurve.CHANNEL_RED, bufferTonemapCurveArray, bufferTonemapCurveMax * 0);
        tonemapCurve.copyColorCurve(TonemapCurve.CHANNEL_GREEN, bufferTonemapCurveArray, bufferTonemapCurveMax * 2);
        tonemapCurve.copyColorCurve(TonemapCurve.CHANNEL_BLUE, bufferTonemapCurveArray, bufferTonemapCurveMax * 4);
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
                        }

                        @Override
                        public void onDisconnected(CameraDevice cameraDevice) {
                        }

                        @Override
                        public void onError(CameraDevice cameraDevice, int i) {
                        }
                    }, null);
                    break;
                }
            }
        } catch (CameraAccessException ex) {
            ex.printStackTrace();
        }
    }

    private void startPreview() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSizes[] = streamConfigurationMap.getOutputSizes(ImageFormat.RAW_SENSOR);
            imageReader = ImageReader.newInstance(previewSizes[0].getWidth(), previewSizes[0].getHeight(), ImageFormat.RAW_SENSOR, 2);
            setOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            setPreviewSize(previewSizes[0]);
            setTonemapCurveMax(cameraCharacteristics.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS));
            rawMaxValue = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateListener() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        captureRequestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON);
                        captureRequestBuilder.addTarget(imageReader.getSurface());

                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureListener() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                setLensShadingMap(result.get(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP));
                                setTonemapCurve(result.get(CaptureResult.TONEMAP_CURVE));
                                requestRender();
                            }
                        }, null);
                    } catch (CameraAccessException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException ex) {
            ex.printStackTrace();
        }
    }

}
