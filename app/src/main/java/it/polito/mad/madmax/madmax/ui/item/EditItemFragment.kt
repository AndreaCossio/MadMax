package it.polito.mad.madmax.madmax.ui.item

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.compressImage
import it.polito.mad.madmax.madmax.createImageFile
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemKey
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.displayMessage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_edit_item.*
import java.io.IOException
import java.text.DateFormat
import java.util.*


class EditItemFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // Item
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var tempItem: Item
    private lateinit var task: String
    private lateinit var originalItem: ItemKey

    // Destination arguments
    private val args: EditItemFragmentArgs by navArgs()

    // Card listener
    private lateinit var cardListener: View.OnLayoutChangeListener

    // Dialogs
    private var openDialog: String = ""

    // Companion
    companion object {
        const val TAG = "MM_EDIT_ITEM"
        private const val RP_CAMERA = 0
        private const val RP_READ_STORAGE = 1
        private const val RC_CAPTURE = 2
        private const val RC_GALLERY = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        task = args.message.split("-")[0]

        // Change the item only locally
        if (task == "create") {
            tempItem = Item().apply {
                userId = Firebase.auth.currentUser!!.uid
            }
            originalItem = ItemKey("", tempItem.copy())
        } else {
            for (i in itemsVM.myItems) {
                if (i.value!!.itemId == args.message.split("-")[1]) {
                    originalItem = i.value!!.copy()
                    tempItem = i.value!!.item.copy()
                }
            }
        }

        // Listener to adjust the photo
        cardListener = View.OnLayoutChangeListener {v , _, _, _, _, _, _, _, _ ->
            (v as CardView).apply {
                // Radius of the card 50%
                radius = measuredHeight / 2F
                // Show the card
                visibility = View.VISIBLE
            }
        }

        requireActivity().main_fab_add_item?.visibility = View.GONE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (task == "create") {
            activity?.findViewById<MaterialToolbar>(R.id.main_toolbar)?.setTitle(R.string.title_create_item_fragment)
        }
        return inflater.inflate(R.layout.fragment_edit_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init categories
        val categories = resources.getStringArray(R.array.item_categories_main)
        val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, categories)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        item_edit_category_main.adapter = dataAdapter
        item_edit_category_main.onItemSelectedListener = this

        // Attach listeners
        item_edit_card.addOnLayoutChangeListener(cardListener)
        item_edit_change_photo.setOnClickListener { selectImage() }
        item_edit_expiry_button.setOnClickListener { showDatePicker() }

        // Display item data
        updateFields()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                updateItem()
                if (tempItem != originalItem.item) {
                    openDialog = "Sure"
                    openSureDialog()
                } else {
                    if (task == "create") {
                        findNavController().navigate(EditItemFragmentDirections.actionCancelCreate())
                    } else {
                        findNavController().navigate(EditItemFragmentDirections.actionSaveItem("Y-details-${originalItem.itemId}"))
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listener
        item_edit_card.removeOnLayoutChangeListener(cardListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateItem()
        outState.putSerializable(getString(R.string.edit_item_state), tempItem)
        outState.putString("edit_item_dialog", openDialog)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable(getString(R.string.edit_item_state))?.also { tempItem = it as Item }
            state.getString("edit_item_dialog")?.also {
                openDialog = it
                when (openDialog) {
                    "Sure" -> openSureDialog()
                    "Change" -> selectImage()
                }
            }
            updateFields()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_save_item, menu)
    }

    override fun onOptionsItemSelected(menuitem: MenuItem): Boolean {
        return when (menuitem.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_save_item_save -> {
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
                    if (DateFormat.getDateInstance().parse(item_edit_expiry.text.toString())!! < Date()) {
                        displayMessage(requireContext(), "Date must be future")
                        invalidFields = true
                    }
                } else {
                    displayMessage(requireContext(), "Date must be set")
                    invalidFields = true
                }

                // Load item data to the db and go back
                if (!invalidFields) {
                    // Show progress before uploading item data to the db
                    requireActivity().main_progress.visibility = View.VISIBLE
                    // Keep reference to the image so that it can be delete after the upload
                    val oldPath = tempItem.photo
                    if (task == "create") {
                        val newId = itemsVM.getNewItemId()
                        itemsVM.createItem(newId, tempItem).addOnCompleteListener {
                            if (oldPath.contains(getString(R.string.file_provider))) {
                                requireContext().contentResolver.delete(Uri.parse(oldPath), null, null)
                            }
                            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                            findNavController().navigate(EditItemFragmentDirections.actionSaveItem("Y-details-$newId"))
                        }
                    } else {
                        itemsVM.updateItem(ItemKey(originalItem.itemId, tempItem), originalItem.item.photo != oldPath).addOnCompleteListener {
                            if (oldPath.contains(getString(R.string.file_provider))) {
                                requireContext().contentResolver.delete(Uri.parse(oldPath), null, null)
                            }
                            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                            findNavController().navigate(EditItemFragmentDirections.actionSaveItem("Y-details-${originalItem.itemId}"))
                        }
                    }
                    true
                } else false
            } else -> super.onOptionsItemSelected(menuitem)
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
                        val newUri = compressImage(requireContext(), tempItem.photo)
                        requireContext().contentResolver.delete(Uri.parse(tempItem.photo), null, null)
                        // Update tempUser field with the new path of the compressed image
                        tempItem.apply { photo = newUri.toString() }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_taken_photo))
                    }
                    // Capturing image aborted
                    else -> {
                        // Delete destination file
                        requireContext().contentResolver.delete(Uri.parse(tempItem.photo), null, null)
                        // Restore tempUser field
                        tempItem.apply { photo = originalItem.item.photo }
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
                        } ?: displayMessage(requireContext(), getString(R.string.message_error_intent))
                    }
                    // Error
                    else -> displayMessage(requireContext(), getString(R.string.message_error_intent))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RP_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted: CAMERA")
                    captureImage()
                }
            }
            RP_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted: READ STORAGE")
                    getImageFromGallery()
                }
            }
        }
    }

    // Click listener for changing the item photo
    private fun selectImage() {
        requireActivity().packageManager?.also { pm ->
            val items = if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                arrayOf<CharSequence>(getString(R.string.photo_dialog_change_take), getString(R.string.photo_dialog_change_gallery), getString(R.string.photo_dialog_change_remove))
            } else {
                arrayOf<CharSequence>(getString(R.string.photo_dialog_change_gallery), getString(R.string.photo_dialog_change_remove))
            }
            openDialog = "Change"
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.photo_dialog_change_title)
                .setItems(items) { dialog, which ->
                    openDialog = ""
                    dialog.cancel()
                    when(items[which]) {
                        getString(R.string.photo_dialog_change_take) -> captureImage()
                        getString(R.string.photo_dialog_change_gallery) -> getImageFromGallery()
                        else -> removeImage()
                    }
                }
                .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ ->
                    openDialog = ""
                    dialog.cancel()
                }
                .setOnKeyListener { dialog, keyCode, _ ->
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            openDialog = ""
                            dialog.cancel()
                            true
                        } else -> {
                        true
                    }
                    }
                }
                .show()
        }
    }

    private fun openSureDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.photo_dialog_sure_title)
            .setMessage(R.string.photo_dialog_sure_text)
            .setPositiveButton(R.string.photo_dialog_ok) { dialog, _ ->
                openDialog = ""
                dialog.cancel()
                if (task == "create") {
                    findNavController().navigate(EditItemFragmentDirections.actionCancelCreate())
                } else {
                    findNavController().navigate(EditItemFragmentDirections.actionSaveItem("Y-details-${originalItem.itemId}"))
                }
            }
            .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ ->
                openDialog = ""
                dialog.cancel()
            }
            .setOnKeyListener { dialog, keyCode, _ ->
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        dialog.cancel()
                        openDialog = ""
                        true
                    } else -> {
                        dialog.cancel()
                        true
                    }
                }
            }
            .show()
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
            item_edit_expiry.text = picker.headerText
        }
        picker.show(childFragmentManager, picker.toString())
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent?.id == R.id.item_edit_category_main) {
            val text: String = parent.getItemAtPosition(position).toString()
            val secondList: Int = when (text) {
                "Arts & Crafts" -> { R.array.item_categories_sub_art_and_crafts }
                "Sports & Hobby" -> { R.array.item_categories_sub_sports_and_hobby }
                "Baby" -> { R.array.item_categories_sub_baby }
                "Women\'s fashion" -> { R.array.item_categories_sub_womens_fashion }
                "Men\'s fashion" -> { R.array.item_categories_sub_mens_fashion }
                "Electronics" -> { R.array.item_categories_sub_electronics }
                "Games & Videogames" -> { R.array.item_categories_sub_games_and_videogames }
                "Automotive" -> { R.array.item_categories_sub_automotive }

                else -> throw Exception()
            }

            val elements = resources.getStringArray(secondList)
            val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, elements)
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dataAdapter.notifyDataSetChanged()
            item_edit_category_sub.adapter = dataAdapter
        }
    }

    // Update user variable using views
    private fun updateItem() {
        tempItem.apply {
            title = item_edit_title.text.toString()
            description = item_edit_description.text.toString()
            category_main = item_edit_category_main.selectedItem.toString()
            category_sub = item_edit_category_sub.selectedItem.toString()
            price = item_edit_price.text.toString().toDoubleOrNull() ?: -1.0
            location = item_edit_location.text.toString()
            expiry = item_edit_expiry.text.toString()
        }
    }

    // Update views using the local variable item
    private fun updateFields() {
        // Show progress
        activity?.main_progress?.visibility = View.VISIBLE

        val secondList: Int? = when (tempItem.category_main) {
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
        item_edit_title.setText(tempItem.title)
        item_edit_description.setText(tempItem.description)
        item_edit_category_main.setSelection(resources.getStringArray(R.array.item_categories_main).indexOf(tempItem.category_main))
        secondList?.also {
            item_edit_category_sub.setSelection(resources.getStringArray(it).indexOf(tempItem.category_sub))
        }
        if (tempItem.price == -1.0) {
            item_edit_price.setText("")
        } else {
            item_edit_price.setText(tempItem.price.toString())
        }
        item_edit_location.setText(tempItem.location)
        item_edit_expiry.text = tempItem.expiry

        // Update photo
        if (tempItem.photo != "") {
            Picasso.with(requireContext()).load(Uri.parse(tempItem.photo)).into(item_edit_photo, object : Callback {
                override fun onSuccess() {
                    // Hide progress
                    activity?.main_progress?.visibility = View.GONE
                }

                // TODO small problem here, if the image cannot be loaded, it is anyway set in the user var
                //      so the card layout does not translate down the icon
                override fun onError() {
                    Log.e(TAG, "Picasso failed to load the image")
                    // Reset drawable
                    item_edit_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
                    // Hide progress
                    activity?.main_progress?.visibility = View.GONE
                }
            })
        } else {
            item_edit_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_camera_white))
            activity?.main_progress?.visibility = View.GONE
        }
    }
}
