package fi.harism.app.opengl3x;

import android.app.Application;
import android.os.CountDownTimer;
import android.renderscript.RenderScript;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.event.GetProgressEvent;
import fi.harism.app.opengl3x.event.SetProgressEvent;

public class MainApplication extends Application {

    private RenderScript renderScript;
    private ScriptC_BlurEffect scriptBlurEffect;

    private SetProgressEvent setProgressEvent = new SetProgressEvent(0, 10);

    @Override
    public void onCreate() {
        super.onCreate();
        renderScript = RenderScript.create(this);
        scriptBlurEffect = new ScriptC_BlurEffect(renderScript);
        EventBus.getDefault().register(this);

        CountDownTimer ctd = new CountDownTimer(6000, 500) {
            @Override
            public void onTick(long l) {
                setProgressEvent = new SetProgressEvent((6000 - (int) l) / 500, 6000 / 500);
                EventBus.getDefault().post(setProgressEvent);
            }

            @Override
            public void onFinish() {
                setProgressEvent = new SetProgressEvent(6000/ 500, 6000 / 500);
                EventBus.getDefault().post(setProgressEvent);
            }
        }.start();
    }

    public RenderScript getRenderScript() {
        return renderScript;
    }

    public ScriptC_BlurEffect getBlurEffect() {
        return scriptBlurEffect;
    }

    public void onEvent(GetProgressEvent event) {
        EventBus.getDefault().post(setProgressEvent);
    }

}
