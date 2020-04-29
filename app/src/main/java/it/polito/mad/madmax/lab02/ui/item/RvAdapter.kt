package it.polito.mad.madmax.lab02.ui.item

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.Item

class RvAdapter(private val items: ArrayList<Item>) : RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(holder: ViewGroup, position: Int): ViewHolder {
        val v = LayoutInflater.from(holder.context).inflate(R.layout.adapter_item_layout, holder, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title_tv)
        private val category: TextView = itemView.findViewById(R.id.category_tv)
        private val rating: TextView = itemView.findViewById(R.id.rating_tv)
        private val price: TextView = itemView.findViewById(R.id.price_tv)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_view)
        private val button: Button =itemView.findViewById(R.id.edit_button)

        fun bind(item: Item) {
            price.text = String.format("%.2f", item.price)
            title.text = item.title
            category.text = item.category
            rating.text = String.format("%.2f", item.stars)
            cardView.setOnClickListener {
                findNavController(itemView).navigate(
                    ItemListFragmentDirections.actionDetailsItem(
                        item
                    )
                )
            }

            button.setOnClickListener {
                findNavController(itemView).navigate(
                    ItemListFragmentDirections.actionNavItemListFragmentToNavEditItemFragment(
                        item
                    )
                )
            }
        }
    }
}