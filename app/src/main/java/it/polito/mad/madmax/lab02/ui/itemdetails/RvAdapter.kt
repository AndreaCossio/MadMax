package it.polito.mad.madmax.lab02.ui.itemdetails

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.madmax.lab02.*
import it.polito.mad.madmax.lab02.data_models.Item

class RvAdapter(val items: ArrayList<Item>) : RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v = LayoutInflater.from(p0?.context).inflate(R.layout.adapter_item_layout, p0, false)
        return ViewHolder(v);
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items[position].title
        holder.description.text = items[position].description
        holder.bind(items[position]);

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val description: TextView = itemView.findViewById(R.id.description)
        fun bind(u: Item) {
            title.text = u.title
            description.text = u.description
        }
    }
}