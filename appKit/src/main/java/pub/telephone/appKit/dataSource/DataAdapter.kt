package pub.telephone.appKit.dataSource

interface DataAdapter<CH : DataViewHolder<*>, CD : DataNode<CH>> {
    val source: DataSource<CH, CD>
    fun getItemViewType(node: CD): Int
    fun notifyItemRangeInserted(positionStart: Int, itemCount: Int)
    fun notifyItemRemoved(position: Int)
    fun notifyItemChanged(position: Int, payload: Any?)
    fun notifyDataSetChanged()
}