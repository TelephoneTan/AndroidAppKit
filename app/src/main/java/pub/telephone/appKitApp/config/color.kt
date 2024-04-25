package pub.telephone.appKitApp.config

import androidx.compose.ui.graphics.Color
import pub.telephone.appKit.dataSource.C
import pub.telephone.appKit.dataSource.CV
import pub.telephone.appKit.dataSource.ColorConfig
import pub.telephone.appKit.dataSource.ColorManager
import pub.telephone.appKit.dataSource.Mode

data class ColorsActivity<T>(
    @JvmField var background: T,
    @JvmField var text: T,
) : ColorConfig<ColorsActivity<T>> {
    override fun copy(): ColorsActivity<T> {
        return copy(background = background)
    }
}

data class ColorsMain<T>(
    @JvmField var text: T,
) : ColorConfig<ColorsMain<T>> {
    override fun copy(): ColorsMain<T> {
        return copy(text = text)
    }
}

data class Colors<T>(
    @JvmField var activity: ColorsActivity<T>,
    @JvmField var main: ColorsMain<T>,
) : ColorConfig<Colors<T>> {
    override fun copy(): Colors<T> {
        return copy(activity = activity)
    }
}

private object Palette {
    val black = C(
        Mode.DEFAULT to CV(Color(0xff000000)),
    )
    val white = C(
        Mode.DEFAULT to CV(Color(0xffffffff)),
    )
}

class Common(
    val background: C = C(
        Mode.DEFAULT to Palette.white,
        Mode.NIGHT to CV(Color(0xff1e1e1e)),
    ),
    val text: C = C(
        Mode.DEFAULT to Palette.black,
        Mode.NIGHT to Palette.white,
    )
)

val colorManager = ColorManager(Common(), null as Colors<Int>?) { common ->
    Colors(
        activity = ColorsActivity(
            background = common.background,
            text = common.text
        ),
        main = ColorsMain(
            text = common.text
        ),
    )
}
