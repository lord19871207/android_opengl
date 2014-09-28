package fi.harism.app.opengl3x.list;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.renderer.RendererFragment;
import fi.harism.app.opengl3x.renderer.test.RandRendererFragment;

public class ListFragment extends Fragment {

    private ArrayList<RendererFragment> rendererFragments;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, null);

        rendererFragments = new ArrayList<>();
        rendererFragments.add(new RandRendererFragment());
        rendererFragments.add(new RandRendererFragment());
        rendererFragments.add(new RandRendererFragment());
        rendererFragments.add(new RandRendererFragment());
        rendererFragments.add(new RandRendererFragment());

        RendererFragmentAdapter baseAdapter = new RendererFragmentAdapter();
        SectionedAdapter adapter = new SectionedAdapter(getActivity(), R.layout.container_recyclerview_section, R.id.textview, baseAdapter);

        SectionedAdapter.Section[] sections = {
                new SectionedAdapter.Section(0, R.string.section_tests),
                new SectionedAdapter.Section(rendererFragments.size(), -1),
        };

        adapter.setSections(sections);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    private class RendererFragmentViewHolder extends RecyclerView.ViewHolder {

        private TextView titleTextView;
        private TextView captionTextView;
        private RendererFragment rendererFragment;

        public RendererFragmentViewHolder(View itemView) {
            super(itemView);
            titleTextView = (TextView) itemView.findViewById(R.id.textview_title);
            captionTextView = (TextView) itemView.findViewById(R.id.textview_caption);
            itemView.findViewById(R.id.view_clickable).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Intent intent = new Intent(getActivity(), mRenderActivity.getClass());
                    //startActivity(intent);
                }
            });
        }

        public void setRendererFragment(RendererFragment rendererFragment) {
            this.rendererFragment = rendererFragment;
            titleTextView.setText(rendererFragment.getTitleStringId());
            captionTextView.setText(rendererFragment.getCaptionStringId());
        }

    }

    private class RendererFragmentAdapter extends RecyclerView.Adapter<RendererFragmentViewHolder> {

        @Override
        public RendererFragmentViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.container_recyclerview_item, viewGroup, false);
            return new RendererFragmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RendererFragmentViewHolder holder, int position) {
            holder.setRendererFragment(rendererFragments.get(position));
        }

        @Override
        public int getItemCount() {
            return rendererFragments.size();
        }
    }

}
