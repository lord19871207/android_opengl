package fi.harism.lib.opengl.model;

import android.opengl.Matrix;

public class GlCamera {

    private final float[] position = new float[3];
    private final float[] direction = new float[3];

    private final float[] viewMatrix = new float[16];
    private final float[] projMatrix = new float[16];
    private final float[] viewProjMatrix = new float[16];

    private boolean calcViewMatrix;
    private boolean calcViewProjMatrix;

    private float sensorHeight;
    private float sensorWidth;
    private float focalLength;
    private float planeInFocus;
    private float apertureDiameter;

    public GlCamera() {
        sensorHeight = 0.024f;
    }

    public GlCamera setPerspective(int width, int height, float fov, float zNear, float zFar) {
        float aspect = (float) width / height;
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, zNear, zFar);
        calcViewProjMatrix = true;

        sensorWidth = sensorHeight * aspect;
        focalLength = (float) ((0.5f * sensorWidth) / Math.tan(Math.PI * fov / 360));

        return this;
    }

    public GlCamera setPosition(float position[]) {
        System.arraycopy(position, 0, this.position, 0, 3);
        calcViewMatrix = true;
        return this;
    }

    public GlCamera setDirection(float direction[]) {
        System.arraycopy(direction, 0, this.direction, 0, 3);
        calcViewMatrix = true;
        return this;
    }

    public float[] position() {
        return position;
    }

    public float[] direction() {
        return direction;
    }

    public float[] viewMatrix() {
        if (calcViewMatrix) {
            Matrix.setLookAtM(viewMatrix, 0,
                    position[0], position[1], position[2],
                    direction[0], direction[1], direction[2],
                    0f, 1f, 0f);
            calcViewMatrix = false;
        }
        return viewMatrix;
    }

    public float[] projMatrix() {
        return projMatrix;
    }

    public float[] viewProjMatrix() {
        float[] viewMatrix = viewMatrix();
        if (calcViewProjMatrix) {
            Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0);
            calcViewProjMatrix = false;
        }
        return viewProjMatrix;
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
