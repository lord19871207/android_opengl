package fi.harism.app.opengl3x.fragment.deferred;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.opengl.Visibility;
import android.os.SystemClock;

import java.util.ArrayList;

import fi.harism.app.opengl3x.MainApplication;
import fi.harism.app.opengl3x.fragment.RendererFragment;
import fi.harism.lib.opengl.model.GlCamera;
import fi.harism.lib.opengl.model.GlObject;
import fi.harism.lib.opengl.model.GlObjectData;

abstract class DeferredRendererFragment extends RendererFragment {

    private ArrayList<Model> modelArray;
    private GlCamera glCamera;

    private float modelMatrix[] = new float[16];
    private float modelViewMatrix[] = new float[16];
    private float modelViewProjMatrix[] = new float[16];

    private int cullResult[] = new int[1];

    protected void prepareScene() {
        GlObject objO = createGlObject("letter_o");
        GlObject objP = createGlObject("letter_p");
        GlObject objE = createGlObject("letter_e");
        GlObject objN = createGlObject("letter_n");
        GlObject objG = createGlObject("letter_g");
        GlObject objL = createGlObject("letter_l");
        GlObject objS = createGlObject("letter_s");
        GlObject obj3 = createGlObject("letter_3");
        GlObject objX = createGlObject("letter_x");

        modelArray = new ArrayList<>();
        modelArray.add(new Model(0.0f, 0, 0, objO));
        modelArray.add(new Model(2.0f, 0, 0, objP));
        modelArray.add(new Model(4.0f, 0, 0, objE));
        modelArray.add(new Model(6.0f, 0, 0, objN));
        modelArray.add(new Model(8.0f, 0, 0, objG));
        modelArray.add(new Model(9.5f, 0, 0, objL));
        modelArray.add(new Model(11.8f, 0, 0, objE));
        modelArray.add(new Model(13.8f, 0, 0, objS));
        modelArray.add(new Model(17.0f, 0, 0, obj3));
        modelArray.add(new Model(19.0f, 0, 0, objX));
    }

    protected void prepareCamera(int width, int height) {
        glCamera = new GlCamera().setPerspective(width, height, 60f, 1f, 100f);
    }

    protected void renderScene(int uModelView, int uModelViewProj) {
        float t = SystemClock.uptimeMillis() % 20000 / 20000f;
        float x = (float) (Math.sin(t * Math.PI * 2.0) * 7.0) + 7.0f;
        float z = (float) (Math.cos(t * Math.PI * 2.0) * 2.0) + 3f;
        glCamera.setPosition(new float[]{x, 0f, z});
        glCamera.setDirection(new float[]{x, 0f, 0f});
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);
        for (Model model : modelArray) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(modelMatrix, 0, 30f, 0, 1, 0);
            Matrix.translateM(modelMatrix, 0, model.x(), model.y(), model.z());
            Matrix.scaleM(modelMatrix, 0, 3f, 3f, 3f);
            Matrix.multiplyMM(modelViewMatrix, 0, glCamera.viewMatrix(), 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjMatrix, 0, glCamera.projMatrix(), 0, modelViewMatrix, 0);

            if (Visibility.frustumCullSpheres(modelViewProjMatrix, 0,
                    model.glObject().bsphere(), 0, 1,
                    cullResult, 0, 1) == 0) {
                continue;
            }

            GLES30.glUniformMatrix4fv(uModelView, 1, false, modelViewMatrix, 0);
            GLES30.glUniformMatrix4fv(uModelViewProj, 1, false, modelViewProjMatrix, 0);

            model.glObject().vertexBuffer().bind(GLES30.GL_ARRAY_BUFFER);
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0);
            model.glObject().vertexBuffer().unbind(GLES30.GL_ARRAY_BUFFER);

            model.glObject().normalBuffer().bind(GLES30.GL_ARRAY_BUFFER);
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0);
            model.glObject().normalBuffer().unbind(GLES30.GL_ARRAY_BUFFER);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, model.glObject.vertexCount());
        }
        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    private GlObject createGlObject(String key) {
        MainApplication app = (MainApplication) getActivity().getApplication();
        GlObjectData objData = app.getObjectData(key);
        return new GlObject(objData.vertexCount(),
                objData.vertexBuffer(), objData.normalBuffer(), null);
    }

    private class Model {
        private final float x;
        private final float y;
        private final float z;
        private final GlObject glObject;

        public Model(float x, float y, float z, GlObject glObject) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.glObject = glObject;
        }

        public float x() {
            return x;
        }

        public float y() {
            return y;
        }

        public float z() {
            return z;
        }

        public GlObject glObject() {
            return glObject;
        }

    }

}
