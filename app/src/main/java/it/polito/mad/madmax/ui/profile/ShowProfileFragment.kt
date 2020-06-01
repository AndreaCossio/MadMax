package it.polito.mad.madmax.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_show_profile.*

class ShowProfileFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var userListener: ListenerRegistration

    // Destination arguments
    private val args: ShowProfileFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showProgress(requireActivity())

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), profile_guideline)

        // Card radius
        profile_card.addOnLayoutChangeListener(cardRadiusConstrain)

        args.user?.also { userId ->
            (requireActivity() as MainActivity).removeTopLevelProfile()
            userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
                if (user.userId != "") {
                    requireActivity().main_toolbar.title = user.name.split(" ")[0] + "'s Profile"
                    updateFields(user)
                }
            })
            userListener = userVM.listenOtherUser(userId)
        } ?: run {
            userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer { updateFields(it) })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop listening to other user data
        if (this::userListener.isInitialized) {
            userListener.remove()
        }
        // Restore top level destination
        args.user?.also { (requireActivity() as MainActivity).addTopLevelProfile() }
        // Detach rate_dialog listener
        profile_card.removeOnLayoutChangeListener(cardRadiusConstrain)
    }

    override fun onDestroy() {
        super.onDestroy()
        args.user?.also { userVM.clearOtherUserData() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Show the possibility to edit profile
        args.user ?: inflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit -> {
                showProgress(requireActivity())
                findNavController().navigate(ShowProfileFragmentDirections.actionEditProfile())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the local variable user
    private fun updateFields(user: User) {
        profile_name.text = user.name
        profile_nickname.text = user.nickname
        profile_email.text = user.email
        profile_location.text = user.location
        profile_phone.text = user.phone

        // Hide some fields for privacy
        args.user?.also {
            profile_phone.visibility = View.GONE
        }

        // Profile photo
        profile_photo.post {
            if (user.photo != "") {
                Picasso.get().load(Uri.parse(user.photo)).into(profile_photo, object : Callback {
                    override fun onSuccess() {
                        profile_photo.translationY = 0F
                        hideProgress(requireActivity())
                    }

                    override fun onError(e: Exception?) {
                        profile_photo.apply {
                            translationY = measuredHeight / 6F
                            setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile))
                        }
                        hideProgress(requireActivity())
                    }
                })
            } else {
                profile_photo.apply {
                    translationY = measuredHeight / 6F
                    setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile))
                }
                hideProgress(requireActivity())
            }
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_SHOW_PROFILE"
    }
}