package pub.telephone.appKit.dataSource

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.viewbinding.ViewBinding
import pub.telephone.javapromise.async.kpromise.PromiseCancelledBroadcast
import pub.telephone.javapromise.async.kpromise.PromiseScope
import pub.telephone.javapromise.async.promise.PromiseCancelledBroadcaster

abstract class ComposableNode(params: DataNodeParameters.State) :
    DataNode<DataViewHolder<ViewBinding>>(
        DataNodeParameters(params)
    ) {
    final override fun __Bind__(changedBindingKeys: MutableSet<Int>?) {}
    final override fun color_ui(holder: DataViewHolder<ViewBinding>, colors: ColorConfig<*>) {}

    private var currentBroadcaster: PromiseCancelledBroadcaster = super.broadcaster
    private var currentScope: PromiseScope = super.scope

    final override fun currentScope() = currentScope

    @Composable
    private fun rememberScope() = remember {
        val broadcaster = PromiseCancelledBroadcaster()
        this.currentBroadcaster = broadcaster
        val scope = object : PromiseScope {
            override val scopeCancelledBroadcast: PromiseCancelledBroadcast
                get() = broadcaster
        }
        this.currentScope = scope
        scope
    }.apply {
        DisposableEffect(true) {
            onDispose {
                currentBroadcaster.Broadcast()
            }
        }
    }

    @Composable
    private fun <D> Binding<D>.rememberBinding(
        getState: Binding<D>.() -> State<Result<D>>
    ): Result<D> = remember { getState(this) }.value

    @Composable
    protected fun <D> Binding<D>.rememberBinding() = rememberBinding {
        Bind(null) { _ -> null }!!
    }

    @Composable
    protected fun <D> Binding<D>.rememberBinding(task: RetrySharedTask<D, *>) = rememberBinding {
        Bind(task, null, { _ -> null }, { _, _ -> null }, null)!!
    }

    @Composable
    protected fun <D> Binding<D>.rememberBinding(stream: Boolean) = rememberBinding {
        Bind(null, stream) { _, _ -> null }!!
    }

    @Composable
    protected fun <D> Binding<D>.rememberBinding(
        stream: Boolean,
        task: RetrySharedTask<D, *>
    ) = rememberBinding {
        Bind(task, null, { _ -> null }, { _, _ -> null }, stream)!!
    }

    @Composable
    @SuppressLint("ComposableNaming")
    abstract fun __Content__()

    @Composable
    fun Content() {
        rememberScope()
        __Content__()
    }
}