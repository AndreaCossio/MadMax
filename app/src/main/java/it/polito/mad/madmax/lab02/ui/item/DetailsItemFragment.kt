package it.polito.mad.madmax.lab02.ui.item

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.Item
import it.polito.mad.madmax.lab02.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_details_item.*

class DetailsItemFragment : Fragment() {

    // Item
    private var item: Item? = null

    // Destination arguments
    private val args: DetailsItemFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.item?.also {
            item = it
        }
        updateFields()

        item_details_card.post {
            item_details_card.radius = (item_details_card.height * 0.5).toFloat()
        }
        args.item ?: findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(null))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit_item, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_item_edit -> {
                findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(this.item))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the local variable item
    private fun updateFields() {
        item?.also { item ->
            item_details_title.text = item.title
            item_details_description.text = item.description
            item_details_category_main.text = item.category_main
            item_details_category_sub.text = item.category_sub
            item_details_price.text = item.price.toString()
            item_details_location.text = item.location
            item_details_expiry.text = item.expiry
            item_details_stars.rating = item.stars.toFloat()
            item.photo?.also { photo ->
                item_details_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(photo))!!)
            }
        }
    }
}