package it.polito.mad.madmax.ui.item

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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
import kotlinx.android.synthetic.main.activity_main.*
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

    // Reference to old status
    private lateinit var status: String

    // Var for scrolling
    private var animatingOut: Boolean = false
    private var start: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        status = args.item.status
        itemsVM.clearItem()
        userVM.clearOtherUserData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false).also {
            (childFragmentManager.findFragmentById(R.id.item_location) as SupportMapFragment).getMapAsync(this)
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

        itemsVM.getItemData().observe(viewLifecycleOwner, Observer { it ->
            it?.also { item ->
                when (item.userId) {
                    // Mine
                    userVM.getCurrentUserId() -> {
                        // Check if status changed to update menu actions
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
                        } else {
                            updateFields(item)
                        }
                    }
                    // Other's
                    else -> {
                        when(item.status) {
                            "Enabled" -> {
                                if (this::userListener.isInitialized) {
                                    userListener.remove()
                                }
                                // Observe owner
                                userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
                                    updateFields(item, user.name)
                                })
                                userListener = userVM.listenOtherUser(item.userId)
                            }
                            "Disabled" -> {
                                showProgress(requireActivity())
                                displayMessage(requireContext(), "This item is no longer available")
                                findNavController().navigateUp()
                            }
                            "Bought" -> {
                                // Bought by someone else
                                if (item.boughtBy != userVM.getCurrentUserId()) {
                                    showProgress(requireActivity())
                                    displayMessage(requireContext(), "This item is no longer available")
                                    findNavController().navigateUp()
                                }
                                updateFields(item)
                            }
                        }

                    }
                }
            } ?: run {
                // Deleted item
                showProgress(requireActivity())
                displayMessage(requireContext(), "This item is no longer available")
                findNavController().navigateUp()
            }
        })

        // Listen and observe item
        itemListener = itemsVM.listenItem(args.item.itemId)

        // Prevent scrolling interfering
        item_transparent_image.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    item_nested_scroll?.also {
                        it.requestDisallowInterceptTouchEvent(true)
                    } ?: requireActivity().main_scroll_view.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    item_nested_scroll?.also {
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
        if (this::itemListener.isInitialized)
            itemListener.remove()
        if (this::userListener.isInitialized)
            userListener.remove()
        item_details_card.removeOnLayoutChangeListener(cardRadiusConstrain)
        requireActivity().main_scroll_view.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
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

    override fun onMapReady(p0: GoogleMap?) {
        gMap = p0?.apply {
            uiSettings.isZoomControlsEnabled = true
        }
        updateMarker(args.item.location)
    }

    // Update views using the ViewModel of the item
    private fun updateFields(item: Item, name: String = "") {
        item_details_title.text = item.title
        item_details_description.text = item.description
        item_details_category_main.text = item.categoryMain
        item_details_category_sub.text = item.categorySub
        item_details_price.text = getString(R.string.item_price_set, item.price)
        item_details_expiry.text = item.expiry

        // Location
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
        when (item.userId) {
            // Mine
            userVM.getCurrentUserId() -> {
                if (item.status == "Bought") {
                    item_details_extra.text = "Sold to: $name"
                    item_details_extra.setOnClickListener {
                        showProgress(requireActivity())
                        findNavController().navigate(MainNavigationDirections.actionGlobalShowProfile(item.boughtBy))
                    }
                } else {
                    item_details_extra.text = "See interested users"
                    item_details_extra.setOnClickListener {
                        showProgress(requireActivity())
                        findNavController().navigate(DetailsItemFragmentDirections.actionSeeInterestedUsers(item))
                    }
                }
            }
            // Other's
            else -> {
                if (item.status == "Bought") {
                    if (item.boughtBy == userVM.getCurrentUserId()) {
                        item_details_extra.text = "Sold to you"
                    } else {
                        item_details_extra.text = "Sold to: $name"
                        item_details_extra.setOnClickListener {
                            showProgress(requireActivity())
                            findNavController().navigate(MainNavigationDirections.actionGlobalShowProfile(item.boughtBy))
                        }
                    }
                } else {
                    item_details_extra.text = name
                    item_details_extra.setOnClickListener {
                        showProgress(requireActivity())
                        findNavController().navigate(MainNavigationDirections.actionGlobalShowProfile(item.userId))
                    }
                }
            }
        }

        // FAB
        if (item.userId != userVM.getCurrentUserId() && item.status != "Bought") {
            if (!item.interestedUsers.contains(userVM.getCurrentUserId())) {
                showFab(requireActivity(), View.OnClickListener { showInterest() }, requireContext().getDrawable(R.drawable.ic_favourite_out))
            } else {
                showFab(requireActivity(), View.OnClickListener { removeInterest() }, requireContext().getDrawable(R.drawable.ic_favourite))
            }
            requireActivity().main_scroll_view.viewTreeObserver.addOnScrollChangedListener(scrollListener)
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

    private fun updateMarker(location: String) {
        if (location != "") {
            gMap?.apply {
                clear()
                getLocationFromAddress(requireContext(), location)?.also { loc ->
                    addMarker(MarkerOptions().position(loc))
                    animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15F))
                }
                if (args.item.userId != userVM.getCurrentUserId()) {
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

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        activity?.also {
            if (start < requireActivity().main_scroll_view.scrollY && !animatingOut && requireActivity().main_fab.visibility == View.VISIBLE) {
                ViewCompat.animate(requireActivity().main_fab).scaleX(0.0f).scaleY(0.0f).alpha(0.0f)
                    .setInterpolator(FastOutSlowInInterpolator()).withLayer()
                    .setListener(object : ViewPropertyAnimatorListener {
                        override fun onAnimationStart(view: View?) {
                            animatingOut = true
                        }

                        override fun onAnimationCancel(view: View?) {
                            animatingOut = false
                        }

                        override fun onAnimationEnd(view: View) {
                            animatingOut = false
                            view.visibility = View.GONE
                        }
                    }).start()
            } else if (start > requireActivity().main_scroll_view.scrollY && requireActivity().main_fab.visibility != View.VISIBLE) {
                requireActivity().main_fab.visibility = View.VISIBLE
                ViewCompat.animate(requireActivity().main_fab).scaleX(1.0f).scaleY(1.0f).alpha(1.0f)
                    .setInterpolator(FastOutSlowInInterpolator()).withLayer().setListener(null)
                    .start()
            }
            start = requireActivity().main_scroll_view.scrollY
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_DETAILS_ITEM"
    }
}