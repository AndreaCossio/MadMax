package it.polito.mad.madmax.madmax

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_edit_profile.*
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*


class EditProfileActivity : AppCompatActivity() {

    private var user: User? = null
    private var photoFile: File? = null
    private var uri:Uri?=null
    private val CAPTURE_IMAGE_REQUEST = 1
    private val GALLERY_IMAGE_REQUEST = 2
    private lateinit var mCurrentPhotoPath: String
    private var imageBitmap = MutableLiveData<Bitmap>();

    // On Create actions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        setContentView(R.layout.activity_edit_profile)

        // Add click listeners
        profile_image.setOnClickListener { selectImage(this) }
        imageBitmap.observe(this, Observer {  (profile_image as CircleImage).setImageBitmap(it) })

        // Get data from intent
        user = intent.getSerializableExtra(R.string.intent_user.toString()) as User?
        updateFields()
    }

    // Inflate options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_save_profile, menu)
        return true
    }

    // Handle clicks on the options menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // TODO check empty fields
        return when (item.itemId) {
            // Save button -> save profile
            R.id.save_profile -> {
                // Get user data
                updateUser()

                // Save user data to shared pref
                val prefs = getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString(R.string.preference_file_user_profile.toString(), Gson().toJson(user))
                    commit()
                }

                // Send result
                val intent = Intent().apply {
                    putExtra(R.string.intent_user.toString(), user)

                }
                setResult(Activity.RESULT_OK, intent)
                finish()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Get current fields values
        updateUser()

        // Save fields and photo into the Bundle
        outState.putSerializable("user", user as Serializable?)
        //outState.putParcelable("bitmap", imageBitmap.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // Retrieve fields and photo from the Bundle
        user = savedInstanceState.getSerializable("user") as User?
        //imageBitmap.value= savedInstanceState.getParcelable("bitmap")

        // Restore fields
        updateFields()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            //var myBitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            uri = Uri.fromFile(photoFile)
            updateUser()
            updateFields()
        }
        else if(requestCode == GALLERY_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data!=null)
        {
           uri=Uri.parse("file://"+getRealPathFromURI(data.data,this))
           updateUser()
           updateFields()
        }

        else {
            displayMessage(baseContext, "Request cancelled or something went wrong.")
        }
    }
    fun getRealPathFromURI(contentURI: Uri?, context: Activity): String? {
        val projection =
            arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.managedQuery(
            contentURI, projection, null,
            null, null
        )
            ?: return null
        val column_index = cursor
            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        return if (cursor.moveToFirst()) {
            // cursor.close();
            cursor.getString(column_index)
        } else null
        // cursor.close();
    }
    // Update user variable
    private fun updateUser() {
        user = User(
            name_tiet.text.toString(),
            nickname_tiet.text.toString(),
            email_tiet.text.toString(),
            location_tiet.text.toString(),
            uri?.toString()
        )
    }
    // Update views using the local variable user
    private fun updateFields() {
        if (user != null) {
            name_tiet.setText(user!!.name)
            nickname_tiet.setText(user!!.nickname)
            email_tiet.setText(user!!.email)
            location_tiet.setText(user!!.location)
            if (user!!.uri != null) {
                uri = Uri.parse(user!!.uri)
                val bi: Bitmap = handleSamplingAndRotationBitmap(this, uri!!)!!
                imageBitmap.value = bi
            }
            //if there is a profile picture display it, if not display the standard user avatar
        }
    }
    private fun selectImage(context: Context) {
        val options =
            arrayOf<CharSequence>("Take Photo", "Choose from Gallery")
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val tv:TextView=TextView(this)
        tv.text = "Choose your profile picture"
        tv.setTextColor(resources.getColor(R.color.colorPrimary))
        tv.gravity=Gravity.CENTER_VERTICAL
        tv.setPadding(60,60,10,10);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,24f)
        builder.setCustomTitle(tv)
        builder.setItems(options) { dialog, item ->
            if (options[item] == "Take Photo") {
                captureImage()
            } else if (options[item] == "Choose from Gallery") {
                getImageFromGallery()
            }
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
        Toast.makeText(this,"Canceled",Toast.LENGTH_LONG)
            .show()
            dialog.cancel()
        }
        builder.show()
    }

    private fun captureImage() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0
            )
        } else {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                try {

                    photoFile = createImageFile()
                    displayMessage(baseContext, photoFile!!.getAbsolutePath())
                    Log.i("Mayank", photoFile!!.getAbsolutePath())

                    // Continue only if the File was successfully created
                    if (photoFile != null) {


                        var photoURI = FileProvider.getUriForFile(
                            this,
                            "it.polito.mad.madmax.madmax.fileprovider",
                            photoFile!!
                        )

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)

                    }
                } catch (ex: Exception) {
                    // Error occurred while creating the File
                    displayMessage(baseContext, "Capture Image Bug: " + ex.message.toString())
                }


            } else {
                displayMessage(baseContext, "Nullll")
            }
        }


    }

    private fun getImageFromGallery(){
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickPhoto, GALLERY_IMAGE_REQUEST)
    }
    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
    }

    private fun displayMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                captureImage()
            }
        }

    }


}


@Throws(IOException::class)
fun handleSamplingAndRotationBitmap(context: Context, selectedImage: Uri): Bitmap? {
    val MAX_HEIGHT = 1024
    val MAX_WIDTH = 1024

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    var imageStream =
        context.contentResolver.openInputStream(selectedImage)
    BitmapFactory.decodeStream(imageStream, null, options)

    imageStream!!.close()


    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    imageStream = selectedImage?.let { context.getContentResolver().openInputStream(it) }
    var img = BitmapFactory.decodeStream(imageStream, null, options)
    img = img?.let { rotateImageIfRequired(context, it, selectedImage) }
    return img
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int, reqHeight: Int
): Int {
    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {

        // Calculate ratios of height and width to requested height and width
        val heightRatio =
            Math.round(height.toFloat() / reqHeight.toFloat())
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
private fun rotateImageIfRequired(
    context: Context,
    img: Bitmap,
    selectedImage: Uri
): Bitmap? {
    val input = context.contentResolver.openInputStream(selectedImage)
    val ei: ExifInterface
    ei = if (Build.VERSION.SDK_INT > 23) ExifInterface(input) else ExifInterface(selectedImage.path)
    val orientation =
        ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
        else -> img
    }
}

private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(degree.toFloat())
    val rotatedImg =
        Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    img.recycle()
    return rotatedImg
}


