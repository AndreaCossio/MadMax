package it.polito.mad.madmax.madmax.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.card.MaterialCardView
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import kotlinx.android.synthetic.main.item.view.*

class ItemAdapterFirebase(options: FirestoreRecyclerOptions<Item>) : FirestoreRecyclerAdapter<Item, ItemAdapterFirebase.ItemViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int, item: Item) {
        holder.bind(item)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.item_card
        private val title: TextView = itemView.item_title
        private val category: TextView = itemView.item_category
        private val price: TextView = itemView.item_price
        private val stars: TextView = itemView.item_stars
        private val button: Button = itemView.item_edit_button

        fun bind(item: Item) {
            title.text = item.title
            category.text = item.category_main
            price.text = String.format("%.2f", item.price)
            stars.text = String.format("%.1f", item.stars)
            /*cardView.setOnClickListener { findNavController(view).navigate(ItemListFragmentDirections.actionDetailsItem(item)) }
            button.setOnClickListener { findNavController(view).navigate(ItemListFragmentDirections.actionEditItem(item)) }*/
        }
    }
}