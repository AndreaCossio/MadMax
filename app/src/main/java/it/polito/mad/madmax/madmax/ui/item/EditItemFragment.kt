package it.polito.mad.madmax.madmax.ui.item

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
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
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemArg
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import kotlinx.android.synthetic.main.fragment_edit_item.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditItemFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // Item
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var tempItem: Item
    private lateinit var itemArg: ItemArg

    // Destination arguments
    private val args: EditItemFragmentArgs by navArgs()

    // Dialogs
    private var openDialog: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        itemArg = args.itemArg

        // Change the item only locally
        tempItem = itemArg.item.copy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (itemArg.task == "Create") {
            activity?.findViewById<MaterialToolbar>(R.id.main_toolbar)?.setTitle(R.string.title_create_item_fragment)
        }
        return inflater.inflate(R.layout.fragment_edit_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), item_edit_guideline)

        // Attach listeners
        item_edit_main_cat.onItemSelectedListener = this
        item_edit_change_photo.setOnClickListener {
            openPhotoDialog(requireContext(), requireActivity(), { a: String -> openDialog = a}, {captureImage()}, {getImageFromGallery()}, {removeImage()})
        }
        //item_edit_expiry_button.setOnClickListener { showDatePicker() }
        item_edit_expiry.setOnClickListener { showDatePicker() }
        // Init categories
        item_edit_main_cat.adapter = getMainCategoryAdapter(requireContext())
        item_edit_main_cat.setSelection(getMainCategories(requireContext()).indexOf(tempItem.category_main))

        // Display item data
        updateFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listener
        item_edit_change_photo.setOnClickListener(null)
        //item_edit_expiry_button.setOnClickListener(null)
        item_edit_expiry.setOnClickListener(null)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                updateItem()
                if (tempItem != itemArg.item) {
                    if (itemArg.task == "Create") {
                        openSureDialog(requireContext(), requireActivity(), { findNavController().navigate(EditItemFragmentDirections.actionCancelCreate()) }, { a: String -> openDialog = a})
                    } else {
                        openSureDialog(requireContext(), requireActivity(), {
                            findNavController().navigate(EditItemFragmentDirections.actionCancelEdit(ItemArg("Details", itemArg.item, itemArg.owned))) }, { a: String -> openDialog = a})
                    }
                } else {
                    showProgress(requireActivity())
                    if (itemArg.task == "Create") {
                        findNavController().navigate(EditItemFragmentDirections.actionCancelCreate())
                    } else {
                        findNavController().navigate(EditItemFragmentDirections.actionCancelEdit(
                            ItemArg("Details", itemArg.item, itemArg.owned)
                        ))
                    }
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
            }
            state.getString(getString(R.string.edit_item_dialog_state))?.also {
                openDialog = it
                when (openDialog) {
                    "Sure" -> {
                        if (itemArg.task == "Create") {
                            openSureDialog(requireContext(), requireActivity(), { findNavController().navigate(EditItemFragmentDirections.actionCancelCreate()) }, { a: String -> openDialog = a})
                        } else {
                            openSureDialog(requireContext(), requireActivity(), {
                                findNavController().navigate(EditItemFragmentDirections.actionCancelEdit(ItemArg("Details", itemArg.item, itemArg.owned))) }, { a: String -> openDialog = a})
                        }
                    }
                    "Change" -> openPhotoDialog(requireContext(), requireActivity(), { a: String -> openDialog = a}, {captureImage()}, {getImageFromGallery()}, {removeImage()})
                }
            }
            updateFields()
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
                // Load modified item data
                updateItem()

                // Validate fields
                var invalidFields = false
                for (field in setOf(item_edit_title, item_edit_price)) {
                    if (field.text.toString() == "") {
                        invalidFields = true
                        field.error = getString(R.string.message_error_field_required)
                    }
                }

                if (item_edit_expiry.text.toString() != "") {
                    if (SimpleDateFormat("dd MMM yyy", Locale.getDefault()).parse(item_edit_expiry.text.toString())!! < Date()) {
                        item_edit_expiry.error = "Date must be future"
                        invalidFields = true
                    }
                } else {
                    item_edit_expiry.error = getString(R.string.message_error_field_required)
                    invalidFields = true
                }

                if (!invalidFields) {
                    // Show progress before uploading user data to the db
                    showProgress(requireActivity())
                    val newId = if (itemArg.task == "Create") {
                        itemsVM.getNewItemId()
                    } else {
                        itemArg.item.itemId
                    }
                    tempItem.apply {
                        itemId = newId
                    }
                    itemsVM.updateItem(tempItem.copy(), tempItem.photo != itemArg.item.photo).addOnCompleteListener {
                        deletePhoto(requireContext(), tempItem.photo)
                        findNavController().navigate(EditItemFragmentDirections.actionSaveItem(
                            ItemArg("Details", tempItem, true)
                        ))
                    }
                    true
                } else false
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Compresses selected images and deletes, if necessary, old files
    // Variable tempItem updated accordingly and fields updated
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_CAPTURE -> {
                when (resultCode) {
                    // Image taken correctly
                    Activity.RESULT_OK -> {
                        // Compress taken image
                        tempItem.apply { photo = compressImage(requireContext(), tempItem.photo).toString() }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_taken_photo))
                    }
                    // Capturing image aborted
                    else -> {
                        // Delete destination file
                        deletePhoto(requireContext(), tempItem.photo)
                        // Restore tempUser field
                        tempItem.apply { photo = itemArg.item.photo }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_error_intent))
                    }
                }
            }
            RC_GALLERY -> {
                when (resultCode) {
                    // Image selected correctly
                    Activity.RESULT_OK -> {
                        data?.data?.also {
                            // Compress the image and update tempUser field
                            tempItem.apply { photo = compressImage(requireContext(), it.toString()).toString() }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_chosen_photo))
                        } ?: run {
                            hideProgress(requireActivity())
                            displayMessage(requireContext(), getString(R.string.message_error_intent))
                        }
                    }
                    // Error
                    else -> {
                        hideProgress(requireActivity())
                        displayMessage(requireContext(), getString(R.string.message_error_intent))
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RP_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureImage()
                }
            }
            RP_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery()
                }
            }
        }
    }

    // Intent to take a picture with the camera
    // Destination saved in tempItem and handled in return
    private fun captureImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), RP_CAMERA)
        } else {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                activity?.packageManager?.also { pm ->
                    takePictureIntent.resolveActivity(pm)?.also {
                        try {
                            // Create the File where the photo should go
                            createImageFile(requireContext()).also { file ->
                                val photoUri = FileProvider.getUriForFile(requireContext(), getString(R.string.file_provider), file)
                                tempItem.apply { photo = photoUri.toString() }
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                                showProgress(requireActivity())
                                startActivityForResult(takePictureIntent, RC_CAPTURE)
                            }
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    // Intent to choose an image from the gallery
    private fun getImageFromGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), RP_READ_STORAGE)
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickPhoto ->
                pickPhoto.type = "image/*"
                if (activity?.packageManager?.queryIntentActivities(pickPhoto, 0)?.isNotEmpty() == true) {
                    showProgress(requireActivity())
                    startActivityForResult(pickPhoto, RC_GALLERY)
                } else {
                    displayMessage(requireContext(), getString(R.string.message_error_gallery_app))
                }
            }
        }
    }

    // Delete image of the user
    private fun removeImage() {
        tempItem.apply { photo = "" }
        updateFields()
    }

    // Show date picker
    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.setCalendarConstraints(CalendarConstraints.Builder().setStart(System.currentTimeMillis()-1000).build()).build()
        picker.addOnPositiveButtonClickListener {
            item_edit_expiry.setText(picker.headerText)
        }
        picker.show(childFragmentManager, picker.toString())
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        // Called when the current selection is removed
        TODO("Not useful in our case")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent?.id == R.id.item_edit_main_cat) {
            item_edit_sub_cat.adapter = getSubCategoryAdapter(requireContext(), parent.getItemAtPosition(position).toString())

            if (itemArg.item.category_sub != "") {
                item_edit_sub_cat.setSelection(getSubcategories(requireContext(), parent.getItemAtPosition(position).toString()).indexOf(itemArg.item.category_sub))
            }
        }
    }

    // Update item variable using views
    private fun updateItem() {
        tempItem.apply {
            title = item_edit_title.text.toString()
            description = item_edit_description.text.toString()
            category_main = item_edit_main_cat.selectedItem.toString()
            category_sub = item_edit_sub_cat.selectedItem.toString()
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

        /*val secondList: Int? = when (tempItem.category_main) {
            "Arts & Crafts" -> { R.array.item_categories_sub_art_and_crafts }
            "Sports & Hobby" -> { R.array.item_categories_sub_sports_and_hobby }
            "Baby" -> { R.array.item_categories_sub_baby }
            "Women\'s fashion" -> { R.array.item_categories_sub_womens_fashion }
            "Men\'s fashion" -> { R.array.item_categories_sub_mens_fashion }
            "Electronics" -> { R.array.item_categories_sub_electronics }
            "Games & Videogames" -> { R.array.item_categories_sub_games_and_videogames }
            "Automotive" -> { R.array.item_categories_sub_automotive }
            else -> null
        }
        filter_dialog_main_cat.setSelection(resources.getStringArray(R.array.item_categories_main).indexOf(tempItem.category_main))
        secondList?.also {
            filter_dialog_sub_cat.setSelection(resources.getStringArray(it).indexOf(tempItem.category_sub))
        }*/

        // Update photo
        item_edit_photo.post {
            item_edit_card.apply {
                radius = measuredHeight * 0.5F
            }
            item_edit_photo.apply {
                if (tempItem.photo != "") {
                    Picasso.get().load(Uri.parse(tempItem.photo)).into(item_edit_photo, object : Callback {
                        override fun onSuccess() {
                            hideProgress(requireActivity())
                        }

                        override fun onError(e: Exception?) {
                            setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
                            hideProgress(requireActivity())
                        }
                    })
                } else {
                    setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
                    hideProgress(requireActivity())
                }
            }
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_EDIT_ITEM"
        private const val RP_CAMERA = 0
        private const val RP_READ_STORAGE = 1
        private const val RC_CAPTURE = 2
        private const val RC_GALLERY = 3
    }
}
