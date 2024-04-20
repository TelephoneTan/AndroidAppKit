package pub.telephone.appKit.dataSource;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DataSourceAdapter<
        VH extends DataViewHolder<?>,
        T extends DataNode<VH>
        > extends RecyclerView.Adapter<VH> {
    public final DataSource<VH, T> Source;

    public DataSourceAdapter(WeakReference<LifecycleOwner> lifecycleOwner, View v) {
        this.Source = new DataSource<>(v, this, lifecycleOwner);
    }

    protected void beforeBindViewHolder_ui(T node) {
    }

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position) {
        T node = Source.Get(position);
        beforeBindViewHolder_ui(node);
        node.bind(holder, null);
    }

    @Override
    public final void onBindViewHolder(
            @NonNull VH holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        Set<Integer> changedBindingKeys = new HashSet<>();
        for (Object payload : payloads) {
            if (payload == null) {
                continue;
            }
            Set<Integer> changedBindingKeysOfOneChange = (Set<Integer>) payload;
            changedBindingKeys.addAll(changedBindingKeysOfOneChange);
        }
        T node = Source.Get(position);
        beforeBindViewHolder_ui(node);
        node.bind(holder, changedBindingKeys);
    }

    @Override
    public final int getItemCount() {
        return Source.Size();
    }

    protected int getItemViewType(T node) {
        return 0;
    }

    @Override
    public final int getItemViewType(int position) {
        return getItemViewType(Source.Get(position));
    }
}
