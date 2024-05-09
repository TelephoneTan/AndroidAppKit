package pub.telephone.appKit.dataSource

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.viewbinding.ViewBinding

abstract class ComposableNode(params: DataNodeParameters.State) :
    DataNode<DataViewHolder<ViewBinding>>(
        DataNodeParameters(params)
    ) {
    final override fun color_ui(holder: DataViewHolder<ViewBinding>, colors: ColorConfig<*>) {}

    @Composable
    @SuppressLint("ComposableNaming")
    protected abstract fun __Content__()

    @Composable
    fun Content() {
        __Content__()
        DisposableEffect(true) {
            onDispose {
                cancel_ui()
            }
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
}