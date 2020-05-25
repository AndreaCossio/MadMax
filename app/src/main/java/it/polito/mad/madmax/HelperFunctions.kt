package it.polito.mad.madmax

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

// Helper function to display a toast
// TODO better message system?
fun displayMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

fun closeKeyboard(activity: Activity) {
    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
}

fun showProgress(activity: Activity) {
    closeKeyboard(activity)
    activity.findViewById<ConstraintLayout>(R.id.main_progress).visibility = View.VISIBLE
}

fun hideProgress(activity: Activity) {
    activity.findViewById<ConstraintLayout>(R.id.main_progress).visibility = View.GONE
}

fun hideFab(activity: Activity) {
    activity.main_fab_add_item.visibility = View.GONE
}

fun showFab(activity: Activity) {
    activity.main_fab_add_item.visibility = View.VISIBLE
}

fun deletePhoto(context: Context, path: String) {
    if (path.contains(context.getString(R.string.file_provider))) {
        context.contentResolver.delete(Uri.parse(path), null, null)
    }
}

fun getScreenSize(context: Context): Point {
    return Point().also {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(it)
    }
}

fun guidelineConstrain(context: Context, guideline: Guideline) {
    guideline.apply {
        val begin = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getScreenSize(context).x
        } else {
            getScreenSize(context).y
        }
        if (begin > 0) {
            setGuidelineBegin((0.33 * begin).toInt())
        } else {
            setGuidelinePercent(0.33F)
        }
    }
}

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun getMainCategories(context: Context): Array<String> {
    return context.resources.getStringArray(R.array.item_categories_main)
}

fun getSubcategories(context: Context, mainCat: String): Array<String> {
    return when(mainCat) {
        "Arts & Crafts" -> R.array.item_categories_sub_art_and_crafts
        "Sports & Hobby" -> R.array.item_categories_sub_sports_and_hobby
        "Baby" -> R.array.item_categories_sub_baby
        "Women\'s fashion" -> R.array.item_categories_sub_womens_fashion
        "Men\'s fashion" -> R.array.item_categories_sub_mens_fashion
        "Electronics" -> R.array.item_categories_sub_electronics
        "Games & Videogames" -> R.array.item_categories_sub_games_and_videogames
        "Automotive" -> R.array.item_categories_sub_automotive
        else -> -1
    }.let {
        if (it == -1)
            arrayOf("")
        else
            context.resources.getStringArray(it)
    }
}

// Return an array adapter for the main categories
fun getMainCategoryAdapter(context: Context): ArrayAdapter<String> {
    return ArrayAdapter(context, com.google.android.material.R.layout.support_simple_spinner_dropdown_item, getMainCategories(context)).apply {
        setDropDownViewResource(com.google.android.material.R.layout.support_simple_spinner_dropdown_item)
    }
}

// Return an array adapter for the main categories
fun getSubCategoryAdapter(context: Context, mainCat: String): ArrayAdapter<String> {
    return ArrayAdapter(context, R.layout.support_simple_spinner_dropdown_item, getSubcategories(context, mainCat)).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}

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
        deletePhoto(context, path)

        // Return the new image uri
        FileProvider.getUriForFile(context, context.getString(R.string.file_provider), file)
    }
}

// Use EXIF to determine if the image must be rotated
fun rotateImageIfRequired(context: Context, bitmap: Bitmap, path: String): Bitmap {
    val ei = if (Build.VERSION.SDK_INT >= 23) {
        ExifInterface(context.contentResolver.openInputStream(Uri.parse(path))!!)
    } else {
        ExifInterface(Uri.parse(path).path!!)
    }
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
        "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())}_",
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

fun openSureDialog(context: Context, activity: Activity, positiveAction: () -> Unit, dialogSetter: (String) -> Unit) {
    closeKeyboard(activity)
    dialogSetter("Sure")
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.photo_dialog_sure_title)
        .setMessage(R.string.photo_dialog_sure_text)
        .setPositiveButton(R.string.photo_dialog_ok) { _, _ ->
            dialogSetter("")
            positiveAction()
        }
        .setNegativeButton(R.string.photo_dialog_cancel) { _, _ -> dialogSetter("") }
        .setOnCancelListener { dialogSetter("") }
        .show()
}

fun openPhotoDialog(context: Context, activity: Activity, dialogSetter: (String) -> Unit, captureImage: () -> Unit, getImageFromGallery: () -> Unit, removeImage: () -> Unit) {
    closeKeyboard(activity)
    dialogSetter("Change")
    activity.packageManager?.also { pm ->
        val items = if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            arrayOf<CharSequence>(context.getString(R.string.dialog_change_take), context.getString(R.string.dialog_change_gallery), context.getString(R.string.dialog_change_remove))
        } else {
            arrayOf<CharSequence>(context.getString(R.string.dialog_change_gallery), context.getString(R.string.dialog_change_remove))
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_change_title)
            .setItems(items) { _, which ->
                dialogSetter("")
                when(items[which]) {
                    context.getString(R.string.dialog_change_take) -> captureImage()
                    context.getString(R.string.dialog_change_gallery) -> getImageFromGallery()
                    else -> removeImage()
                }
            }
            .setOnCancelListener { dialogSetter("") }
            .show()
    }
}