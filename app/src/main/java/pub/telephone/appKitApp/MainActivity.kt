package pub.telephone.appKitApp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import pub.telephone.appKit.browser.BrowserState
import pub.telephone.appKit.dataSource.ColorConfig
import pub.telephone.appKit.dataSource.EmbeddedDataNode
import pub.telephone.appKit.dataSource.EmbeddedDataNodeAPI
import pub.telephone.appKit.dataSource.TagKey
import pub.telephone.appKitApp.config.colorManager
import pub.telephone.appKitApp.databinding.ActivityMainBinding
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

private typealias MainActivityINFO = Any?

class MainActivity : Activity<MainActivity.ViewHolder, MainActivity.DataNode>() {
    private val currentBrowserR = AtomicReference<BrowserState>()
    private val currentBrowser: BrowserState? get() = currentBrowserR.get()
    private val browserCreator =
        object :
            EmbeddedDataNodeAPI.DataNodeCreator<BrowserState.ViewHolder, MainActivityINFO, BrowserState> {
            override fun createChild(
                lifecycleOwner: WeakReference<LifecycleOwner>?,
                holder: BrowserState.ViewHolder?
            ): BrowserState {
                return BrowserState(
                    lifecycleOwner,
                    holder,
                    "https://appassets.androidplatform.net/assets/圣诞树.html"
                ) {
                    title = it
                }.also { currentBrowserR.set(it) }
            }
        }

    inner class DataNode(
        lifecycleOwner: WeakReference<LifecycleOwner>?,
        holder: MainActivity.ViewHolder?
    ) :
        EmbeddedDataNode<BrowserState.ViewHolder, ViewHolder, MainActivityINFO, BrowserState>(
            lifecycleOwner, holder, browserCreator
        ),
        EmbeddedDataNodeAPI.DataNodeCreator<BrowserState.ViewHolder, MainActivityINFO, BrowserState> by browserCreator {
        override fun loadKey(): TagKey {
            return TagKey(R.id.tagKey_MainActivityLoad, R.id.tagInitKey_MainActivityLoad)
        }

        init {
            watchColor()
        }

        override fun color_ui(holder: MainActivity.ViewHolder, colors: ColorConfig<*>) {
            colors.of(colorManager)?.let { c ->
                holder.view.input.setTextColor(c.main.text)
            }
        }
    }

    class ViewHolder(inflater: LayoutInflater, container: ViewGroup?) :
        EmbeddedDataNode.ViewHolder<ActivityMainBinding, BrowserState.ViewHolder>(
            inflater,
            container,
            ActivityMainBinding::class.java,
            Creator
        ), EmbeddedDataNodeAPI.ViewHolderCreator<BrowserState.ViewHolder> by Creator {
        override fun retrieveContainer(): ViewGroup {
            return view.mainContent
        }

        private object Creator :
            EmbeddedDataNodeAPI.ViewHolderCreator<BrowserState.ViewHolder> {
            override fun createChild(
                inflater: LayoutInflater,
                container: ViewGroup?
            ): BrowserState.ViewHolder {
                return BrowserState.ViewHolder(inflater, container)
            }
        }
    }

    override val title_ui: String
        get() = "冰河导航"

    override val useWhiteBarText_ui: Boolean
        get() = true

    override fun createChild(inflater: LayoutInflater, container: ViewGroup?): ViewHolder {
        return ViewHolder(inflater, container)
    }

    override fun createChild(
        lifecycleOwner: WeakReference<LifecycleOwner>?,
        holder: ViewHolder?
    ): DataNode {
        return DataNode(lifecycleOwner, holder)
    }

    override fun handleAndroidHome() {
        super_onBackPressed()
    }

    override fun onBackPressed_ui() {
        currentBrowser?.onBackPressed_ui(::super_onBackPressed)?.also { consumed ->
            if (!consumed) {
                Toast.makeText(this, "已经是第一页", Toast.LENGTH_LONG).show()
            }
        } ?: super_onBackPressed()
    }
}