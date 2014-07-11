package fi.harism.opengl.app.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
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
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import fi.harism.opengl.app.R;
import fi.harism.opengl.app.RenderActivity;
import fi.harism.opengl.lib.egl.EglCore;
import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.util.GlRenderer;
import fi.harism.opengl.lib.view.GlTextureView;

public class BasicCameraRenderActivity extends RenderActivity {

    private static final String TAG = "BasicCameraRenderActivity";

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    private GlTextureView mGlTextureView;
    private BasicCameraRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRenderer = new BasicCameraRenderer();
        mGlTextureView = new GlTextureView(this);
        mGlTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        mGlTextureView.setGlRenderer(mRenderer);
        setContentView(mGlTextureView);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGlTextureView.onDestroy();
    }

    @Override
    public int getRendererTitleId() {
        return R.string.camera2_basic_renderer_title;
    }

    @Override
    public int getRendererCaptionId() {
        return R.string.camera2_basic_renderer_caption;
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
        if (mCameraDevice == null || mRenderer.getSurface() == null) {
            return;
        }
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraDevice.getId());
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSizes[] = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);


            Log.e(TAG, "SUPPORTED ?  " + streamConfigurationMap.isOutputSupportedFor(ImageFormat.RAW_SENSOR));


            mRenderer.setOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            mRenderer.setSurfaceTextureSize(previewSizes[0]);
            mCameraDevice.createCaptureSession(Arrays.asList(mRenderer.getSurface()), new CameraCaptureSession.StateListener() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        captureRequestBuilder.addTarget(mRenderer.getSurface());

                        HandlerThread thread = new HandlerThread("CameraPreview");
                        thread.start();
                        Handler backgroundHandler = new Handler(thread.getLooper());

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

    private class BasicCameraRenderer implements GlRenderer, SurfaceTexture.OnFrameAvailableListener {

        private static final String VS_SOURCE = "" +
                "uniform vec2 uAspectRatio;\n" +
                "uniform mat4 uFrameMatrix;\n" +
                "uniform mat4 uOrientationMatrix;\n" +
                "attribute vec4 position;\n" +
                "varying vec2 vTexPosition;\n" +
                "void main() {\n" +
                "  gl_Position = uOrientationMatrix * position;\n" +
                "  gl_Position /= gl_Position.w;\n" +
                "  vec4 texPosition = uFrameMatrix * vec4(gl_Position.xy * 0.5 + 0.5, 0.0, 1.0);\n" +
                "  gl_Position.xy *= uAspectRatio;\n" +
                "  vTexPosition = texPosition.xy / texPosition.w;\n" +
                "}";

        private static final String FS_SOURCE = "" +
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "varying vec2 vTexPosition;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTexPosition);\n" +
                "}";

        private final float mFrameMatrix[] = new float[16];
        private final float mOrientationMatrix[] = new float[16];
        private GlTexture mTexture;
        private SurfaceTexture mSurfaceTexture;
        private Surface mSurface;
        private GlProgram mProgram;
        private ByteBuffer mVertices;
        private boolean mFrameAvailable = false;
        private final Point mSurfaceSize = new Point();
        private Size mPreviewSize;
        private int mCameraOrientation;

        @Override
        public void onSurfaceCreated() {
            mTexture = new GlTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            mSurfaceTexture = new SurfaceTexture(mTexture.getTexture());
            mSurfaceTexture.setOnFrameAvailableListener(this);
            mSurface = new Surface(mSurfaceTexture);

            final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
            mVertices = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
            mVertices.put(VERTICES).position(0);

            mProgram = new GlProgram(VS_SOURCE, FS_SOURCE, null);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            });
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            mSurfaceSize.set(width, height);
        }

        @Override
        public void onRenderFrame() {
            if (mFrameAvailable) {
                mFrameAvailable = false;
                mSurfaceTexture.getTransformMatrix(mFrameMatrix);
                mSurfaceTexture.updateTexImage();

                mProgram.useProgram();

                float aspectSurface = (float) mSurfaceSize.x / mSurfaceSize.y;
                float aspectPreview = (float) mPreviewSize.getWidth() / mPreviewSize.getHeight();
                aspectPreview = mCameraOrientation % 180 == 0 ? aspectPreview : 1.0f / aspectPreview;
                float aspectX = Math.max(aspectPreview / aspectSurface, 1.0f);
                float aspectY = Math.max(aspectSurface / aspectPreview, 1.0f);

                GLES30.glUniform2f(mProgram.getUniformLocation("uAspectRatio"), aspectX, aspectY);
                GLES30.glUniformMatrix4fv(mProgram.getUniformLocation("uFrameMatrix"), 1, false, mFrameMatrix, 0);
                GLES30.glUniformMatrix4fv(mProgram.getUniformLocation("uOrientationMatrix"), 1, false, mOrientationMatrix, 0);

                GLES30.glVertexAttribPointer(mProgram.getAttribLocation("position"), 2, GLES30.GL_BYTE, false, 0, mVertices);
                GLES30.glEnableVertexAttribArray(mProgram.getAttribLocation("position"));

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                mTexture.bind();

                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

                GLES30.glDisableVertexAttribArray(mProgram.getAttribLocation("position"));

                mTexture.unbind();
            }
        }

        @Override
        public void onSurfaceReleased() {
            mSurface.release();
            mSurfaceTexture.release();
            mSurface = null;
            mSurfaceTexture = null;
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mFrameAvailable = true;
            mGlTextureView.renderFrame(surfaceTexture.getTimestamp());
        }

        public Surface getSurface() {
            return mSurface;
        }

        public void setOrientation(int orientation) {
            mCameraOrientation = orientation;
            Matrix.setRotateM(mOrientationMatrix, 0, orientation, 0, 0, -1);
        }

        public void setSurfaceTextureSize(Size previewSize) {
            mPreviewSize = previewSize;
            mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        }

    }

}
