package pub.telephone.appKit.dataSource

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.IntDef
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import pub.telephone.appKit.AppKit
import pub.telephone.appKit.dataSource.DataViewHolder.DataViewHolderParameters.Inflater
import pub.telephone.appKit.databinding.ComposableNodeBinding

abstract class ComposableNode(params: DataNodeParameters.State) :
    DataNode<ComposableNode.UI>(
        DataNodeParameters(params)
    ) {
    final override fun color_ui(holder: UI, colors: ColorConfig<*>) {}

    @Suppress("FunctionName")
    protected abstract fun __Bind___(changedBindingKeys: MutableSet<Int>?)

    final override fun __Bind__(changedBindingKeys: MutableSet<Int>?) {
        init.Bind(init.BindParameters(changedBindingKeys).setInit {
            it.view.composeView.setContent { Content() }
            null
        })
        __Bind___(changedBindingKeys)
    }

    @Composable
    @SuppressLint("ComposableNaming")
    protected abstract fun __Content__()

    @Composable
    fun Content() {
        __Content__()
        val broadcaster = currentBroadcaster()
        DisposableEffect(true) {
            onDispose {
                cancel_ui(broadcaster)
            }
        }
        val currentView by rememberUpdatedState(LocalView.current)
        remember(emitState.value) {
            currentView.requestLayout()
            true
        }
    }

    @Composable
    fun ListContent() {
        remember {
            EmitChange_ui(null)
            true
        }
        Content()
    }

    class UI(params: Inflater) :
        DataViewHolder<ComposableNodeBinding>(params, ComposableNodeBinding::class.java)

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(MATCH_PARENT, WRAP_CONTENT)
    annotation class UISize

    companion object {
        private fun createViewHolder(
            params: Inflater,
            lo: LifecycleOwner?,
            @UISize w: Int,
            @UISize h: Int,
            content: (@Composable () -> Unit)?
        ): UI {
            AppKit.ensureMainThread()
            return UI(params).apply {
                itemView.layoutParams?.let {
                    itemView.layoutParams = it.apply {
                        width = w
                        height = h
                    }
                }
                view.composeView.apply {
                    setViewCompositionStrategy(
                        lo?.let { ViewCompositionStrategy.DisposeOnLifecycleDestroyed(it) }
                            ?: ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
                    )
                    content?.let { setContent { it() } }
                }
            }
        }

        fun createListViewHolder(
            params: Inflater,
            lo: LifecycleOwner,
            @UISize w: Int = MATCH_PARENT,
            @UISize h: Int = WRAP_CONTENT,
        ) = createViewHolder(params, lo, w, h, null)
    }

    fun createViewHolder(
        params: Inflater,
        lo: LifecycleOwner,
        @UISize w: Int = MATCH_PARENT,
        @UISize h: Int = MATCH_PARENT
    ) = Companion.createViewHolder(params, lo, w, h) {
        Content()
    }
}