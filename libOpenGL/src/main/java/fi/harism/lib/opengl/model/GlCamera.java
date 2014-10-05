package fi.harism.lib.opengl.model;

import android.opengl.Matrix;

public class GlCamera {

    private final float[] perspectiveM = new float[16];
    private final float[] lookAtM = new float[16];
    private final float[] multipliedM = new float[16];
    private final float[] lookAtV = new float[3];
    private boolean needsMultiply = true;

    private float sensorHeight;
    private float sensorWidth;
    private float focalLength;
    private float planeInFocus;
    private float apertureDiameter;

    public GlCamera() {
        sensorHeight = 0.024f;
    }

    public void setPerspectiveM(int width, int height, float fov, float zNear, float zFar) {
        float aspect = (float) width / height;
        Matrix.perspectiveM(perspectiveM, 0, fov, aspect, zNear, zFar);
        needsMultiply = true;

        sensorWidth = sensorHeight * aspect;
        focalLength = (float) ((0.5f * sensorWidth) / Math.tan(Math.PI * fov / 360));
    }

    public float[] getPerspectiveM() {
        return perspectiveM;
    }

    public void setLookAtM(float posX, float posY, float posZ, float lookAtX, float lookAtY, float lookAtZ, float upX, float upY, float upZ) {
        lookAtV[0] = posX;
        lookAtV[1] = posY;
        lookAtV[2] = posZ;
        Matrix.setLookAtM(lookAtM, 0, posX, posY, posZ, lookAtX, lookAtY, lookAtZ, upX, upY, upZ);
        needsMultiply = true;
    }

    public float[] getLookAtM() {
        return lookAtM;
    }

    public float[] getLookAtV() {
        return lookAtV;
    }

    public float[] getMultipliedM() {
        if (needsMultiply) {
            needsMultiply = false;
            Matrix.multiplyMM(multipliedM, 0, perspectiveM, 0, lookAtM, 0);
        }
        return multipliedM;
    }

    public float getFocalLength() {
        return focalLength;
    }

    public float getPlaneInFocus() {
        return planeInFocus;
    }

    public void setPlaneInFocus(float planeInFocus) {
        this.planeInFocus = planeInFocus;
    }

    public float getApertureDiameter() {
        return apertureDiameter;
    }

    public void setApertureDiameter(float apertureDiameter) {
        this.apertureDiameter = apertureDiameter;
    }

    public float getSensorHeight() {
        return sensorHeight;
    }

    public float getSensorWidth() {
        return sensorWidth;
    }

}
