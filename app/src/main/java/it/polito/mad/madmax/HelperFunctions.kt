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
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

fun displayMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

fun closeKeyboard(activity: Activity) {
    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
}

fun showProgress(activity: Activity) {
    closeKeyboard(activity)
    activity.main_progress.visibility = View.VISIBLE
}

fun hideProgress(activity: Activity) {
    activity.main_progress.visibility = View.GONE
}

fun showFab(activity: Activity, clickListener: View.OnClickListener, drawable: Drawable?) {
    activity.main_fab.apply {
        scaleX = 1F
        scaleY = 1F
        alpha = 1F
        setOnClickListener(clickListener)
        setImageDrawable(drawable)
        visibility = View.VISIBLE
    }
}

fun hideFab(activity: Activity) {
    activity.main_fab.visibility = View.GONE
}

fun deletePhoto(context: Context, path: String) {
    if (path.contains(context.getString(R.string.file_provider))) {
        context.contentResolver.delete(Uri.parse(path), null, null)
    }
}

fun getFragmentSpaceSize(context: Context): Point {
    return Point().also {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(it)
    }
}

fun guidelineConstrain(context: Context, guideline: Guideline) {
    val begin: Int = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        getFragmentSpaceSize(context).x
    } else {
        getFragmentSpaceSize(context).y
    }
    guideline.apply {
        if (begin > 0) {
            setGuidelineBegin((0.33 * begin).toInt())
        } else {
            setGuidelinePercent(0.33F)
        }
    }
}

var cardRadiusConstrain = View.OnLayoutChangeListener { v: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
    (v as MaterialCardView).apply {
        radius = measuredHeight / 2F
    }
}

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun getMainCategories(context: Context): Array<String> {
    return arrayOf("") + context.resources.getStringArray(R.array.item_categories_main)
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
            arrayOf("") + context.resources.getStringArray(it)
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
    return ArrayAdapter(context, com.google.android.material.R.layout.support_simple_spinner_dropdown_item, getSubcategories(context, mainCat)).apply {
        setDropDownViewResource(com.google.android.material.R.layout.support_simple_spinner_dropdown_item)
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
                rotateImageIfRequired(context, bitmap, path).compress(Bitmap.CompressFormat.JPEG, 50, file.outputStream())
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

fun openPhotoDialog(context: Context, activity: Activity, dialogSetter: (String) -> Unit, captureImage: () -> Unit, getImageFromGallery: () -> Unit, removeImage: () -> Unit){
    closeKeyboard(activity)
    activity.packageManager?.also { pm ->
        val items = if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            arrayOf<CharSequence>(context.getString(R.string.dialog_change_take), context.getString(R.string.dialog_change_gallery), context.getString(R.string.dialog_change_remove))
        } else {
            arrayOf<CharSequence>(context.getString(R.string.dialog_change_gallery), context.getString(R.string.dialog_change_remove))
        }
        dialogSetter("Change")
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_change_title)
            .setItems(items) { _, which ->
                dialogSetter("")
                when (items[which]) {
                    context.getString(R.string.dialog_change_take) -> captureImage()
                    context.getString(R.string.dialog_change_gallery) -> getImageFromGallery()
                    else -> removeImage()
                }
            }
            .setOnCancelListener { dialogSetter("") }
            .show()
    }
}

fun getColorIdCategory(category: String): Int {
    return when(category) {
        "Arts & Crafts" -> R.color.cat_0
        "Sports & Hobby" -> R.color.cat_1
        "Baby" -> R.color.cat_2
        "Women\'s fashion" -> R.color.cat_3
        "Men\'s fashion" -> R.color.cat_4
        "Electronics" -> R.color.cat_5
        "Games & Videogames" -> R.color.cat_6
        "Automotive" -> R.color.cat_7
        else -> -1
    }
}

fun getAddressFromLocation(context: Context, location: LatLng): String {
    return Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)[0].getAddressLine(0)
}

fun getLocationFromAddress(context: Context, address: String): LatLng? {
    return Geocoder(context, Locale.getDefault()).getFromLocationName(address, 1).let { list ->
        if (list.size > 0)
            list[0].let {
                LatLng(it.latitude, it.longitude)
            }
        else {
            null
        }
    }
}