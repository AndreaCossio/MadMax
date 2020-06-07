package it.polito.mad.madmax.ui.profile

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.MapDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_show_profile.*
import java.io.IOException

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
        args.userId?.also { userVM.clearOtherUserData() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false).also {
            (childFragmentManager.findFragmentById(R.id.profile_location) as SupportMapFragment).getMapAsync(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showProgress(requireActivity())

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), profile_guideline)

        // Card radius
        profile_card.addOnLayoutChangeListener(cardRadiusConstrain)

        args.userId?.also { userId ->
            // Other's profile
            (requireActivity() as MainActivity).removeTopLevelProfile()
            userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
                if (user.userId != "") {
                    requireActivity().main_toolbar.title = user.name.split(" ")[0] + "'s Profile"
                    updateFields(user)
                }
            })
            // Listen user data
            userListener = userVM.listenOtherUser(userId)
        } ?: run {
            // My profile
            userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
                updateFields(it)
            })
        }

        // Prevent scrolling interfering
        profile_transparent_image.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    profile_nested_scroll?.also {
                        it.requestDisallowInterceptTouchEvent(true)
                    } ?: requireActivity().main_scroll_view.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    profile_nested_scroll?.also {
                        it.requestDisallowInterceptTouchEvent(true)
                    } ?: requireActivity().main_scroll_view.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        args.userId?.also {
            // Stop listening to other user data
            userListener.remove()
            // Restore top level destination
            (requireActivity() as MainActivity).addTopLevelProfile()
        }
        // Detach layout listener
        profile_card.removeOnLayoutChangeListener(cardRadiusConstrain)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Show the possibility to edit profile
        args.userId ?: inflater.inflate(R.menu.menu_edit, menu)
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
        gMap = p0?.apply {
            uiSettings.isZoomControlsEnabled = true
        }
        args.userId?.also {
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
        args.userId?.also {
            profile_phone.visibility = View.GONE
        } ?: run {
            profile_phone.text = user.phone
        }

        // Location
        updateMarker(user.location)

        // Rating
        if (user.ratings.size > 0) {
            profile_rating_card.visibility = View.VISIBLE
            profile_rating.rating = user.ratings.map { a -> a.split("+/")[1].toDouble() }.average().toFloat()
        } else {
            profile_rating_card.visibility = View.GONE
        }

        // Profile photo
        profile_photo.post {
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
        }
    }

    private fun updateMarker(location: String) {
        if (location != "") {
            gMap?.apply {
                clear()
                try {
                    getLocationFromAddress(requireContext(), location)?.also { loc ->
                        addMarker(MarkerOptions().position(loc))
                        animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15F))
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "Couldn't retrieve location")
                    displayMessage(requireContext(), "Cannot access geocoder service")
                }
                args.userId?.also {
                    setOnMapClickListener {
                        MapDialog().apply {
                            setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MadMax_Dialog)
                            arguments = bundleOf("location" to location, "editMode" to false)
                        }.show(childFragmentManager, TAG)
                    }
                }
            }
        } else {
            gMap?.clear()
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_SHOW_PROFILE"
    }
}