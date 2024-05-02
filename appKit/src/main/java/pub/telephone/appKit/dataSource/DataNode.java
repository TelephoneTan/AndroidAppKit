package pub.telephone.appKit.dataSource;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager2.widget.ViewPager2;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import pub.telephone.appKit.MyApp;
import pub.telephone.javapromise.async.Async;
import pub.telephone.javapromise.async.promise.Promise;
import pub.telephone.javapromise.async.promise.PromiseFulfilledListener;
import pub.telephone.javapromise.async.promise.PromiseJob;
import pub.telephone.javapromise.async.promise.PromiseRejectedListener;
import pub.telephone.javapromise.async.promise.PromiseStatefulFulfilledListener;

public abstract class DataNode<VH extends DataViewHolder<?>> {
    public static class RetrySharedTask<T, M> {
        public static class Token<T> {
            final T value;

            private Token(T value) {
                this.value = value;
            }
        }

        public static class Reference<T, R> {
            private T value;

            public R Retry(T value) throws Throwable {
                this.value = value;
                throw _RETRY;
            }

            public Token<R> Set(T value, R result) {
                this.value = value;
                return new Token<>(result);
            }
        }

        private static final Throwable _RETRY = new Throwable();
        final PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test;
        final PromiseStatefulFulfilledListener<M, Object> retry;
        final Function2<
                PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>>,
                PromiseStatefulFulfilledListener<M, Object>,
                PromiseJob<LazyRes<T>>> task;

        private static <M, T> PromiseFulfilledListener<Reference<M, T>, Token<T>> buildStatelessTest(
                Function0<Promise<T>> job
        ) {
            return mid -> job.invoke().Then(res -> mid.Set(null, res));
        }

        private static <M, T> PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> toTest(
                PromiseFulfilledListener<Reference<M, T>, Token<T>> statelessTest
        ) {
            return (v, promiseState) -> statelessTest.OnFulfilled(v);
        }

        public static <T> RetrySharedTask<T, Object> Simple(Function0<Promise<T>> job) {
            return new RetrySharedTask<>(
                    buildStatelessTest(job),
                    null
            );
        }

        public RetrySharedTask(
                PromiseFulfilledListener<Reference<M, T>, Token<T>> test,
                PromiseFulfilledListener<M, Object> retry
        ) {
            this(
                    toTest(test),
                    (v, promiseState) -> retry.OnFulfilled(v)
            );
        }

        public RetrySharedTask(
                PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                PromiseStatefulFulfilledListener<M, Object> retry
        ) {
            this(test, retry, null);
        }

        public RetrySharedTask(
                PromiseFulfilledListener<Reference<M, T>, Token<T>> test,
                PromiseFulfilledListener<M, Object> retry,
                Boolean refreshCache
        ) {
            this(
                    (v, promiseState) -> test.OnFulfilled(v),
                    (v, promiseState) -> retry.OnFulfilled(v),
                    refreshCache
            );
        }

        public RetrySharedTask(
                PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                PromiseStatefulFulfilledListener<M, Object> retry,
                Boolean refreshCache
        ) {
            this.test = test;
            this.retry = retry;
            this.task = (test1, retry1) -> (rs, re) -> {
                AtomicReference<LazyRes<T>> res = new AtomicReference<>();
                Reference<M, T> mid = new Reference<>();
                Promise<Reference<M, T>> provideMid = new Promise<>((rs1, re1) -> rs1.Resolve(mid));
                Function1<M, Promise<M>> provideMidValue = midValue ->
                        new Promise<>((rs1, re1) -> rs1.Resolve(midValue));
                Function1<M, Promise<T>> generateLazyFetch = midValue ->
                        provideMidValue.invoke(midValue)
                                .Then(retry1)
                                .Then(o -> provideMid.Then(test1).Then(r -> r.value));
                Function1<T, LazyRes<T>> generateLazyResFromCache = cache ->
                        retry1 == null ?
                                new LazyRes<>(cache, null) :
                                new LazyRes<>(
                                        cache,
                                        refreshCache != null && refreshCache ?
                                                generateLazyFetch.invoke(mid.value) :
                                                null
                                );
                rs.Resolve(provideMid.Then(test1)
                        .Then(r -> {
                            res.set(generateLazyResFromCache.invoke(r.value));
                            return null;
                        })
                        .Catch(throwable -> {
                            if (throwable != _RETRY || retry1 == null) {
                                throw throwable;
                            }
                            Promise<T> lazyFetch = generateLazyFetch.invoke(mid.value);
                            return lazyFetch.Then(t -> {
                                res.set(new LazyRes<>(t, null));
                                return null;
                            });
                        })
                        .Then(x -> res.get()));
            };
        }

        public Promise<LazyRes<T>> Do(
                PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                PromiseStatefulFulfilledListener<M, Object> retry
        ) {
            return new Promise<>(this.task.invoke(test, retry));
        }
    }

    public static class LazyRes<T> {
        public final T Cache;
        public final Promise<T> Latest;

        public LazyRes(T cache, Promise<T> latest) {
            Cache = cache;
            Latest = latest;
        }
    }

    public static class Result<T> {
        public enum StatusType {
            Init,
            Success,
        }

        public final StatusType status;
        public final T value;

        private Result(StatusType status, T value) {
            this.status = status;
            this.value = value;
        }

        public final boolean isInit() {
            return this.status == StatusType.Init;
        }

        public final boolean isSuccess() {
            return this.status == StatusType.Success;
        }

        public static <T> Result<T> Init() {
            return new Result<>(StatusType.Init, null);
        }

        public static <T> Result<T> Succeed(T value) {
            return new Result<>(StatusType.Success, value);
        }
    }

    private static <D, M> PromiseJob<LazyRes<D>> buildFetchJob(RetrySharedTask<D, M> task) {
        return (rs, re) -> rs.Resolve(task.Do(task.test, task.retry));
    }

    protected <D> Binding<D> bindTask(TagKey key, RetrySharedTask<D, ?> task) {
        return new Binding<>(key.Key, key.InitKey, buildFetchJob(task));
    }

    protected <D> Binding<D> emptyBinding(@NotNull TagKey key) {
        return new Binding<>(key.Key, key.InitKey, null);
    }

    protected Binding<Object> emptyBinding() {
        return new Binding<>(null, null, null);
    }

    public class Binding<D> {
        final Integer key;
        final Integer initKey;
        final PromiseJob<LazyRes<D>> fetchJob;
        final MutableState<Result<D>> state = DataNodeKt.mutableStateOf(Result.Init());
        Promise<?> fetchPromise;
        Result<D> data;

        public Binding(Integer key, Integer initKey, PromiseJob<LazyRes<D>> fetchJob) {
            this.key = key;
            this.initKey = initKey;
            this.fetchJob = fetchJob;
        }

        protected boolean alive(Promise<?> currentFetch) {
            if (!DataNode.this.alive()) {
                return false;
            }
            if (currentFetch != fetchPromise) {
                return false;
            }
            return true;
        }

        protected boolean visible(Promise<?> currentFetch) {
            if (!DataNode.this.visible()) {
                return false;
            }
            if (!alive(currentFetch)) {
                return false;
            }
            return true;
        }

        protected <T> @Nullable T whenAlive(Promise<?> currentFetch, Function1<VH, T> runnable) {
            if (!alive(currentFetch)) {
                return null;
            }
            return DataNode.this.whenAlive(runnable);
        }

        protected void whenAlive(Promise<?> currentFetch, Runnable runnable) {
            whenAlive(currentFetch, holder -> {
                runnable.run();
                return null;
            });
        }

        protected void whenVisible(Promise<?> currentFetch, Function1<VH, Void> runnable) {
            if (!visible(currentFetch)) {
                return;
            }
            DataNode.this.whenVisible(runnable);
        }

        protected void whenVisible(Promise<?> currentFetch, Runnable runnable) {
            whenVisible(currentFetch, holder -> {
                runnable.run();
                return null;
            });
        }

        void succeed(D value, Promise<?> currentFetch) {
            MyApp.Companion.post(() -> whenAlive(currentFetch, () -> {
                if (fetchPromise != currentFetch) {
                    return;
                }
                data = Result.Succeed(value);
                state.setValue(data);
                EmitChange_ui(Collections.singleton(key));
            }));
        }

        public Integer SetResult(D result) {
            data = Result.Succeed(result);
            state.setValue(data);
            return key;
        }

        public Integer ReInit() {
            data = Result.Init();
            state.setValue(data);
            return initKey;
        }

        public @Nullable State<Result<D>> Bind(Set<Integer> changedBindingKeys, Function1<VH, Void> init) {
            return Bind(changedBindingKeys, init, null);
        }

        public @Nullable State<Result<D>> Bind(Set<Integer> changedBindingKeys, Function2<VH, D, Void> onSucceed) {
            return Bind(changedBindingKeys, (Boolean) null, onSucceed);
        }

        public @Nullable State<Result<D>> Bind(
                Set<Integer> changedBindingKeys,
                Boolean stream,
                Function2<VH, D, Void> onSucceed
        ) {
            return Bind(changedBindingKeys, null, onSucceed, stream);
        }

        public @Nullable State<Result<D>> Bind(
                Set<Integer> changedBindingKeys,
                Function1<VH, Void> init,
                Function2<VH, D, Void> onSucceed
        ) {
            return Bind(changedBindingKeys, init, onSucceed, null);
        }

        public @Nullable State<Result<D>> Bind(
                Set<Integer> changedBindingKeys,
                Function1<VH, Void> init,
                Function2<VH, D, Void> onSucceed,
                Boolean stream
        ) {
            return Bind(fetchJob, changedBindingKeys, init, onSucceed, stream);
        }

        public @Nullable State<Result<D>> Bind(
                RetrySharedTask<D, ?> task,
                Set<Integer> changedBindingKeys,
                Function1<VH, Void> init,
                Function2<VH, D, Void> onSucceed,
                Boolean stream
        ) {
            return Bind(buildFetchJob(task), changedBindingKeys, init, onSucceed, stream);
        }

        public @Nullable State<Result<D>> Bind(
                PromiseJob<LazyRes<D>> fetchJob,
                Set<Integer> changedBindingKeys,
                Function1<VH, Void> init,
                Function2<VH, D, Void> onSucceed,
                Boolean stream
        ) {
            Function1<VH, Void> renderer = holder -> {
                if (holder == null) {
                    return null;
                }
                if (data == null) {
                    return null;
                }
                if (data.status == Result.StatusType.Init) {
                    if (init != null) {
                        init.invoke(holder);
                    }
                } else if (data.status == Result.StatusType.Success) {
                    if (onSucceed != null) {
                        onSucceed.invoke(holder, data.value);
                    }
                }
                data = null;
                return null;
            };
            return whenAlive(fetchPromise, holder -> {
                if (changedBindingKeys == null || changedBindingKeys.isEmpty() || (initKey != null && changedBindingKeys.contains(initKey))) {
                    data = Result.Init();
                    state.setValue(data);
                    if (key != null && fetchJob != null) {
                        Promise<LazyRes<D>> currentFetch = new Promise<>(fetchJob);
                        fetchPromise = currentFetch;
                        PromiseFulfilledListener<LazyRes<D>, Object>[] s =
                                new PromiseFulfilledListener[1];
                        PromiseRejectedListener<Object>[] f = new PromiseRejectedListener[1];
                        Function1<Long, Promise<Object>> again = delaySeconds ->
                                Async.Delay(Duration.ofSeconds(delaySeconds)).Then(o -> {
                                    MyApp.Companion.post(() -> {
                                        Runnable next =
                                                () -> new Promise<>(fetchJob).Then(s[0]).Catch(f[0]);
                                        Runnable[] callNext = new Runnable[1];
                                        callNext[0] = () -> {
                                            if (visible(currentFetch)) {
                                                next.run();
                                            } else if (alive(currentFetch)) {
                                                LifecycleOwner lifecycleOwner =
                                                        DataNode.this.lifecycleOwner.get();
                                                if (lifecycleOwner == null) {
                                                    return;
                                                }
                                                lifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
                                                    @Override
                                                    public void onStateChanged(@NonNull LifecycleOwner lo, @NonNull Lifecycle.Event e) {
                                                        if (e.getTargetState() == Lifecycle.State.STARTED) {
                                                            lo.getLifecycle().removeObserver(this);
                                                            callNext[0].run();
                                                        }
                                                    }
                                                });
                                            }
                                        };
                                        callNext[0].run();
                                    });
                                    return null;
                                });
                        f[0] = reason -> {
                            reason.printStackTrace();
                            again.invoke(2L);
                            return null;
                        };
                        s[0] = lazy -> {
                            succeed(lazy.Cache, currentFetch);
                            Runnable callAgain = () -> {
                                if (stream == null || !stream) {
                                    return;
                                }
                                again.invoke(5L);
                            };
                            if (lazy.Latest == null) {
                                callAgain.run();
                                return null;
                            }
                            return lazy.Latest.Then(value -> {
                                succeed(value, currentFetch);
                                callAgain.run();
                                return null;
                            });
                        };
                        currentFetch.Then(s[0]).Catch(f[0]);
                    }
                    renderer.invoke(holder);
                    return state;
                } else if (key != null && changedBindingKeys.contains(key)) {
                    renderer.invoke(holder);
                    return null;
                } else {
                    return null;
                }
            });
        }
    }

    protected volatile WeakReference<LifecycleOwner> lifecycleOwner;
    DataSource<VH, DataNode<VH>> source;
    @Nullable
    final VH holder;
    @Nullable
    WeakReference<VH> binding = null;
    protected int position;

    public static class DataNodeParameters<VH> {
        @Nullable
        final WeakReference<LifecycleOwner> lifecycleOwner;
        @Nullable
        final VH holder;

        public DataNodeParameters(
                @Nullable WeakReference<LifecycleOwner> lifecycleOwner,
                @Nullable VH holder
        ) {
            this.lifecycleOwner = lifecycleOwner;
            this.holder = holder;
        }
    }

    public DataNode(@Nullable WeakReference<LifecycleOwner> lifecycleOwner, @Nullable VH holder) {
        this.lifecycleOwner = lifecycleOwner;
        this.holder = holder;
    }

    public DataNode(@NotNull DataNodeParameters<VH> parameters) {
        this(parameters.lifecycleOwner, parameters.holder);
    }

    protected boolean alive() {
        LifecycleOwner lifecycleOwner = this.lifecycleOwner.get();
        if (lifecycleOwner == null) {
            return false;
        }
        if (!lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            return false;
        }
        if (binding != null) {
            VH currentBinding = binding.get();
            if (currentBinding == null || currentBinding.itemView.getTag(TagKey.Companion.getDataNode().Key) != DataNode.this) {
                return false;
            }
        }
        return true;
    }

    protected boolean visible() {
        if (!alive()) {
            return false;
        }
        LifecycleOwner lifecycleOwner = this.lifecycleOwner.get();
        if (lifecycleOwner == null) {
            return false;
        }
        if (!lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return false;
        }
        return true;
    }

    protected <T> @Nullable T whenAlive(Function1<VH, T> runnable) {
        if (!alive()) {
            return null;
        }
        VH currentBinding = null;
        if (binding != null) {
            currentBinding = binding.get();
            if (currentBinding == null) {
                return null;
            }
        }
        return runnable.invoke(currentBinding);
    }

    protected void whenVisible(Function1<VH, Void> runnable) {
        if (!visible()) {
            return;
        }
        whenAlive(runnable);
    }

    public final void EmitChange_ui(Set<Integer> keys) {
        if (source != null) {
            if (source.view == null) {
                return;
            }
            source.change(x -> {
                View v = source.view.get();
                if (v == null) {
                    return null;
                }
                if (!(v instanceof ViewPager2)) {
                    return Collections.singletonList(
                            new AbstractMap.SimpleEntry<>(DataNode.this, keys)
                    );
                }
                wrapBind(keys);
                return null;
            });
        } else if (holder != null) {
            bind(holder, keys);
        }
    }

    protected final void removeSelf(Runnable... after) {
        if (source == null) {
            return;
        }
        source.RemoveAndPrepend(nodes -> new AbstractMap.SimpleEntry<>(Collections.singleton(position), null), after);
    }

    protected final void getAll(Function1<List<DataNode<VH>>, Void> onAll) {
        if (source == null) {
            return;
        }
        source.change(all -> {
            onAll.invoke(all);
            return null;
        });
    }

    final void bind(@NotNull VH holder, Set<Integer> changedBindingKeys) {
        holder.itemView.setTag(TagKey.Companion.getDataNode().Key, DataNode.this);
        binding = new WeakReference<>(holder);
        wrapBind(changedBindingKeys);
    }

    protected final Binding<Object> init = emptyBinding();
    public final Binding<ColorConfig<?>> ColorBinding = emptyBinding(TagKey.Companion.getDataNodeColor());

    protected @Nullable ColorManager<?, ?, ?> getMyColorManager() {
        return MyApp.Companion.getMyColorManager();
    }

    protected final void watchColor() {
        ColorManager<?, ?, ?> manager = getMyColorManager();
        if (manager == null) {
            return;
        }
        manager.getManager().Register(this);
    }

    protected void color_ui(@NotNull VH holder, @NotNull ColorConfig<?> colors) {
    }

    final void wrapBind(Set<Integer> changedBindingKeys) {
        ColorBinding.Bind(changedBindingKeys, holder -> {
            ColorManager<?, ?, ?> manager = getMyColorManager();
            if (manager != null) {
                color_ui(holder, manager.getCurrent());
            }
            return null;
        }, (holder, colors) -> {
            color_ui(holder, colors);
            return null;
        });
        __Bind__(changedBindingKeys);
    }

    protected abstract void __Bind__(Set<Integer> changedBindingKeys);
}
