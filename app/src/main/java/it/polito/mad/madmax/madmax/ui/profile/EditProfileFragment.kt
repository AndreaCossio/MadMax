package it.polito.mad.madmax.madmax.ui.profile

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
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import java.io.IOException

// TODO show alert when going back with unmodified data
//      dialog disappears on rotation
class EditProfileFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var tempUser: User

    // Card listener
    private lateinit var cardListener: View.OnLayoutChangeListener

    companion object {
        const val TAG = "MM_EDIT_PROFILE"
        private const val RP_CAMERA = 0
        private const val RP_READ_STORAGE = 1
        private const val RC_CAPTURE = 2
        private const val RC_GALLERY = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Change the user only locally
        tempUser = userVM.user.value!!.copy()

        // Listener to adjust the photo
        cardListener = View.OnLayoutChangeListener {v , _, _, _, _, _, _, _, _ ->
            (v as CardView).apply {
                // Offset the drawable
                (getChildAt(0) as ViewGroup).getChildAt(0).apply {
                    translationY = if (tempUser.photo == "") {
                        measuredHeight / 6F
                    } else {
                        0F
                    }
                }
                // Radius of the card 50%
                radius = measuredHeight / 2F
                // Show the card
                visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Attach listeners
        profile_edit_card.addOnLayoutChangeListener(cardListener)
        profile_edit_change_photo.setOnClickListener { selectImage() }

        // Display user data
        updateFields()
    }

    override fun onDestroyView() {
        // Detach listener
        profile_edit_card.removeOnLayoutChangeListener(cardListener)
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        updateUser()
        outState.putSerializable(getString(R.string.edit_profile_state), tempUser)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable(getString(R.string.edit_profile_state))?.also { tempUser = it as User }
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
                // Load modified user data
                updateUser()

                // Validate fields
                var invalidFields = false
                for (field in setOf(profile_edit_email, profile_edit_name)) {
                    if (field == profile_edit_email && !isEmailValid(field.text.toString())) {
                        invalidFields = true
                        field.error = getString(R.string.message_error_field_invalid_email)
                    } else {
                        if (field.text.toString() == "") {
                            invalidFields = true
                            field.error = getString(R.string.message_error_field_required)
                        }
                    }
                }

                // Load user data to the db and go back
                if (!invalidFields) {
                    // Show progress before uploading user data to the db
                    activity?.main_progress?.visibility = View.VISIBLE
                    // Keep reference to the image so that it can be delete after the upload
                    val oldPath = tempUser.photo
                    userVM.updateUser(tempUser).addOnCompleteListener {
                        if (oldPath.contains(getString(R.string.file_provider))) {
                            requireContext().contentResolver.delete(Uri.parse(oldPath), null, null)
                        }
                        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                        findNavController().navigate(EditProfileFragmentDirections.actionSaveProfile())
                    }
                    true
                } else false
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    // Compresses selected images and deletes, if necessary, old files
    // Variable tempUser updated accordingly and fields updated
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_CAPTURE -> {
                when (resultCode) {
                    // Image taken correctly
                    Activity.RESULT_OK -> {
                        // Compress taken image
                        val newUri = compressImage(requireContext(), tempUser.photo)
                        requireContext().contentResolver.delete(Uri.parse(tempUser.photo), null, null)
                        // Update tempUser field with the new path of the compressed image
                        tempUser.apply { photo = newUri.toString() }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_taken_photo))
                    }
                    // Capturing image aborted
                    else -> {
                        // Delete destination file
                        requireContext().contentResolver.delete(Uri.parse(tempUser.photo), null, null)
                        // Restore tempUser field
                        tempUser.apply { photo = userVM.user.value?.photo ?: "" }
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
                            tempUser.apply { photo = compressImage(requireContext(), it.toString()).toString() }
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

    // Click listener for changing the user profile photo
    private fun selectImage() {
        requireActivity().packageManager?.also { pm ->
            val builder = MaterialAlertDialogBuilder(requireContext())
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                builder.setTitle(R.string.photo_dialog_choose_photo)
                    .setItems(arrayOf<CharSequence>(getString(R.string.photo_dialog_take_photo), getString(R.string.photo_dialog_gallery_photo), getString(R.string.photo_dialog_delete_image))) { _, which ->
                        when (which) {
                            0 -> captureImage()
                            1 -> getImageFromGallery()
                            else -> removeImage()
                        }
                    }
                    .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ -> dialog.cancel() }
            } else {
                builder.setTitle(R.string.photo_dialog_choose_photo)
                    .setSingleChoiceItems(arrayOf<CharSequence>(getString(R.string.photo_dialog_gallery_photo), getString(R.string.photo_dialog_delete_image)), 0) { _, which ->
                        when (which) {
                            0 -> getImageFromGallery()
                            else -> removeImage()
                        }
                    }
                    .setNegativeButton(R.string.photo_dialog_cancel) { dialog, _ -> dialog.cancel() }
            }
            builder.show()
        }
    }

    // Intent to take a picture with the camera
    // Destination saved in tempUser and handled in return
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
                                tempUser.apply { photo = photoUri.toString() }
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
        tempUser.apply { photo = "" }
        updateFields()
    }

    // Update user variable using views
    private fun updateUser() {
        tempUser.apply {
            name = profile_edit_name.text.toString()
            nickname = profile_edit_nickname.text.toString()
            email = profile_edit_email.text.toString()
            location = profile_edit_location.text.toString()
            phone = profile_edit_phone.text.toString()
        }
    }

    // Update views
    private fun updateFields() {
        // Show progress
        activity?.main_progress?.visibility = View.VISIBLE

        // Update fields
        profile_edit_name.setText(tempUser.name)
        profile_edit_nickname.setText(tempUser.nickname)
        profile_edit_email.setText(tempUser.email)
        profile_edit_location.setText(tempUser.location)
        profile_edit_phone.setText(tempUser.phone)

        // Update photo
        if (tempUser.photo != "") {
            Picasso.with(requireContext()).load(Uri.parse(tempUser.photo)).into(profile_edit_photo, object : Callback {
                override fun onSuccess() {
                    // Hide progress
                    activity?.main_progress?.visibility = View.GONE
                }

                // TODO small problem here, if the image cannot be loaded, it is anyway set in the user var
                //      so the card layout does not translate down the icon
                override fun onError() {
                    Log.e(TAG, "Picasso failed to load the image")
                    // Reset drawable
                    profile_edit_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile_white))
                    // Hide progress
                    activity?.main_progress?.visibility = View.GONE
                }
            })
        } else {
            profile_edit_photo.setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile_white))
            activity?.main_progress?.visibility = View.GONE
        }
    }
}