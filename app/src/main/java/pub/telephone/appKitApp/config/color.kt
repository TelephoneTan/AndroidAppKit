package pub.telephone.appKitApp.config

import androidx.compose.ui.graphics.Color
import pub.telephone.appKit.dataSource.CV
import pub.telephone.appKit.dataSource.ColorConfig
import pub.telephone.appKit.dataSource.ColorManager
import pub.telephone.appKit.dataSource.ColorMode
import pub.telephone.appKit.dataSource.ColorSelector
import pub.telephone.appKit.dataSource.ColorValue
import pub.telephone.appKit.dataSource.ICV

data class ColorsActivity<T : ColorValue>(
    @JvmField var background: T,
    @JvmField var text: T,
) : ColorConfig<ColorsActivity<T>> {
    override fun copyConfig(): ColorsActivity<T> {
        return copy()
    }
}

data class ColorsMain<T : ColorValue>(
    @JvmField var text: T,
) : ColorConfig<ColorsMain<T>> {
    override fun copyConfig(): ColorsMain<T> {
        return copy()
    }
}

data class Colors<T : ColorValue>(
    @JvmField var activity: ColorsActivity<T>,
    @JvmField var main: ColorsMain<T>,
) : ColorConfig<Colors<T>> {
    override fun copyConfig(): Colors<T> {
        return copy()
    }
}

private object Palette {
    val black = ColorSelector(
        ColorMode.DEFAULT to CV(Color(0xff000000)),
    )
    val white = ColorSelector(
        ColorMode.DEFAULT to CV(Color(0xffffffff)),
    )
}

class CommonColors(
    val background: ColorValue,
    val text: ColorValue
) {
    companion object {
        val instance = CommonColors(
            background = ColorSelector(
                ColorMode.DEFAULT to Palette.white,
                ColorMode.NIGHT to CV(Color(0xff1e1e1e)),
            ),
            text = ColorSelector(
                ColorMode.DEFAULT to Palette.black,
                ColorMode.NIGHT to Palette.white,
            )
        )

        fun toColors(common: CommonColors) = Colors(
            activity = ColorsActivity(
                background = common.background,
                text = common.text
            ),
            main = ColorsMain(
                text = common.text
            ),
        )
    }
}

val colorManager = ColorManager(CommonColors.instance, null as Colors<ICV>?, CommonColors::toColors)
