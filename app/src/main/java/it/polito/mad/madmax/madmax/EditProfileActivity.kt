package it.polito.mad.madmax.madmax

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_edit_profile.*
import java.io.File
import java.io.IOException
import java.io.Serializable


class EditProfileActivity : AppCompatActivity() {

    // User
    private var user: User? = null
    private var uri: Uri? = null

    // Intents
    private val CAPTURE_PERMISSIONS_REQUEST = 0
    private val GALLERY_PERMISSIONS_REQUEST = 1
    private val CAPTURE_IMAGE_REQUEST = 2
    private val GALLERY_IMAGE_REQUEST = 3

    // Keep bitmap
    //private var imageBitmap = MutableLiveData<Bitmap>();

    // On Create actions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        setContentView(R.layout.activity_edit_profile)

        // Add click listeners
        profile_image.setOnClickListener { selectImage(this) }

        // Get data from intent
        user = intent.getSerializableExtra(getString(R.string.intent_user)) as User?
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
        return when (item.itemId) {
            // Save button -> save profile
            R.id.save_profile -> {
                // Get user data
                updateUser()

                // TODO check empty fields

                // Save user data to shared pref
                val prefs = getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString(getString(R.string.preference_file_user_profile), Gson().toJson(user))
                    commit()
                }

                // Send result
                val intent = Intent().apply {
                    putExtra(getString(R.string.intent_user), user)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Save instance when the activity is temporarily destroyed
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Get current fields values
        updateUser()

        // Save fields and photo into the Bundle
        outState.putSerializable("user", user as Serializable?)
    }

    // Restore activity from a saved instance state
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // Retrieve fields and photo from the Bundle
        user = savedInstanceState.getSerializable("user") as User?

        // Restore fields
        updateFields()
    }

    // Receive result from intents
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Capture image intent
        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            updateUser()
            updateFields()
        }
        // Capture image bad
        else if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode != Activity.RESULT_OK) {
            displayMessage(baseContext, "Request cancelled or something went wrong.")
            // Delete created file
            File(uri!!.path!!).delete()
        }
        // Gallery intent
        else if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            uri = data.data
           updateUser()
           updateFields()
        } else {
            displayMessage(baseContext, "Request cancelled or something went wrong.")
        }
    }

    // Receive result from request permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAPTURE_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    captureImage()
                }
            }
            GALLERY_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery()
                }
            }
        }
    }

    // Update user variable
    private fun updateUser() {
        var uriString: String? = null
        if (uri != null) {
            uriString = uri.toString()
        }
        user = User(
            name_tiet.text.toString(),
            nickname_tiet.text.toString(),
            email_tiet.text.toString(),
            location_tiet.text.toString(),
            uriString
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
                (profile_image as CircleImage).setImageBitmap(
                    handleSamplingAndRotationBitmap(
                        this,
                        uri!!
                    )!!
                )
            }
        }
    }

    // Click listener for changing the user profile
    private fun selectImage(context: Context) {

        val options = arrayOf<CharSequence>(
            getString(R.string.photo_dialog_take_photo),
            getString(R.string.photo_dialog_gallery_photo)
        )
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val tv: TextView = TextView(this)

        // TODO improve dialog box
        tv.text = getString(R.string.photo_dialog_choose_photo)
        tv.setTextColor(resources.getColor(R.color.colorPrimary))
        tv.gravity = Gravity.CENTER_VERTICAL
        tv.setPadding(60, 60, 10, 10)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,24f)

        builder.setCustomTitle(tv)
        builder.setItems(options) { _, item ->
            if (options[item] == getString(R.string.photo_dialog_take_photo)) {
                captureImage()
            } else if (options[item] == getString(R.string.photo_dialog_gallery_photo)) {
                getImageFromGallery()
            }
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show()
            dialog.cancel()
        }
        builder.show()
    }

    // Handle capturing the Image
    private fun captureImage() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask permissions (the callback will call again this method)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAPTURE_PERMISSIONS_REQUEST
            )
        } else {
            // TODO Handle multiple pictures
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile(this)
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                        null
                    }

                    // If file generated correctly, generate intent
                    photoFile?.also {
                        uri = Uri.fromFile(it)
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                this,
                                "it.polito.mad.madmax.madmax.fileprovider",
                                it
                            )
                        )
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
                    }
                }
            }
        }
    }

    // Handle selecting the image from the gallery
    private fun getImageFromGallery() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask permissions (the callback will call again this method)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSIONS_REQUEST
            )
        } else {
            val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickPhoto, GALLERY_IMAGE_REQUEST)
        }
    }

    // Helper function to display a toast
    private fun displayMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

