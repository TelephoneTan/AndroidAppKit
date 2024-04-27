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

        private fun decodeSampledBitmapFrom(ba: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
            return decodeSampledBitmapFrom(
                reqWidth,
                reqHeight,
            ) { options -> BitmapFactory.decodeByteArray(ba, 0, ba.size, options) }
        }

        fun getScreenSizePx(): Map.Entry<Int, Int> {
            val metrics: DisplayMetrics = MyApp.resources.displayMetrics
            return AbstractMap.SimpleEntry(metrics.widthPixels, metrics.heightPixels)
        }

        fun getHalfVMinPx(): Int {
            val (w, h) = getScreenSizePx()
            return (min(w.toDouble(), h.toDouble()) / 2).toInt()
        }

        fun getMagnifiedScreenSizePx(magnification: Float): Map.Entry<Int, Int> {
            val (w, h) = getScreenSizePx()
            return AbstractMap.SimpleEntry(
                (w * magnification).toInt(),
                (h * magnification).toInt()
            )
        }

        private fun decodeThumbnailBitmapFrom(decoder: (w: Int, h: Int) -> Bitmap?): Bitmap? {
            val sizePx = getHalfVMinPx()
            return decoder(sizePx, sizePx)
        }

        fun decodeThumbnailBitmapFrom(uri: Uri) =
            decodeThumbnailBitmapFrom { w, h -> decodeSampledBitmapFrom(uri, w, h) }

        fun decodeThumbnailBitmapFrom(resID: Int) =
            decodeThumbnailBitmapFrom { w, h -> decodeSampledBitmapFrom(resID, w, h) }

        fun decodeThumbnailBitmapFrom(ba: ByteArray) =
            decodeThumbnailBitmapFrom { w, h -> decodeSampledBitmapFrom(ba, w, h) }

        private fun decodeMagnifiedScreenBitmapFrom(
            magnification: Float,
            decoder: (w: Int, h: Int) -> Bitmap?
        ): Bitmap? {
            val (w, h) = getMagnifiedScreenSizePx(magnification)
            return decoder(w, h)
        }

        fun decodeMagnifiedScreenBitmapFrom(uri: Uri, magnification: Float) =
            decodeMagnifiedScreenBitmapFrom(magnification) { w, h ->
                decodeSampledBitmapFrom(
                    uri,
                    w,
                    h
                )
            }

        fun decodeMagnifiedScreenBitmapFrom(resID: Int, magnification: Float) =
            decodeMagnifiedScreenBitmapFrom(magnification) { w, h ->
                decodeSampledBitmapFrom(
                    resID,
                    w,
                    h
                )
            }

        fun decodeMagnifiedScreenBitmapFrom(ba: ByteArray, magnification: Float) =
            decodeMagnifiedScreenBitmapFrom(magnification) { w, h ->
                decodeSampledBitmapFrom(
                    ba,
                    w,
                    h
                )
            }

        private fun colorOfBitmap(decoder: (w: Int, h: Int) -> Bitmap?) =
            decoder(1, 1)?.getPixel(0, 0)

        fun colorOfBitmap(uri: Uri) = colorOfBitmap { w, h -> decodeSampledBitmapFrom(uri, w, h) }
        fun colorOfBitmap(resID: Int) =
            colorOfBitmap { w, h -> decodeSampledBitmapFrom(resID, w, h) }

        fun colorOfBitmap(ba: ByteArray) =
            colorOfBitmap { w, h -> decodeSampledBitmapFrom(ba, w, h) }

        private fun decodeScreenBitmapFrom(decoder: (w: Int, h: Int) -> Bitmap?) =
            decodeMagnifiedScreenBitmapFrom(1f, decoder)

        fun decodeScreenBitmapFrom(uri: Uri) =
            decodeScreenBitmapFrom { w, h -> decodeSampledBitmapFrom(uri, w, h) }

        fun decodeScreenBitmapFrom(resID: Int) =
            decodeScreenBitmapFrom { w, h -> decodeSampledBitmapFrom(resID, w, h) }

        fun decodeScreenBitmapFrom(ba: ByteArray) =
            decodeScreenBitmapFrom { w, h -> decodeSampledBitmapFrom(ba, w, h) }
    }
}