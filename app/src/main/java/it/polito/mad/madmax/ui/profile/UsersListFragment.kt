package it.polito.mad.madmax.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.AutoFitGridLayoutManager
import kotlinx.android.synthetic.main.fragment_users_list.*

class UsersListFragment : Fragment() {

    // Model
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var interestedUsersListener: ListenerRegistration

    // Adapter
    private lateinit var usersAdapter: UserAdapter

    // Args
    private val args: UsersListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usersAdapter = UserAdapter(actionVisit, actionSell)
        userVM.clearInterestedUsersData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_users_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showProgress(requireActivity())
        hideFab(requireActivity())

        // Init recyclerview
        users_list_rv.apply {
            setHasFixedSize(false)
            layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
            adapter = usersAdapter
        }

        userVM.getInterestedUsersData().observe(viewLifecycleOwner, Observer {
            usersAdapter.setUsers(it)
            hideProgress(requireActivity())
            if (it.size == 0) {
                user_list_empty.animate().alpha(1F).startDelay = 300
            } else {
                user_list_empty.animate().alpha(0F).startDelay = 300
            }
        })

        interestedUsersListener = userVM.listenInterestedUsers(args.item.itemId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::interestedUsersListener.isInitialized) {
            interestedUsersListener.remove()
        }
    }

    private var actionVisit = { userId: String ->
        findNavController().navigate(MainNavigationDirections.actionGlobalShowProfile(userId))
    }

    private var actionSell = { user: User ->
        itemsVM.sellItem(requireContext(), args.item, user).addOnSuccessListener {
            displayMessage(requireContext(), "Successfully sold item to ${user.name}")
            findNavController().navigateUp()
        }.addOnFailureListener {
            displayMessage(requireContext(), "Failed to sell item")
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_USER_LIST"
    }
}