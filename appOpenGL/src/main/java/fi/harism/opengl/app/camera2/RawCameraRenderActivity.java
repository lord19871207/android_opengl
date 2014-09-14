package fi.harism.opengl.app.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
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
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import fi.harism.opengl.app.R;
import fi.harism.opengl.app.RenderActivity;
import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.view.GlSurfaceView;

public class RawCameraRenderActivity extends RenderActivity {

    private static final String TAG = RawCameraRenderActivity.class.getSimpleName();

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    private GlSurfaceView mGlSurfaceView;
    private BasicCameraRenderer mRenderer;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRenderer = new BasicCameraRenderer();
        mGlSurfaceView = new GlSurfaceView(this);
        mGlSurfaceView.setEglContext(EglCore.VERSION_GLES3, 0);
        mGlSurfaceView.setGlRenderer(mRenderer);
        setContentView(mGlSurfaceView);
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGlSurfaceView.onDestroy();
    }

    @Override
    public int getRendererTitleId() {
        return R.string.camera2_raw_renderer_title;
    }

    @Override
    public int getRendererCaptionId() {
        return R.string.camera2_raw_renderer_caption;
    }

    private void openCamera() {
        try {
            String cameraIds[] = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                int lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraManager.openCamera(cameraId, new CameraDevice.StateListener() {
                        @Override
                        public void onOpened(CameraDevice cameraDevice) {
                            mCameraDevice = cameraDevice;
                            startPreview();
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
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraDevice.getId());
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSizes[] = streamConfigurationMap.getOutputSizes(ImageFormat.RAW_SENSOR);
            mImageReader = ImageReader.newInstance(previewSizes[0].getWidth(), previewSizes[0].getHeight(), ImageFormat.RAW_SENSOR, 2);
            mRenderer.setOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            mRenderer.setPreviewSize(previewSizes[0]);
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateListener() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        captureRequestBuilder.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_HIGH_QUALITY);
                        captureRequestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON);
                        captureRequestBuilder.addTarget(mImageReader.getSurface());

                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureListener() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                mRenderer.setLensShadingMap(result.get(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP));
                                mGlSurfaceView.renderFrame();
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

    private class BasicCameraRenderer implements GlRenderer {

        private static final String VS_SOURCE = "" +
                "#version 300 es\n" +
                "uniform vec2 uAspectRatio;\n" +
                "uniform mat4 uOrientationMatrix;\n" +
                "uniform vec2 uTextureRawSize;\n" +
                "layout (location = 0) in vec4 position;\n" +
                "out vec4 vTexPosition;\n" +
                "void main() {\n" +
                "  gl_Position = position;\n" +
                "  //gl_Position = uOrientationMatrix * position;\n" +
                "  //gl_Position.xy *= uAspectRatio;\n" +
                "  vTexPosition.xy = position.xy * 0.5 + 0.5;\n" +
                "  vTexPosition.zw = vTexPosition.xy * uTextureRawSize;\n" +
                "  //vTexPosition.y = 1.0 - vTexPosition.y;\n" +
                "}\n";

        private static final String FS_SOURCE = "" +
                "#version 300 es\n" +
                "uniform usampler2D sTextureRaw;\n" +
                "uniform sampler2D sTextureLensMap;\n" +
                "in vec4 vTexPosition;\n" +
                "out vec4 outColor;\n" +
                "void main() {\n" +
                "  ivec2 samplePos = ivec2(vTexPosition.zw) | 0x01;\n" +
                "  vec2 sampleFract = fract(vTexPosition.zw);\n" +
                "  #define RAW(x, y) texelFetchOffset(sTextureRaw, samplePos, 0, ivec2(x, y)).r\n" +
                "  vec4 raw = vec4(RAW(1, 1), RAW(3, 1), RAW(1, 3), RAW(3, 3));\n" +
                "  raw.x = mix(raw.x, raw.y, sampleFract.x);\n" +
                "  raw.z = mix(raw.z, raw.a, sampleFract.x);\n" +
                "  outColor.r = mix(raw.x, raw.z, sampleFract.y);\n" +
                "  raw = vec4(RAW(1, 0), RAW(3, 0), RAW(1, 2), RAW(3, 2));\n" +
                "  raw.x = mix(raw.x, raw.y, sampleFract.x);\n" +
                "  raw.z = mix(raw.z, raw.a, sampleFract.x);\n" +
                "  outColor.g = mix(raw.x, raw.z, sampleFract.y);\n" +
                "  raw = vec4(RAW(0, 1), RAW(2, 1), RAW(0, 3), RAW(2, 3));\n" +
                "  raw.x = mix(raw.x, raw.y, sampleFract.x);\n" +
                "  raw.z = mix(raw.z, raw.a, sampleFract.x);\n" +
                "  outColor.b = mix(raw.x, raw.z, sampleFract.y);\n" +
                "  raw = vec4(RAW(0, 0), RAW(2, 0), RAW(0, 2), RAW(2, 2));\n" +
                "  raw.x = mix(raw.x, raw.y, sampleFract.x);\n" +
                "  raw.z = mix(raw.z, raw.a, sampleFract.x);\n" +
                "  outColor.a = mix(raw.x, raw.z, sampleFract.y);\n" +
                "  outColor *= texture(sTextureLensMap, vTexPosition.xy);\n" +
                "  outColor /= 1023.0;\n" +
                "  outColor = vec4(outColor.r, (outColor.g + outColor.b) * 0.5, outColor.a, 1.0);\n" +
                "}\n";

        private final float mFrameMatrix[] = new float[16];
        private final float mOrientationMatrix[] = new float[16];
        private final Point mSurfaceSize = new Point();
        private GlTexture mTextureRaw;
        private GlTexture mTextureLensMap;
        private GlProgram mProgram;
        private ByteBuffer mVertices;
        private Size mPreviewSize;
        private int mCameraOrientation;
        private final FloatBuffer mBufferLensMap;
        private final float[] mBufferLensMapArray = new float[4 * 64 * 64];
        private int mBufferLensMapColumns;
        private int mBufferLensMapRows;

        public BasicCameraRenderer() {
            mBufferLensMap = FloatBuffer.wrap(mBufferLensMapArray);
        }

        @Override
        public void onSurfaceCreated() {
            mTextureRaw = new GlTexture();
            mTextureRaw.bind(GLES30.GL_TEXTURE_2D);
            mTextureRaw.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            mTextureRaw.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            mTextureRaw.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            mTextureRaw.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            mTextureRaw.unbind(GLES30.GL_TEXTURE_2D);

            mTextureLensMap = new GlTexture();
            mTextureLensMap.bind(GLES30.GL_TEXTURE_2D);
            mTextureLensMap.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            mTextureLensMap.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            mTextureLensMap.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            mTextureLensMap.parameter(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            mTextureLensMap.unbind(GLES30.GL_TEXTURE_2D);

            final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
            mVertices = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
            mVertices.put(VERTICES).position(0);

            mProgram = new GlProgram(VS_SOURCE, FS_SOURCE, null);
            mProgram.useProgram();
            GLES30.glUniform1i(mProgram.getUniformLocation("sTextureRaw"), 0);
            GLES30.glUniform1i(mProgram.getUniformLocation("sTextureLensMap"), 1);
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            mSurfaceSize.set(width, height);
        }

        @Override
        public void onRenderFrame() {
            if (mImageReader != null) {
                Image image = mImageReader.acquireLatestImage();
                if (image != null) {
                    mTextureRaw.bind(GLES30.GL_TEXTURE_2D);
                    mTextureRaw.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, image.getWidth(), image.getHeight(), 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, image.getPlanes()[0].getBuffer());
                    mTextureRaw.unbind(GLES30.GL_TEXTURE_2D);
                    mProgram.useProgram();
                    GLES30.glUniform2f(mProgram.getUniformLocation("uTextureRawSize"), image.getWidth(), image.getHeight());
                    image.close();

                    mBufferLensMap.position(0);
                    mTextureLensMap.bind(GLES30.GL_TEXTURE_2D);
                    mTextureLensMap.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, mBufferLensMapColumns, mBufferLensMapRows, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, mBufferLensMap);
                    mTextureLensMap.unbind(GLES30.GL_TEXTURE_2D);
                }

                mProgram.useProgram();

                float aspectSurface = (float) mSurfaceSize.x / mSurfaceSize.y;
                float aspectPreview = (float) mPreviewSize.getWidth() / mPreviewSize.getHeight();
                aspectPreview = mCameraOrientation % 180 == 0 ? aspectPreview : 1.0f / aspectPreview;
                float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
                float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

                GLES30.glUniform2f(mProgram.getUniformLocation("uAspectRatio"), aspectX, aspectY);
                GLES30.glUniformMatrix4fv(mProgram.getUniformLocation("uOrientationMatrix"), 1, false, mOrientationMatrix, 0);

                GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, mVertices);
                GLES30.glEnableVertexAttribArray(0);

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                mTextureRaw.bind(GLES30.GL_TEXTURE_2D);
                GLES30.glUniform1i(mProgram.getUniformLocation("sTextureRaw"), 0);

                GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                mTextureLensMap.bind(GLES30.GL_TEXTURE_2D);
                GLES30.glUniform1i(mProgram.getUniformLocation("sTextureLensMap"), 1);

                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

                GLES30.glDisableVertexAttribArray(0);

                mTextureRaw.unbind(GLES30.GL_TEXTURE_2D);
                mTextureLensMap.unbind(GLES30.GL_TEXTURE_2D);
            }
        }

        @Override
        public void onSurfaceReleased() {
        }

        public void setOrientation(int orientation) {
            mCameraOrientation = orientation;
            Matrix.setRotateM(mOrientationMatrix, 0, orientation, 0, 0, -1);
        }

        public void setPreviewSize(Size previewSize) {
            mPreviewSize = previewSize;
        }

        public void setLensShadingMap(LensShadingMap lensShadingMap) {
            mBufferLensMapColumns = lensShadingMap.getColumnCount();
            mBufferLensMapRows = lensShadingMap.getRowCount();
            lensShadingMap.copyGainFactors(mBufferLensMapArray, 0);
        }

    }

}
