package fi.harism.app.opengl3x.fragment.camera2;

import android.app.Activity;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.fragment.RendererFragment;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;

public class Camera2YuvRendererFragment extends RendererFragment {

    private static final String RENDERER_ID = "renderer.camera2.yuv";
    private static final int IN_POSITION = 0;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private HandlerThread cameraHandlerThread;
    private Handler cameraHandler;

    private ImageReader imageReader;

    private boolean surfaceCreated;
    private GlTexture glTextureY;
    private GlTexture glTextureU;
    private GlTexture glTextureV;
    private GlTexture glTextureTonemapCurve;
    private GlSampler glSamplerNearest;
    private GlSampler glSamplerLinear;
    private GlProgram glProgramConvert;
    private ByteBuffer verticesQuad;

    private int cameraOrientation;
    private Size surfaceSize;
    private Size previewSize;

    private float[] bufferTonemapCurveArray;
    private FloatBuffer bufferTonemapCurve;
    private int bufferTonemapCurveMax;

    private final float[] orientationMatrix = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        cameraHandlerThread = new HandlerThread("CameraHandler");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());
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
        return R.string.renderer_camera2_yuv_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_camera2_yuv_caption;
    }

    @Override
    public void onSurfaceCreated() {
        glTextureY = new GlTexture();
        glTextureU = new GlTexture();
        glTextureV = new GlTexture();
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

        final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
        verticesQuad = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
        verticesQuad.put(VERTICES).position(0);

        try {
            final String CONVERT_VS = GlUtils.loadString(getActivity(), "shaders/camera2/yuv/convert_vs.txt");
            final String CONVERT_FS = GlUtils.loadString(getActivity(), "shaders/camera2/yuv/convert_fs.txt");
            glProgramConvert = new GlProgram(CONVERT_VS, CONVERT_FS, null).useProgram();
            GLES30.glUniform1i(glProgramConvert.getUniformLocation("sTextureY"), 0);
            GLES30.glUniform1i(glProgramConvert.getUniformLocation("sTextureU"), 1);
            GLES30.glUniform1i(glProgramConvert.getUniformLocation("sTextureV"), 2);
            GLES30.glUniform1i(glProgramConvert.getUniformLocation("sTextureTonemapCurve"), 3);

            surfaceCreated = true;
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
                setTextureData(glTextureY, imageWidth, imageHeight, image.getPlanes()[0]);
                setTextureData(glTextureU, imageWidth / 2, imageHeight / 2, image.getPlanes()[1]);
                setTextureData(glTextureV, imageWidth / 2, imageHeight / 2, image.getPlanes()[2]);
                image.close();

                glTextureTonemapCurve
                        .bind(GLES30.GL_TEXTURE_2D)
                        .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG16F, bufferTonemapCurveMax, 3, 0, GLES30.GL_RG, GLES30.GL_FLOAT, bufferTonemapCurve)
                        .unbind(GLES30.GL_TEXTURE_2D);
            }

            float aspectSurface = (float) surfaceSize.getWidth() / surfaceSize.getHeight();
            float aspectPreview = (float) previewSize.getWidth() / previewSize.getHeight();
            aspectPreview = cameraOrientation % 180 == 0 ? aspectPreview : 1.0f / aspectPreview;
            float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
            float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

            glProgramConvert.useProgram();
            GLES30.glUniform2f(glProgramConvert.getUniformLocation("uAspectRatio"), aspectX, aspectY);
            GLES30.glUniformMatrix4fv(glProgramConvert.getUniformLocation("uOrientationMatrix"), 1, false, orientationMatrix, 0);
            GLES30.glVertexAttribPointer(IN_POSITION, 2, GLES30.GL_BYTE, false, 0, verticesQuad);
            GLES30.glEnableVertexAttribArray(IN_POSITION);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            glTextureY.bind(GLES30.GL_TEXTURE_2D);
            glSamplerNearest.bind(0);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            glTextureU.bind(GLES30.GL_TEXTURE_2D);
            glSamplerNearest.bind(1);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
            glTextureV.bind(GLES30.GL_TEXTURE_2D);
            glSamplerNearest.bind(2);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
            glTextureTonemapCurve.bind(GLES30.GL_TEXTURE_2D);
            glSamplerLinear.bind(3);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(IN_POSITION);
        }
    }

    @Override
    public void onSurfaceReleased() {
        surfaceCreated = false;
    }

    private void setTextureData(GlTexture texture, int width, int height, Image.Plane plane) {
        int internalFormat = GLES30.GL_R8;
        int format = GLES30.GL_RED;
        switch (plane.getPixelStride()) {
            case 1:
                break;
            case 2:
                internalFormat = GLES30.GL_RG8;
                format = GLES30.GL_RG;
                break;
            case 3:
                internalFormat = GLES30.GL_RGB8;
                format = GLES30.GL_RGB;
                break;
            case 4:
                internalFormat = GLES30.GL_RGBA8;
                format = GLES30.GL_RGBA;
                break;
            default:
                Log.e("ERROR", "Pixel stride larger than 4.");
                break;
        }

        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, plane.getRowStride() / plane.getPixelStride());
        texture.bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GLES30.GL_UNSIGNED_BYTE, plane.getBuffer())
                .unbind(GLES30.GL_TEXTURE_2D);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
    }

    private void setOrientation(int orientation) {
        Display d = ((WindowManager)getActivity()
                .getSystemService(Activity.WINDOW_SERVICE))
                .getDefaultDisplay();
        cameraOrientation = orientation - d.getRotation() * 90;
        Matrix.setRotateM(orientationMatrix, 0, cameraOrientation, 0, 0, -1);
    }

    private void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
    }

    private void setTonemapCurveMax(int max) {
        bufferTonemapCurveMax = max;
        bufferTonemapCurveArray = new float[2 * 3 * max];
        bufferTonemapCurve = FloatBuffer.wrap(bufferTonemapCurveArray);
    }

    private void setTonemapCurve(TonemapCurve tonemapCurve) {
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
        if (!surfaceCreated) {
            return;
        }
        try {
            CameraCharacteristics cameraCharacteristics =
                    cameraManager.getCameraCharacteristics(cameraDevice.getId());
            StreamConfigurationMap streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSizes[] = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);
            imageReader = ImageReader.newInstance(
                    previewSizes[0].getWidth(), previewSizes[0].getHeight(),
                    ImageFormat.YUV_420_888, 2);
            setOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            setTonemapCurveMax(cameraCharacteristics.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS));
            setPreviewSize(previewSizes[0]);
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            try {
                                CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                                captureRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST);
                                captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
                                captureRequestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF);
                                captureRequestBuilder.addTarget(imageReader.getSurface());

                                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                                    @Override
                                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                        setTonemapCurve(result.get(CaptureResult.TONEMAP_CURVE));
                                        requestRender();
                                    }
                                }, cameraHandler);
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
