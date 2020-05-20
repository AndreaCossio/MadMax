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
    private var filteredItems: ArrayList<ItemKey> = ArrayList()

    private var minPrice: Double? = null
    private var maxPrice: Double? = null
    private var mainCat: String? = null
    private var subCat: String? = null

    val myCount: MutableLiveData<Int> = MutableLiveData(0)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun getItemCount() = filteredItems.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(filteredItems[position], recycler, cardClickListener, actionClickListener)
    }

    fun setFilters(minPrice: Double? = null, maxPrice: Double? = null, mainCat: String? = null, subCat: String? = null) {
        this.minPrice = minPrice
        this.maxPrice = maxPrice
        this.mainCat = mainCat
        this.subCat = subCat

        filteredItems.clear()
        myCount.value = 0
        notifyDataSetChanged()
        for (i in items) {
            if (isOk(i.item)) {
                handleFiltered(i)
            }
        }
    }

    private fun isOk(item: Item): Boolean {
        minPrice?.also {
            if (item.price < it) {
                return false
            }
        }
        maxPrice?.also {
            if (item.price > it) {
                return false
            }
        }
        mainCat?.also {
            if (item.category_main != it) {
                return false
            }
        }
        subCat?.also {
            if (item.category_sub != it) {
                return false
            }
        }
        return true
    }

    private fun handleFiltered(item: ItemKey) {
        var alreadyIn = false
        for (i in filteredItems) {
            if (i.itemId == item.itemId) {
                if (isOk(item.item)) {
                    i.item = item.item
                    notifyItemChanged(filteredItems.indexOf(i))
                } else {
                    val position = filteredItems.indexOf(i)
                    filteredItems.removeAt(position)
                    myCount.value = myCount.value!! - 1
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, itemCount)
                }
                alreadyIn = true
                break
            }
        }
        if (!alreadyIn) {
            if (isOk(item.item)) {
                filteredItems.add(item)
                myCount.value = myCount.value!! + 1
                notifyItemInserted(filteredItems.indexOf(item))
            }
        }
    }

    fun addItem(item: ItemKey) {
        var alreadyIn = false
        for (i in items) {
            if (i.itemId == item.itemId) {
                i.item = item.item
                alreadyIn = true
                break
            }
        }
        if (!alreadyIn) {
            items.add(item)
        }
        handleFiltered(item)
    }

    fun changeItem(item: ItemKey) {
        for (i in items) {
            if (i.itemId == item.itemId) {
                i.item = item.item
                break
            }
        }
        handleFiltered(item)
    }

    fun removeItem(item: ItemKey) {
        items.remove(item)
        if (filteredItems.contains(item)) {
            val position = filteredItems.indexOf(item)
            filteredItems.removeAt(position)
            myCount.value = myCount.value!! - 1
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.item_card
        private val title: TextView = itemView.item_title
        private val category: TextView = itemView.item_category
        private val price: TextView = itemView.item_price
        private val stars: TextView = itemView.item_stars
        private val starsIcon: ImageView = itemView.item_star_icon
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
            if (item.stars == -1.0) {
                stars.visibility = View.GONE
                starsIcon.visibility = View.GONE
            } else {
                stars.visibility = View.VISIBLE
                starsIcon.visibility = View.VISIBLE
                stars.text = String.format("%.1f", item.stars)
            }
            if (item.photo != "") {
                Picasso.with(context).load(Uri.parse(item.photo)).into(image)
            } else {
                image.setImageDrawable(context.getDrawable(R.drawable.ic_camera_white))
            }
        }
    }
}