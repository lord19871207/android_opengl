package fi.harism.opengl.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import fi.harism.opengl.app.camera2.BasicCameraRenderActivity;
import fi.harism.opengl.app.test.TestRenderActivity;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private ArrayList<RenderActivity> mRenderActivities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRenderActivities = new ArrayList<>();
        mRenderActivities.add(new TestRenderActivity());
        mRenderActivities.add(new BasicCameraRenderActivity());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RenderActivityAdapter());
    }

    private class RenderActivityViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;
        private TextView mCaption;
        private RenderActivity mRenderActivity;

        public RenderActivityViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.textview_title);
            mCaption = (TextView) itemView.findViewById(R.id.textview_caption);
            itemView.findViewById(R.id.view_clickable).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, mRenderActivity.getClass());
                    startActivity(intent);
                }
            });
        }

        public void setRenderActivity(RenderActivity renderActivity) {
            mRenderActivity = renderActivity;
            mTitle.setText(mRenderActivity.getRendererTitleId());
            mCaption.setText(mRenderActivity.getRendererCaptionId());
        }

    }

    private class RenderActivityAdapter extends RecyclerView.Adapter<RenderActivityViewHolder> {

        @Override
        public RenderActivityViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = getLayoutInflater().inflate(R.layout.view_renderactivity, viewGroup, false);
            return new RenderActivityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RenderActivityViewHolder renderFragmentViewHolder, int position) {
            renderFragmentViewHolder.setRenderActivity(mRenderActivities.get(position));
        }

        @Override
        public int getItemCount() {
            return mRenderActivities.size();
        }
    }

}
