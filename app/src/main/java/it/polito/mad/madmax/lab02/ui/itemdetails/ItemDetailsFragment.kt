package it.polito.mad.madmax.lab02.ui.itemdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import it.polito.mad.madmax.lab02.*
import kotlinx.android.synthetic.main.item_details_fragment.*
import java.io.Serializable

class ItemDetailsFragment : Fragment() {
    val item = MutableLiveData<Item>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        return inflater.inflate(R.layout.item_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        item.observe(context as AppCompatActivity, Observer {
            price_tv.text = it.price.toString()
            title_tv.text = it.title
            description_tv.text = it.description
            category_tv.text = it.category
            location_tv.text = it.location
            expiry_tv.text = it.expiry
            rating_bar.rating=it.stars!!
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
}