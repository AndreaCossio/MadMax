package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import kotlinx.android.synthetic.main.item.view.*
import java.util.*

class ItemAdapter(private var items: ArrayList<Item>, private val recycler: RecyclerView) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() , Filterable{

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], position, items.size, recycler)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.item_card
        private val title: TextView = itemView.item_title
        private val category: TextView = itemView.item_category
        private val price: TextView = itemView.item_price
        private val stars: TextView = itemView.item_stars
        private val image: ImageView = itemView.item_photo
        private val button: Button = itemView.item_edit_button

        fun bind(item: Item, position: Int, size: Int, recycler: RecyclerView) {
            updateFields(item, recycler.context)
            initClickListeners(item, recycler)
/*
            if (position in (size-(recycler.layoutManager as ItemListFragment.AutoFitGridLayoutManager).spanCount)..size) {
                itemView.item_card.layoutParams = (itemView.item_card.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    bottomMargin = 88.toPx()
                }
            }*/
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

    fun setItems(newItems: ArrayList<Item>){
        val diffs = DiffUtil.calculateDiff(ItemDiffCallback(items,newItems))
        items = newItems
        diffs.dispatchUpdatesTo(this)
    }

    override fun getFilter(): Filter {
        return object :Filter(){
            override fun performFiltering(filterString: CharSequence?): FilterResults {
                val results = FilterResults()
                if(filterString == null || filterString.isEmpty()){
                    results.values = items
                }else{
                    results.values = items.filter { it -> it.title.toLowerCase(Locale.ROOT).contains(filterString) }
                }
                return  results
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val newItems = p1!!.values as ArrayList<Item>
                setItems(newItems)
            }

        }
    }

}