package it.polito.mad.madmax.lab02.ui.item

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
        val rootView: View = inflater.inflate(R.layout.item_list_fragment, container, false)

        // 1. get a reference to recyclerView
        val recyclerView = rootView.findViewById(R.id.recyclerView) as RecyclerView

        // 2. set layoutManger
        recyclerView.layoutManager = LinearLayoutManager(activity)

        // this is data fro recycler view
        val itemsData: ArrayList<Item> = arrayListOf<Item>(
            Item (
                title = getString(R.string.item_name),
                description = getString(R.string.item_description),
                price = getString(R.string.item_price).toDouble(),
                category = getString(R.string.item_category),
                location = getString(R.string.item_location),
                expiry = getString(R.string.item_expiry),
                stars = 3.75
            )
        )

        // 3. create an adapter
        val mAdapter = RvAdapter(itemsData)
        // 4. set adapter
        recyclerView.adapter = mAdapter
        // 5. set item animator to DefaultAnimator
        recyclerView.itemAnimator = DefaultItemAnimator()

        return rootView

    }


}