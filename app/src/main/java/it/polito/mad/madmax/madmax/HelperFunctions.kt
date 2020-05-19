package it.polito.mad.madmax.madmax

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern

// Compress an image into a new file
fun compressImage(context: Context, path: String): Uri {
    // Options to sample the image
    val options = BitmapFactory.Options().apply {
        inSampleSize = 2
    }

    // Crate a new file to store the compressed image
    return createImageFile(context).let { file ->
        // Open the original image as a stream
        context.contentResolver.openInputStream(Uri.parse(path)).also { stream ->
            // Decode the bitmap and compress it
            // Also rotate if necessary
            BitmapFactory.decodeStream(stream, null, options)?.also { bitmap ->
                rotateImageIfRequired(context, bitmap, path).compress(Bitmap.CompressFormat.JPEG, 25, file.outputStream())
            }
            stream?.close()
        }

        // Delete original image if was captured with the app
        if (path.contains(context.getString(R.string.file_provider))) {
            context.contentResolver.delete(Uri.parse(path), null, null)
        }

        // Return the new image uri
        FileProvider.getUriForFile(context, context.getString(R.string.file_provider), file)
    }
}

// Use EXIF to determine if the image must be rotated
fun rotateImageIfRequired(context: Context, bitmap: Bitmap, path: String): Bitmap {
    val ei = ExifInterface(context.contentResolver.openInputStream(Uri.parse(path))!!)/*if (Build.VERSION.SDK_INT >= 23) {

    } else {
        ExifInterface(Uri.parse(path).path!!)
    }*/
    return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270)
        else -> bitmap
    }
}

// Rotate bitmap
fun rotateImage(bitmap: Bitmap, angle: Int): Bitmap {
    val matrix = Matrix().apply {
        postRotate(angle.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Create jpeg file using current datetime
fun createImageFile(context: Context): File {
    return File.createTempFile(
        "JPEG_${DateFormat.getDateTimeInstance().format(Date())}_",
        ".jpg",
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    )
}

// Check if an email is valid
fun isEmailValid(email: String): Boolean {
    return Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
            + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
            + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
            + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
    ).matcher(email).matches()
}

// Helper function to display a toast
fun displayMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

// Convert dp to px
fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()