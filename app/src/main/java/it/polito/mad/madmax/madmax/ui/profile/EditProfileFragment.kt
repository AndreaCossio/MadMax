package it.polito.mad.madmax.madmax.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import java.io.IOException

class EditProfileFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var tempUser: User

    // Dialogs
    private var openDialog: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        // Create a copy of the user data
        tempUser = userVM.getCurrentUserData().value!!.copy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), profile_edit_guideline)

        // Attach listener
        profile_edit_change_photo.setOnClickListener {
            openPhotoDialog(requireContext(), requireActivity(), { a: String -> openDialog = a}, {captureImage()}, {getImageFromGallery()}, {removeImage()})
        }

        // Display user data
        updateFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listener
        profile_edit_change_photo.setOnClickListener(null)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Custom back navigation to alert the user that any unsaved changes will be discarded
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                updateUser()
                if (tempUser != userVM.getCurrentUserData().value) {
                    openSureDialog(requireContext(), requireActivity()) { a: String -> openDialog = a}
                } else {
                    showProgress(requireActivity())
                    findNavController().navigateUp()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateUser()
        outState.putSerializable(getString(R.string.edit_profile_state), tempUser)
        outState.putString(getString(R.string.edit_profile_dialog_state), openDialog)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable(getString(R.string.edit_profile_state))?.also {
                tempUser = it as User
            }
            state.getString(getString(R.string.edit_profile_dialog_state))?.also {
                openDialog = it
                when (openDialog) {
                    "Sure" -> openSureDialog(requireContext(), requireActivity()) { a: String -> openDialog = a}
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

                if (!invalidFields) {
                    // Show progress before uploading user data to the db
                    showProgress(requireActivity())

                    // // Load user data to the db and go back
                    userVM.updateUser(tempUser.copy()).addOnCompleteListener {
                        deletePhoto(requireContext(), tempUser.photo)
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
                        // Compress Image
                        tempUser.apply { photo = compressImage(requireContext(), tempUser.photo).toString() }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_taken_photo))
                    }
                    // Capturing image aborted
                    else -> {
                        // Delete destination file
                        deletePhoto(requireContext(), tempUser.photo)
                        // Restore tempUser field
                        tempUser.apply { photo = userVM.getCurrentUserData().value!!.photo }
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
        profile_edit_name.setText(tempUser.name)
        profile_edit_nickname.setText(tempUser.nickname)
        profile_edit_email.setText(tempUser.email)
        profile_edit_location.setText(tempUser.location)
        profile_edit_phone.setText(tempUser.phone)

        // Update photo
        profile_edit_photo.post {
            profile_edit_card.apply {
                radius = measuredHeight * 0.5F
            }
            profile_edit_photo.apply {
                if (tempUser.photo != "") {
                    Picasso.get().load(Uri.parse(tempUser.photo)).into(profile_edit_photo, object : Callback {
                        override fun onSuccess() {
                            translationY = 0F
                            hideProgress(requireActivity())
                        }

                        override fun onError(e: Exception?) {
                            translationY = measuredHeight / 6F
                            setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile_white))
                            hideProgress(requireActivity())
                        }
                    })
                } else {
                    translationY = measuredHeight / 6F
                    setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile_white))
                    hideProgress(requireActivity())
                }
            }
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_EDIT_PROFILE"
        private const val RP_CAMERA = 0
        private const val RP_READ_STORAGE = 1
        private const val RC_CAPTURE = 2
        private const val RC_GALLERY = 3
    }
}