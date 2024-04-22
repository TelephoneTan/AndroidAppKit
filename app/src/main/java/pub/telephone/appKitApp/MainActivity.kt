package pub.telephone.appKitApp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import pub.telephone.appKit.AppKit
import pub.telephone.appKit.browser.BrowserState
import pub.telephone.appKit.dataSource.ColorConfig
import pub.telephone.appKit.dataSource.EmbeddedDataNode
import pub.telephone.appKit.dataSource.EmbeddedDataNodeAPI
import pub.telephone.appKit.dataSource.TagKey
import pub.telephone.appKitApp.config.colorManager
import pub.telephone.appKitApp.databinding.ActivityMainBinding
import pub.telephone.javapromise.async.promise.Promise
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

private typealias MainActivityINFO = Any?

class MainActivity : Activity<MainActivity.ViewHolder, MainActivity.DataNode>() {
    private val currentBrowserR = AtomicReference<BrowserState>()
    private val currentBrowser: BrowserState? get() = currentBrowserR.get()
    private val browserCreator =
        object :
            EmbeddedDataNodeAPI.DataNodeCreator<BrowserState.UI, MainActivityINFO, BrowserState> {
            override fun createChild(
                lifecycleOwner: WeakReference<LifecycleOwner>?,
                holder: BrowserState.UI?
            ): BrowserState {
                return BrowserState(
                    lifecycleOwner,
                    holder,
                    "https://www.apple.com.cn/"
                ) {
                    title = it
                }.also { currentBrowserR.set(it) }
            }
        }

    data class ImagePack(val bitmap: Bitmap, @ColorInt val color: Int)

    inner class DataNode(
        lifecycleOwner: WeakReference<LifecycleOwner>?,
        holder: MainActivity.ViewHolder?
    ) :
        EmbeddedDataNode<BrowserState.UI, ViewHolder, MainActivityINFO, BrowserState>(
            lifecycleOwner, holder, browserCreator
        ),
        EmbeddedDataNodeAPI.DataNodeCreator<BrowserState.UI, MainActivityINFO, BrowserState> by browserCreator {
        override fun loadKey(): TagKey {
            return TagKey(R.id.tagKey_MainActivityLoad, R.id.tagInitKey_MainActivityLoad)
        }

        init {
            watchColor()
        }

        private val image: Binding<ImagePack?> = bindTask(
            TagKey(R.id.tagKey_MainActivityImage, R.id.tagInitKey_MainActivityImage),
            RetrySharedTask.Simple {
                Promise.Resolve(
                    R.drawable.leaf.let { id ->
                        AppKit.colorOfBitmap(id)?.let { color ->
                            ImagePack(
                                bitmap = BitmapFactory.decodeResource(resources, id),
                                color = color
                            )
                        }
                    }
                )
            }
        )

        override fun color_ui(holder: MainActivity.ViewHolder, colors: ColorConfig<*>) {
            colors.of(colorManager)?.let { c ->
                holder.view.input.setTextColor(c.main.text)
            }
//            EmitChange_ui(mutableSetOf(image.ReInit()))
        }

        override fun __bind__(changedBindingKeys: MutableSet<Int>?) {
//            image.Bind(changedBindingKeys) { holder, pack ->
//                pack?.run {
//                    background_ui = BitmapDrawable(resources, bitmap)
//                    applyBackgroundColor_ui(color)
//                    colorsOf(if (isLightColor(color)) Mode.DEFAULT else Mode.NIGHT)?.let { c ->
//                        setTextColor_ui(titleColor(c))
//                        holder.view.input.setTextColor(c.main.text)
//                    }
//                }
//                null
//            }
        }
    }

    class ViewHolder(inflater: LayoutInflater, container: ViewGroup?) :
        EmbeddedDataNode.ViewHolder<ActivityMainBinding, BrowserState.UI>(
            inflater,
            container,
            ActivityMainBinding::class.java,
            Creator
        ), EmbeddedDataNodeAPI.ViewHolderCreator<BrowserState.UI> by Creator {
        override fun retrieveContainer(): ViewGroup {
            return view.mainContent
        }

        private object Creator :
            EmbeddedDataNodeAPI.ViewHolderCreator<BrowserState.UI> {
            override fun createChild(
                inflater: LayoutInflater,
                container: ViewGroup?
            ): BrowserState.UI {
                return BrowserState.UI(inflater, container)
            }
        }
    }

    override val title_ui: String
        get() = "冰河导航"

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

    override val menu_ui: Int
        get() = R.menu.browser

    override fun isMenuShownAsAction_ui(m: MenuItem): Boolean {
        return m.itemId != R.id.refresh
    }
}