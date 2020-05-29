package it.polito.mad.madmax.ui.item

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.model.ItemArg
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.AutoFitGridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_item_list.*

class ItemListFragment : Fragment() {

    // View models
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()

    // Items
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var itemListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init adapter
        itemAdapter = ItemAdapter(itemDetails, actionEditItem, {null})
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show loading here because top level destination
        showProgress(requireActivity())

        // Show add item FAB
        showFab(requireActivity(), View.OnClickListener {
            showProgress(requireActivity())
            findNavController().navigate(ItemListFragmentDirections.actionEditOrCreateItem(ItemArg("Create", Item(userId = userVM.getCurrentUserData().value!!.userId), true)))
        }, requireContext().getDrawable(R.drawable.ic_add))

        // Init recyclerview
        item_list_rv.apply {
            setHasFixedSize(false)
            layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
            adapter = itemAdapter
        }
        item_list_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private var animatingOut: Boolean = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && !animatingOut && requireActivity().main_fab.visibility == View.VISIBLE) {
                    ViewCompat.animate(requireActivity().main_fab).scaleX(0.0f).scaleY(0.0f).alpha(0.0f)
                        .setInterpolator(FastOutSlowInInterpolator()).withLayer()
                        .setListener(object : ViewPropertyAnimatorListener {
                            override fun onAnimationStart(view: View?) {
                                animatingOut = true
                            }

                            override fun onAnimationCancel(view: View?) {
                                animatingOut = false
                            }

                            override fun onAnimationEnd(view: View) {
                                animatingOut = false
                                view.visibility = View.GONE
                            }
                        }).start()
                } else if (dy < 0 && requireActivity().main_fab.visibility != View.VISIBLE) {
                    requireActivity().main_fab.visibility = View.VISIBLE
                    ViewCompat.animate(requireActivity().main_fab).scaleX(1.0f).scaleY(1.0f).alpha(1.0f)
                        .setInterpolator(FastOutSlowInInterpolator()).withLayer().setListener(null)
                        .start()
                }
            }
        })
        item_list_empty_tv.text = getString(R.string.message_empty_list)
        item_list_empty_tv.setOnClickListener {
            findNavController().navigate(ItemListFragmentDirections.actionOpenMap())
        }

        // Observe the list of items
        itemsVM.getItemList().observe(viewLifecycleOwner, Observer {
            itemAdapter.setItems(it)
            if (it.size == 0) {
                item_list_empty.animate().alpha(1F).startDelay = 300
            } else {
                item_list_empty.animate().alpha(0F).startDelay = 300
            }
        })

        // Observe userId
        userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
            if (it.userId == "") {
                Log.d(TAG, "Not logged user clicked on an item")
                showProgress(requireActivity())
                if (this::itemListener.isInitialized) {
                    itemListener.remove()
                }
            } else {
                itemListener = itemsVM.listenMyItems(it.userId)
                hideProgress(requireActivity())
            }
        })

        hideProgress(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized) {
            itemListener.remove()
        }
    }

    private var itemDetails = { item: Item ->
        showProgress(requireActivity())
        findNavController().navigate(ItemListFragmentDirections.actionEditOrCreateItem(ItemArg("Details", item, item.userId == userVM.getCurrentUserId())))
    }

    private var actionEditItem = { item: Item ->
        showProgress(requireActivity())
        findNavController().navigate(ItemListFragmentDirections.actionEditOrCreateItem(ItemArg("Edit", item, item.userId == userVM.getCurrentUserId())))
    }

    // Companion
    companion object {
        const val TAG = "MM_MY_LIST"
    }
}