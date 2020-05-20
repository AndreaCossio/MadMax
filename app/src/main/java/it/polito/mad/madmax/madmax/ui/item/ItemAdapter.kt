package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemKey
import kotlinx.android.synthetic.main.item.view.*

class ItemAdapter(
    private val recycler: RecyclerView,
    private val cardClickListener: (String) -> Unit,
    private val actionClickListener: (String) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: ArrayList<ItemKey> = ArrayList()
    val myCount: MutableLiveData<Int> = MutableLiveData(0)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], recycler, cardClickListener, actionClickListener)
    }

    fun addItem(item: ItemKey) {
        var alreadyIn = false
        for (i in items) {
            if (i.itemId == item.itemId) {
                i.item = item.item
                notifyItemChanged(items.indexOf(i))
                alreadyIn = true
                break
            }
        }
        if (!alreadyIn) {
            items.add(item)
            myCount.value = myCount.value!! + 1
            notifyItemInserted(items.indexOf(item))
        }
    }

    fun changeItem(item: ItemKey) {
        for (i in items) {
            if (i.itemId == item.itemId) {
                i.item = item.item
                notifyItemChanged(items.indexOf(i))
            }
        }
    }

    fun removeItem(item: ItemKey) {
        val position = items.indexOf(item)
        items.remove(item)
        myCount.value = myCount.value!! - 1
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.item_card
        private val title: TextView = itemView.item_title
        private val category: TextView = itemView.item_category
        private val price: TextView = itemView.item_price
        private val stars: TextView = itemView.item_stars
        private val image: ImageView = itemView.item_photo
        private val button: Button = itemView.item_edit_button

        fun bind(item: ItemKey, recycler: RecyclerView, cardClickListener: (String) -> Unit, actionClickListener: (String) -> Unit) {
            updateFields(item.item, recycler.context)
            cardView.setOnClickListener { cardClickListener(item.itemId) }
            button.setOnClickListener { actionClickListener(item.itemId) }
        }

        private fun updateFields(item: Item, context: Context) {
            title.text = item.title
            category.text = item.category_main
            price.text = String.format("%.2f", item.price)
            stars.text = String.format("%.1f", item.stars)
            if (item.photo != "") {
                Picasso.with(context).load(Uri.parse(item.photo)).into(image)
            } else {
                image.setImageDrawable(context.getDrawable(R.drawable.ic_camera_white))
            }
        }
    }
}