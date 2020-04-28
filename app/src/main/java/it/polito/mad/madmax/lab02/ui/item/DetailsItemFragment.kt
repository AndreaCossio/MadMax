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
        args.item?.also {
            item = it
        } ?: findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(null))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFields()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit_item, menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_edit_item_edit -> {
                findNavController().navigate(DetailsItemFragmentDirections.actionEditItem(item))
                true
            } else -> super.onOptionsItemSelected(menuItem)
        }
    }

    // Update views using the local variable item
    private fun updateFields() {
        item?.also { item ->
            price_tv.text = item.price.toString()
            title_tv.text = item.title
            description_tv.text = item.description
            category_tv.text = item.category
            profile_location.text = item.location
            expiry_tv.text = item.expiry
            rating_bar.rating = item.stars.toFloat()
            item.photo?.also { photo ->
                item_image.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(photo)))
            }
        }
    }
}