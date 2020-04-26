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

    private lateinit var appBarConfig: AppBarConfiguration
    private var user: User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs =
            getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)

        prefs.getString(getString(R.string.preference_file_user_profile), null)?.let {
            user = Gson().fromJson(it, User::class.java)
        }

        if (user != null) {
            nav_view.getHeaderView(0).findViewById<TextView>(R.id.user_nickname).text =
                user!!.nickname
            nav_view.getHeaderView(0).findViewById<TextView>(R.id.user_email).text = user!!.email
            if (user!!.uri != null) {
                nav_view.getHeaderView(0).findViewById<ImageView>(R.id.user_picture).setImageBitmap(
                    handleSamplingAndRotationBitmap(this, Uri.parse(user!!.uri))
                )
            }
        }

        // Set custom app bar (stock one removed as specified in the manifest)
        setSupportActionBar(main_toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // Define the app bar configuration
        // Each top-level destination's id is passed
        appBarConfig = AppBarConfiguration(
            setOf(
                R.id.nav_item_list
            ), main_drawer_layout
        )

        // Setup the navigation Controller
        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController, appBarConfig)
        nav_view.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu right now it is only a settings menu
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

}
