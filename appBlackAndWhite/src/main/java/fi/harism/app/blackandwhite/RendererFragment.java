package fi.harism.app.blackandwhite;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.util.GlRenderer;
import fi.harism.lib.opengl.view.GlTextureView;

public class RendererFragment extends Fragment {

    private static final String KEY_INDEX = "index";

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            glTextureView.renderFrame(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    private Choreographer choreographer;
    private GlTextureView glTextureView;

    public static RendererFragment create(int index) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_INDEX, index);
        RendererFragment rendererFragment = new RendererFragment();
        rendererFragment.setArguments(bundle);
        return rendererFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        choreographer = Choreographer.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        GlRenderer glRenderer = null;
        switch (getArguments().getInt(KEY_INDEX, 0)) {
            case 1:
                glRenderer = new EffectRenderer1(getActivity());
                break;
            case 2:
                glRenderer = new EffectRenderer2(getActivity());
                break;
            default:
                glRenderer = new EffectRenderer1(getActivity());
        }

        glTextureView = new GlTextureView(getActivity());
        glTextureView.setEglContext(EglCore.VERSION_GLES3, EglCore.FLAG_DEPTH_BUFFER);
        glTextureView.setGlRenderer(glRenderer);
        glTextureView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        return glTextureView;
    }

    @Override
    public void onResume() {
        super.onResume();
        choreographer.postFrameCallback(frameCallback);

    }

    @Override
    public void onPause() {
        super.onPause();
        choreographer.removeFrameCallback(frameCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        glTextureView.onDestroy();
    }

}
