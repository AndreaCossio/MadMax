package it.polito.mad.madmax.ui.item

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.viewmodel.ItemViewModel

class RateItemDialog : DialogFragment() {

    private val itemVM: ItemViewModel by activityViewModels()
    private lateinit var item: Item

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = itemVM.getSingleItem().value!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            builder.setView(inflater.inflate(R.layout.rate_dialog, null))
                .setPositiveButton("Rate",
                    DialogInterface.OnClickListener { dialog, id ->
                        val rating: Float = R.id.ratingBar.toFloat()
                        itemVM.rateUser(item, rating)
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
