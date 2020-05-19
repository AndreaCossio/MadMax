package it.polito.mad.madmax.madmax

import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.ui.item.ItemListFragmentDirections
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity() {

    // User data
    private val userVM: UserViewModel by viewModels()

    // Firebase auth
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Appbar config
    private lateinit var appBarConfig: AppBarConfiguration

    // Companion
    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Navigation
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfig = AppBarConfiguration(setOf(R.id.nav_item_list_fragment, R.id.nav_on_sale_list_fragment, R.id.nav_show_profile_fragment), main_drawer_layout)
        nav_view.setupWithNavController(navController)

        // Action bar
        setSupportActionBar(main_toolbar)
        setupActionBarWithNavController(navController, appBarConfig)

        // Init FAB
        main_fab_add_item.setOnClickListener { navController.navigate(ItemListFragmentDirections.actionCreateItem(null)) }

        // Observe user data
        userVM.user.observe(this, Observer {
            nav_view.getHeaderView(0).also { navView ->
                navView.nav_header_nickname.text = it.name
                navView.nav_header_email.text = it.email
                if (it.photo != "") {
                    navView.nav_header_profile_photo.apply {
                        translationY = 0F
                        //setImageBitmap(handleSamplingAndRotationBitmap(context, Uri.parse(it.photo))!!)
                        Picasso.with(context).load(Uri.parse(it.photo)).into(this)
                    }
                }
            }
        })

        // LOGIN
        auth = Firebase.auth
        auth.currentUser?.also { userVM.loginOrCreateUser(it) } ?: signIn()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                if (resultCode == 0) {
                    // TODO improve
                    signIn()
                }
                try {
                    val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)!!
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(account.idToken, null)).addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            auth.currentUser?.also {
                                userVM.loginOrCreateUser(it)
                                displayMessage(this, "Welcome back ${it.displayName}")
                            } ?: signIn()
                        } else {
                            signIn()
                        }
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Intent for logging in with google
    private fun signIn() {
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        )
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }
}
