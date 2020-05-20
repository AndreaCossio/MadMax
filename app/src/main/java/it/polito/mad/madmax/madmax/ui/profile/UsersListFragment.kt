package it.polito.mad.madmax.madmax.ui.item

import UserAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.toPx
import kotlinx.android.synthetic.main.fragment_item_list.*
import kotlinx.android.synthetic.main.users_list.*

class UsersListFragment : Fragment() {

    private var usersList: ArrayList<User>? = null
    private val args: UsersListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val user = User("hafhf","Dario Piazza","spapsps","dariodjoajsdo@saas","Marsala")

        args.item?.apply {
            val users = this.interestedUsers
        }

        return usersList?.let {
            inflater.inflate(R.layout.users_list, container, false)
        } ?: inflater.inflate(R.layout.users_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usersList?.also {
            users_list_rv.apply {
                //check
                this.setHasFixedSize(true)
                layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
                adapter = UserAdapter(it, this)
                itemAnimator = DefaultItemAnimator()
            }
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