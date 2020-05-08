package it.polito.mad.madmax.madmax

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModelFactory
import it.polito.mad.madmax.madmax.ui.item.ItemListFragmentDirections
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity() {

    // User data (without login and user creation the id is fixed)
    // TODO Google sign-in?
    // TODO Images from different devices?
    // TODO Beware that now we share the same db while testing,
    //  so you should create a user for your testing purposes
    private val userId: String = "user00"
    private val userVM: UserViewModel by viewModels {
        UserViewModelFactory(userId)
    }

    // Appbar config
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Navigation
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfig = AppBarConfiguration(setOf(R.id.nav_item_list_fragment, R.id.nav_show_profile_fragment), main_drawer_layout)
        nav_view.setupWithNavController(navController)

        // Action bar
        setSupportActionBar(main_toolbar)
        setupActionBarWithNavController(navController, appBarConfig)

        // Init FAB
        main_fab_add_item.setOnClickListener { navController.navigate(ItemListFragmentDirections.actionCreateItem(null)) }

        // Observe user changes to update drawer
        userVM.user.observe(this, Observer {
            nav_view.getHeaderView(0).also { navView ->
                navView.nav_header_nickname.text = it.name
                navView.nav_header_email.text = it.email
                if (it.photo != "") {
                    navView.nav_header_profile_photo.apply {
                        translationY = 0F
                        setImageBitmap(handleSamplingAndRotationBitmap(context, Uri.parse(it.photo))!!)
                    }
                }
            }
        })
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
