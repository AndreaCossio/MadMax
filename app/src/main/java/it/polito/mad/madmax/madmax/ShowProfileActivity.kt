package it.polito.mad.madmax.madmax

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_show_profile.*
import java.io.Serializable

class ShowProfileActivity : AppCompatActivity() {

    private var user: User? = null
    private val EDIT_PROFILE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        setContentView(R.layout.activity_show_profile)

        // Get user data from shared pref
        val prefs = getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)
        val profile = prefs.getString(getString(R.string.preference_file_user_profile), null)
        if (profile != null) {
            user = Gson().fromJson(profile, User::class.java)
            updateFields()
        }
    }

    // Inflate options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_edit_profile, menu)
        return true
    }

    // Handle clicks on the options menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Pencil button -> edit profile
            R.id.edit_profile -> {
                editProfile()
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Receive result from intents
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Edit profile intent
        if (requestCode == EDIT_PROFILE && resultCode == Activity.RESULT_OK) {
            user = data?.getSerializableExtra(getString(R.string.intent_user)) as User
            updateFields()
            displayMessage(this, "Profile saved successfully")
        } else {
            displayMessage(this, "Error saving the profile")
        }
    }

    // Function to create the intent for the Edit Profile activity
    private fun editProfile(): Boolean {
        // Build explicit intent
        val intent: Intent = Intent(this, EditProfileActivity::class.java).apply {
            // Insert user data
            putExtra(getString(R.string.intent_user), user as Serializable?)
        }

        // Start Edit Profile activity for result
        startActivityForResult(intent, EDIT_PROFILE)
        return true
    }

    // Update views using the local variable user
    private fun updateFields() {
        if (user != null) {
            (name_tv as MaterialTextView).text = user!!.name
            (nickname_tv as MaterialTextView).text = user!!.nickname
            (email_tv as MaterialTextView).text = user!!.email
            (location_tv as MaterialTextView).text = user!!.location
            (phone_tv as MaterialTextView).text = user!!.phone
            if (user!!.uri != null) {
                val bi = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(user!!.uri))
                (profile_image as CircleImage).setImageBitmap(bi)
            }
        }
    }
}
