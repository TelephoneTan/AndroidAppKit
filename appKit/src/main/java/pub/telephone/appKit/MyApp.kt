package pub.telephone.appKit

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import pub.telephone.appKit.dataSource.ColorManager
import java.util.concurrent.atomic.AtomicReference

abstract class MyApp : Application() {
    private val uiHandler: Handler = Handler(Looper.getMainLooper())
    private val myColorManager by lazy { colorManager_ui }

    companion object {
        private val appR = AtomicReference<MyApp>()
        private val app: MyApp get() = appR.get()
        private val ui get() = app.uiHandler
        val context: Context get() = app.applicationContext
        val myColorManager get() = app.myColorManager
        val resources: Resources get() = app.resources
        val isInNightMode: Boolean get() = isNight(resources.configuration)
        fun post(r: Runnable) = ui.post(r)
        private fun isNight(config: Configuration): Boolean {
            return config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }

    final override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //
        ColorManager.manager.CallOnAll {
            it.commit(night = isNight(newConfig))
            null
        }
    }

    @Suppress("PropertyName")
    protected open val colorManager_ui: ColorManager<*, *, *>? = null

    @Suppress("FunctionName")
    protected open fun onCreated_ui() {
    }

    final override fun onCreate() {
        super.onCreate()
        //
        appR.set(this)
        //
        myColorManager
        //
        onConfigurationChanged(resources.configuration)
        //
        onCreated_ui()
    }
}