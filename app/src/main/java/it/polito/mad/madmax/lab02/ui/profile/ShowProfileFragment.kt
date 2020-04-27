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

    // User
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
        activity?.getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE)?.getString(getString(R.string.preference_user), null)?.let {
            user = Gson().fromJson(it, User::class.java)
        }
        updateFields()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_profile_edit -> {
                findNavController().navigate(ShowProfileFragmentDirections.actionEditProfile(user))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the local variable user
    private fun updateFields() {
        user?.also { user ->
            profile_name.text = user.name
            profile_nickname.text = user.nickname
            profile_email.text = user.email
            profile_location.text = user.location
            profile_phone.text = user.phone
            user.photo?.also { uri ->
                profile_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(uri))!!)
            }
        }
    }
}