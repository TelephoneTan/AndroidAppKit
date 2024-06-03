package pub.telephone.appKit.dataSource

import pub.telephone.appKit.R

data class TagKey(
    @Suppress("PropertyName")
    @JvmField
    val Key: Int,
    @Suppress("PropertyName")
    @JvmField
    val InitKey: Int
) {
    companion object {
        val DataNodeVH = TagKey(
            R.id.tagKey_DataNodeVH,
            R.id.tagInitKey_DataNodeVH
        )
        val DataNodeColor = TagKey(R.id.tagKey_DataNodeColor, R.id.tagInitKey_DataNodeColor)
        val MyActivityLoad = TagKey(R.id.tagKey_MyActivityLoad, R.id.tagInitKey_MyActivityLoad)
        val BrowserSetCurrentWebView = TagKey(
            R.id.tagKey_BrowserSetCurrentWebView,
            R.id.tagInitKey_BrowserSetCurrentWebView
        )
        val BrowserSetTitle = TagKey(R.id.tagKey_BrowserSetTitle, R.id.tagInitKey_BrowserSetTitle)
        val DataSource = TagKey(R.id.tagKey_DataSource, R.id.tagInitKey_DataSource)
        val DataNode = TagKey(R.id.tagKey_DataNode, R.id.tagInitKey_DataNode)
    }
}