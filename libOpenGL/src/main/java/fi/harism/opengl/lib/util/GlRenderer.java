package fi.harism.opengl.lib.util;

public interface GlRenderer {

    public void onCreate();

    public void onRelease();

    public void onSizeChanged(int width, int height);

    public void onRender();

}
