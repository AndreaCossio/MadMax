package it.polito.mad.madmax.ui.profile

import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.MapsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_show_profile.*
import java.util.*

class ShowProfileFragment : Fragment(), OnMapReadyCallback {

    // User
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var userListener: ListenerRegistration

    // Google map
    private var gMap: GoogleMap? = null

    // Destination arguments
    private val args: ShowProfileFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false).also {
            (childFragmentManager.findFragmentById(R.id.profile_map_view) as SupportMapFragment).getMapAsync(this)
        }
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

        // Listen user data
        args.user?.also { userId ->
            // Other's profile
            (requireActivity() as MainActivity).removeTopLevelProfile()
            userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
                if (user.userId != "") {
                    requireActivity().main_toolbar.title = user.name.split(" ")[0] + "'s Profile"
                    updateFields(user)
                }
            })
            userListener = userVM.listenOtherUser(userId)
        } ?: run {
            // My profile
            userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
                updateFields(it)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        args.user?.also {
            // Stop listening to other user data
            userListener.remove()
            // Restore top level destination
            (requireActivity() as MainActivity).addTopLevelProfile()
        }
        // Detach layout listener
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

    override fun onMapReady(p0: GoogleMap?) {
        gMap = p0
        args.user?.also {
            userVM.getOtherUserData().value?.location?.also {
                updateMarker(it)
            } ?: gMap?.clear()
        } ?: run {
            userVM.getCurrentUserData().value?.location?.also {
                updateMarker(it)
            } ?: gMap?.clear()
        }
    }

    // Update views using the local variable user
    private fun updateFields(user: User) {
        profile_name.text = user.name
        profile_nickname.text = user.nickname
        profile_email.text = user.email

        // Hide some fields for privacy
        args.user?.also {
            profile_phone.visibility = View.GONE
        } ?: run {
            profile_phone.text = user.phone
        }

        // Location
        updateMarker(user.location)

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

    private fun updateMarker(location: String) {
        if (location != "") {
            val address = Geocoder(requireContext(), Locale.getDefault()).getFromLocationName(location, 1)[0]
            val position = LatLng(address.latitude, address.longitude)
            gMap?.apply {
                clear()
                addMarker(MarkerOptions().position(position))
                animateCamera(CameraUpdateFactory.newLatLngZoom(position, 13.5F))
                uiSettings.isZoomControlsEnabled = true
                args.user?.also {
                    setOnMapClickListener {
                        val mapsDialog = MapsFragment().apply {
                            setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MadMax_Dialog)
                            val locationArg = bundleOf("locationArg" to location)
                            arguments = locationArg
                        }
                        mapsDialog.show(childFragmentManager, "")
                    }
                }
            }
        } else {
            gMap?.clear()
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_SHOW_PROFILE"
    }
}