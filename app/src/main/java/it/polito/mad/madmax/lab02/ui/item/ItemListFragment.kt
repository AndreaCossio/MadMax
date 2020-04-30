package it.polito.mad.madmax.lab02.ui.item

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.Item

class ItemListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View = inflater.inflate(R.layout.fragment_item_list, container, false)

        // 1. get a reference to recyclerView
        val recyclerView = rootView.findViewById(R.id.recyclerView) as RecyclerView

        // 2. set layoutManger
        recyclerView.layoutManager = LinearLayoutManager(activity)

        // this is data fro recycler view
        val prefs = activity?.getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE)
        var itemsData: ArrayList<Item> = ArrayList()

        prefs?.also {
            if (it.contains("itemList")) {
                val listType = object : TypeToken<ArrayList<Item>>() {}.type
                itemsData = Gson().fromJson(it.getString("itemList","[]"), listType)
            } else {
                itemsData.add(Item())
            }
        }

        // 3. create an adapter
        val mAdapter = RvAdapter(itemsData)
        // 4. set adapter
        recyclerView.adapter = mAdapter
        // 5. set item animator to DefaultAnimator
        recyclerView.itemAnimator = DefaultItemAnimator()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<FloatingActionButton>(R.id.main_fab_add_item)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<FloatingActionButton>(R.id.main_fab_add_item)?.visibility = View.GONE
    }
}