package pub.telephone.appKitApp

import pub.telephone.appKit.MyApp
import pub.telephone.appKit.dataSource.ColorManager
import pub.telephone.appKitApp.config.colorManager

class App : MyApp() {
    override val colorManager_ui: ColorManager<*, *, *>
        get() = colorManager
}