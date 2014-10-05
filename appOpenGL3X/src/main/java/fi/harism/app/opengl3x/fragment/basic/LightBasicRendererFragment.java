package fi.harism.app.opengl3x.fragment.basic;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlUtils;

public class LightBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.light";
    private static final String PREFERENCE_AMBIENT = "renderer.basic.light.ambient";
    private static final String PREFERENCE_DIFFUSE = "renderer.basic.light.diffuse";
    private static final String PREFERENCE_SPECULAR = "renderer.basic.light.specular";
    private static final String PREFERENCE_SHININESS = "renderer.basic.light.shininess";
    private static final int DEFAULT_AMBIENT = 2;
    private static final int DEFAULT_DIFFUSE = 3;
    private static final int DEFAULT_SPECULAR = 3;
    private static final int DEFAULT_SHININESS = 8;

    private GlProgram glProgram;

    private long lastRenderTime;

    private final float material[] = new float[4];

    private final float projectionMatrix[] = new float[16];
    private final float lookAtMatrix[] = new float[16];
    private final float rotationMatrix[] = new float[16];
    private final float modelViewProjectionMatrix[] = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEglFlags(EglCore.FLAG_DEPTH_BUFFER);

        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        material[0] = prefs.getInt(PREFERENCE_AMBIENT, DEFAULT_AMBIENT) / 10f;
        material[1] = prefs.getInt(PREFERENCE_DIFFUSE, DEFAULT_DIFFUSE) / 10f;
        material[2] = prefs.getInt(PREFERENCE_SPECULAR, DEFAULT_SPECULAR) / 10f;
        material[3] = prefs.getInt(PREFERENCE_SHININESS, DEFAULT_SHININESS) + 1f;
    }

    @Override
    public Fragment getSettingsFragment() {
        return new SettingsFragment();
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_basic_light_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_basic_light_caption;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        try {
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/light/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/light/shader_fs.txt"),
                    null);
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
        float aspect = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 60.0f, aspect, 1f, 100f);
        Matrix.setLookAtM(lookAtMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.setIdentityM(rotationMatrix, 0);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 10f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMatrix, 0, diff, 1f, 1.5f, 0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, lookAtMatrix, 0, rotationMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewMatrix"), 1, false, modelViewProjectionMatrix, 0);

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewProjectionMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);

        GLES30.glUniform4fv(glProgram.getUniformLocation("uMaterial"), 1, material, 0);
        renderCubeFilled();
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void onEvent(SettingsEvent event) {
        material[0] = event.getAmbient() / 10f;
        material[1] = event.getDiffuse() / 10f;
        material[2] = event.getSpecular() / 10f;
        material[3] = event.getShininess() + 1f;
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                postFilterValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveFilterValues();
            }
        };

        private SeekBar seekBarAmbient;
        private SeekBar seekBarDiffuse;
        private SeekBar seekBarSpecular;
        private SeekBar seekBarShininess;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_basic_light, container, false);

            seekBarAmbient = (SeekBar) view.findViewById(R.id.seekbar_ambient);
            seekBarDiffuse = (SeekBar) view.findViewById(R.id.seekbar_diffuse);
            seekBarSpecular = (SeekBar) view.findViewById(R.id.seekbar_specular);
            seekBarShininess = (SeekBar) view.findViewById(R.id.seekbar_shininess);

            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            seekBarAmbient.setProgress(prefs.getInt(PREFERENCE_AMBIENT, DEFAULT_AMBIENT));
            seekBarDiffuse.setProgress(prefs.getInt(PREFERENCE_DIFFUSE, DEFAULT_DIFFUSE));
            seekBarSpecular.setProgress(prefs.getInt(PREFERENCE_SPECULAR, DEFAULT_SPECULAR));
            seekBarShininess.setProgress(prefs.getInt(PREFERENCE_SHININESS, DEFAULT_SHININESS));

            seekBarAmbient.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarDiffuse.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarSpecular.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarShininess.setOnSeekBarChangeListener(seekBarChangeListener);

            return view;
        }

        private void postFilterValues() {
            int ambient = seekBarAmbient.getProgress();
            int diffuse = seekBarDiffuse.getProgress();
            int specular = seekBarSpecular.getProgress();
            int shininess = seekBarShininess.getProgress();
            EventBus.getDefault().post(
                    new SettingsEvent(ambient, diffuse, specular, shininess));
        }

        private void saveFilterValues() {
            SharedPreferences.Editor editor =
                    getActivity().getPreferences(Context.MODE_PRIVATE).edit();
            editor.putInt(PREFERENCE_AMBIENT, seekBarAmbient.getProgress());
            editor.putInt(PREFERENCE_DIFFUSE, seekBarDiffuse.getProgress());
            editor.putInt(PREFERENCE_SPECULAR, seekBarSpecular.getProgress());
            editor.putInt(PREFERENCE_SHININESS, seekBarShininess.getProgress());
            editor.commit();
        }

    }

    public static class SettingsEvent {

        private int ambient;
        private int diffuse;
        private int specular;
        private int shininess;

        public SettingsEvent(int ambient, int diffuse, int specular, int shininess) {
            this.ambient = ambient;
            this.diffuse = diffuse;
            this.specular = specular;
            this.shininess = shininess;
        }

        public int getAmbient() {
            return ambient;
        }

        public int getDiffuse() {
            return diffuse;
        }

        public int getSpecular() {
            return specular;
        }

        public int getShininess() {
            return shininess;
        }

    }
}
