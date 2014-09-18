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
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import fi.harism.opengl.app.R;
import fi.harism.opengl.app.RenderActivity;
import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.gl.GlFramebuffer;
import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlSampler;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.gl.GlUtils;
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

        private final float mFrameMatrix[] = new float[16];
        private final float mOrientationMatrix[] = new float[16];
        private final Point mSurfaceSize = new Point();
        private GlTexture mTextureRaw;
        private GlTexture mTextureRgb;
        private GlTexture mTextureLensMap;
        private GlSampler mSamplerNearest;
        private GlFramebuffer mFramebufferRgb;
        private GlProgram mProgramConvert;
        private GlProgram mProgramCopy;
        private ByteBuffer mQuadVertices;
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
            mTextureRgb = new GlTexture().bind(GLES30.GL_TEXTURE_2D);
            mTextureRgb.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, 2048, 2048, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
            mTextureRgb.unbind(GLES30.GL_TEXTURE_2D);
            mTextureLensMap = new GlTexture();

            mSamplerNearest = new GlSampler();
            mSamplerNearest.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            mSamplerNearest.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            mSamplerNearest.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            mSamplerNearest.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            mFramebufferRgb = new GlFramebuffer().bind(GLES30.GL_FRAMEBUFFER);
            mFramebufferRgb.texture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mTextureRgb.getTexture(), 0);
            final int[] ATTACHMENTS = {GLES30.GL_COLOR_ATTACHMENT0};
            GLES30.glDrawBuffers(1, ATTACHMENTS, 0);
            if (mFramebufferRgb.checkStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RawCameraRenderActivity.this, "Framebuffer create error.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            mFramebufferRgb.unbind(GLES30.GL_FRAMEBUFFER);

            final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
            mQuadVertices = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
            mQuadVertices.put(VERTICES).position(0);

            try {
                final String CONVERT_VS = GlUtils.loadString(RawCameraRenderActivity.this, "shaders/camera2raw/convert_vs.txt");
                final String CONVERT_FS = GlUtils.loadString(RawCameraRenderActivity.this, "shaders/camera2raw/convert_fs.txt");
                mProgramConvert = new GlProgram(CONVERT_VS, CONVERT_FS, null).useProgram();
                GLES30.glUniform1i(mProgramConvert.getUniformLocation("sTextureRaw"), 0);
                GLES30.glUniform1i(mProgramConvert.getUniformLocation("sTextureLensMap"), 1);

                final String COPY_VS = GlUtils.loadString(RawCameraRenderActivity.this, "shaders/camera2raw/copy_vs.txt");
                final String COPY_FS = GlUtils.loadString(RawCameraRenderActivity.this, "shaders/camera2raw/copy_fs.txt");
                mProgramCopy = new GlProgram(COPY_VS, COPY_FS, null).useProgram();
                GLES30.glUniform1i(mProgramCopy.getUniformLocation("sTextureRgb"), 0);

                startPreview();
            } catch (final Exception ex) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RawCameraRenderActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
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
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    mTextureRaw.bind(GLES30.GL_TEXTURE_2D);
                    mTextureRaw.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, imageWidth, imageHeight, 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, image.getPlanes()[0].getBuffer());
                    mTextureRaw.unbind(GLES30.GL_TEXTURE_2D);
                    image.close();

                    mBufferLensMap.position(0);
                    mTextureLensMap.bind(GLES30.GL_TEXTURE_2D);
                    mTextureLensMap.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, mBufferLensMapColumns, mBufferLensMapRows, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, mBufferLensMap);
                    mTextureLensMap.unbind(GLES30.GL_TEXTURE_2D);

                    mFramebufferRgb.bind(GLES30.GL_FRAMEBUFFER);
                    GLES30.glViewport(0, 0, 2048, 2048);
                    mProgramConvert.useProgram();
                    GLES30.glUniform2f(mProgramConvert.getUniformLocation("uTextureRawSize"), imageWidth, imageHeight);
                    GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, mQuadVertices);
                    GLES30.glEnableVertexAttribArray(0);
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                    mTextureRaw.bind(GLES30.GL_TEXTURE_2D);
                    mSamplerNearest.bind(0);
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                    mTextureLensMap.bind(GLES30.GL_TEXTURE_2D);
                    mSamplerNearest.bind(1);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
                    GLES30.glDisableVertexAttribArray(0);
                    mTextureRaw.unbind(GLES30.GL_TEXTURE_2D);
                    mTextureLensMap.unbind(GLES30.GL_TEXTURE_2D);
                    mFramebufferRgb.unbind(GLES30.GL_FRAMEBUFFER);
                    GLES30.glViewport(0, 0, mSurfaceSize.x, mSurfaceSize.y);
                }

                float aspectSurface = (float) mSurfaceSize.x / mSurfaceSize.y;
                float aspectPreview = (float) mPreviewSize.getWidth() / mPreviewSize.getHeight();
                aspectPreview = mCameraOrientation % 180 == 0 ? aspectPreview : 1.0f / aspectPreview;
                float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
                float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

                mProgramCopy.useProgram();
                GLES30.glUniform2f(mProgramCopy.getUniformLocation("uAspectRatio"), aspectX, aspectY);
                GLES30.glUniformMatrix4fv(mProgramCopy.getUniformLocation("uFrameMatrix"), 1, false, mFrameMatrix, 0);
                GLES30.glUniformMatrix4fv(mProgramCopy.getUniformLocation("uOrientationMatrix"), 1, false, mOrientationMatrix, 0);
                GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, mQuadVertices);
                GLES30.glEnableVertexAttribArray(0);
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                mTextureRgb.bind(GLES30.GL_TEXTURE_2D);
                mSamplerNearest.bind(0);
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
                GLES30.glDisableVertexAttribArray(0);
                mTextureRgb.unbind(GLES30.GL_TEXTURE_2D);
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
