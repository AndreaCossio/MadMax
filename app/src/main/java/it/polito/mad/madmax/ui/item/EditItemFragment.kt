package it.polito.mad.madmax.ui.item

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import kotlinx.android.synthetic.main.fragment_edit_item.*
import java.text.SimpleDateFormat
import java.util.*

class EditItemFragment : Fragment(), AdapterView.OnItemClickListener {

    // Item
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var tempItem: Item

    // Destination arguments
    private val args: EditItemFragmentArgs by navArgs()

    // Dialogs
    private var openDialog: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Change the item only locally
        tempItem = args.item.copy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (tempItem.itemId == "") {
            activity?.findViewById<MaterialToolbar>(R.id.main_toolbar)?.setTitle(R.string.title_create_item_fragment)
        }
        return inflater.inflate(R.layout.fragment_edit_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showProgress(requireActivity())

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), item_edit_guideline)

        // Card radius
        item_edit_card.addOnLayoutChangeListener(cardRadiusConstrain)

        // Listen for main category change
        View.OnFocusChangeListener { v: View, b: Boolean ->
            if (b) (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
        }.also {
            item_edit_main_cat.onFocusChangeListener = it
            item_edit_sub_cat.onFocusChangeListener = it
        }
        item_edit_main_cat.onItemClickListener = this

        item_edit_change_photo.setOnClickListener {
            openPhotoDialog(requireContext(), requireActivity(), { a: String -> openDialog = a}, {captureImage()}, {getImageFromGallery()}, {removeImage()})
        }

        item_edit_expiry.setOnClickListener { showDatePicker() }

        // Display item data
        updateFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listener
        item_edit_change_photo.setOnClickListener(null)
        item_edit_expiry.setOnClickListener(null)
        item_edit_card.removeOnLayoutChangeListener(cardRadiusConstrain)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                updateItem()
                if (tempItem != args.item) {
                    openSureDialog(requireContext(), requireActivity(), {
                        showProgress(requireActivity())
                        findNavController().navigateUp()
                    }, { a: String -> openDialog = a})
                } else {
                    showProgress(requireActivity())
                    findNavController().navigateUp()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateItem()
        outState.putSerializable(getString(R.string.edit_item_state), tempItem)
        outState.putString(getString(R.string.edit_item_dialog_state), openDialog)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable(getString(R.string.edit_item_state))?.also {
                tempItem = it as Item
                updateFields()
            }
            state.getString(getString(R.string.edit_item_dialog_state))?.also {
                openDialog = it
                when (openDialog) {
                    "Sure" ->
                        openSureDialog(requireContext(), requireActivity(), {
                            showProgress(requireActivity())
                            findNavController().navigateUp()
                        }, { a: String -> openDialog = a})
                    "Change" ->
                        openPhotoDialog(requireContext(), requireActivity(), {
                            a: String -> openDialog = a
                        }, {captureImage()}, {getImageFromGallery()}, {removeImage()})
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_save, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Custom back navigation
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_save -> {
                // Validate fields
                if (validateFields()) {
                    // Show progress before uploading item data to the db
                    showProgress(requireActivity())
                    tempItem.apply {
                        if (tempItem.itemId == "") {
                            itemId = itemsVM.getNewItemId()
                        }
                    }
                    itemsVM.updateItem(tempItem.copy(), tempItem.photo != args.item.photo).addOnCompleteListener {
                        deletePhoto(requireContext(), tempItem.photo)
                        findNavController().navigate(EditItemFragmentDirections.actionSaveItem(tempItem))
                    }
                    true
                } else false
            } else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        item_edit_sub_cat.setAdapter(getSubCategoryAdapter(requireContext(), parent!!.getItemAtPosition(position).toString()))
        item_edit_sub_cat.setText("", false)
    }

    private fun validateFields(): Boolean {
        // Load modified item data
        updateItem()

        var valid = true
        for (field in setOf(item_edit_title, item_edit_price, item_edit_main_cat)) {
            if (field.text.toString() == "") {
                valid = false
                field.error = getString(R.string.message_error_field_required)
            }
        }
        if (item_edit_expiry.text.toString() != "") {
            if (SimpleDateFormat("dd MMM yyy", Locale.UK).parse(item_edit_expiry.text.toString())!! < Date()) {
                item_edit_expiry.error = "Date must be future"
                valid = false
            }
        } else {
            item_edit_expiry.error = getString(R.string.message_error_field_required)
            valid = false
        }
        return valid
    }

    private fun captureImage() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                createImageFile(requireContext()).also { file ->
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        getString(R.string.file_provider),
                        file
                    )
                    tempItem.apply { photo = photoUri.toString() }
                    registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
                        if (taken) {
                            tempItem.apply { photo = compressImage(requireContext(), tempItem.photo).toString() }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_taken_photo))
                        } else {
                            deletePhoto(requireContext(), tempItem.photo)
                            tempItem.apply { photo = args.item.photo }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_error_intent))
                        }
                    }.also { picture -> picture.launch(photoUri) }
                }
            }
        }.also { permission -> permission.launch(Manifest.permission.CAMERA) }
    }

    private fun getImageFromGallery() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    uri?.also {
                        tempItem.apply { photo = compressImage(requireContext(), it.toString()).toString() }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_chosen_photo))
                    } ?: displayMessage(requireContext(), getString(R.string.message_error_intent))
                }.also { content -> content.launch("image/*") }
            }
        }.also { permission -> permission.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
    }

    private fun removeImage() {
        tempItem.apply { photo = "" }
        updateFields()
    }

    // Show date picker
    private fun showDatePicker() {
        val localeBackup = resources.configuration.locale
        Locale.setDefault(Locale.UK)
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.setCalendarConstraints(CalendarConstraints.Builder().setStart(System.currentTimeMillis()-1000).build()).build()
        picker.addOnPositiveButtonClickListener {
            item_edit_expiry.setText(picker.headerText.replace(",", ""))
            Locale.setDefault(localeBackup)
        }
        picker.show(childFragmentManager, picker.toString())
    }

    // Update item variable using views
    private fun updateItem() {
        tempItem.apply {
            title = item_edit_title.text.toString()
            description = item_edit_description.text.toString()
            categoryMain = item_edit_main_cat.text.toString()
            categorySub = item_edit_sub_cat.text.toString()
            price = item_edit_price.text.toString().toDoubleOrNull() ?: -1.0
            location = item_edit_location.text.toString()
            expiry = item_edit_expiry.text.toString()
        }
    }

    // Update views
    private fun updateFields() {
        item_edit_title.setText(tempItem.title)
        item_edit_description.setText(tempItem.description)
        item_edit_location.setText(tempItem.location)
        item_edit_expiry.setText(tempItem.expiry)
        item_edit_price.apply {
            if (tempItem.price == -1.0) {
                setText("")
            } else {
                setText(String.format("%.2f", tempItem.price))
            }
        }

        // Categories
        item_edit_main_cat.setAdapter(getMainCategoryAdapter(requireContext()))
        item_edit_sub_cat.setAdapter(getSubCategoryAdapter(requireContext(), tempItem.categoryMain))
        item_edit_main_cat.setText(tempItem.categoryMain, false)
        item_edit_sub_cat.setText(tempItem.categorySub, false)

        // Update photo
        if (tempItem.photo != "") {
            Picasso.get().load(Uri.parse(tempItem.photo)).into(item_edit_photo, object : Callback {
                override fun onSuccess() {
                    hideProgress(requireActivity())
                }

                override fun onError(e: Exception?) {
                    item_edit_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera))
                    hideProgress(requireActivity())
                }
            })
        } else {
            item_edit_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera))
            hideProgress(requireActivity())
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_EDIT_ITEM"
    }
}
