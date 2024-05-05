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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import pub.telephone.appKit.AppKit;
import pub.telephone.appKit.MyApp;
import pub.telephone.javapromise.async.Async;
import pub.telephone.javapromise.async.kpromise.PromiseCancelledBroadcast;
import pub.telephone.javapromise.async.kpromise.PromiseScope;
import pub.telephone.javapromise.async.promise.Promise;
import pub.telephone.javapromise.async.promise.PromiseCancelledBroadcaster;
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
        final @Nullable PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test;
        final @Nullable PromiseStatefulFulfilledListener<M, Object> retry;
        final Function2<
                PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>>,
                PromiseStatefulFulfilledListener<M, Object>,
                PromiseJob<LazyRes<T>>> task;

        private static <M, T> PromiseFulfilledListener<Reference<M, T>, Token<T>> buildStatelessTest(
                Function0<Promise<T>> job
        ) {
            return mid -> job.invoke().Then(res -> mid.Set(null, res));
        }

        public static <T> RetrySharedTask<T, Object> Simple(Function0<Promise<T>> job) {
            return new RetrySharedTask<>(
                    buildStatelessTest(job),
                    null
            );
        }

        public static <T> RetrySharedTask<T, Object> Simple() {
            return new RetrySharedTask<>();
        }

        public RetrySharedTask() {
            this((Boolean) null, null, null);
        }

        public RetrySharedTask(
                @Nullable PromiseFulfilledListener<Reference<M, T>, Token<T>> test,
                @Nullable PromiseFulfilledListener<M, Object> retry
        ) {
            this(
                    test == null ? null : (v, promiseState) -> test.OnFulfilled(v),
                    retry == null ? null : (v, promiseState) -> retry.OnFulfilled(v)
            );
        }

        public RetrySharedTask(
                @Nullable PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                @Nullable PromiseStatefulFulfilledListener<M, Object> retry
        ) {
            this(test, retry, null);
        }

        public RetrySharedTask(
                @Nullable PromiseFulfilledListener<Reference<M, T>, Token<T>> test,
                @Nullable PromiseFulfilledListener<M, Object> retry,
                Boolean refreshCache
        ) {
            this(
                    test == null ? null : (v, promiseState) -> test.OnFulfilled(v),
                    retry == null ? null : (v, promiseState) -> retry.OnFulfilled(v),
                    refreshCache
            );
        }

        public RetrySharedTask(
                @Nullable PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                @Nullable PromiseStatefulFulfilledListener<M, Object> retry,
                Boolean refreshCache
        ) {
            this(refreshCache, test, retry);
        }

        private RetrySharedTask(
                Boolean refreshCache,
                @Nullable PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                @Nullable PromiseStatefulFulfilledListener<M, Object> retry
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
                @NotNull PromiseStatefulFulfilledListener<Reference<M, T>, Token<T>> test,
                @Nullable PromiseStatefulFulfilledListener<M, Object> retry
        ) {
            return new Promise<>(this.task.invoke(test, retry));
        }

        public Promise<LazyRes<T>> Do() {
            return Do(Objects.requireNonNull(test), retry);
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

        public final T get(T defaultValue) {
            return isInit() ? defaultValue : value;
        }

        public static <T> Result<T> Init() {
            return new Result<>(StatusType.Init, null);
        }

        public static <T> Result<T> Succeed(T value) {
            return new Result<>(StatusType.Success, value);
        }
    }

    private static <D, M> @Nullable PromiseJob<LazyRes<D>> buildFetchJob(
            @Nullable RetrySharedTask<D, M> originalTask,
            @Nullable RetrySharedTask<D, M> updatedTask
    ) {
        if (originalTask == null || updatedTask == null) {
            return null;
        }
        return (rs, re) -> rs.Resolve(
                originalTask.Do(
                        Objects.requireNonNull(updatedTask.test),
                        updatedTask.retry
        ));
    }

    protected <D, M> BindingX<D, M> bindTaskX(TagKey key, @Nullable RetrySharedTask<D, M> task) {
        return new BindingX<>(key.Key, key.InitKey, task);
    }

    protected <D> Binding<D> bindTask(TagKey key, @Nullable RetrySharedTask<D, Object> task) {
        return new Binding<>(key.Key, key.InitKey, task);
    }

    protected <D, M> BindingX<D, M> emptyBindingX(@NotNull TagKey key) {
        return new BindingX<>(key.Key, key.InitKey, null);
    }

    protected <D> Binding<D> emptyBinding(@NotNull TagKey key) {
        return new Binding<>(key.Key, key.InitKey, null);
    }

    protected BindingX<Object, Object> emptyBindingX() {
        return new BindingX<>(null, null, null);
    }

    protected Binding<Object> emptyBinding() {
        return new Binding<>(null, null, null);
    }

    public class Binding<D> extends BindingX<D, Object> {
        public Binding(Integer key, Integer initKey, @Nullable RetrySharedTask<D, Object> task) {
            super(key, initKey, task);
        }
    }

    public class BindingX<D, M> {
        final Integer key;
        final Integer initKey;
        @Nullable
        final RetrySharedTask<D, M> task;
        @Nullable
        final PromiseJob<LazyRes<D>> fetchJob;
        final MutableState<Result<D>> state = DataNodeKt.mutableStateOf(Result.Init());
        Promise<?> fetchPromise;
        Result<D> data;

        public BindingX(Integer key, Integer initKey, @Nullable RetrySharedTask<D, M> task) {
            AppKit.Companion.ensureMainThread();
            this.key = key;
            this.initKey = initKey;
            this.task = task;
            this.fetchJob = buildFetchJob(task, task);
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

        public class BindParameters {
            @Nullable
            PromiseJob<LazyRes<D>> fetchJob = BindingX.this.fetchJob;
            public @Nullable Set<Integer> changedBindingKeys;
            public @Nullable Function1<VH, Void> init;
            public @Nullable Function2<VH, D, Void> onSucceed;
            public @Nullable Boolean stream;
            public @Nullable Boolean noGeneralInit;

            BindParameters setFetchJob(@Nullable PromiseJob<LazyRes<D>> fetchJob) {
                this.fetchJob = fetchJob;
                return this;
            }

            public BindParameters setFetchJob(@Nullable RetrySharedTask<D, M> newTask) {
                return setFetchJob(buildFetchJob(task, newTask));
            }

            public BindParameters setChangedBindingKeys(@Nullable Set<Integer> changedBindingKeys) {
                this.changedBindingKeys = changedBindingKeys;
                return this;
            }

            public BindParameters setInit(@Nullable Function1<VH, Void> init) {
                this.init = init;
                return this;
            }

            public BindParameters setOnSucceed(@Nullable Function2<VH, D, Void> onSucceed) {
                this.onSucceed = onSucceed;
                return this;
            }

            public BindParameters setStream(@Nullable Boolean stream) {
                this.stream = stream;
                return this;
            }

            public BindParameters setNoGeneralInit(@Nullable Boolean noGeneralInit) {
                this.noGeneralInit = noGeneralInit;
                return this;
            }
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
                @Nullable RetrySharedTask<D, M> newTask,
                Set<Integer> changedBindingKeys,
                Function1<VH, Void> init,
                Function2<VH, D, Void> onSucceed,
                Boolean stream
        ) {
            return Bind(buildFetchJob(task, newTask), changedBindingKeys, init, onSucceed, stream);
        }

        public @Nullable State<Result<D>> Bind(
                @Nullable PromiseJob<LazyRes<D>> fetchJob,
                Set<Integer> changedBindingKeys,
                Function1<VH, Void> init,
                Function2<VH, D, Void> onSucceed,
                Boolean stream
        ) {
            return Bind(
                    new BindParameters()
                            .setFetchJob(fetchJob)
                            .setChangedBindingKeys(changedBindingKeys)
                            .setInit(init)
                            .setOnSucceed(onSucceed)
                            .setStream(stream)
            );
        }

        public @Nullable State<Result<D>> Bind(@NotNull BindParameters params) {
            Function1<VH, Void> renderer = holder -> {
                if (holder == null) {
                    return null;
                }
                if (data == null) {
                    return null;
                }
                if (data.status == Result.StatusType.Init) {
                    if (params.init != null) {
                        params.init.invoke(holder);
                    }
                } else if (data.status == Result.StatusType.Success) {
                    if (params.onSucceed != null) {
                        params.onSucceed.invoke(holder, data.value);
                    }
                }
                data = null;
                return null;
            };
            return whenAlive(fetchPromise, holder -> {
                Function0<Boolean> isSpecialInit = () ->
                        params.changedBindingKeys != null &&
                                initKey != null &&
                                params.changedBindingKeys.contains(initKey);
                if (
                        params.changedBindingKeys == null ||
                                params.changedBindingKeys.isEmpty() ||
                                isSpecialInit.invoke()
                ) {
                    data = Result.Init();
                    state.setValue(data);
                    if (
                            key != null &&
                                    params.fetchJob != null &&
                                    (
                                            params.noGeneralInit == null ||
                                                    !params.noGeneralInit ||
                                                    isSpecialInit.invoke()
                                    )
                    ) {
                        Promise<LazyRes<D>> currentFetch = new Promise<>(params.fetchJob);
                        fetchPromise = currentFetch;
                        PromiseFulfilledListener<LazyRes<D>, Object>[] s =
                                new PromiseFulfilledListener[1];
                        PromiseRejectedListener<Object>[] f = new PromiseRejectedListener[1];
                        Function1<Long, Promise<Object>> again = delaySeconds ->
                                Async.Delay(Duration.ofSeconds(delaySeconds)).Then(o -> {
                                    MyApp.Companion.post(() -> {
                                        Runnable next =
                                                () -> new Promise<>(params.fetchJob).Then(s[0]).Catch(f[0]);
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
                                if (params.stream == null || !params.stream) {
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
                } else if (key != null && params.changedBindingKeys.contains(key)) {
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
    final PromiseCancelledBroadcaster broadcaster = new PromiseCancelledBroadcaster();
    final PromiseScope scope = new PromiseScope() {
        @NonNull
        @Override
        public PromiseCancelledBroadcast getScopeCancelledBroadcast() {
            return broadcaster;
        }
    };

    protected PromiseScope currentScope() {
        return scope;
    }

    public static class DataNodeParameters<VH> {
        public static class State {
            @Nullable
            final WeakReference<LifecycleOwner> lifecycleOwner;

            public State(@Nullable WeakReference<LifecycleOwner> lifecycleOwner) {
                this.lifecycleOwner = lifecycleOwner;
            }
        }

        @NotNull
        final State state;

        @Nullable
        final VH holder;

        public DataNodeParameters(
                @Nullable WeakReference<LifecycleOwner> lifecycleOwner,
                @Nullable VH holder
        ) {
            this.state = new State(lifecycleOwner);
            this.holder = holder;
        }

        DataNodeParameters(@NotNull State state) {
            this.state = state;
            this.holder = null;
        }
    }

    public DataNode(@Nullable WeakReference<LifecycleOwner> lifecycleOwner, @Nullable VH holder) {
        AppKit.Companion.ensureMainThread();
        this.lifecycleOwner = lifecycleOwner;
        this.holder = holder;
    }

    public DataNode(@NotNull DataNodeParameters<VH> parameters) {
        this(parameters.state.lifecycleOwner, parameters.holder);
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
        source.ChangeAll(all -> {
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
