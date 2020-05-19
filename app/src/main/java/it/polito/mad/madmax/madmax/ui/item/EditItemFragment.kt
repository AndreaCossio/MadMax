package it.polito.mad.madmax.madmax.ui.item

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.Item
import kotlinx.android.synthetic.main.fragment_edit_item.*
import java.io.File
import java.io.IOException


class EditItemFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // Item
    private var item: Item? = null
    private lateinit var tempItem: Item
    private var newPhotoUri: String? = null

    // Destination arguments
    private val args: EditItemFragmentArgs by navArgs()

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    // Companion
    companion object {
        // Intent codes
        private const val RP_CAMERA = 0
        private const val RP_GALLERY = 1
        private const val RC_CAPTURE = 2
        private const val RC_GALLERY = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initListeners()
        item = args.item
        tempItem = item?.copy() ?: Item()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(args.item == null){
            activity?.findViewById<MaterialToolbar>(R.id.main_toolbar)?.setTitle(R.string.title_create_item_fragment)
        }
        return inflater.inflate(R.layout.fragment_edit_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categories = resources.getStringArray(R.array.item_categories_main)
        val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, categories)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        item_edit_category_main.adapter = dataAdapter
        item_edit_category_main.onItemSelectedListener = this
        item_edit_expiry_button.setOnClickListener { showDatePicker() }
        item_edit_change_photo.setOnClickListener { selectImage() }
        updateFields()
        item_edit_card.addOnLayoutChangeListener(cardListener)
    }

    override fun onDestroyView() {
        item_edit_card.removeOnLayoutChangeListener(cardListener)
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateItem()
        outState.putSerializable(getString(R.string.edit_item_state), tempItem)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable(getString(R.string.edit_item_state))?.also { tempItem = it as Item }
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
                // TODO improve
                if (item!!.title == "" || item!!.description == "" || item!!.price.toString() == "" || item!!.location == "" || item!!.expiry == "") {
                    for (field in setOf(item_edit_title, item_edit_description, item_edit_price, item_edit_location)) {
                        if (field.text.toString() == "") {
                            field.error = getString(R.string.message_error_field_required)
                        }
                    }
                    false
                } else {

                    writeToFirestore()

                    // Close keyboard
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                    findNavController().navigate(EditItemFragmentDirections.actionSaveItem(item))
                    true
                }
            } else -> super.onOptionsItemSelected(menuitem)
        }
    }

    // TODO imbarazzante
    private fun writeToFirestore(){

        val db = Firebase.firestore
        db.collection("items")
            .document()
            .set(
                item!!
            )
            .addOnSuccessListener {
            Log.d("XXX","Success")
        }.addOnFailureListener{
            Log.d("XXX","Error")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_CAPTURE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_taken_photo))
                    }
                    else -> {
                        tempItem.also {
                            File(it.photo).delete()
                        }
                        displayMessage(requireContext(), getString(R.string.message_error_intent))
                    }
                }
            }
            RC_GALLERY -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.data?.also {
                            tempItem.apply { photo = it.toString() }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_chosen_photo))
                        } ?: displayMessage(requireContext(), getString(R.string.message_error_intent))
                    }
                    else -> displayMessage(requireContext(), getString(R.string.message_error_intent))
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
            RP_GALLERY -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery()
                }
            }
        }
    }

    // Click listener for changing the user profile photo
    private fun selectImage() {
        val builder = AlertDialog.Builder(requireActivity())
        requireActivity().packageManager?.also { pm ->
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                builder.setTitle(R.string.photo_dialog_change_title)
                    .setItems(arrayOf<CharSequence>(getString(R.string.photo_dialog_change_take), getString(R.string.photo_dialog_change_gallery))) { _, item ->
                        if (item == 0) captureImage()
                        else getImageFromGallery()
                    }
                    .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ -> dialog.cancel() }
            } else {
                builder.setTitle(R.string.photo_dialog_change_title)
                    .setItems(arrayOf<CharSequence>(getString(R.string.photo_dialog_change_gallery))) { _, _ -> getImageFromGallery() }
                    .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ -> dialog.cancel() }
            }
            builder.show()
        }
    }

    // Handle capturing the Image
    private fun captureImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), RP_CAMERA)
        } else {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                activity?.packageManager?.also { pm ->
                    takePictureIntent.resolveActivity(pm)?.also {
                        // Create the File where the photo should go
                        try {
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

    // Handle selecting the image from the gallery
    private fun getImageFromGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), RP_GALLERY)
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
        tempItem.apply {
            title = item_edit_title.text.toString()
            description = item_edit_description.text.toString()
            category_main = item_edit_category_main.selectedItem.toString()
            category_sub = item_edit_category_sub.selectedItem.toString()
            price = item_edit_price.text.toString().toDoubleOrNull() ?: 0.0
            location = item_edit_location.text.toString()
            expiry = item_edit_expiry.text.toString()
        }
    }

    // Update views using the local variable item
    private fun updateFields() {
        val secondList: Int? = when (tempItem?.category_main) {
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
        item_edit_price.setText(tempItem.price.toString())
        item_edit_location.setText(tempItem.location)
        item_edit_expiry.text = tempItem.expiry
        if (tempItem.photo == "") {
            Picasso.with(requireContext()).load(Uri.parse(tempItem.photo)).into(item_edit_photo)
        }
    }

    // Initialize listeners
    private fun initListeners() {
        // This listener is necessary to make sure that the cardView has always 50% radius (circle)
        // and that if the image is the icon, it is translated down
        cardListener = View.OnLayoutChangeListener {v, _, _, _, _, _, _, _, _ ->
            val cardView: CardView = v as CardView
            val imageView = (cardView.getChildAt(0) as ViewGroup).getChildAt(0)

            // Radius of the card
            cardView.apply { radius = measuredHeight / 2F }

            // Translation of the photo
            imageView.apply {
                if (tempItem.photo == "") {
                    setPadding(16.toPx(), 16.toPx(), 16.toPx(), 32.toPx())
                } else {
                    setPadding(0,0,0,0)
                }
            }

            // Visibility
            cardView.visibility = View.VISIBLE
        }
    }
}
