package it.polito.mad.madmax.madmax

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


@SuppressLint("SimpleDateFormat")
@Throws(IOException::class)
fun createImageFile(context: Context): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)/*.apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }*/
}

@Throws(IOException::class)
fun handleSamplingAndRotationBitmap(context: Context, selectedImage: Uri): Bitmap? {
    val MAX_HEIGHT = 1024
    val MAX_WIDTH = 1024

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    var imageStream = context.contentResolver.openInputStream(selectedImage)
    BitmapFactory.decodeStream(imageStream, null, options)

    imageStream!!.close()

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    imageStream = selectedImage.let { context.contentResolver.openInputStream(it) }
    return BitmapFactory.decodeStream(imageStream, null, options)?.let { rotateImageIfRequired(context, it, selectedImage) }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {

        // Calculate ratios of height and width to requested height and width
        val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
        val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())

        // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
        // with both dimensions larger than or equal to the requested height and width.
        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio

        // This offers some additional logic in case the image has a strange
        // aspect ratio. For example, a panorama may have a much larger
        // width than height. In these cases the total pixels might still
        // end up being too large to fit comfortably in memory, so we should
        // be more aggressive with sample down the image (=larger inSampleSize).
        val totalPixels = width * height.toFloat()

        // Anything more than 2x the requested pixels we'll sample down further
        val totalReqPixelsCap = reqWidth * reqHeight * 2.toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
    }
    return inSampleSize
}

@Throws(IOException::class)
fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap? {
    val input = context.contentResolver.openInputStream(selectedImage)
    val ei: ExifInterface
    ei = if (Build.VERSION.SDK_INT > 23) ExifInterface(input!!) else ExifInterface(selectedImage.path!!)
    val orientation =
        ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
        else -> img
    }
}

fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(degree.toFloat())
    val rotatedImg =
        Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    img.recycle()
    return rotatedImg
}

// Helper function to display a toast
fun displayMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}