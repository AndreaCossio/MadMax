package it.polito.mad.madmax.madmax

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_show_profile.*

class ShowProfileActivity : AppCompatActivity() {

    private val INTENT_REQUEST_CODE = 1

    var user:User? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_profile)
        val prefs = getSharedPreferences(getString(R.string.preference_file_key),Context.MODE_PRIVATE)
        val profile  = prefs.getString("profile",null);
        user = if(profile != null){
            Gson().fromJson(profile,User::class.java)
        }else{
            User(
                getString(R.string.name_default),
                getString(R.string.nickname_default),
                getString(R.string.email_default),
                getString(R.string.location_default)
            )
        }

        name_tv.text = user?.fullName
        nickname_tv.text = user?.nickname
        email_tv.text = user?.email
        location_tv.text = user?.location
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_edit_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
             R.id.edit_profile -> {
                editProfile()
            }
            else -> return super.onOptionsItemSelected(item)
        }


    }

    private fun editProfile(): Boolean {
        intent = Intent(applicationContext, EditProfileActivity::class.java)
        intent.putExtra(getString(R.string.edited_name), user?.fullName)
        intent.putExtra(getString(R.string.edited_nickname), user?.nickname)
        intent.putExtra(getString(R.string.edited_email), user?.email)
        intent.putExtra(getString(R.string.edited_location), user?.location)
        startActivityForResult(intent, INTENT_REQUEST_CODE)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==INTENT_REQUEST_CODE && resultCode== Activity.RESULT_OK){
            name_tv.text = data?.getStringExtra(getString(R.string.edited_name))
            nickname_tv.text = data?.getStringExtra(getString(R.string.edited_nickname))
            email_tv.text = data?.getStringExtra(getString(R.string.edited_email))
            location_tv.text = data?.getStringExtra(getString(R.string.edited_location))
        }
    }

}
