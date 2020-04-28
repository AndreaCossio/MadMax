package it.polito.mad.madmax.lab02.ui.profile

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.createImageFile
import it.polito.mad.madmax.lab02.data_models.User
import it.polito.mad.madmax.lab02.displayMessage
import it.polito.mad.madmax.lab02.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import java.io.File
import java.io.IOException

class EditProfileFragment : Fragment() {

    // User
    private var user: User? = null

    // Destination arguments
    private val args: EditProfileFragmentArgs by navArgs()

    // Intent codes
    private val capturePermissionRequest = 0
    private val galleryPermissionRequest = 1
    private val captureIntentRequest = 2
    private val galleryIntentRequest = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        user = args.user
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profile_edit_photo.setOnClickListener { selectImage() }
        updateFields()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateUser()
        outState.putSerializable("user", user)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getSerializable("user")?.also {
            user = it as User
            updateFields()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_save_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_profile_save -> {
                updateUser()
                // TODO handle errors
                // TODO pan fields up with keyboard (like whatsapp)
                /*if (user!!.name == "" || user!!.email == "" || user!!.nickname == "" || user!!.location == "" || user!!.phone == "") {
                    if (user!!.name == "") {
                        name_tiet.error = getString(R.string.error_required)
                    }
                    //displayMessage(requireContext(), "Fill every field to continue")
                    false
                } else {*/
                    // TODO better notifications here
                    // Save user data to shared pref
                    val prefs = activity?.getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE) ?: return false
                    with (prefs.edit()) {
                        putString(getString(R.string.preference_user), Gson().toJson(user))
                        apply()
                    }

                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                    findNavController().navigate(EditProfileFragmentDirections.actionSaveProfile(user))
                    true
                //}
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureIntentRequest && resultCode == Activity.RESULT_OK) {
            updateFields()
            displayMessage(requireContext(), "Picture taken correctly")
        }
        else if (requestCode == captureIntentRequest && resultCode != Activity.RESULT_OK) {
            user?.photo?.also { File(it).delete() }
            displayMessage(requireContext(), "There was an error taking the picture")
        }
        else if (requestCode == galleryIntentRequest && resultCode == Activity.RESULT_OK && data != null) {
            val photoUri = data.data!!.toString()
            user = user?.apply { photo = photoUri } ?: User(photo = photoUri)
            updateFields()
            displayMessage(requireContext(), "Picture loaded correctly")
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
        val builder = AlertDialog.Builder(requireActivity())
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
            requestPermissions(arrayOf(Manifest.permission.CAMERA), capturePermissionRequest)
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
                            user = user?.apply { photo = photoUri.toString() } ?: User(photo = photoUri.toString())
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

    // Update user variable using views
    private fun updateUser() {
        user = user?.apply {
            name = profile_edit_name.text.toString()
            nickname = profile_edit_nickname.text.toString()
            email = profile_edit_email.text.toString()
            location = profile_edit_location.text.toString()
            phone = profile_edit_phone.text.toString()
        } ?: User(
            name = profile_edit_name.text.toString(),
            nickname = profile_edit_nickname.text.toString(),
            email = profile_edit_email.text.toString(),
            location = profile_edit_location.text.toString(),
            phone = profile_edit_phone.text.toString()
        )
    }

    // Update views using the local variable user
    private fun updateFields() {
        user?.also { user ->
            profile_edit_name.setText(user.name)
            profile_edit_nickname.setText(user.nickname)
            profile_edit_email.setText(user.email)
            profile_edit_location.setText(user.location)
            profile_edit_phone.setText(user.phone)
            user.photo?.also { uri ->
                profile_edit_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(uri))!!)
            }
        }
    }
}