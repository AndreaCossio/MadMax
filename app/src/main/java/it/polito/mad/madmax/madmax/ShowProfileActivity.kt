package it.polito.mad.madmax.madmax

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_show_profile.*
import java.io.Serializable

const val EDIT_PROFILE = 1

class ShowProfileActivity : AppCompatActivity() {

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        setContentView(R.layout.activity_show_profile)

        // Get user data from shared pref
        val prefs = getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)
        val profile = prefs.getString(R.string.preference_file_user_profile.toString(),null)
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
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Function to create the intent for the Edit Profile activity
    private fun editProfile(): Boolean {
        // TODO Wouldn't it better to pass nothing and get user data directly from shared pref in
        //      Edit profile activity?
        // Build explicit intent
        val intent: Intent = Intent(this, EditProfileActivity::class.java).apply {
            // Insert user data
            putExtra(R.string.intent_user.toString(), user as Serializable?)
            /* putExtra(R.string.edited_name.toString(), user?.name)
            putExtra(R.string.edited_nickname.toString(), user?.nickname)
            putExtra(R.string.edited_email.toString(), user?.email)
            putExtra(R.string.edited_location.toString(), user?.location) */
        }

        // Start Edit Profile activity for result
        startActivityForResult(intent, EDIT_PROFILE)
        return true
    }

    // Receive result from intents
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Edit profile intent
        if (requestCode==EDIT_PROFILE && resultCode==Activity.RESULT_OK) {
            user = data?.getSerializableExtra(R.string.intent_user.toString()) as User
            /* user = if (data?.getStringExtra(R.string.edited_name.toString()) != null) {
                User(
                    data.getStringExtra(R.string.edited_name.toString())!!,
                    data.getStringExtra(R.string.edited_nickname.toString())!!,
                    data.getStringExtra(R.string.edited_email.toString())!!,
                    data.getStringExtra(R.string.edited_location.toString())!!
                )
            } else {
                null
            } */
            updateFields()
        }
    }

    // Update views using the local variable user
    private fun updateFields() {
        if (user != null) {
            (name_tv as MaterialTextView).text = user!!.name
            (nickname_tv as MaterialTextView).text = user!!.nickname
            (email_tv as MaterialTextView).text = user!!.email
            (location_tv as MaterialTextView).text = user!!.location
        }
    }
}
