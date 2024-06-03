package pub.telephone.appKit.dataSource

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import pub.telephone.javapromise.async.kpromise.ProcessFunc
import pub.telephone.javapromise.async.kpromise.PromiseJob
import pub.telephone.javapromise.async.kpromise.WorkFunc
import pub.telephone.javapromise.async.kpromise.process
import pub.telephone.javapromise.async.kpromise.toProcessFunc

fun <RESULT> DataNode<*>.processed(builder: ProcessFunc<RESULT>) = currentScope().process(builder)

fun DataNode<*>.worked(builder: WorkFunc) = processed(builder.toProcessFunc())
fun <RESULT> DataNode<*>.promised(job: PromiseJob<RESULT>) = processed { promise { job() } }

internal fun <T> mutableStateOf(value: T) = androidx.compose.runtime.mutableStateOf(value)

@Composable
fun <VH : DataViewHolder<*>> DataNode<VH>.Content(
    factory: (LayoutInflater) -> VH,
    modifier: Modifier = Modifier,
) {
    var initialized by remember { mutableStateOf(false) }
    AndroidView(
        factory = {
            factory(LayoutInflater.from(it)).let { vh ->
                vh.itemView.also {
                    ViewCompat.setNestedScrollingEnabled(it, true)
                    it.setTag(TagKey.DataNodeVH.Key, vh)
                }
            }
        },
        modifier = modifier,
        onReset = {},
        onRelease = {
            cancel_ui(it.getTag(TagKey.DataNodeVH.Key))
        },
        update = updateReturn@{
            if (!initialized) {
                initialized = true
                (it.getTag(TagKey.DataNodeVH.Key) as? VH)?.let {
                    bind(it, null)
                }
            }
            emitState.value
            it.requestLayout()
        }
    )
    DisposableEffect(true) {
        val token = object {}
        setCancelToken_ui(token)
        onDispose {
            cancel_ui(token)
        }
    }
}