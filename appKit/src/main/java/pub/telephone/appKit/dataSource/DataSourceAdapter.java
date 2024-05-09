package pub.telephone.appKit.dataSource;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DataSourceAdapter<
        VH extends DataViewHolder<?>,
        T extends DataNode<VH>
        > extends RecyclerView.Adapter<VH> implements DataAdapter<VH, T> {
    protected final WeakReference<LifecycleOwner> lifecycleOwner;
    public final DataSource<VH, T> Source;

    @NonNull
    @Override
    public final DataSource<VH, T> getSource() {
        return Source;
    }

    @NonNull
    @Override
    public final WeakReference<LifecycleOwner> getLifecycleOwner() {
        return lifecycleOwner;
    }

    public DataSourceAdapter(@NotNull WeakReference<LifecycleOwner> lifecycleOwner, @NotNull View v) {
        this(new DataSource.DataSourceParameters(v, lifecycleOwner));
    }

    public DataSourceAdapter(@NotNull DataSource.DataSourceParameters parameters) {
        this.lifecycleOwner = parameters.state.lifecycleOwner;
        this.Source = new DataSource<>(parameters, this);
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

    @Override
    public int getItemViewType(@NonNull T node) {
        return 0;
    }

    @Override
    public final int getItemViewType(int position) {
        return getItemViewType(Source.Get(position));
    }

    @Override
    public final void onViewRecycled(@NonNull VH holder) {
        int pos = holder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) {
            return;
        }
        Source.Get(pos).cancel_ui();
    }
}
