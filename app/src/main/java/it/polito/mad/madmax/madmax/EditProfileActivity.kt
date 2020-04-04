package it.polito.mad.madmax.madmax

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        //TODO get data from intent
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_save_profile, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", findViewById<TextInputEditText>(R.id.name_tiet).text.toString())
        outState.putString("nickname", findViewById<TextInputEditText>(R.id.nickname_tiet).text.toString())
        outState.putString("email", findViewById<TextInputEditText>(R.id.email_tiet).text.toString())
        outState.putString("location", findViewById<TextInputEditText>(R.id.location_tiet).text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        findViewById<TextInputEditText>(R.id.name_tiet).setText(savedInstanceState.getString("name"))
        findViewById<TextInputEditText>(R.id.nickname_tiet).setText(savedInstanceState.getString("nickname"))
        findViewById<TextInputEditText>(R.id.email_tiet).setText(savedInstanceState.getString("email"))
        findViewById<TextInputEditText>(R.id.location_tiet).setText(savedInstanceState.getString("location"))
    }
}
