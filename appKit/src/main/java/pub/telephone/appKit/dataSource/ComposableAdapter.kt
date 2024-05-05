package pub.telephone.appKit.dataSource

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.viewbinding.ViewBinding
import pub.telephone.appKit.dataSource.DataSource.DataSourceParameters

abstract class ComposableAdapter<D, CD : ComposableNode>(
    params: DataSourceParameters.State,
    private val srcState: State<DataNode.Result<List<D>>>
) : DataAdapter<DataViewHolder<ViewBinding>, CD> {
    class ComposableAdapterParameters<D>(
        val params: DataSourceParameters.State,
        val srcState: State<DataNode.Result<List<D>>>
    )

    constructor(params: ComposableAdapterParameters<D>) : this(params.params, params.srcState)

    final override val lifecycleOwner = params.lifecycleOwner

    @Suppress("LeakingThis")
    final override val source = DataSource(DataSourceParameters(params), this)

    private val state: MutableState<DataNode.Result<List<CD>>> =
        mutableStateOf(DataNode.Result.Init())

    private fun releaseState() {
        state.value = DataNode.Result.Succeed(source.GetAll())
    }

    final override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
        releaseState()
    }

    final override fun notifyItemRemoved(position: Int) {
        releaseState()
    }

    final override fun notifyItemChanged(position: Int, payload: Any?) {
        // Compose 使用场景下不支持刷新指定子项
        // do nothing
    }

    final override fun notifyDataSetChanged() {
        releaseState()
    }

    override fun getItemViewType(node: CD): Int {
        return 0
    }

    @Composable
    @SuppressLint("ComposableNaming")
    protected abstract fun __Content__(list: DataNode.Result<List<CD>>)

    protected abstract fun map(data: D): CD

    @Composable
    fun Content() {
        remember {
            derivedStateOf {
                if (srcState.value.isSuccess) {
                    source.ShuffleAll({ _ -> srcState.value.value.map(::map) })
                }
                true
            }
        }.value
        __Content__(state.value)
    }
}