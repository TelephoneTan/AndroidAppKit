package pub.telephone.appKitApp

import pub.telephone.appKit.MyActivity
import pub.telephone.appKit.dataSource.ColorConfig
import pub.telephone.appKit.dataSource.DataNode
import pub.telephone.appKit.dataSource.DataViewHolder
import pub.telephone.appKitApp.config.colorManager

abstract class Activity<CH : DataViewHolder<*>, CD : DataNode<CH>> : MyActivity<CH, CD>() {
    override fun backgroundColor_ui(colors: ColorConfig<*>): Int {
        return colors.of(colorManager)!!.activity.background.color
    }

    override fun titleColor_ui(colors: ColorConfig<*>): Int {
        return colors.of(colorManager)!!.activity.text.color
    }
}