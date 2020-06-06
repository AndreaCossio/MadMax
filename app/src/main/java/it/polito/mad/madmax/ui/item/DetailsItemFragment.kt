package it.polito.mad.madmax.ui.item

import android.annotation.SuppressLint
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.MapDialog
import it.polito.mad.madmax.ui.profile.ShowProfileFragment
import kotlinx.android.synthetic.main.fragment_details_item.*

class DetailsItemFragment : Fragment(),OnMapReadyCallback {

    // Models
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var userListener: ListenerRegistration
    private lateinit var itemListener: ListenerRegistration

    // Google map
    private var gMap: GoogleMap? = null

    // Destination arguments
    private val args: DetailsItemFragmentArgs by navArgs()

    // Reference to menu
    private lateinit var status: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        status = args.item.status
        itemsVM.clearItem()
        userVM.clearOtherUserData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false).also {
            (childFragmentManager.findFragmentById(R.id.item_map_view) as SupportMapFragment).getMapAsync(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showProgress(requireActivity())
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), item_details_guideline)

        // Card radius
        item_details_card.addOnLayoutChangeListener(cardRadiusConstrain)

        itemsVM.getItemData().observe(viewLifecycleOwner, Observer { item ->
            // If the item became unavailable go back
            if (item == null || (item.userId != userVM.getCurrentUserId() && (item.status == "Disabled" || (item.status == "Bought" && item.boughtBy != userVM.getCurrentUserId())))) {
                showProgress(requireActivity())
                displayMessage(requireContext(), "This item is no longer available")
                findNavController().navigateUp()
            }
            if (item.itemId != "") {
                if (item.userId == userVM.getCurrentUserId()) {
                    if (item.status != status) {
                        status = item.status
                        requireActivity().invalidateOptionsMenu()
                    }
                    if (item.status == "Bought") {
                        if (this::userListener.isInitialized) {
                            userListener.remove()
                        }
                        // Observe buyer
                        userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
                            updateFields(item, user.name)
                        })
                        userListener = userVM.listenOtherUser(item.boughtBy)
                    }
                } else {
                    if (this::userListener.isInitialized) {
                        userListener.remove()
                    }
                    // Observe owner
                    userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
                        updateFields(item, user.name)
                    })
                    userListener = userVM.listenOtherUser(item.userId)
                }
                updateFields(item)
            }
        })

        // Listen and observe item
        itemListener = itemsVM.listenItem(args.item.itemId)


        /**
         * Allow scrolling inside map
         * */
        item_transparent_image.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action){
                MotionEvent.ACTION_DOWN -> {
                    item_main_scroll_view.requestDisallowInterceptTouchEvent(true);
                    // Disable touch on transparent view
                    false;
                }
                MotionEvent.ACTION_UP -> {
                    //view.performClick()
                    item_main_scroll_view.requestDisallowInterceptTouchEvent(false);
                    true
                }
                else -> true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized)
            itemListener.remove()
        if (this::userListener.isInitialized)
            userListener.remove()
        item_details_card.removeOnLayoutChangeListener(cardRadiusConstrain)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (args.item.userId == userVM.getCurrentUserId()) {
            inflater.inflate(R.menu.menu_edit_item, menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (args.item.userId == userVM.getCurrentUserId()) {
            when (status) {
                "Enabled" -> menu.findItem(R.id.menu_disable).title = "Disable"
                "Disabled" -> menu.findItem(R.id.menu_disable).title = "Enable"
                "Bought" -> menu.findItem(R.id.menu_disable).isVisible = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_disable -> {
                when (item.title) {
                    "Enable" -> {
                        itemsVM.enableItem(requireContext(), args.item, userVM.getCurrentUserId()).addOnSuccessListener {
                            displayMessage(requireContext(), "Successfully enabled item")
                        }.addOnFailureListener {
                            displayMessage(requireContext(), "Error enabling item")
                        }
                    }
                    "Disable" -> {
                        itemsVM.disableItem(requireContext(), args.item, userVM.getCurrentUserData().value!!.userId).addOnSuccessListener {
                            displayMessage(requireContext(), "Successfully disabled item")
                        }.addOnFailureListener {
                            displayMessage(requireContext(), "Error disabling item")
                        }
                    }
                }
                true
            }
            R.id.menu_delete -> {
                itemsVM.deleteItem(requireContext(), args.item)
                true
            }
            R.id.menu_edit -> {
                showProgress(requireActivity())
                findNavController().navigate(DetailsItemFragmentDirections.actionGlobalEditItem(args.item))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the ViewModel of the item
    private fun updateFields(item: Item, name: String = "") {
        item_details_title.text = item.title
        item_details_description.text = item.description
        item_details_category_main.text = item.categoryMain
        item_details_category_sub.text = item.categorySub
        item_details_price.text = getString(R.string.item_price_set, item.price)
        item_details_expiry.text = item.expiry

        updateMarker(item.location)

        // Photo
        item_details_photo.apply {
            if (item.photo != "") {
                Picasso.get().load(Uri.parse(item.photo)).into(item_details_photo, object: Callback {
                    override fun onSuccess() {
                        hideProgress(requireActivity())
                    }

                    override fun onError(e: Exception?) {
                        item_details_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera))
                        hideProgress(requireActivity())
                    }
                })
            } else {
                item_details_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera))
                hideProgress(requireActivity())
            }
        }

        // Owner / Buyer / Interested users
        if (item.status == "Bought") {
            item_details_extra.text = "Sold to: $name"
            item_details_extra.setOnClickListener {
                showProgress(requireActivity())
                findNavController().navigate(MainNavigationDirections.actionGlobalShowProfile(item.boughtBy))
            }
        } else if (item.userId != userVM.getCurrentUserId()) {
            item_details_extra.text = name
            item_details_extra.setOnClickListener {
                showProgress(requireActivity())
                findNavController().navigate(MainNavigationDirections.actionGlobalShowProfile(item.userId))
            }
        } else {
            item_details_extra.text = "See interested users"
            item_details_extra.setOnClickListener {
                showProgress(requireActivity())
                findNavController().navigate(DetailsItemFragmentDirections.actionSeeInterestedUsers(item))
            }
        }

        // FAB
        if (item.userId != userVM.getCurrentUserId()) {
            if (!item.interestedUsers.contains(userVM.getCurrentUserId())) {
                showFab(requireActivity(), View.OnClickListener { showInterest() }, requireContext().getDrawable(R.drawable.ic_favourite_out))
            } else {
                showFab(requireActivity(), View.OnClickListener { removeInterest() }, requireContext().getDrawable(R.drawable.ic_favourite))
            }
        }
    }

    private fun showInterest() {
        itemsVM.notifyInterest(requireContext(), args.item, userVM.getCurrentUserId()).addOnSuccessListener {
            displayMessage(requireContext(), "Successfully showed interest")
        }.addOnFailureListener {
            displayMessage(requireContext(), "Failed to show interest")
        }
    }

    private fun removeInterest() {
        itemsVM.removeInterest(requireContext(), args.item, userVM.getCurrentUserId()).addOnSuccessListener {
            displayMessage(requireContext(), "Successfully removed interest")
        }.addOnFailureListener {
            displayMessage(requireContext(), "Failed to remove interest")
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_DETAILS_ITEM"
    }

    override fun onMapReady(p0: GoogleMap?) {
        gMap = p0?.apply {
            uiSettings.isZoomControlsEnabled = true
        }
        args.item.location.also {
            updateMarker(it)
        }
    }


     private fun updateMarker(location: String) {
        if (location != "") {
            gMap?.apply {
                clear()
                getLocationFromAddress(requireContext(), location)?.also { loc ->
                    addMarker(MarkerOptions().position(loc))
                    animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15F))
                }
                args.item.also {
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
}