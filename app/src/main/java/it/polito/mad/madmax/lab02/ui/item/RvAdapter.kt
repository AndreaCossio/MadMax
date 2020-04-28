package it.polito.mad.madmax.lab02.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.Item

class RvAdapter(private val items: ArrayList<Item>) : RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v = LayoutInflater.from(p0.context).inflate(R.layout.adapter_item_layout, p0, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items[position].title
        holder.description.text = items[position].description
        holder.bind(items[position])

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title_tv)
        val description: TextView = itemView.findViewById(R.id.description_tv)
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_view)
        fun bind(item: Item) {
            title.text = item.title
            description.text = item.description
            cardView.setOnClickListener {
                findNavController(itemView).navigate(ItemListFragmentDirections.actionDetailsItem(item))
            }
        }
    }
}