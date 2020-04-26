package it.polito.mad.madmax.lab02.ui.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.User
import it.polito.mad.madmax.lab02.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_show_profile.*

class ShowProfileFragment : Fragment() {

    // User variable to hold user information
    private var user: User? = null

    // Called once, when the fragment is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Add specific options menu
        setHasOptionsMenu(true)
    }

    // Called every time the fragment must be rendered (rotation, backstack)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Get user values from shared pref if any
        // TODO put it in onCreate a get data back in some other way? View model?
        val prefs = activity?.getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)
        prefs?.getString(getString(R.string.preference_file_user_profile), null)?.let {
            user = Gson().fromJson(it, User::class.java)
        }

        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    // Called always after onCreateView (when the layout has been rendered)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFields()
    }

    // Inflate options menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_profile, menu)
    }

    // Handle menu clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Pencil button -> edit profile
            R.id.edit_profile -> {
                val action = ShowProfileFragmentDirections.actionEditProfile(user)
                // TODO add animations
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the local variable user
    private fun updateFields() {
        user?.also { user ->
            name_tv.text = user.name
            nickname_tv.text = user.nickname
            email_tv.text = user.email
            location_tv.text = user.location
            phone_tv.text = user.phone
            user.uri?.also { uri ->
                profile_image.setImageBitmap(handleSamplingAndRotationBitmap(activity as Context, Uri.parse(uri))!!)
            }
        }
    }
}