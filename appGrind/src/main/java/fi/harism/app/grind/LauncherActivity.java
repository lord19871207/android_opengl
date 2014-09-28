package fi.harism.app.grind;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

public class LauncherActivity extends Activity {

    private static final String PREFERENCE_RESOLUTION = "PREFERENCE_RESOLUTION";
    private static final String PREFERENCE_SHOW_FPS = "PREFERENCE_SHOW_FPS";
    final int[] RADIOBUTTON_IDS = {R.id.radiobutton_1080p, R.id.radiobutton_720p};

    private RadioGroup radioGroupResolution;
    private CheckBox checkBoxFps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        radioGroupResolution = (RadioGroup) findViewById(R.id.radiogroup_resolution);
        checkBoxFps = (CheckBox) findViewById(R.id.checkbox_fps);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        radioGroupResolution.check(RADIOBUTTON_IDS[prefs.getInt(PREFERENCE_RESOLUTION, 0)]);
        checkBoxFps.setChecked(prefs.getBoolean(PREFERENCE_SHOW_FPS, false));

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                SharedPreferences.Editor prefsEditor = getPreferences(MODE_PRIVATE).edit();

                int resolutionId = radioGroupResolution.getCheckedRadioButtonId();
                for (int i = 0; i < RADIOBUTTON_IDS.length; ++i) {
                    if (RADIOBUTTON_IDS[i] == resolutionId) {
                        prefsEditor.putInt(PREFERENCE_RESOLUTION, i);
                        switch (i) {
                            case 0:
                                intent.putExtra(MainActivity.EXTRA_RESOLUTION_WIDTH, 1920);
                                intent.putExtra(MainActivity.EXTRA_RESOLUTION_HEIGHT, 1080);
                                break;
                            case 1:
                                intent.putExtra(MainActivity.EXTRA_RESOLUTION_WIDTH, 1280);
                                intent.putExtra(MainActivity.EXTRA_RESOLUTION_HEIGHT, 720);
                                break;
                        }
                        break;
                    }
                }
                prefsEditor.putBoolean(PREFERENCE_SHOW_FPS, checkBoxFps.isChecked());
                intent.putExtra(MainActivity.EXTRA_SHOW_FPS, checkBoxFps.isChecked());
                prefsEditor.commit();

                startActivity(intent);
                finish();
            }
        });
    }

}
