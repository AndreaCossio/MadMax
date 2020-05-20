package it.polito.mad.madmax.madmax.ui.item

import UserAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository
import it.polito.mad.madmax.madmax.data.viewmodel.InterestedUsersViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.InterestedUsersViewModelFactory
import it.polito.mad.madmax.madmax.toPx
import kotlinx.android.synthetic.main.users_list.*

class UsersListFragment : Fragment() {

    private lateinit var usersListVM: InterestedUsersViewModel
    lateinit var usersAdapter: UserAdapter
    lateinit var item: Item
    private val args: UsersListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        item= args.item!!
        usersListVM = ViewModelProvider(this,InterestedUsersViewModelFactory(args.item!!.id!!))
            .get(InterestedUsersViewModel::class.java)

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {


        return inflater.inflate(R.layout.users_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usersAdapter = UserAdapter(ArrayList<User>(),users_list_rv,false)

            users_list_rv.apply {
                //check
                this.setHasFixedSize(true)
                layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
                adapter = usersAdapter
                itemAnimator = DefaultItemAnimator()
            }

            item.apply {
                usersListVM.getUsersList().observe(requireActivity(), Observer{
                    usersAdapter.setUsers(it)
                })
            }

    }



    class AutoFitGridLayoutManager(context: Context?, columnWidth: Int) : GridLayoutManager(context, 1) {

        private var columnWidth = 0
        private var columnWidthChanged = true

        fun setColumnWidth(newColumnWidth: Int) {
            if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
                columnWidth = newColumnWidth
                columnWidthChanged = true
            }
        }

        override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
            if (columnWidthChanged && columnWidth > 0) {
                val totalSpace = if (orientation == LinearLayoutManager.VERTICAL) {
                    width - paddingRight - paddingLeft
                } else {
                    height - paddingTop - paddingBottom
                }
                val spanCount = Math.max(1, totalSpace / columnWidth)
                setSpanCount(spanCount)
                columnWidthChanged = false
            }
            super.onLayoutChildren(recycler, state)
        }

        init {
            setColumnWidth(columnWidth)
        }
    }
}