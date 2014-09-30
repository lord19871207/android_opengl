package fi.harism.app.opengl3x.fragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Comparator;

public class SectionedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int SECTION_TYPE = 0;

    private final Context context;
    private final int sectionResourceId;
    private final int textResourceId;
    private final LayoutInflater layoutInflater;
    private final RecyclerView.Adapter baseAdapter;
    private final SparseArray<Section> sectionArray = new SparseArray<Section>();

    private boolean isValid = true;

    public SectionedAdapter(Context context, int sectionResourceId, int textResourceId,
                            RecyclerView.Adapter adapter) {

        layoutInflater = LayoutInflater.from(context);
        this.sectionResourceId = sectionResourceId;
        this.textResourceId = textResourceId;
        this.baseAdapter = adapter;
        this.context = context;

        baseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                isValid = baseAdapter.getItemCount() > 0;
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                isValid = baseAdapter.getItemCount() > 0;
                notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                isValid = baseAdapter.getItemCount() > 0;
                notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                isValid = baseAdapter.getItemCount() > 0;
                notifyItemRangeRemoved(positionStart, itemCount);
            }
        });
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int typeView) {
        if (typeView == SECTION_TYPE) {
            final View view = layoutInflater.inflate(sectionResourceId, parent, false);
            return new SectionViewHolder(view, textResourceId);
        } else {
            return baseAdapter.onCreateViewHolder(parent, typeView - 1);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder sectionViewHolder, int position) {
        if (isSectionHeaderPosition(position)) {
            ((SectionViewHolder) sectionViewHolder).setText(sectionArray.get(position).title);
        } else {
            baseAdapter.onBindViewHolder(sectionViewHolder, sectionedPositionToPosition(position));
        }

    }

    @Override
    public int getItemViewType(int position) {
        return isSectionHeaderPosition(position)
                ? SECTION_TYPE
                : baseAdapter.getItemViewType(sectionedPositionToPosition(position)) + 1;
    }

    @Override
    public long getItemId(int position) {
        return isSectionHeaderPosition(position)
                ? Integer.MAX_VALUE - sectionArray.indexOfKey(position)
                : baseAdapter.getItemId(sectionedPositionToPosition(position));
    }

    @Override
    public int getItemCount() {
        return (isValid ? baseAdapter.getItemCount() + sectionArray.size() : 0);
    }

    public void setSections(Section[] sections) {
        sectionArray.clear();

        Arrays.sort(sections, new Comparator<Section>() {
            @Override
            public int compare(Section o, Section o1) {
                return (o.firstPosition == o1.firstPosition)
                        ? 0
                        : ((o.firstPosition < o1.firstPosition) ? -1 : 1);
            }
        });

        // offset positions for the headers we're adding
        int offset = 0;
        for (Section section : sections) {
            section.sectionedPosition = section.firstPosition + offset;
            sectionArray.append(section.sectionedPosition, section);
            ++offset;
        }

        notifyDataSetChanged();
    }

    public int positionToSectionedPosition(int position) {
        int offset = 0;
        for (int i = 0; i < sectionArray.size(); i++) {
            if (sectionArray.valueAt(i).firstPosition > position) {
                break;
            }
            ++offset;
        }
        return position + offset;
    }

    public int sectionedPositionToPosition(int sectionedPosition) {
        if (isSectionHeaderPosition(sectionedPosition)) {
            return RecyclerView.NO_POSITION;
        }

        int offset = 0;
        for (int i = 0; i < sectionArray.size(); i++) {
            if (sectionArray.valueAt(i).sectionedPosition > sectionedPosition) {
                break;
            }
            --offset;
        }
        return sectionedPosition + offset;
    }

    public boolean isSectionHeaderPosition(int position) {
        return sectionArray.get(position) != null;
    }

    public static class Section {
        int firstPosition;
        int sectionedPosition;
        int title;

        public Section(int firstPosition, int title) {
            this.firstPosition = firstPosition;
            this.title = title;
        }

        public int getTitle() {
            return title;
        }
    }

    private class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        public SectionViewHolder(View view, int textResourceid) {
            super(view);
            title = (TextView) view.findViewById(textResourceid);
        }

        public void setText(int resId) {
            if (resId > 0) {
                title.setVisibility(View.VISIBLE);
                title.setText(resId);
            } else {
                title.setVisibility(View.GONE);
            }
        }

    }

}
