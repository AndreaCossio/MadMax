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
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.createImageFile
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.displayMessage
import it.polito.mad.madmax.madmax.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_edit_item.*
import java.io.File
import java.io.IOException


class EditItemFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // Item
    private var item: Item? = null
    private var newPhotoUri: String? = null

    // Destination arguments
    private val args: EditItemFragmentArgs by navArgs()

    // Intent codes
    private val capturePermissionRequest = 0
    private val galleryPermissionRequest = 1
    private val captureIntentRequest = 2
    private val galleryIntentRequest = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        item = args.item
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.item?.also {
            activity?.findViewById<MaterialToolbar>(R.id.main_toolbar)?.setTitle(R.string.title_create_item_fragment)
        }
        val categories = resources.getStringArray(R.array.item_categories_main)
        val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, categories)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        item_edit_category_main.adapter = dataAdapter
        item_edit_category_main.onItemSelectedListener = this
        item_edit_expiry_button.setOnClickListener { showDatePicker() }
        item_edit_change_photo.setOnClickListener { selectImage() }
        updateFields()

        item_edit_card.post {
            item_edit_card.radius = (item_edit_card.height * 0.5).toFloat()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateItem()
        outState.putSerializable("item_edit_item_state", item)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getSerializable("item_edit_item_state")?.also {
            item = it as Item
            updateFields()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_save_item, menu)
    }

    override fun onOptionsItemSelected(menuitem: MenuItem): Boolean {
        return when (menuitem.itemId) {
            R.id.menu_save_item_save -> {
                updateItem()
                if (item!!.title == "" || item!!.description == "" || item!!.price.toString() == "" || item!!.location == "" || item!!.expiry == "") {
                    for (field in setOf(item_edit_title, item_edit_description, item_edit_price, item_edit_location)) {
                        if (field.text.toString() == "") {
                            field.error = getString(R.string.message_error_field_required)
                        }
                    }
                    false
                } else {
                    // Save to shared pref
                    val prefs = activity?.getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE)
                    prefs?.also { pref ->
                        var itemId:Int
                        if (pref.contains("itemList")) {
                            val listType = object : TypeToken<MutableList<Item>>() {}.type
                            val itemList = Gson().fromJson<MutableList<Item>> (
                                pref.getString("itemList", "[]"),
                                listType
                            )
                            if (itemList.any { it.id == item?.id && it.id != null}) {
                                val index = itemList.indexOfFirst { it.id == item?.id }
                                itemList[index] = item!!
                            } else {
                                itemId = pref.getInt("nextId",1)
                                item!!.id = itemId
                                itemId++
                                pref.edit().putInt("nextId",itemId).apply()
                                itemList.add(item!!)
                            }

                            prefs.edit().remove("itemList").putString("itemList", Gson().toJson(itemList)).apply()
                        } else {
                            item!!.id = 1
                            val itemList = listOf<Item>(item!!)
                            prefs.edit().putString("itemList", Gson().toJson(itemList)).putInt("nextId",2).apply()
                        }
                    }

                    // Close keyboard
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                    findNavController().navigate(EditItemFragmentDirections.actionSaveItem(item))
                    true
                }
            } else -> super.onOptionsItemSelected(menuitem)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureIntentRequest && resultCode == Activity.RESULT_OK) {
            item = item?.apply { photo = newPhotoUri } ?: Item(
                photo = newPhotoUri
            )
            updateFields()
            displayMessage(this.requireContext(), "Picture taken correctly")
        }
        else if (requestCode == captureIntentRequest && resultCode != Activity.RESULT_OK) {
            newPhotoUri?.also {
                File(it).delete()
            }
            displayMessage(this.requireContext(), "There was an error taking the picture")
        }
        else if (requestCode == galleryIntentRequest && resultCode == Activity.RESULT_OK && data != null) {
            val photoUri = data.data!!.toString()
            item = item?.apply { photo = photoUri } ?: Item(
                photo = photoUri
            )
            updateFields()
            displayMessage(this.requireContext(), "Picture loaded correctly")
        } else {
            displayMessage(requireContext(), "Request cancelled or something went wrong.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            capturePermissionRequest -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureImage()
                }
            }
            galleryPermissionRequest -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery()
                }
            }
        }
    }

    // Click listener for changing the user profile photo
    // Shows a dialog
    private fun selectImage() {
        val builder = android.app.AlertDialog.Builder(requireActivity())
        requireActivity().packageManager?.also { pm ->
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY).also { hasCamera ->
                if (hasCamera) {
                    builder.setTitle(R.string.photo_dialog_choose_photo)
                        .setItems(arrayOf<CharSequence>(getString(R.string.photo_dialog_take_photo), getString(R.string.photo_dialog_gallery_photo))) { _, item ->
                            if (item == 0) captureImage()
                            else getImageFromGallery()
                        }
                        .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ -> dialog.cancel() }
                } else {
                    builder.setTitle(R.string.photo_dialog_choose_photo)
                        .setItems(arrayOf<CharSequence>(getString(R.string.photo_dialog_gallery_photo))) { _, _ -> getImageFromGallery() }
                        .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ -> dialog.cancel() }
                }
                builder.show()
            }
        }
    }

    // Handle capturing the Image
    private fun captureImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), capturePermissionRequest)
        }else{
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                activity?.packageManager?.also { pm ->
                    takePictureIntent.resolveActivity(pm)?.also {
                        // Create the File where the photo should go
                        val photoFile: File? = try {
                            createImageFile(requireContext())
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                            null
                        }

                        // If file generated correctly, generate intent
                        photoFile?.also {
                            val photoUri = FileProvider.getUriForFile(requireContext(), getString(R.string.file_provider), it)
                            newPhotoUri = photoUri.toString()
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                            startActivityForResult(takePictureIntent, captureIntentRequest)
                        }
                    }
                }
            }
        }
    }

    // Handle selecting the image from the gallery
    private fun getImageFromGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), galleryPermissionRequest)
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickPhoto ->
                pickPhoto.type = "image/*"
                if (activity?.packageManager?.queryIntentActivities(pickPhoto, 0)?.isNotEmpty() == true) {
                    startActivityForResult(pickPhoto, galleryIntentRequest)
                }
            }
        }
    }

    // Show date picker
    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { item_edit_expiry.text = picker.headerText }
        picker.show(childFragmentManager, picker.toString())
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

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
        item = item?.apply {
            title = item_edit_title.text.toString()
            description = item_edit_description.text.toString()
            category_main = item_edit_category_main.selectedItem.toString()
            category_sub = item_edit_category_sub.selectedItem.toString()
            price = item_edit_price.text.toString().toDoubleOrNull() ?: 0.0
            location = item_edit_location.text.toString()
            expiry = item_edit_expiry.text.toString()
        } ?: Item(
            title = item_edit_title.text.toString(),
            description = item_edit_description.text.toString(),
            category_main = item_edit_category_main.selectedItem.toString(),
            category_sub = item_edit_category_sub.selectedItem.toString(),
            price = item_edit_price.text.toString().toDoubleOrNull() ?: 0.0,
            location = item_edit_location.text.toString(),
            expiry = item_edit_expiry.text.toString()
        )
    }

    // Update views using the local variable item
    private fun updateFields() {
        val secondList: Int? = when (item?.category_main) {
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
        item?.also { item ->
            item_edit_title.setText(item.title)
            item_edit_description.setText(item.description)
            item_edit_category_main.setSelection(resources.getStringArray(R.array.item_categories_main).indexOf(item.category_main))
            secondList?.also {
                item_edit_category_sub.setSelection(resources.getStringArray(it).indexOf(item.category_sub))
            }
            item_edit_price.setText(item.price.toString())
            item_edit_location.setText(item.location)
            item_edit_expiry.text = item.expiry
            item.photo?.also { photo ->
                item_edit_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(photo))!!)
            }
        }
    }
}
