package it.polito.mad.madmax.ui.item

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.getColorIdCategory
import kotlinx.android.synthetic.main.item_card.view.*

class ItemAdapter(
    private val cardClickListener: (Item) -> Any,
    private val actionListener: (Item) -> Any,
    private val onCheckedChangeListener: (Item) -> CompoundButton.OnCheckedChangeListener?
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: ArrayList<Item> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], cardClickListener, actionListener, onCheckedChangeListener)
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: ArrayList<Item>) {
        val diffs = DiffUtil.calculateDiff(ItemDiffCallback(items, newItems))
        items = newItems
        diffs.dispatchUpdatesTo(this)
    }

    class ItemViewHolder(private val itemV: View) : RecyclerView.ViewHolder(itemV) {

        fun bind(item: Item, cardClickListener: (Item) -> Any, actionEdit: (Item) -> Any, onCheckedChangeListener: (Item) -> CompoundButton.OnCheckedChangeListener?) {
            // Title
            itemV.item_title.text = item.title

            // Description
            itemV.item_description.text = item.description

            // Price
            itemV.item_price.text = item.price.toInt().toString() + " €"

            // Category
//            itemV.item_category.setCardBackgroundColor(itemV.resources.getColor(getColorIdCategory(item.category_main)))
            itemV.item_category.post {
                itemV.item_category.apply {
                    if (radius == 0F) {
                        radius = measuredHeight / 2F
                    }
                }
            }
            itemV.item_category_text.text = item.category_main

            // Image
            if (item.photo != "") {
                Picasso.get().load(Uri.parse(item.photo)).into(itemV.item_photo)
            } else {
                itemV.item_photo.setImageDrawable(itemV.context.getDrawable(R.drawable.ic_camera))
            }

            // Interested people
            itemV.item_hot.text = item.interestedUsers.size.toString()

            // Status
            /*if (item.status == "Disabled") {
                itemV.item_card.setCardBackgroundColor(ContextCompat.getColor(itemV.context, R.color.colorGrey))
            }
            if (item.status == "Bought") {
                itemV.item_card.setCardBackgroundColor(ContextCompat.getColor(itemV.context, R.color.colorSecondaryLight))
            }*/

            // Button
            if (item.userId != Firebase.auth.currentUser?.uid) {
                itemV.item_button_like.visibility = View.VISIBLE
                itemV.item_button_like.setOnCheckedChangeListener(null)
                itemV.item_button_like.isChecked = item.interestedUsers.contains(Firebase.auth.currentUser?.uid)
                itemV.item_button_like.setOnCheckedChangeListener(onCheckedChangeListener(item))
            } else {
                itemV.item_button_edit.visibility = View.VISIBLE
                itemV.item_button_edit.setOnClickListener { actionEdit(item) }
            }
            itemV.item_card.setOnClickListener { cardClickListener(item) }
        }
    }

    class ItemDiffCallback(private val oldList: List<Item>, private val newList: List<Item>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].itemId == newList[newItemPosition].itemId
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }
}