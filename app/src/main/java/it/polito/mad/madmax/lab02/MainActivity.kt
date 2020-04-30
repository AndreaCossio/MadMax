package it.polito.mad.madmax.lab02

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import it.polito.mad.madmax.lab02.data_models.User
import it.polito.mad.madmax.lab02.ui.item.ItemListFragmentDirections
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity() {

    // User
    private var user: User? = null

    var temp: NavigationView? = null

    // Appbar config
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        temp = findViewById(R.id.nav_view)

        // Setup Navigation
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfig = AppBarConfiguration(setOf(R.id.nav_item_list_fragment, R.id.nav_show_profile_fragment), main_drawer_layout)
        nav_view.setupWithNavController(navController)

        // Action bar
        setSupportActionBar(main_toolbar)
        setupActionBarWithNavController(navController, appBarConfig)

        // Init FAB
        main_fab_add_item.setOnClickListener { navController.navigate(ItemListFragmentDirections.actionCreateItem(null)) }

        // Load user data
        getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE)?.getString(getString(R.string.preference_user), null)?.also {
            user = Gson().fromJson(it, User::class.java)
        }

        // Update drawer header
        user?.also { user ->
            nav_view.getHeaderView(0).nav_header_nickname.text = user.name
            nav_view.getHeaderView(0).nav_header_email.text = user.email
            user.photo?.also { photo ->
                nav_view.getHeaderView(0).nav_header_profile_photo.setImageBitmap(
                    handleSamplingAndRotationBitmap(this, Uri.parse(photo))!!
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (main_drawer_layout.isDrawerOpen(GravityCompat.START)) {
            main_drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
