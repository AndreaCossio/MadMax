package it.polito.mad.madmax.madmax

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ShowProfileActivity : AppCompatActivity() {

    private val INTENT_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_profile)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_edit_profile, menu)
        return editProfile()
    }

    private fun editProfile(): Boolean {
        intent = Intent(applicationContext, EditProfileActivity::class.java)
        intent.putExtra("it.polito.mad.madmax.madmax.name", findViewById<TextView>(R.id.name_tiet).toString())
        intent.putExtra("it.polito.mad.madmax.madmax.nickname", findViewById<TextView>(R.id.nickname_tiet).toString())
        intent.putExtra("it.polito.mad.madmax.madmax.email", findViewById<TextView>(R.id.email_tiet).toString())
        intent.putExtra("it.polito.mad.madmax.madmax.location", findViewById<TextView>(R.id.location_tiet).toString())
        startActivityForResult(intent, INTENT_REQUEST_CODE)
        return true
    }
}
