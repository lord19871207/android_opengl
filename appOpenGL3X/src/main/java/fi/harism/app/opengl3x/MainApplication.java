package fi.harism.app.opengl3x;

import android.app.Application;
import android.renderscript.RenderScript;

import fi.harism.app.opengl3x.ScriptC_BlurEffect;

public class MainApplication extends Application {

    private RenderScript renderScript;
    private ScriptC_BlurEffect scriptBlurEffect;

    @Override
    public void onCreate() {
        super.onCreate();
        renderScript = RenderScript.create(this);
        scriptBlurEffect = new ScriptC_BlurEffect(renderScript);
    }

    public RenderScript getRenderScript() {
        return renderScript;
    }

    public ScriptC_BlurEffect getBlurEffect() {
        return scriptBlurEffect;
    }

}
