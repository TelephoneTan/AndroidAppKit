package pub.telephone.appKit.dataSource;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.jvm.functions.Function1;
import pub.telephone.appKit.MyApp;

public class DataSource<
        VH extends DataViewHolder<?>,
        T extends DataNode<VH>
        > {
    private final List<T> source = new ArrayList<>();
    final @Nullable WeakReference<View> view;
    final DataAdapter<VH, T> adapter;
    protected final WeakReference<LifecycleOwner> lifecycleOwner;

    public static class DataSourceParameters {
        public static class State {
            @NotNull
            final WeakReference<LifecycleOwner> lifecycleOwner;

            public State(@NotNull WeakReference<LifecycleOwner> lifecycleOwner) {
                this.lifecycleOwner = lifecycleOwner;
            }
        }

        @Nullable
        final View view;
        @NotNull
        final State state;

        public DataSourceParameters(
                @NotNull View view,
                @NotNull WeakReference<LifecycleOwner> lifecycleOwner
        ) {
            this.view = view;
            this.state = new State(lifecycleOwner);
        }

        DataSourceParameters(@NotNull State state) {
            this.view = null;
            this.state = state;
        }
    }

    private DataSource(
            @NotNull WeakReference<LifecycleOwner> lifecycleOwner,
            @NotNull DataAdapter<VH, T> adapter,
            @Nullable View view
    ) {
        this.view = view == null ? null : new WeakReference<>(view);
        this.adapter = adapter;
        this.lifecycleOwner = lifecycleOwner;
        //
        if (view != null) {
            MyApp.Companion.post(
                    () -> view.setTag(TagKey.Companion.getDataSource().Key, DataSource.this)
            );
        }
    }

    public DataSource(
            @NotNull View view,
            @NotNull DataAdapter<VH, T> adapter,
            @NotNull WeakReference<LifecycleOwner> lifecycleOwner
    ) {
        this(lifecycleOwner, adapter, view);
    }

    public DataSource(@NotNull DataSourceParameters parameters, @NotNull DataAdapter<VH, T> adapter) {
        this(parameters.state.lifecycleOwner, adapter, parameters.view);
    }

    private void initItem(T item) {
        item.lifecycleOwner = lifecycleOwner;
        item.source = (DataSource<VH, DataNode<VH>>) this;
    }

    public final T Get(int position) {
        return source.get(position);
    }

    public final int Size() {
        return source.size();
    }

    @NonNull
    public final List<T> GetAll() {
        return new ArrayList<>(source);
    }

    static boolean isViewChanging(RecyclerView view, boolean strict) {
        if (view.hasPendingAdapterUpdates()) {
            return true;
        }
        RecyclerView.ItemAnimator itemAnimator = view.getItemAnimator();
        if (strict && itemAnimator != null && itemAnimator.isRunning()) {
            return true;
        }
        return false;
    }

    static boolean isViewChanging(View view, boolean strict) {
        if (view == null) {
            return false;
        }
        if (view instanceof RecyclerView) {
            return isViewChanging((RecyclerView) view, strict);
        }
        return false;
    }

    private void post(Runnable runnable, boolean strict) {
        LifecycleOwner lifecycleOwner = this.lifecycleOwner.get();
        if (lifecycleOwner == null) {
            return;
        }
        if (!lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            return;
        }
        if (this.view != null) {
            View view = this.view.get();
            if (view == null) {
                return;
            }
            MyApp.Companion.post(() -> {
                if (view.getTag(TagKey.Companion.getDataSource().Key) != DataSource.this) {
                    return;
                }
                if (isViewChanging(view, strict)) {
                    post(runnable, strict);
                    return;
                }
                runnable.run();
            });
        } else {
            MyApp.Companion.post(runnable);
        }
    }

    private void post(Runnable runnable) {
        post(runnable, false);
    }

    void postAfter(Runnable... after) {
        if (after == null) {
            return;
        }
        if (after.length == 0) {
            return;
        }
        Runnable r = after[0];
        if (r == null) {
            return;
        }
        post(r, true);
    }

    int appendSource(List<T> newItems) {
        int oldSize = source.size();
        for (int i = 0; i < newItems.size(); i++) {
            T t = newItems.get(i);
            initItem(t);
            t.position = oldSize + i;
        }
        source.addAll(newItems);
        return oldSize;
    }

    public final void Append(List<T> newItems, Runnable... after) {
        post(() -> {
            int oldSize = appendSource(newItems);
            adapter.notifyItemRangeInserted(oldSize, newItems.size());
            postAfter(after);
        });
    }

    public final void Append(T newItem, Runnable... after) {
        Append(Collections.singletonList(newItem), after);
    }

    void prependSource(List<T> newItems) {
        for (int i = 0; i < newItems.size(); i++) {
            T t = newItems.get(i);
            initItem(t);
            t.position = i;
        }
        for (T t : source) {
            t.position += newItems.size();
        }
        source.addAll(0, newItems);
    }

    public final void Prepend(List<T> newItems, Runnable... after) {
        post(() -> {
            prependSource(newItems);
            adapter.notifyItemRangeInserted(0, newItems.size());
            postAfter(after);
        });
    }

    public final void Prepend(T newItem, Runnable... after) {
        Prepend(Collections.singletonList(newItem), after);
    }

    void rangeInsertSource(int position, List<T> newItems) {
        for (int i = 0; i < newItems.size(); i++) {
            T t = newItems.get(i);
            initItem(t);
            t.position = i + position;
        }
        for (int i = position; i < source.size(); i++) {
            T t = source.get(i);
            t.position += newItems.size();
        }
        source.addAll(position, newItems);
    }

    public final void RangeInsert(int position, List<T> newItems, Runnable... after) {
        post(() -> {
            rangeInsertSource(position, newItems);
            adapter.notifyItemRangeInserted(position, newItems.size());
            postAfter(after);
        });
    }

    public final void Insert(int position, T newItem, Runnable... after) {
        RangeInsert(position, Collections.singletonList(newItem), after);
    }

    public final void RemoveAndRangeInsert(
            Function1<List<T>, Map.Entry<Set<Integer>, Map.Entry<Integer, List<T>>>> changer,
            Runnable... after
    ) {
        post(() -> {
            Map.Entry<Set<Integer>, Map.Entry<Integer, List<T>>> changed = changer.invoke(GetAll());
            if (changed == null) {
                return;
            }
            Set<Integer> removeIndexSet = changed.getKey();
            if (removeIndexSet != null) {
                List<Integer> indexes = new ArrayList<>(removeIndexSet);
                indexes.sort(Comparator.reverseOrder());
                for (Integer index : indexes) {
                    source.remove((int) index);
                    for (int i = index; i < source.size(); i++) {
                        source.get(i).position--;
                    }
                    adapter.notifyItemRemoved(index);
                }
            }
            Map.Entry<Integer, List<T>> inserted = changed.getValue();
            if (inserted != null) {
                Integer position = inserted.getKey();
                List<T> newItemList = inserted.getValue();
                if (newItemList != null) {
                    if (position == null) {
                        position = source.size();
                    }
                    rangeInsertSource(position, newItemList);
                    adapter.notifyItemRangeInserted(position, newItemList.size());
                }
            }
            postAfter(after);
        });
    }

    Map.Entry<Set<Integer>, Map.Entry<Integer, List<T>>> addPositionInfo(
            Integer position,
            Map.Entry<Set<Integer>, List<T>> result
    ) {
        if (result == null) {
            return null;
        }
        Map.Entry<Integer, List<T>> v = null;
        List<T> newItems = result.getValue();
        if (newItems != null) {
            v = new AbstractMap.SimpleEntry<>(position, newItems);
        }
        return new AbstractMap.SimpleEntry<>(result.getKey(), v);
    }

    public final void RemoveAndPrepend(Function1<List<T>, Map.Entry<Set<Integer>, List<T>>> changer, Runnable... after) {
        RemoveAndRangeInsert(nodes -> addPositionInfo(0, changer.invoke(nodes)), after);
    }

    public final void RemoveAndAppend(Function1<List<T>, Map.Entry<Set<Integer>, List<T>>> changer, Runnable... after) {
        RemoveAndRangeInsert(nodes -> addPositionInfo(null, changer.invoke(nodes)), after);
    }

    final void change(Function1<List<T>, List<Map.Entry<T, Set<Integer>>>> changer, Runnable... after) {
        post(() -> {
            List<Map.Entry<T, Set<Integer>>> changed = changer.invoke(GetAll());
            if (changed == null) {
                return;
            }
            for (Map.Entry<T, Set<Integer>> entry : changed) {
                if (entry == null) {
                    continue;
                }
                T changedItem = entry.getKey();
                if (changedItem == null || !source.contains(changedItem)) {
                    continue;
                }
                adapter.notifyItemChanged(changedItem.position, entry.getValue());
            }
            postAfter(after);
        });
    }

    public final void ShuffleAll(Function1<List<T>, List<T>> shuffler, Runnable... after) {
        post(() -> {
            List<T> res = shuffler.invoke(GetAll());
            if (res == null) {
                return;
            }
            source.clear();
            source.addAll(res);
            adapter.notifyDataSetChanged();
            postAfter(after);
        });
    }

    public final void ChangeAll(Function1<List<T>, Boolean> changer, Runnable... after) {
        ShuffleAll(origin -> {
            Boolean confirmed = changer.invoke(origin);
            if (confirmed == null || !confirmed) {
                return null;
            }
            return origin;
        }, after);
    }
}
