package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemKey
import kotlinx.android.synthetic.main.item.view.*

class ItemAdapter(private val recycler: RecyclerView) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private lateinit var items: ArrayList<ItemKey>

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position].item, recycler)
    }

    fun setItems(items: ArrayList<ItemKey>) {
        this.items = items
        notifyDataSetChanged()
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.item_card
        private val title: TextView = itemView.item_title
        private val category: TextView = itemView.item_category
        private val price: TextView = itemView.item_price
        private val stars: TextView = itemView.item_stars
        private val image: ImageView = itemView.item_photo
        private val button: Button = itemView.item_edit_button

        fun bind(item: Item, recycler: RecyclerView) {
            updateFields(item, recycler.context)
            initClickListeners(item, recycler)
        }

        private fun updateFields(item: Item, context: Context) {
            title.text = item.title
            category.text = item.category_main
            price.text = String.format("%.2f", item.price)
            stars.text = String.format("%.1f", item.stars)
            //item.photo?.also { image.setImageBitmap(handleSamplingAndRotationBitmap(context, Uri.parse(it))) }
        }

        private fun initClickListeners(item: Item, view: View) {
            cardView.setOnClickListener { findNavController(view).navigate(ItemListFragmentDirections.actionDetailsItem(item)) }
            button.setOnClickListener { findNavController(view).navigate(ItemListFragmentDirections.actionEditItem(item)) }
        }
    }
}