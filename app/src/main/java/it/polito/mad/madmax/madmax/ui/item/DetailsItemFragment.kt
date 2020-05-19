package it.polito.mad.madmax.madmax.ui.item

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.toPx
import kotlinx.android.synthetic.main.fragment_details_item.*

class DetailsItemFragment : Fragment() {

    // Item
    private val itemVM: ItemViewModel by activityViewModels()

    // Destination arguments
    private val args: DetailsItemFragmentArgs by navArgs()

    // User owns the item
    private var own: Boolean = false

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initListeners()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.item?.also {
            own = true
            updateFields()
        } ?: itemVM.items.observe(viewLifecycleOwner, Observer { updateFields() })
        args.item ?: findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(null))
        item_details_card.addOnLayoutChangeListener(cardListener)
    }

    override fun onDestroyView() {
        item_details_card.removeOnLayoutChangeListener(cardListener)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (own) {
            inflater.inflate(R.menu.menu_edit_item, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_item_edit -> {
                findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(itemVM.items.value?.get(0)))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the ViewModel of the item
    private fun updateFields() {
        args.item?.also { item ->
            item_details_title.text = item.title
            item_details_description.text = item.description
            item_details_category_main.text = item.category_main
            item_details_category_sub.text = item.category_sub
            item_details_price.text = item.price.toString()
            item_details_location.text = item.location
            item_details_expiry.text = item.expiry
            item_details_stars.rating = item.stars.toFloat()
            if (item.photo != "") {
                Picasso.with(requireContext()).load(Uri.parse(item.photo)).into(item_details_photo)
            }
        }
    }

    // Initialize listeners
    private fun initListeners() {
        // This listener is necessary to make sure that the cardView has always 50% radius (circle)
        // and that if the image is the icon, it is translated down
        cardListener = View.OnLayoutChangeListener {v, _, _, _, _, _, _, _, _ ->
            val cardView: CardView = v as CardView
            val imageView = (cardView.getChildAt(0) as ViewGroup).getChildAt(0)
            val item = args.item //?: userVM.user.value

            // Radius of the card
            cardView.apply { radius = measuredHeight / 2F }

            // Translation of the photo
            imageView.apply {
                if (item?.photo == "") {
                    setPadding(16.toPx(), 16.toPx(), 16.toPx(), 32.toPx())
                } else {
                    setPadding(0,0,0,0)
                }
            }

            // Visibility
            cardView.visibility = View.VISIBLE
        }
    }
}