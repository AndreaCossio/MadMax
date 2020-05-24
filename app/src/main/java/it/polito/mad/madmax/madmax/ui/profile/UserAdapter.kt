
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.User
import kotlinx.android.synthetic.main.user_card.view.*
import java.util.*

class UserAdapter(private val actionVisit: (String) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: ArrayList<User> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_card, parent, false))
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], actionVisit)
    }

    fun setUsers(newUsers: ArrayList<User>){
        val diffs = DiffUtil.calculateDiff(UserDiffCallback(users, newUsers))
        users = newUsers
        diffs.dispatchUpdatesTo(this)
    }

    class UserViewHolder(private val userView: View) : RecyclerView.ViewHolder(userView) {
        fun bind(user: User, actionVisit: (String) -> Unit) {
            userView.user_card_photo.apply {
                Picasso.get().load(Uri.parse(user.photo)).into(this, object : Callback {
                    override fun onSuccess() {
                        translationY = 0F
                    }

                    override fun onError(e: Exception?) {
                        translationY = measuredHeight / 6F
                        setImageDrawable(userView.context.getDrawable(R.drawable.ic_profile_white))
                    }
                })
            }
            userView.user_card_name.text = user.name
            userView.setOnClickListener { actionVisit(user.userId) }
        }
    }

    class UserDiffCallback(private val oldList: List<User>, private val newList: List<User>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].userId == newList[newItemPosition].userId
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }

}