package fi.harism.lib.opengl.model;

import android.opengl.Matrix;

public class GlLight {

    private final float position[] = new float[3];
    private final float direction[] = new float[3];

    private final float biasMatrix[] = new float[16];
    private final float viewMatrix[] = new float[16];
    private final float projMatrix[] = new float[16];
    private final float viewProjMatrix[] = new float[16];
    private final float shadowMatrix[] = new float[16];

    private boolean calcViewMatrix;
    private boolean calcViewProjMatrix;

    public GlLight() {
        Matrix.setIdentityM(biasMatrix, 0);
        Matrix.translateM(biasMatrix, 0, 0.5f, 0.5f, 0.5f);
        Matrix.scaleM(biasMatrix, 0, 0.5f, 0.5f, 0.5f);
    }

    public GlLight setPerspective(int width, int height, float fovy, float zNear, float zFar) {
        float aspect = (float) width / height;
        Matrix.perspectiveM(projMatrix, 0, fovy, aspect, zNear, zFar);
        calcViewProjMatrix = true;
        return this;
    }

    public GlLight setPosition(float[] position) {
        System.arraycopy(position, 0, this.position, 0, 3);
        calcViewMatrix = true;
        return this;
    }

    public GlLight setDirection(float[] direction) {
        System.arraycopy(direction, 0, this.direction, 0, 3);
        calcViewMatrix = true;
        return this;
    }

    public float[] viewMatrix() {
        if (calcViewMatrix) {
            Matrix.setLookAtM(viewMatrix, 0,
                    position[0], position[1], position[2],
                    direction[0], direction[1], direction[2],
                    0f, 1f, 0f);
            calcViewMatrix = false;
            calcViewProjMatrix = true;
        }
        return viewMatrix;
    }

    public float[] projMatrix() {
        return projMatrix;
    }

    public float[] viewProjMatrix() {
        final float[] viewMatrix = viewMatrix();
        if (calcViewProjMatrix) {
            Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0);
            calcViewProjMatrix = false;
        }
        return viewProjMatrix;
    }

    public float[] shadowMatrix(float viewMatrix[]) {
        Matrix.invertM(shadowMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(shadowMatrix, 0, viewProjMatrix(), 0, shadowMatrix, 0);
        Matrix.multiplyMM(shadowMatrix, 0, biasMatrix, 0, shadowMatrix, 0);
        return shadowMatrix;
    }


}
