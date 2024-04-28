package pub.telephone.appKit.dataSource

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

interface ColorValue

interface ColorConfig<T : Config<*>> : Config<T> {
    @Suppress("UNCHECKED_CAST")
    infix fun <Common, T : ColorConfig<*>, R : ColorConfig<*>> of(
        manager: ColorManager<Common, T, R>
    ): R? = this as? R
}

@JvmInline
value class CV(val color: Color) : ColorValue

@JvmInline
value class ICV(@JvmField @ColorInt val color: Int) : ColorValue

class ColorSelector(vararg pairs: Pair<ColorMode, ColorValue>) :
    Selector<ColorMode, CV, ICV>(ColorMode.fallback, mapOf(*pairs), { ICV(it.color.toArgb()) }),
    ColorValue


enum class ColorMode {
    DEFAULT,
    NIGHT;

    companion object {
        internal val fallback = DEFAULT
    }

    fun calc(from: ColorConfig<*>): ColorConfig<*> = Selector.transform(from.copyConfig(), this)
}

class ColorManager<Common, T : ColorConfig<*>, R : ColorConfig<*>>(
    private var common: Common,
    @Suppress("UNUSED_PARAMETER") to: R?,
    private val from: (common: Common) -> T
) {
    private var night: Boolean? = null

    @Volatile
    var current: ColorConfig<*> = ColorMode.fallback.calc(from(common))

    val manager = DataNodeManager<DataNode<*>>()
    private fun calc(night: Boolean? = null, common: Common? = null): ColorConfig<*> {
        synchronized(this) {
            val nightF = night ?: this.night
            val commonF = common ?: this.common
            //
            return when (nightF!!) {
                true -> ColorMode.NIGHT
                false -> ColorMode.DEFAULT
            }.calc(from(commonF))
        }
    }

    fun of(night: Boolean? = null, common: Common? = null) = calc(night, common).of(this) as R

    fun commit(night: Boolean? = null, common: Common? = null) {
        synchronized(this) {
            if (
                (night == null || night == this.night) &&
                (common == null || common == this.common)
            ) {
                return
            }
            //
            this.night = night ?: this.night
            this.common = common ?: this.common
            //
            calc().also {
                current = it
                manager.CallOnAll { node ->
                    node.EmitChange_ui(mutableSetOf(node.ColorBinding.SetResult(it)))
                    null
                }
            }
        }
    }

    companion object {
        val manager = Manager<ColorManager<*, *, *>>()
    }
}