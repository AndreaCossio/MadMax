package it.polito.mad.madmax

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity() {

    // User VM
    private val userVM: UserViewModel by viewModels()

    // Firebase auth
    private val auth: FirebaseAuth = Firebase.auth
    private var loggingIn: Boolean = false

    // Navigation
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Action bar
        setSupportActionBar(main_toolbar)

        // Setup Navigation
        navController = findNavController(R.id.nav_host_fragment)
        nav_view.setupWithNavController(navController)
        appBarConfig = AppBarConfiguration(setOf(
            R.id.nav_item_list_fragment,
            R.id.nav_on_sale_list_fragment,
            R.id.nav_show_profile_fragment
        ), main_drawer_layout)
        setupActionBarWithNavController(navController, appBarConfig)

        // Observe user data
        userVM.getCurrentUserData().observe(this, Observer { user ->
            nav_view.getHeaderView(0).apply {
                nav_header_nickname.text = user.name
                nav_header_email.text = user.email
                nav_header_profile_photo.post {
                    nav_header_profile_photo.apply {
                        if (user.photo != "") {
                            translationY = 0F
                            Picasso.get().load(Uri.parse(user.photo)).into(this)
                        } else {
                            translationY = measuredHeight / 6F
                            setImageDrawable(getDrawable(R.drawable.ic_profile))
                        }
                    }
                }
            }
        })

        // If the user has no id it means that we just created the VM and we need to login
        if (userVM.getCurrentUserData().value?.userId == "" && !loggingIn) {
            loggingIn = true
            login()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        // Close drawer instead of exiting
        if (main_drawer_layout.isDrawerOpen(GravityCompat.START)) {
            main_drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_GOOGLE_SIGN_IN -> {
                try {
                    // Sign in
                    val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)!!
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(account.idToken, null)).addOnCompleteListener(this) { login() }
                } catch (e: ApiException) {
                    e.printStackTrace()
                    login()
                }
            }
        }
    }

    private fun login() {
        // If the user is already logged in, just start listening on its data
        auth.currentUser?.also {
            loggingIn = false
            userVM.listenCurrentUser(it)
            FirebaseMessaging.getInstance().subscribeToTopic("/topics/${it.uid}")
            displayMessage(this, "Welcome back ${it.displayName}")
        } ?: run {
            // Intent dialog for logging in with google
            // TODO better dialog? More explanation to the user for the need of logging in?
            val googleSignInClient = GoogleSignIn.getClient(
                this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            )
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }
    }

    fun removeTopLevelProfile() {
        appBarConfig = AppBarConfiguration(
            setOf(
                R.id.nav_item_list_fragment,
                R.id.nav_on_sale_list_fragment
            ), main_drawer_layout
        )
        setupActionBarWithNavController(navController, appBarConfig)
    }

    fun addTopLevelProfile() {
        appBarConfig = AppBarConfiguration(
            setOf(
                R.id.nav_item_list_fragment,
                R.id.nav_on_sale_list_fragment,
                R.id.nav_show_profile_fragment
            ), main_drawer_layout
        )
        setupActionBarWithNavController(navController, appBarConfig)
    }

    // Companion
    companion object {
        private const val TAG = "MM_MAIN_ACTIVITY"
        private const val RC_GOOGLE_SIGN_IN = 9001
    }
}
