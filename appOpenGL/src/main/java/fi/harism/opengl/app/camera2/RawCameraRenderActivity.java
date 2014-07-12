package fi.harism.opengl.app.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import fi.harism.opengl.app.R;
import fi.harism.opengl.app.RenderActivity;
import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlSampler;
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
            mImageReader = ImageReader.newInstance(previewSizes[0].getWidth(), previewSizes[0].getHeight(), ImageFormat.RAW_SENSOR, 3);
            mRenderer.setOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            mRenderer.setPreviewSize(previewSizes[0]);
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateListener() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
                        captureRequestBuilder.addTarget(mImageReader.getSurface());

                        HandlerThread thread = new HandlerThread("CameraPreview");
                        thread.start();
                        Handler backgroundHandler = new Handler(thread.getLooper());

                        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                            @Override
                            public void onImageAvailable(ImageReader imageReader) {
                                mGlSurfaceView.renderFrame();
                            }
                        }, backgroundHandler);

                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
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
                "layout (location = 0) in vec4 position;\n" +
                "out vec2 vTexPosition;\n" +
                "void main() {\n" +
                "  gl_Position = uOrientationMatrix * position;\n" +
                "  gl_Position.xy *= uAspectRatio;\n" +
                "  vTexPosition = position.xy * 0.5 + 0.5;\n" +
                "  vTexPosition.y = -vTexPosition.y;\n" +
                "}";

        private static final String FS_SOURCE = "" +
                "#version 300 es\n" +
                "in vec2 vTexPosition;\n" +
                "out vec4 outColor;\n" +
                "uniform usampler2D sTexture;\n" +
                "void main() {\n" +
                "  uint value = texture(sTexture, vTexPosition).r;\n" +
                "  outColor = vec4(value) / 2047.0;\n" +
                "}";

        private final float mFrameMatrix[] = new float[16];
        private final float mOrientationMatrix[] = new float[16];
        private GlTexture mTexture;
        private GlProgram mProgram;
        private GlSampler mSampler;
        private ByteBuffer mVertices;
        private final Point mSurfaceSize = new Point();
        private Size mPreviewSize;
        private int mCameraOrientation;

        @Override
        public void onSurfaceCreated() {
            mTexture = new GlTexture();
            mSampler = new GlSampler();
            mSampler.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            mSampler.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

            final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
            mVertices = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
            mVertices.put(VERTICES).position(0);

            mProgram = new GlProgram(VS_SOURCE, FS_SOURCE, null);
            mProgram.useProgram();
            GLES30.glUniform1i(mProgram.getUniformLocation("sTexture"), 0);
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
                    mTexture.bind(GLES30.GL_TEXTURE_2D);
                    mTexture.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, image.getWidth(), image.getHeight(), 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, image.getPlanes()[0].getBuffer());
                    image.close();
                }

                mProgram.useProgram();

                float aspectSurface = (float) mSurfaceSize.x / mSurfaceSize.y;
                float aspectPreview = (float) mPreviewSize.getWidth() / mPreviewSize.getHeight();
                aspectPreview = mCameraOrientation % 180 == 0 ? aspectPreview : 1.0f / aspectPreview;
                float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
                float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

                GLES30.glUniform2f(mProgram.getUniformLocation("uAspectRatio"), aspectX, aspectY);
                GLES30.glUniformMatrix4fv(mProgram.getUniformLocation("uOrientationMatrix"), 1, false, mOrientationMatrix, 0);

                GLES30.glVertexAttribPointer(mProgram.getAttribLocation("position"), 2, GLES30.GL_BYTE, false, 0, mVertices);
                GLES30.glEnableVertexAttribArray(mProgram.getAttribLocation("position"));

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                mTexture.bind(GLES30.GL_TEXTURE_2D);
                mSampler.bind(0);

                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

                GLES30.glDisableVertexAttribArray(mProgram.getAttribLocation("position"));

                mTexture.unbind(GLES30.GL_TEXTURE_2D);
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

    }

}
