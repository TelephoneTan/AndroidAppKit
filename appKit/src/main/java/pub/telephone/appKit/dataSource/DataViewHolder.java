package pub.telephone.appKit.dataSource;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class DataViewHolder<B extends ViewBinding> extends RecyclerView.ViewHolder {
    public final B view;
    public final DataViewBinding<B> binding;

    public DataViewHolder(DataViewBinding<B> binding) {
        super(binding.binding.getRoot());
        this.binding = binding;
        this.view = binding.binding;
    }

    public DataViewHolder(
            @NotNull Class<B> viewBindingClass,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        this(CreateBinding(CreateView(viewBindingClass, inflater, container)));
    }

    protected static <B extends ViewBinding> B CreateView(
            Class<B> viewBindingClass,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        try {
            return (B) viewBindingClass.getMethod(
                    "inflate",
                    LayoutInflater.class,
                    ViewGroup.class,
                    boolean.class
            ).invoke(null, inflater, container, false);
        } catch (
                IllegalAccessException |
                InvocationTargetException |
                NoSuchMethodException e
        ) {
            throw new RuntimeException(e);
        }
    }

    protected static <B extends ViewBinding> DataViewBinding<B> CreateBinding(B view) {
        return new DataViewBinding<>(view);
    }
}
