package it.polito.mad.madmax.madmax

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModelFactory
import it.polito.mad.madmax.madmax.ui.item.ItemListFragmentDirections
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity() {

    // User data (without login and user creation the id is fixed)
    // TODO Images from different devices (ie same account different device different paths)?
    // TODO Beware that now we share the same db while testing,
    //  so you should create a user for your testing purposes


    // Firebase auth
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

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
        main_fab_add_item.setOnClickListener { navController.navigate(ItemListFragmentDirections.actionEditItem(null)) }

        // Check if logged in
        auth = Firebase.auth
        auth.currentUser?.also { user ->
            initUser(user)
        } ?: signIn()
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
        if (requestCode == 9001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Snackbar.make(nav_view, "Authentication Succeded.", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(nav_view, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun signIn() {
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        )
        startActivityForResult(googleSignInClient.signInIntent, 9001)
    }

    private fun initUser(user: FirebaseUser) {
        userVM.changeUser(user.uid)
        userVM.user.value ?: userVM.createUser(user)
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
}
