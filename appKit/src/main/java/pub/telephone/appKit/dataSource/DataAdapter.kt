package pub.telephone.appKit.dataSource

import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

interface DataAdapter<CH : DataViewHolder<*>, CD : DataNode<CH>> {
    val lifecycleOwner: WeakReference<LifecycleOwner>
    val source: DataSource<CH, CD>
    fun getItemViewType(node: CD): Int
    fun notifyItemRangeInserted(positionStart: Int, itemCount: Int)
    fun notifyItemRemoved(position: Int)
    fun notifyItemChanged(position: Int, payload: Any?)
    fun notifyDataSetChanged()
}