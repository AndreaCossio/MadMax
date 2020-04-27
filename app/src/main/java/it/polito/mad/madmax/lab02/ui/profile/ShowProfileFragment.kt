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

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Update user from shared preferences (put in here to load also after edit profile)
        // TODO we could find a better solution to this (firebase)
        activity?.getSharedPreferences(getString(R.string.preference_file_user), Context.MODE_PRIVATE)?.getString(getString(R.string.preference_file_user_profile), null)?.let {
            user = Gson().fromJson(it, User::class.java)
        }
        updateFields()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.menu_edit_profile, menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_profile -> {
                findNavController().navigate(ShowProfileFragmentDirections.actionEditProfile(user))
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