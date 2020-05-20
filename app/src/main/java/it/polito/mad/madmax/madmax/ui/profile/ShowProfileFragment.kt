package it.polito.mad.madmax.madmax.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_show_profile.*

class ShowProfileFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()
    private var otherUser: User? = null

    // Destination arguments
    // If not null, the user is visiting the profile of another user
    private val args: ShowProfileFragmentArgs by navArgs()
    private var localUser: Boolean = true

    private lateinit var userListener: ListenerRegistration

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    companion object {
        const val TAG = "MM_SHOW_PROFILE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Listener to adjust the photo
        cardListener = View.OnLayoutChangeListener {v , _, _, _, _, _, _, _, _ ->
            (v as CardView).apply {
                // Offset the drawable
                (getChildAt(0) as ViewGroup).getChildAt(0).apply {
                    translationY = args.user?.let {
                        if (otherUser?.photo == "") {
                            measuredHeight / 6F
                        } else {
                            0F
                        }
                    } ?: if (userVM.user.value?.photo == "") {
                        measuredHeight / 6F
                    } else {
                        0F
                    }
                }
                // Radius of the card 50%
                radius = measuredHeight / 2F
                // Show the card
                visibility = View.VISIBLE
            }
        }

        requireActivity().main_fab_add_item?.visibility = View.GONE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.user?.also {
            localUser = false
            userListener = userVM.getUser(it) { document ->
                otherUser = document.toObject(User::class.java)
                updateFields(document.toObject(User::class.java)!!)
            }
        } ?: userVM.user.observe(viewLifecycleOwner, Observer { updateFields(it) })
        profile_card.addOnLayoutChangeListener(cardListener)
    }

    override fun onDestroyView() {
        profile_card.removeOnLayoutChangeListener(cardListener)
        args.user?.also {
            userListener.remove()
        }
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Show the edit profile only if local user
        if (localUser) {
            inflater.inflate(R.menu.menu_edit_profile, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_profile_edit -> {
                requireActivity().main_progress.visibility = View.VISIBLE
                findNavController().navigate(ShowProfileFragmentDirections.actionEditProfile())
                true
            } else -> super.onOptionsItemSelected(item)
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
        if (!localUser) {
            profile_location.visibility = View.GONE
            profile_phone.visibility = View.GONE
        }

        if (user.photo != "") {
            //profile_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(user.photo))!!)
            Picasso.with(requireContext()).load(Uri.parse(user.photo)).into(profile_photo, object : Callback {
                override fun onSuccess() {
                    activity?.main_progress?.visibility = View.GONE
                }

                override fun onError() {
                    Log.d(TAG, "Error waiting picasso to load ${user.photo}")
                }
            })
        } else {
            profile_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile_white))
            activity?.main_progress?.visibility = View.GONE
        }
    }
}