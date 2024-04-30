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

    public static class DataViewHolderParameters {
        public static class Binding<B extends ViewBinding> {
            @NotNull
            final DataViewBinding<B> binding;

            public Binding(@NotNull DataViewBinding<B> binding) {
                this.binding = binding;
            }
        }

        public static class Inflater {
            @NonNull
            final LayoutInflater inflater;
            @Nullable
            final ViewGroup container;

            public Inflater(
                    @NonNull LayoutInflater inflater,
                    @Nullable ViewGroup container
            ) {
                this.inflater = inflater;
                this.container = container;
            }
        }
    }

    public DataViewHolder(@NotNull DataViewBinding<B> binding) {
        super(binding.binding.getRoot());
        this.binding = binding;
        this.view = binding.binding;
    }

    public DataViewHolder(@NotNull DataViewHolderParameters.Binding<B> parameters) {
        this(parameters.binding);
    }

    public DataViewHolder(
            @NotNull Class<B> viewBindingClass,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        this(CreateBinding(CreateView(viewBindingClass, inflater, container)));
    }

    public DataViewHolder(
            @NotNull DataViewHolderParameters.Inflater parameters,
            @NotNull Class<B> viewBindingClass
    ) {
        this(viewBindingClass, parameters.inflater, parameters.container);
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
