package pub.telephone.appKit.dataSource;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewbinding.ViewBinding;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Set;

public abstract class EmbeddedDataNode<
        CH extends DataViewHolder<?>,
        VH extends EmbeddedDataNode.ViewHolder<?, CH>,
        INFO,
        CD extends DataNode<CH>
        >
        extends DataNode<VH> implements EmbeddedDataNodeAPI.DataNode<CH, INFO, CD> {
    public static abstract class ViewHolder<CT extends ViewBinding, CH extends DataViewHolder<?>>
            extends DataViewHolder<CT> implements EmbeddedDataNodeAPI.ViewHolder<CH> {
        protected abstract @NotNull ViewGroup retrieveContainer();

        public final CH ChildHolder;

        public ViewHolder(
                @NonNull LayoutInflater inflater,
                @Nullable ViewGroup parent,
                @NotNull Class<CT> containerBindingClass,
                @NotNull EmbeddedDataNodeAPI.ViewHolderCreator<CH> embeddedCreator
        ) {
            super(containerBindingClass, inflater, parent);
            //
            ChildHolder = embeddedCreator.createChild(inflater, retrieveContainer());
            //
            retrieveContainer().addView(ChildHolder.itemView);
        }
    }

    protected abstract @NotNull TagKey loadKey();

    final CD childNode;

    public EmbeddedDataNode(
            @Nullable WeakReference<LifecycleOwner> lifecycleOwner,
            @Nullable VH holder,
            @NotNull EmbeddedDataNodeAPI.DataNodeCreator<CH, INFO, CD> embeddedCreator
    ) {
        super(lifecycleOwner, holder);
        //
        childNode = embeddedCreator.createChild(
                lifecycleOwner,
                holder == null ? null : holder.ChildHolder
        );
        load = bindTask(
                loadKey(),
                RetrySharedTask.Simple(embeddedCreator::load)
        );
    }

    final Binding<Object> init = emptyBinding();
    final Binding<INFO> load;

    protected void init_ui(@NotNull VH holder) {
    }

    protected void loading_ui(@NotNull VH holder) {
    }

    protected void loaded_ui(@NotNull VH holder) {
    }

    protected void __bind__(Set<Integer> changedBindingKeys) {
    }

    @Override
    protected final void __Bind__(Set<Integer> changedBindingKeys) {
        init.Bind(
                changedBindingKeys,
                holder -> {
                    init_ui(holder);
                    return null;
                }, null, null
        );
        load.Bind(
                changedBindingKeys,
                holder -> {
                    loading_ui(holder);
                    initChild_ui(lifecycleOwner, holder.ChildHolder);
                    return null;
                },
                (holder, info) -> {
                    loaded_ui(holder);
                    childLoaded_ui(lifecycleOwner, holder.ChildHolder, info);
                    childNode.EmitChange_ui(null);
                    return null;
                },
                null
        );
        __bind__(changedBindingKeys);
    }
}
