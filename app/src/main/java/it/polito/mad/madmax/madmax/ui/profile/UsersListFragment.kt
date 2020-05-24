package it.polito.mad.madmax.madmax.ui.profile

import UserAdapter
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
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.hideProgress
import it.polito.mad.madmax.madmax.toPx
import it.polito.mad.madmax.madmax.ui.AutoFitGridLayoutManager
import kotlinx.android.synthetic.main.fragment_users_list.*

class UsersListFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var usersAdapter: UserAdapter

    // User listener
    private lateinit var userListener: ListenerRegistration

    // Args
    private val args: UsersListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usersAdapter = UserAdapter(actionVisit)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_users_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideProgress(requireActivity())

        users_list_rv.apply {
            setHasFixedSize(false)
            layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
            adapter = usersAdapter
        }

        userVM.getInterestedUsersData().observe(viewLifecycleOwner, Observer {
            usersAdapter.setUsers(it)
            if (usersAdapter.itemCount == 0) {
                user_list_empty.visibility = View.VISIBLE
            } else {
                user_list_empty.visibility = View.GONE
            }
        })

        userListener = userVM.listenInterestedUsers(args.item.itemId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::userListener.isInitialized) {
            userListener.remove()
        }
        userVM.clearInterestedUsersData()
    }

    private var actionVisit = { userId: String ->
        findNavController().navigate(UsersListFragmentDirections.actionVisitProfile(userId))
    }
}