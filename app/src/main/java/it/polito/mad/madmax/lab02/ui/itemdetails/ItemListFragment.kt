package it.polito.mad.madmax.lab02.ui.itemdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.Item


class ItemListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView: View =  inflater.inflate(R.layout.item_list_fragment, container, false)
        // 1. get a reference to recyclerView
        // 1. get a reference to recyclerView
        val recyclerView = rootView.findViewById(R.id.recyclerView) as RecyclerView

        // 2. set layoutManger

        // 2. set layoutManger
        recyclerView.layoutManager = LinearLayoutManager(activity)

        // this is data fro recycler view

        // this is data fro recycler view
        val itemsData: ArrayList<Item> = arrayListOf<Item>(
            Item(
                null,
                getString(R.string.item_name),
                getString(R.string.item_description),
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            ),
            Item(
                null,
                "Item 2",
                getString(R.string.item_description),
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            ),
            Item(
                null,
                getString(R.string.item_name),
                "Item 3",
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            ),
            Item(
                null,
                "Item 4",
                getString(R.string.item_description),
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            ),
            Item(
                null,
                getString(R.string.item_name),
                getString(R.string.item_description),
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            ),
            Item(
                null,
                getString(R.string.item_name),
                getString(R.string.item_description),
                getString(R.string.item_price).toDouble(),
                getString(R.string.item_category),
                getString(R.string.item_location),
                getString(R.string.item_expiry),
                3.75.toFloat()
            )
        )


        // 3. create an adapter


        // 3. create an adapter
        val mAdapter = RvAdapter(itemsData)
        // 4. set adapter
        // 4. set adapter
        recyclerView.adapter = mAdapter
        // 5. set item animator to DefaultAnimator
        // 5. set item animator to DefaultAnimator
        recyclerView.itemAnimator = DefaultItemAnimator()

        return rootView

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }



}