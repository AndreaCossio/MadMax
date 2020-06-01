package it.polito.mad.madmax.ui.item

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.getColorIdCategory
import kotlinx.android.synthetic.main.item_card.view.*

class BoughtItemAdapter(
    private val cardClickListener: (Item) -> Any,
    private val actionListener: (Item) -> Any = {},
    private val showAction: Boolean = true
) : RecyclerView.Adapter<BoughtItemAdapter.ItemViewHolder>() {

    private var items: ArrayList<Item> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_card_rate, parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], cardClickListener, actionListener, showAction)
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: ArrayList<Item>) {
        val diffs = DiffUtil.calculateDiff(ItemDiffCallback(items, newItems))
        items = newItems
        diffs.dispatchUpdatesTo(this)
    }

    class ItemViewHolder(private val itemV: View) : RecyclerView.ViewHolder(itemV) {

        fun bind(item: Item, cardClickListener: (Item) -> Any, action: (Item) -> Any, showAction: Boolean) {
            // Title
            itemV.item_title.text = item.title

            // Description
            itemV.item_description.text = item.description

            // Price
            itemV.item_price.text = itemV.context.getString(R.string.item_price_set, item.price.toFloat())

            // Category
            itemV.item_category.setCardBackgroundColor(itemV.resources.getColor(getColorIdCategory(item.categoryMain)))
            itemV.item_category.post {
                itemV.item_category.apply {
                    if (radius == 0F) {
                        radius = measuredHeight / 2F
                    }
                }
            }
            itemV.item_category_text.text = item.categoryMain

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

            // Action
            if (showAction) {
                itemV.item_action.setOnClickListener{
                    val filterDialog = RateItemDialog().apply {
                        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MadMax_Dialog)
                    }
                    filterDialog.show(FragmentManager, TAG)
                }
            } else {
                itemV.item_action.visibility = View.INVISIBLE
            }
            itemV.item_card.setOnClickListener { cardClickListener(item) }
        }
    }

    // Companion
    companion object {
        const val TAG = "RATE"
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