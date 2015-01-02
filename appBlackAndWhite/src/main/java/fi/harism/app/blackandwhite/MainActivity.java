package fi.harism.app.blackandwhite;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class MainActivity extends FragmentActivity {

    private ViewPager viewPager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(0);
        viewPager.setAdapter(new RendererAdapter(getSupportFragmentManager()));
        viewPager.setAlpha(0f);
        viewPager.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(500)
                .start();
    }

    private class RendererAdapter extends FragmentPagerAdapter {

        RendererAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public Fragment getItem(int position) {
            return RendererFragment.create(position + 1);
        }

    }

}
