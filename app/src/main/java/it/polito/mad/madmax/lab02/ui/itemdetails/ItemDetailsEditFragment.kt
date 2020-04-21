package it.polito.mad.madmax.lab02.ui.itemdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.Item
import kotlinx.android.synthetic.main.item_details_edit_fragement.*


class ItemDetailsEditFragment : Fragment() {

    val item = MutableLiveData<Item>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        return inflater.inflate(R.layout.item_details_edit_fragement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        item.observe(context as AppCompatActivity, Observer {
            price_tv.setText(it.price.toString())
            title_tv.setText(it.title)
            description_tv.setText(it.description)
            category_tv.setText(it.category)
            location_tv.setText(it.location)
            expiry_tv.setText(it.expiry)
        })
        item.value = arguments?.get("item") as Item
    }
}
