package it.polito.mad.madmax.madmax

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_show_profile.*


class ShowProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_profile)
        fullName.text = getString(R.string.fullName)
        nickname.text = getString(R.string.nickname)
        email.text = getString(R.string.email)
        location.text = getString(R.string.location)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_edit_profile, menu)
        return true
    }
}
