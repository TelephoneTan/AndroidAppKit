package pub.telephone.appKit.dataSource

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

interface Value

interface ColorConfig<T : Config<*>> : Config<T> {
    @Suppress("UNCHECKED_CAST")
    infix fun <Common, T : ColorConfig<*>, R : ColorConfig<*>> of(
        manager: ColorManager<Common, T, R>
    ): R? = this as? R
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

class ColorManager<Common, T : ColorConfig<*>, R : ColorConfig<*>>(
    private var common: Common,
    @Suppress("UNUSED_PARAMETER") to: R?,
    private val from: (common: Common) -> T
) {
    private var night: Boolean? = null

    @Volatile
    var current = Mode.fallback.calc(from(common))
    fun calc(night: Boolean? = null, common: Common? = null): ColorConfig<*> {
        synchronized(this) {
            val nightF = night ?: this.night
            val commonF = common ?: this.common
            //
            return when (nightF!!) {
                true -> Mode.NIGHT
                false -> Mode.DEFAULT
            }.calc(from(commonF))
        }
    }

    fun commit(night: Boolean? = null, common: Common? = null) {
        synchronized(this) {
            if (
                (night != null && night == this.night) &&
                (common != null && common == this.common)
            ) {
                return
            }
            //
            this.night = night ?: this.night
            this.common = common ?: this.common
            //
            calc().also {
                current = it
                DataNodeManager.DataNodeColor.CallOnAll { node ->
                    node.EmitChange_ui(mutableSetOf(node.Color.SetResult(it)))
                    null
                }
            }
        }
    }

    companion object {
        val manager = Manager<ColorManager<*, *, *>>()
    }
}