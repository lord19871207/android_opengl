package fi.harism.lib.opengl.util;

public interface GlRenderer {

    public void onSurfaceCreated();

    public void onSurfaceChanged(int width, int height);

    public void onRenderFrame();

    public void onSurfaceReleased();

}
