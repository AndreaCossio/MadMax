package it.polito.mad.madmax.lab02.ui.item

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.createImageFile
import it.polito.mad.madmax.lab02.data_models.Item
import it.polito.mad.madmax.lab02.displayMessage
import it.polito.mad.madmax.lab02.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_edit_item.*
import java.io.File
import java.io.IOException


class EditItemFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // Item
    private var item: Item? = null

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

        val categories = resources.getStringArray(R.array.item_categories_main)
        val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, categories)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        spinner1.adapter = dataAdapter
        spinner1.onItemSelectedListener = this

        change_date.setOnClickListener { showDatePicker() }
        camera_button.setOnClickListener { selectImage() }

        updateFields()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateItem()
        outState.putSerializable("item", item)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getSerializable("item")?.also {
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
                // TODO save to shared pref or database and handle missing entries
                updateItem()
                findNavController().navigate(EditItemFragmentDirections.actionSaveItem(item))
                true
            } else -> super.onOptionsItemSelected(menuitem)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureIntentRequest && resultCode == Activity.RESULT_OK) {
            updateFields()
            displayMessage(this.requireContext(), "Picture taken correctly")
        }
        else if (requestCode == captureIntentRequest && resultCode != Activity.RESULT_OK) {
            item?.photo?.also { File(it).delete() }
            displayMessage(this.requireContext(), "There was an error taking the picture")
        }
        else if (requestCode == galleryIntentRequest && resultCode == Activity.RESULT_OK && data != null) {
            val photoUri = data.data!!.toString()
            item = item?.apply { photo = photoUri } ?: Item(photo = photoUri)
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
        } else {
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
                            item = item?.apply { photo = photoUri.toString() } ?: Item(photo = photoUri.toString())
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
        picker.addOnPositiveButtonClickListener { expiry_tv.text = picker.headerText }
        picker.show(childFragmentManager, picker.toString())
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val text: String = parent?.getItemAtPosition(position).toString()

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
        spinner2.adapter = dataAdapter
    }

    // Update user variable using views
    private fun updateItem() {
        item = item?.apply {
            title = title_tv.text.toString()
            description = description_tv.text.toString()
            category = spinner1.selectedItem.toString() // 1 or 2?
            price = price_tv.text.toString().toDouble()
            location = profile_location.text.toString()
            expiry = expiry_tv.text.toString()
        } ?: Item (
            title = title_tv.text.toString(),
            description = description_tv.text.toString(),
            category = spinner1.selectedItem.toString(), // 1 or 2?
            price = price_tv.text.toString().toDouble(),
            location = profile_location.text.toString(),
            expiry = expiry_tv.text.toString()
        )
    }

    // Update views using the local variable item
    private fun updateFields() {
        item?.also { item ->
            title_tv.setText(item.title)
            description_tv.setText(item.description)
            price_tv.setText(item.price.toString())
            profile_location.setText(item.location)
            expiry_tv.text = (item.expiry)
            item.photo?.also { photo ->
                item_image.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(photo)))
            }
        }
    }
}