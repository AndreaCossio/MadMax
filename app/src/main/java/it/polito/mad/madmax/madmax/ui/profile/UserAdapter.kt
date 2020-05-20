
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.User
import kotlinx.android.synthetic.main.item.view.*
import kotlinx.android.synthetic.main.user_card.view.*
import java.util.*

class UserAdapter(private var users: ArrayList<User>, private val recycler: RecyclerView, private val isMine:Boolean = true) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): UserViewHolder {

        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_card, parent, false)).apply {
            if(!isMine) this.itemView.item_edit_button.visibility = View.GONE
        }
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], position, users.size, recycler)
    }

    class UserViewHolder(userView: View) : RecyclerView.ViewHolder(userView) {
        private val profilePic  = userView.user_card_photo
        private val nameTV = userView.user_card_name

        fun bind(user: User, position: Int, size: Int, recycler: RecyclerView) {

            nameTV.text = user.name

        }
    }

    fun setUsers(newUsers: ArrayList<User>){
        val diffs = DiffUtil.calculateDiff(UserDiffCallback(users,newUsers))
        users = newUsers
        diffs.dispatchUpdatesTo(this)
    }

    class UserDiffCallback(private val oldList: List<User>, private val newList: List<User>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id === newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val u1 = oldList[oldPosition]
            val u2 = newList[newPosition]

            return u1.id == u2.id
        }
    }

}