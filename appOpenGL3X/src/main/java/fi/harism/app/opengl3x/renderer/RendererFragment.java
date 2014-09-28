package fi.harism.app.opengl3x.renderer;

import android.app.Fragment;
import android.opengl.GLES30;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.util.GlRenderer;
import fi.harism.lib.opengl.view.GlTextureView;

public abstract class RendererFragment extends Fragment implements GlRenderer {

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            glTextureView.renderFrame(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    private Choreographer choreographer;
    private GlTextureView glTextureView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        choreographer = Choreographer.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_renderer, null);
        glTextureView = (GlTextureView) view.findViewById(R.id.textureview);
        glTextureView.setEglContext(EglCore.VERSION_GLES3, 0);
        glTextureView.setGlRenderer(this);
        return view;
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

    public abstract int getTitleStringId();

    public abstract int getCaptionStringId();

}
