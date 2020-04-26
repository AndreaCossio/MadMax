package it.polito.mad.madmax.lab02.ui.itemdetails

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import it.polito.mad.madmax.lab02.*
import it.polito.mad.madmax.lab02.data_models.Item
import it.polito.mad.madmax.madmax.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.item_details_fragment.*
import java.io.Serializable

class ItemDetailsFragment : Fragment() {
    val item = MutableLiveData<Item>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.item_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        val fm: FragmentManager? = fragmentManager
        if (fm != null) {
            for (entry in 0 until fm.backStackEntryCount) {
                Log.i(ContentValues.TAG, "Found fragment: " + fm.getBackStackEntryAt(entry).getId())
            }
        }*/
        if (arguments != null) {
            item.value = requireArguments().get("item") as Item

        } else {
            item.value = Item(
                null,
                getString(R.string.item_name),
                getString(R.string.item_description),
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            )

        }

        item.observe(context as AppCompatActivity, Observer {
            if (it.photo != null) {
                item_image.setImageBitmap(
                    handleSamplingAndRotationBitmap(
                        context as AppCompatActivity,
                        Uri.parse(it.photo!!)
                    )
                )
            }
            price_tv.text = it.price.toString()
            title_tv.text = it.title
            description_tv.text = it.description
            category_tv.text = it.category
            location_tv.text = it.location
            expiry_tv.text = it.expiry
            rating_bar.rating = it.stars!!
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (item.value != null)
            outState.putSerializable("item", item.value as Serializable)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.getSerializable("item") != null)
            item.value = savedInstanceState.getSerializable("item") as Item
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit, menu)
    }

    // Handle clicks on the options menu
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            // Pencil button -> edit profile
            R.id.nav_edit_fragment -> {
                /*            return NavigationUI.onNavDestinationSelected(
                        menuItem,
                        requireView().findNavController()

                    )
                            || super.onOptionsItemSelected(menuItem)*/
                val bundle = bundleOf("item" to item.value)
                findNavController().navigate(R.id.action_nav_item_to_nav_edit_fragment, bundle)
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

}