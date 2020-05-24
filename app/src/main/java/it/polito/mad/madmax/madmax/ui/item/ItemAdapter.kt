package it.polito.mad.madmax.madmax.ui.item

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import kotlinx.android.synthetic.main.item.view.*

class ItemAdapter(
    private val cardClickListener: (Item) -> Unit,
    private val actionClickListener: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: ArrayList<Item> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], cardClickListener, actionClickListener)
    }

    fun setItems(newItems: ArrayList<Item>) {
        // Start removing items
        val toBeRemoved: ArrayList<Item> = ArrayList()
        for (item in items) {
            newItems.find {
                it.itemId == item.itemId
            } ?: run {
                toBeRemoved.add(item)
            }
        }

        for (item in toBeRemoved) {
            val position = items.indexOf(item)
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }

        // Add or edit items
        for (newItem in newItems) {
            items.find {  oldItem ->
                oldItem.itemId == newItem.itemId
            }?.also { oldItem ->
                if (oldItem != newItem) {
                    val position = items.indexOf(oldItem)
                    items[position] = newItem
                    notifyItemChanged(position)
                }
            } ?: run {
                items.add(newItem)
                notifyItemInserted(items.indexOf(newItem))
            }
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: Item, cardClickListener: (Item) -> Unit, actionClickListener: (Item) -> Unit) {
            // Update fields
            itemView.item_title.text = item.title
            itemView.item_category.text = item.category_main
            itemView.item_price.text = String.format("%.2f", item.price)

            // Stars
            if (item.stars == -1.0) {
                itemView.item_stars.visibility = View.GONE
                itemView.item_star_icon.visibility = View.GONE
            } else {
                itemView.item_stars.text = String.format("%.1f", item.stars)
                itemView.item_stars.visibility = View.VISIBLE
                itemView.item_star_icon.visibility = View.VISIBLE
            }

            // Image
            if (item.photo != "") {
                Picasso.get().load(Uri.parse(item.photo)).into(itemView.item_photo)
            } else {
                itemView.item_photo.setImageDrawable(itemView.context.getDrawable(R.drawable.ic_camera_white))
            }

            itemView.item_card.setOnClickListener { cardClickListener(item) }
            itemView.item_button.setOnClickListener { actionClickListener(item) }
        }
    }
}