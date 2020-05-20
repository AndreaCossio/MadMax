package it.polito.mad.madmax.madmax.ui.item

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemKey
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.displayMessage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_details_item.*

class DetailsItemFragment : Fragment() {

    // Items
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var itemLive: MutableLiveData<ItemKey>

    // User
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var user: User

    // Realtime listener
    private lateinit var listener: ListenerRegistration
    private lateinit var userListener: ListenerRegistration

    // Destination arguments
    private val args: DetailsItemFragmentArgs by navArgs()

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    companion object {
        const val TAG = "MM_DETAILS_ITEM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Listener to adjust the photo
        cardListener = View.OnLayoutChangeListener {v , _, _, _, _, _, _, _, _ ->
            (v as CardView).apply {
                // Radius of the card 50%
                radius = measuredHeight / 2F
                // Show the card
                visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.message.startsWith("create")) {
            findNavController().navigate(DetailsItemFragmentDirections.actionEditItem("create"))
        } else {
            val itemId = args.message.split("-")[2]
            if (args.message.startsWith("Y")) {
                for (i in itemsVM.myItems) {
                    if (i.value!!.itemId == itemId) {
                        itemLive = i
                        break
                    }
                }
            } else {
                for (i in itemsVM.othersItems) {
                    if (i.value!!.itemId == itemId) {
                        itemLive = i
                        break
                    }
                }
                userListener = userVM.getUser(itemLive.value!!.item.userId, userChange)
            }
            itemLive.observe(viewLifecycleOwner, Observer {
                updateFields(it.item)
            })
            item_details_card.addOnLayoutChangeListener(cardListener)

            if (args.message.startsWith("Y-edit")) {
                findNavController().navigate(DetailsItemFragmentDirections.actionEditItem("edit-$itemId"))
            }

            // TODO for future implement rating system
            /*if (!args.message.startsWith("Y")) {
                item_details_stars.setIsIndicator(false)
            } else {
                item_details_stars.setIsIndicator(true)
            }

            item_details_stars.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                if (fromUser) {
                    ratingBar.setIsIndicator(true)
                    itemsVM.updateItemRating(itemId, rating)
                }
            }*/
        }

        listener = itemsVM.listenOnItems(args.message.startsWith("Y"), null)

        if (args.message.startsWith("N")) {
            requireActivity().main_fab_add_item.setOnClickListener {
                itemsVM.notifyInterest(args.message.split("-")[2])
            }
            requireActivity().main_fab_add_item.setImageDrawable(requireContext().getDrawable(R.drawable.ic_favorite))
            requireActivity().main_fab_add_item.visibility = View.VISIBLE
        } else {
            requireActivity().main_fab_add_item?.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!args.message.startsWith("create")) {
            listener.remove()
            if (args.message.startsWith("N")) {
                userListener.remove()
                requireActivity().main_fab_add_item.setOnClickListener(null)
            }
        }
        item_details_card.removeOnLayoutChangeListener(cardListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (args.message.startsWith("Y")) {
            inflater.inflate(R.menu.menu_edit_item, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_item_edit -> {
                requireActivity().main_progress.visibility = View.VISIBLE
                findNavController().navigate(DetailsItemFragmentDirections.actionEditItem("edit-${args.message.split("-")[2]}"))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    private val userChange = { document: DocumentSnapshot ->
        item_details_owner.text = document.getString("name")
        user = document.toObject(User::class.java)!!
    }

    // Update views using the ViewModel of the item
    private fun updateFields(item: Item) {
        item_details_title.text = item.title
        item_details_description.text = item.description
        item_details_category_main.text = item.category_main
        item_details_category_sub.text = item.category_sub
        item_details_price.text = item.price.toString()
        item_details_location.text = item.location
        item_details_expiry.text = item.expiry
        if (args.message.startsWith("N")) {
            item_details_owner.visibility = View.VISIBLE
            item_details_owner.setOnClickListener {
                findNavController().navigate(DetailsItemFragmentDirections.actionVisitProfile(item.userId))
            }
        }
        if (item.stars.toFloat() == -1.0F && args.message.startsWith("Y")) {
            item_details_stars_container.visibility = View.GONE
        } else {
            item_details_stars_container.visibility = View.VISIBLE
            item_details_stars.rating = item.stars.toFloat()
        }
        if (item.photo != "") {
            Picasso.with(requireContext()).load(Uri.parse(item.photo)).into(item_details_photo, object : Callback {
                override fun onSuccess() {
                    requireActivity().main_progress.visibility = View.GONE
                }

                override fun onError() {
                    Log.d(TAG, "Error waiting picasso to load ${item.photo}")
                }
            })
        } else {
            item_details_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
            requireActivity().main_progress.visibility = View.GONE
        }
    }
}