package fi.harism.opengl.lib.model;

import android.opengl.Matrix;

public class GlCamera {

    private final float[] mPerspectiveM = new float[16];
    private final float[] mLookAtM = new float[16];
    private final float[] mMultipliedM = new float[16];
    private final float[] mLookAtV = new float[3];
    private boolean mNeedsMultiply = true;

    public void setPerspectiveM(int width, int height, float fov, float zNear, float zFar) {
        float aspect = (float) height / width;
        Matrix.perspectiveM(mPerspectiveM, 0, fov, aspect, zNear, zFar);
        mNeedsMultiply = true;
    }

    public float[] getPerspectiveM() {
        return mPerspectiveM;
    }

    public void setLookAtM(float posX, float posY, float posZ, float lookAtX, float lookAtY, float lookAtZ, float upX, float upY, float upZ) {
        mLookAtM[0] = posX;
        mLookAtM[1] = posY;
        mLookAtM[2] = posZ;
        Matrix.setLookAtM(mLookAtM, 0, posX, posY, posZ, lookAtX, lookAtY, lookAtZ, upX, upY, upZ);
        mNeedsMultiply = true;
    }

    public float[] getLookAtM() {
        return mLookAtM;
    }

    public float[] getLookAtV() {
        return mLookAtV;
    }

    public float[] getMultipliedM() {
        if (mNeedsMultiply) {
            mNeedsMultiply = false;
            Matrix.multiplyMM(mMultipliedM, 0, mPerspectiveM, 0, mLookAtM, 0);
        }
        return mMultipliedM;
    }

}
