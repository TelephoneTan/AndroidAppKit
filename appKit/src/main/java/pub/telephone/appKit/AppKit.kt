package pub.telephone.appKit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.DisplayMetrics
import java.util.AbstractMap
import kotlin.math.min

class AppKit {
    companion object {
        private fun overflow(w: Int, h: Int): Boolean {
            return w * h * 4 > 90 * 1024 * 1024
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth || overflow(width, height)) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth ||
                    overflow(width / inSampleSize, height / inSampleSize)
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        private fun decodeSampledBitmapFrom(
            reqWidth: Int,
            reqHeight: Int,
            decoder: (BitmapFactory.Options?) -> Bitmap?
        ): Bitmap? {
            //
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            decoder.invoke(options)
            if (options.outWidth < 0 || options.outHeight < 0) {
                return null
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return decoder.invoke(options)
        }

        private fun decodeSampledBitmapFrom(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
            return decodeSampledBitmapFrom(
                reqWidth,
                reqHeight
            ) { options -> BitmapFactory.decodeFile(uri.path, options) }
        }

        private fun decodeSampledBitmapFrom(resID: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
            return decodeSampledBitmapFrom(
                reqWidth,
                reqHeight,
            ) { options -> BitmapFactory.decodeResource(MyApp.resources, resID, options) }
        }

        private fun getScreenSizePx(): Map.Entry<Int, Int> {
            val metrics: DisplayMetrics = MyApp.resources.displayMetrics
            return AbstractMap.SimpleEntry(metrics.widthPixels, metrics.heightPixels)
        }

        private fun getHalfVMinPx(): Int {
            val (w, h) = getScreenSizePx()
            return (min(w.toDouble(), h.toDouble()) / 2).toInt()
        }

        private fun getMagnifiedScreenSizePx(magnification: Float): Map.Entry<Int, Int> {
            val (w, h) = getScreenSizePx()
            return AbstractMap.SimpleEntry(
                (w * magnification).toInt(),
                (h * magnification).toInt()
            )
        }

        fun decodeThumbnailBitmapFrom(uri: Uri): Bitmap? {
            val sizePx = getHalfVMinPx()
            return decodeSampledBitmapFrom(uri, sizePx, sizePx)
        }

        fun decodeMagnifiedScreenBitmapFrom(uri: Uri, magnification: Float): Bitmap? {
            val (w, h) = getMagnifiedScreenSizePx(magnification)
            return decodeSampledBitmapFrom(uri, w, h)
        }

        private fun decodeOnePixelBitmapFrom(uri: Uri) = decodeSampledBitmapFrom(uri, 1, 1)
        fun colorOfBitmap(uri: Uri) = decodeOnePixelBitmapFrom(uri)?.getPixel(0, 0)

        fun decodeScreenBitmapFrom(uri: Uri) = decodeMagnifiedScreenBitmapFrom(uri, 1f)


        fun decodeMagnifiedScreenBitmapFrom(resID: Int, magnification: Float): Bitmap? {
            val (w, h) = getMagnifiedScreenSizePx(magnification)
            return decodeSampledBitmapFrom(resID, w, h)
        }

        private fun decodeOnePixelBitmapFrom(resID: Int) = decodeSampledBitmapFrom(resID, 1, 1)
        fun colorOfBitmap(resID: Int) = decodeOnePixelBitmapFrom(resID)?.getPixel(0, 0)

        fun decodeScreenBitmapFrom(resID: Int) = decodeMagnifiedScreenBitmapFrom(resID, 1f)
    }
}