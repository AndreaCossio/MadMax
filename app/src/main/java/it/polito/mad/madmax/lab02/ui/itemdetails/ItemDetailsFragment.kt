package it.polito.mad.madmax.lab02.ui.itemdetails

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import it.polito.mad.madmax.lab02.*
import it.polito.mad.madmax.lab02.data_models.Item
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

        if(arguments!=null)
        {
           item.value= requireArguments().get("item") as Item

        }
        else
        {
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
                var bundle = bundleOf("item" to item.value)
                findNavController().navigate(R.id.action_nav_item_to_nav_edit_fragment,bundle)
                return true
            }
            else -> return super.onOptionsItemSelected(menuItem)
        }
    }

    fun getNewItemData(){
        price_tv.text = (item.value?.price.toString())
        title_tv.text = (item.value?.price.toString())
        description_tv.text = (item.value?.description)
        category_tv.text = (item.value?.category)
        location_tv.text = (item.value?.location)
        expiry_tv.text = (item.value?.expiry)
    }

}