package fi.harism.app.opengl3x.fragment.deferred;

import android.opengl.GLES30;
import android.os.Bundle;
import android.widget.Toast;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlUtils;

public class BasicDeferredRendererFragment extends DeferredRendererFragment {

    private static final String RENDERER_ID = "renderer.deferred.basic";

    private static final String UNIFORM_NAMES[] = {"uModelViewMatrix", "uModelViewProjMatrix"};
    private final int uniformLocations[] = new int[UNIFORM_NAMES.length];

    private GlProgram glProgram;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEglFlags(EglCore.FLAG_DEPTH_BUFFER);
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_deferred_basic_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_deferred_basic_caption;
    }

    @Override
    public void onSurfaceCreated() {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        prepareScene();

        try {
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/deferred/basic/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/deferred/basic/shader_fs.txt"),
                    null).useProgram();
            glProgram.getUniformIndices(UNIFORM_NAMES, uniformLocations);
            setContinuousRendering(true);
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
        prepareCamera(width, height);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glClearColor(.1f, .1f, .1f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        glProgram.useProgram();
        renderScene(uniformLocations[0], uniformLocations[1]);
    }

    @Override
    public void onSurfaceReleased() {

    }
}
