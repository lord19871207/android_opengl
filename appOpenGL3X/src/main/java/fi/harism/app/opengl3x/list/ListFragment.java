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
import java.util.Collection;
import java.util.Collections;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.event.GetRendererFragmentEvent;
import fi.harism.app.opengl3x.event.SetRendererFragmentEvent;
import fi.harism.app.opengl3x.renderer.RendererFragment;
import fi.harism.app.opengl3x.renderer.camera2.Camera2BasicRendererFragment;
import fi.harism.app.opengl3x.renderer.camera2.Camera2RawRendererFragment;
import fi.harism.app.opengl3x.renderer.camera2.Camera2YuvRendererFragment;
import fi.harism.app.opengl3x.renderer.test.ClearRendererFragment;

public class ListFragment extends Fragment {

    private int selectedPosition = 0;
    private ArrayList<RendererFragment> rendererFragments;
    private RecyclerView recyclerView;
    private SectionedAdapter recyclerViewAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, null);

        rendererFragments = new ArrayList<>();
        ArrayList<SectionedAdapter.Section> sections = new ArrayList<>();

        sections.add(new SectionedAdapter.Section(rendererFragments.size(), R.string.section_camera2));
        rendererFragments.add(new Camera2BasicRendererFragment());
        rendererFragments.add(new Camera2YuvRendererFragment());
        rendererFragments.add(new Camera2RawRendererFragment());
        sections.add(new SectionedAdapter.Section(rendererFragments.size(), R.string.section_test));
        rendererFragments.add(new ClearRendererFragment());
        sections.add(new SectionedAdapter.Section(rendererFragments.size(), -1));

        RendererFragmentAdapter baseAdapter = new RendererFragmentAdapter();
        recyclerViewAdapter = new SectionedAdapter(getActivity(),
                R.layout.container_recyclerview_section, R.id.textview, baseAdapter);
        recyclerViewAdapter.setSections(
                sections.toArray(new SectionedAdapter.Section[sections.size()]));

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(recyclerViewAdapter);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(GetRendererFragmentEvent event) {
        EventBus.getDefault().post(new SetRendererFragmentEvent(rendererFragments.get(selectedPosition)));
    }

    private void onSelectPosition(int position) {
        if (position != selectedPosition) {
            RendererFragmentViewHolder holder =
                    (RendererFragmentViewHolder) recyclerView.findViewHolderForPosition(
                            recyclerViewAdapter.positionToSectionedPosition(selectedPosition));
            if (holder != null) {
                holder.clickableView.setSelected(false);
            }
        }
        RendererFragmentViewHolder holder =
                (RendererFragmentViewHolder) recyclerView.findViewHolderForPosition(
                        recyclerViewAdapter.positionToSectionedPosition(position));
        if (holder != null) {
            holder.clickableView.setSelected(true);
            selectedPosition = position;
        }
    }

    private class RendererFragmentViewHolder extends RecyclerView.ViewHolder {

        private final View clickableView;
        private final TextView titleTextView;
        private final TextView captionTextView;
        private RendererFragment rendererFragment;
        private int position;

        public RendererFragmentViewHolder(View itemView) {
            super(itemView);
            titleTextView = (TextView) itemView.findViewById(R.id.textview_title);
            captionTextView = (TextView) itemView.findViewById(R.id.textview_caption);
            clickableView = itemView.findViewById(R.id.view_clickable);
            clickableView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (position == selectedPosition) {
                        return;
                    }
                    onSelectPosition(position);
                    EventBus.getDefault().post(new SetRendererFragmentEvent(rendererFragment));
                }
            });
        }

        public void setRendererFragment(int position) {
            this.position = position;
            this.rendererFragment = rendererFragments.get(position);
            titleTextView.setText(rendererFragment.getTitleStringId());
            captionTextView.setText(rendererFragment.getCaptionStringId());
            clickableView.setSelected(position == selectedPosition);
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
            holder.setRendererFragment(position);
        }

        @Override
        public int getItemCount() {
            return rendererFragments.size();
        }
    }

}
