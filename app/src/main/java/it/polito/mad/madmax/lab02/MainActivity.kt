package it.polito.mad.madmax.lab02

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import it.polito.mad.madmax.lab02.data_models.User
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // User
    private var user: User? = null

    // Appbar config
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // Set custom app bar
        setSupportActionBar(main_toolbar)
        appBarConfig = AppBarConfiguration(setOf(R.id.nav_item_list_fragment), main_drawer_layout)

        // Setup the navigation Controller
        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController, appBarConfig)
        nav_view.setupWithNavController(navController)

        // Load user data
        getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE)?.getString(getString(R.string.preference_user), null)?.let {
            user = Gson().fromJson(it, User::class.java)
        }

        if (user != null) {
            nav_view.getHeaderView(0).findViewById<TextView>(R.id.nav_header_nickname).text =
                user!!.nickname
            nav_view.getHeaderView(0).findViewById<TextView>(R.id.nav_header_email).text = user!!.email
            if (user!!.photo != null) {
                nav_view.getHeaderView(0).findViewById<ImageView>(R.id.nav_header_photo).setImageBitmap(
                    handleSamplingAndRotationBitmap(this, Uri.parse(user!!.photo))
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }
}
