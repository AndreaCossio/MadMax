package it.polito.mad.madmax.madmax.ui.item

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemArg
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_details_item.*
import org.json.JSONException
import org.json.JSONObject

class DetailsItemFragment : Fragment() {

    private lateinit var FCM_API :String
    private lateinit var serverKey: String
    private lateinit var contentType :String

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(requireActivity().applicationContext)
    }


    // User
    private val userVM: UserViewModel by activityViewModels()

    // Items
    private lateinit var itemArg: ItemArg
    private val itemsVM: ItemViewModel by activityViewModels()

    // Realtime listener
    private lateinit var itemListener: ListenerRegistration
    private lateinit var userListener: ListenerRegistration

    // Destination arguments
    private val args: DetailsItemFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        itemArg = args.itemArg
        FCM_API = getString(R.string.apiUrl)
        serverKey = "key=" + getString(R.string.serverKey)
        contentType = getString(R.string.cloudContentType)
        userVM.setItemId(itemArg.item.itemId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showProgress(requireActivity())
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), item_details_guideline)

        // Create
        when (itemArg.task) {
            "Create" -> findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(itemArg))
            "Edit" -> findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(itemArg))
            "Details" -> {
                // Listen and observe item
                itemListener = itemsVM.listenSingleItem(itemArg.item.itemId)
                itemsVM.getSingleItem().observe(viewLifecycleOwner, Observer {
                    updateFields(it)
                })
                // If other's item
                if (!itemArg.owned) {
                    // Listen and observe owner
                    userListener = userVM.listenOtherUser(itemArg.item.userId)
                    userVM.getOtherUserData().observe(viewLifecycleOwner, Observer {
                        updateUserField(it.name)
                    })
                    item_details_stars.setIsIndicator(false)
                    requireActivity().main_fab_add_item.setOnClickListener {
                        itemsVM.notifyInterest(itemArg.item, userVM.getCurrentUserData().value!!.userId)
                            .addOnSuccessListener {
                                val notification = createNotification();

                                try {
                                    sendNotification(notification)
                                } catch (e: JSONException) {
                                    Log.e("TAG", "onCreate: " + e.message)

                                }
                            }


                    }
                    }
                    item_details_owner.setOnClickListener {
                        findNavController().navigate(DetailsItemFragmentDirections.actionVisitProfile(itemArg.item.userId))
                    }
                    requireActivity().main_fab_add_item.setImageDrawable(requireContext().getDrawable(R.drawable.ic_favorite))
                    showFab(requireActivity())
                } else {
                    item_details_stars.setIsIndicator(true)
                    item_details_interested_users.setOnClickListener {
                        findNavController().navigate(DetailsItemFragmentDirections.actionSeeInterestedUsers(itemArg.item))
                    }
                }
            }
        }

        // TODO rating system
        /*item_details_stars.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (fromUser) {
                ratingBar.setIsIndicator(true)
                itemsVM.updateItemRating(itemId, rating)
            }
        }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized)
            itemListener.remove()
        if (this::userListener.isInitialized)
            userListener.remove()
        requireActivity().main_fab_add_item.setOnClickListener(null)
        item_details_owner.setOnClickListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        userVM.clearOtherUserData()
        itemsVM.clearSingleItemData()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (itemArg.owned) {
            inflater.inflate(R.menu.menu_edit, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit -> {
                showProgress(requireActivity())
                findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(itemArg))
                true
            } else -> super.onOptionsItemSelected(item)
        }
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

        // Other owner
        if (!itemArg.owned) {
            item_details_owner.visibility = View.VISIBLE
        } else {
            item_details_interested_users.visibility = View.VISIBLE
        }

        // Rating
        if (item.stars.toFloat() == -1.0F /*&& itemArg.owned*/) {
            item_details_stars_container.visibility = View.GONE
        } else {
            item_details_stars_container.visibility = View.VISIBLE
            item_details_stars.rating = item.stars.toFloat()
        }

        // Photo
        item_details_photo.post {
            item_details_card.apply {
                radius = measuredHeight * 0.5F
            }
            item_details_photo.apply {
                if (item.photo != "") {
                    Picasso.get().load(Uri.parse(item.photo)).into(item_details_photo, object: Callback {
                        override fun onSuccess() {
                            hideProgress(requireActivity())
                        }

                        override fun onError(e: Exception?) {
                            item_details_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
                            hideProgress(requireActivity())
                        }
                    })
                } else {
                    item_details_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
                    hideProgress(requireActivity())
                }
            }
        }

    }

    private fun updateUserField(name: String) {
        item_details_owner.text = name
    }

    // Companion
    companion object {
        const val TAG = "MM_DETAILS_ITEM"
    }


    //NOTIFICATION
    private fun sendNotification(notification: JSONObject) {
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            com.android.volley.Response.Listener{ response ->
                Log.i("TAG", "onResponse: $response")
            },
            com.android.volley.Response.ErrorListener {
                Toast.makeText(requireContext(), "Request error", Toast.LENGTH_LONG).show()
                Log.i("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun createNotification(): JSONObject{
        val topic = "/topics/${itemArg.item.userId}" //topic has to match what the receiver subscribed to

        val notification = JSONObject()
        val notificationBody = JSONObject()
        notificationBody.put("title", "MadMax")
        notificationBody.put("message", "Someone is interested in your article ${item!!.title}")   //Enter your notification message
        notification.put("to", topic)
        notification.put("data", notificationBody)

        return  notification
    }
}