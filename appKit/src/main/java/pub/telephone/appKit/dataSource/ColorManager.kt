package pub.telephone.appKit.dataSource

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

interface Value

interface ColorConfig<T : Config<*>> : Config<T> {
    @Suppress("UNCHECKED_CAST")
    infix fun <T : ColorConfig<*>, R : ColorConfig<*>> of(manager: ColorManager<T, R>): R? =
        this as? R
}

@JvmInline
value class CV(val color: Color) : Value

class C(vararg pairs: Pair<Mode, Value>) :
    Selector<Mode, CV, Int>(Mode.fallback, mapOf(*pairs), { it.color.toArgb() }), Value


enum class Mode {
    DEFAULT,
    NIGHT;

    companion object {
        internal val fallback = DEFAULT
    }

    fun calc(from: ColorConfig<*>): ColorConfig<*> = Selector.transform(from.copy(), this)
}

class ColorManager<T : ColorConfig<*>, R : ColorConfig<*>>(
    private val from: T,
    @Suppress("UNUSED_PARAMETER") to: R?
) {
    @Volatile
    private var night: Boolean? = null

    @Volatile
    var current = Mode.fallback.calc(from)
    fun commit(night: Boolean) {
        if (night == this.night) {
            return
        }
        when (night) {
            true -> Mode.NIGHT
            false -> Mode.DEFAULT
        }.calc(from).also {
            synchronized(this) {
                if (night == this.night) {
                    return
                }
                this.night = night
                current = it
                DataNodeManager.DataNodeColor.CallOnAll { node ->
                    node.EmitChange_ui(mutableSetOf(node.Color.SetResult(it)))
                    null
                }
            }
        }
    }
}