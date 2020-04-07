package it.polito.mad.madmax.madmax

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_edit_profile.*
import java.io.Serializable

class EditProfileActivity : AppCompatActivity() {

    private var user: User? = null
    private var imageBitmap= MutableLiveData<Bitmap>()
    private val REQUEST_IMAGE_CAPTURE = 1

    // On Create actions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        setContentView(R.layout.activity_edit_profile)

        // Add click listeners
        profile_edit_iv.setOnClickListener{dispatchTakePictureIntent()}
        profile_image.setOnClickListener{dispatchTakePictureIntent()}
        imageBitmap.observe(this, Observer{ profile_image.setImageBitmap(it)})

        // Get data from intent
        user = intent.getSerializableExtra(R.string.intent_user.toString()) as User?
        /* user = if (intent.getStringExtra(R.string.edited_name.toString()) != null) {
            User(
                intent.getStringExtra(R.string.edited_name.toString())!!,
                intent.getStringExtra(R.string.edited_nickname.toString())!!,
                intent.getStringExtra(R.string.edited_email.toString())!!,
                intent.getStringExtra(R.string.edited_location.toString())!!
            )
        } else {
            null
        } */
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
                    /* putExtra(R.string.edited_name.toString(), user?.name)
                    putExtra(R.string.edited_nickname.toString(), user?.nickname)
                    putExtra(R.string.edited_email.toString(), user?.email)
                    putExtra(R.string.edited_location.toString(), user?.location) */
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
        outState.putParcelable("bitmap", imageBitmap.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // Retrieve fields and photo from the Bundle
        user = savedInstanceState.getSerializable("user") as User?
        imageBitmap.value= savedInstanceState.getParcelable("bitmap")

        // Restore fields
        updateFields()

        // Restore image
        if (imageBitmap.value!=null) {
            profile_image.visibility= View.VISIBLE
            profile_edit_iv.visibility= View.INVISIBLE
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageBitmap.value = data?.extras?.get("data") as Bitmap
            profile_image.setImageBitmap(imageBitmap.value)
            profile_image.visibility= View.VISIBLE
            profile_edit_iv.visibility= View.INVISIBLE
        }
    }

    // Update user variable
    private fun updateUser() {
        user = User(
            name_tiet.text.toString(),
            nickname_tiet.text.toString(),
            email_tiet.text.toString(),
            location_tiet.text.toString()
        )
    }

    // Update views using the local variable user
    private fun updateFields() {
        if (user != null) {
            name_tiet.setText(user!!.name)
            nickname_tiet.setText(user!!.nickname)
            email_tiet.setText(user!!.email)
            location_tiet.setText(user!!.location)
        }
    }
}
