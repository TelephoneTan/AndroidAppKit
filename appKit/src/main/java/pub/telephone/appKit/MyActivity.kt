package pub.telephone.appKit

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.forEach
import androidx.lifecycle.LifecycleOwner
import pub.telephone.appKit.dataSource.ColorConfig
import pub.telephone.appKit.dataSource.ColorManager
import pub.telephone.appKit.dataSource.DataNode
import pub.telephone.appKit.dataSource.DataViewHolder
import pub.telephone.appKit.dataSource.EmbeddedDataNode
import pub.telephone.appKit.dataSource.EmbeddedDataNodeAPI
import pub.telephone.appKit.dataSource.TagKey
import pub.telephone.appKit.databinding.MyActivityBinding
import java.lang.ref.WeakReference

private typealias MyActivityINFO = Any?

abstract class MyActivity<CH : DataViewHolder<*>, CD : DataNode<CH>>
    : AppCompatActivity(), EmbeddedDataNodeAPI.All<CH, MyActivityINFO, CD> {
    inner class UI(inflater: LayoutInflater, parent: ViewGroup?) :
        EmbeddedDataNode.ViewHolder<MyActivityBinding, CH>(
            inflater, parent, MyActivityBinding::class.java, this
        ) {
        override fun retrieveContainer(): ViewGroup {
            return view.myActivityContent
        }
    }

    private val defaultColorManager: ColorManager<*, *, *>? = MyApp.myColorManager

    protected open val myColorManager: ColorManager<*, *, *>? = null

    inner class State(
        lifecycleOwner: WeakReference<LifecycleOwner>?,
        holder: MyActivity<CH, CD>.UI?
    ) : EmbeddedDataNode<CH, UI, MyActivityINFO, CD>(
        lifecycleOwner, holder, this
    ) {
        init {
            watchColor()
        }

        override fun loadKey(): TagKey {
            return TagKey.MyActivityLoad
        }

        override fun getMyColorManager(): ColorManager<*, *, *>? {
            return this@MyActivity.myColorManager ?: super.getMyColorManager()
        }

        override fun color_ui(holder: MyActivity<CH, CD>.UI, colors: ColorConfig<*>) {
            if (myColorManager != defaultColorManager) {
                return
            }
            backgroundColor_ui(colors).also {
                holder.view.myActivityBackground.setBackgroundColor(it)
                applyBackgroundColor_ui(it)
            }
            setTextColor_ui(holder, titleColor_ui(colors))
        }
    }

    protected open fun noTitle() = false
    protected open fun noHome() = false

    @Suppress("FunctionName")
    protected abstract fun backgroundColor_ui(colors: ColorConfig<*>): Int

    @Suppress("FunctionName")
    protected abstract fun titleColor_ui(colors: ColorConfig<*>): Int

    @Suppress("PropertyName")
    protected abstract val title_ui: String
    private var insetsController: WindowInsetsControllerCompat? = null

    @Suppress("FunctionName")
    protected fun applyBackgroundColor_ui(useWhiteText: Boolean) {
        insetsController?.apply {
            isAppearanceLightStatusBars = !useWhiteText
            isAppearanceLightNavigationBars = !useWhiteText
        }
    }

    @Suppress("FunctionName")
    protected fun applyBackgroundColor_ui(@ColorInt color: Int) {
        applyBackgroundColor_ui(isLightColor(color).not())
    }

    companion object {
        private val onApplyWindowInsetsListener = OnApplyWindowInsetsListener { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.findViewById<View>(R.id.my_activity_foreground)
                .setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        fun isLightColor(@ColorInt color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) >= 0.5
        }
    }

    @Suppress("FunctionName")
    protected open fun handleAndroidHome_ui() {
        handleOnBackPressed()
    }

    @Suppress("FunctionName")
    protected open fun handleOptionsItemSelected_ui(itemID: Int) {
    }

    final override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (val id = item.itemId) {
            android.R.id.home -> handleAndroidHome_ui()
            else -> handleOptionsItemSelected_ui(id)
        }
        return true
    }

    @Suppress("OVERRIDE_DEPRECATION")
    final override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    @Suppress("FunctionName")
    protected fun super_onBackPressed_ui() {
        onBackPressedCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
    }

    @Suppress("FunctionName")
    protected open fun onBackPressed_ui() {
        super_onBackPressed_ui()
    }

    private fun handleOnBackPressed() {
        onBackPressed_ui()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            this@MyActivity.handleOnBackPressed()
        }
    }

    private var holder: UI? = null

    @Suppress("FunctionName")
    protected open fun onSplash_ui() {
    }

    @Suppress("FunctionName")
    protected open fun onCreated_ui(savedInstanceState: Bundle?) {
    }

    @Suppress("PropertyName")
    protected open val noElevation_ui = true

    @Suppress("FunctionName")
    protected open fun beforeCreateChild_ui() {
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        onSplash_ui()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        enableEdgeToEdge()
        window.apply {
            navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                navigationBarDividerColor = Color.TRANSPARENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isNavigationBarContrastEnforced = false
            }
        }.apply {
            statusBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isStatusBarContrastEnforced = false
            }
        }
        beforeCreateChild_ui()
        val holder = UI(layoutInflater, null)
        this.holder = holder
        ViewCompat.setOnApplyWindowInsetsListener(holder.itemView, onApplyWindowInsetsListener)
        insetsController = WindowCompat.getInsetsController(window, holder.itemView)
        holder.view.toolBar.takeUnless { noTitle() }?.let {
            it.takeIf { noHome() }?.navigationIcon = null
            it.takeIf { noElevation_ui }?.elevation = 0f
            setSupportActionBar(it)
        } ?: let {
            holder.view.toolBar.visibility = View.GONE
        }
        title = title_ui
        setContentView(holder.itemView)
        State(WeakReference(this), holder).EmitChange_ui(null)
        //
        onCreated_ui(savedInstanceState)
    }

    final override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        onCreate(savedInstanceState)
    }

    @SuppressLint("MissingSuperCall")
    final override fun onSaveInstanceState(outState: Bundle) {
    }

    final override fun onSaveInstanceState(
        outState: Bundle,
        outPersistentState: PersistableBundle
    ) {
        onSaveInstanceState(outState)
    }

    final override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    @Suppress("PropertyName")
    protected var background_ui
        get() = holder?.view?.myActivityBackground?.drawable
        set(value) {
            holder?.view?.myActivityBackground?.setImageDrawable(value)
        }

    @Suppress("FunctionName")
    protected open fun isMenuShownAsAction_ui(m: MenuItem) = true

    @Suppress("FunctionName")
    private fun setTextColor_ui(holder: UI?, @ColorInt color: Int) {
        holder?.view?.toolBar?.apply {
            setTitleTextColor(color)
            setNavigationIconTint(color)
            overflowIcon?.apply {
                colorFilter = null
                setTint(color)
            }
            menuColor_ui = color
            renderMenu_ui(menu)
        }
    }

    @Suppress("FunctionName")
    protected fun setTextColor_ui(@ColorInt color: Int) = setTextColor_ui(holder, color)

    @Suppress("PropertyName")
    protected open val menu_ui: Int? = null

    @Suppress("PrivatePropertyName")
    private var menuColor_ui: Int? = null

    @Suppress("FunctionName")
    private fun renderMenu_ui(menu: Menu?): Menu? {
        menuColor_ui?.let { color ->
            menu?.forEach menuForEach@{ m ->
                if (!isMenuShownAsAction_ui(m)) {
                    return@menuForEach
                }
                m.title = m.title?.let {
                    SpannableString(it.toString()).apply {
                        setSpan(
                            ForegroundColorSpan(color),
                            0,
                            it.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }
        return menu
    }

    final override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu_ui?.also {
            menuInflater.inflate(it, menu)
            renderMenu_ui(menu)
        }
        return true
    }

    final override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        renderMenu_ui(menu)
        return true
    }
}