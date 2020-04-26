package it.polito.mad.madmax.lab02.ui.itemdetails

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.createImageFile
import it.polito.mad.madmax.lab02.data_models.Item
import it.polito.mad.madmax.lab02.displayMessage
import it.polito.mad.madmax.lab02.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.item_details_edit_fragement.*
import java.io.File
import java.io.IOException
import java.io.Serializable


class ItemDetailsEditFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var uri: Uri? = null

    // Intents
    private val CAPTURE_PERMISSIONS_REQUEST = 0
    private val GALLERY_PERMISSIONS_REQUEST = 1
    private val CAPTURE_IMAGE_REQUEST = 2
    private val GALLERY_IMAGE_REQUEST = 3

    val item = MutableLiveData<Item>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.item_details_edit_fragement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        val fm: FragmentManager? = fragmentManager

        if (fm != null) {
            for (entry in 0 until fm.backStackEntryCount) {
                Log.i(TAG, "Found fragment: " + fm.getBackStackEntryAt(entry).getId())
            }
        }*/
        item.observe(context as AppCompatActivity, Observer {
            price_tv.setText(it.price.toString())
            title_tv.setText(it.title.toString())
            description_tv.setText(it.description)
            location_tv.setText(it.location)
            expiry_tv.text = (it.expiry)
            if (item.value!!.photo != null) {
                uri = Uri.parse(item.value!!.photo)
                item_image.setImageBitmap(
                    handleSamplingAndRotationBitmap(
                        requireContext(),
                        uri!!
                    )
                )
            }
        })

        val categories = resources.getStringArray(R.array.main_categories)
        val dataAdapter = ArrayAdapter<String>(
            this.requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            categories
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        spinner1.adapter = dataAdapter

        spinner1.onItemSelectedListener = this

        change_date.setOnClickListener {
            showDatePicker()
        }

        camera_button.setOnClickListener { selectImage(requireContext()) }

        item.value = requireArguments().get("item") as Item

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_save, menu)
    }


    override fun onOptionsItemSelected(menuitem: MenuItem): Boolean {

        return when (menuitem.itemId) {
            R.id.nav_save_fragment -> {
                updateItem()
                val bundle = bundleOf("item" to item.value)
                findNavController().navigate(R.id.action_nav_edit_fragment_to_nav_item, bundle)
                return true
            }
            else -> super.onOptionsItemSelected(menuitem)
        }

    }


    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { expiry_tv.text = picker.headerText }
        picker.show(childFragmentManager, picker.toString())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateItem()
        outState.putSerializable("item", item.value as Serializable)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.getSerializable("item") != null)
            item.value = savedInstanceState.getSerializable("item") as Item
    }


    private fun updateItem(): Boolean {
        var uriString: String? = null
        if (uri != null) uriString = uri.toString()
        item.value = Item(
                uriString,
                title_tv.text.toString(),
                description_tv.text.toString(),
                price_tv.text.toString().toDoubleOrNull() ?: item.value?.price,
                spinner2.selectedItem.toString(),
                location_tv.text.toString(),
            expiry_tv.text.toString(),
            item.value?.stars
        )
        return true;
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val text: String = parent?.getItemAtPosition(position).toString()
        val secondList: Int

        when (text) {

            "Arts & Crafts" -> {
                secondList = R.array.art_and_crafts_sub
            }
            "Sports & Hobby" -> {
                secondList = R.array.sports_and_hobby_sub
            }
            "Baby" -> {
                secondList = R.array.baby_sub
            }
            "Women\'s fashion" -> {
                secondList = R.array.womens_fashion_sub
            }
            "Men\'s fashion" -> {
                secondList = R.array.mens_fashion_sub
            }
            "Electronics" -> {
                secondList = R.array.electronics_sub
            }
            "Games & Videogames" -> {
                secondList = R.array.games_and_videogames_sub
            }
            "Automotive" -> {
                secondList = R.array.automotive_sub
            }


            else -> throw Exception()
        }

        val elements = resources.getStringArray(secondList)
        val dataAdapter = ArrayAdapter<String>(
            this.requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            elements
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        spinner2.adapter = dataAdapter
    }


    private fun selectImage(context: Context) {

        val options = arrayOf<CharSequence>(
            getString(R.string.photo_dialog_take_photo),
            getString(R.string.photo_dialog_gallery_photo)
        )
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val tv: TextView = TextView(this.requireContext())

        tv.text = getString(R.string.photo_dialog_choose_photo)
        tv.setTextColor(resources.getColor(R.color.colorPrimary))
        tv.gravity = Gravity.CENTER_VERTICAL
        tv.setPadding(60, 60, 10, 10)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)

        builder.setCustomTitle(tv)
        builder.setItems(options) { _, item ->
            if (options[item] == getString(R.string.photo_dialog_take_photo)) {
                captureImage()
            } else if (options[item] == getString(R.string.photo_dialog_gallery_photo)) {
                getImageFromGallery()
            }
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            Toast.makeText(this.requireContext(), "Canceled", Toast.LENGTH_LONG).show()
            dialog.cancel()
        }
        builder.show()
    }

    // Handle capturing the Image
    private fun captureImage() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask permissions (the callback will call again this method)
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAPTURE_PERMISSIONS_REQUEST
            )
        } else {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile(requireContext())
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                        null
                    }

                    // If file generated correctly, generate intent
                    photoFile?.also {
                        uri = Uri.fromFile(it)
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                this.requireContext(),
                                "it.polito.mad.madmax.lab02.fileprovider",
                                it
                            )
                        )
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
                    }
                }
            }
        }
    }


    // Handle selecting the image from the gallery
    private fun getImageFromGallery() {
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask permissions (the callback will call again this method)
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSIONS_REQUEST
            )
        } else {
            val pickPhoto =
                Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickPhoto.type = "image/*"
            startActivityForResult(pickPhoto, GALLERY_IMAGE_REQUEST)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Capture image intent
        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            displayMessage(this.requireContext(), "Picture taken correctly")
        }
        // Capture image bad
        else if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode != Activity.RESULT_OK) {
            displayMessage(requireContext(), "Request cancelled or something went wrong.")
            // Delete created file
            File(uri!!.path!!).delete()
            displayMessage(this.requireContext(), "There was an error taking the picture")
        }
        // Gallery intent
        else if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            uri = data.data
            updateItem()
            displayMessage(this.requireContext(), "Picture loaded correctly")
        } else {
            displayMessage(requireContext(), "Request cancelled or something went wrong.")
        }
    }
}
