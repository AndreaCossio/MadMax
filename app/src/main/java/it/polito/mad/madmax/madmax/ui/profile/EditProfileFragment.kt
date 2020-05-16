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
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.createImageFile
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.displayMessage
import it.polito.mad.madmax.madmax.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import java.io.File
import java.io.IOException

//TODO strings, messages, animations, resize of screen, fields check
class EditProfileFragment : Fragment() {

    // User view model of the activity
    private val userVM: UserViewModel by activityViewModels()

    // User
    private lateinit var tempUser: User

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
        tempUser = userVM.user.value!!.copy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFields()
        profile_edit_card.addOnLayoutChangeListener(cardListener)
        profile_edit_change_photo.setOnClickListener { selectImage() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profile_edit_card.removeOnLayoutChangeListener(cardListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateUser()
        outState.putSerializable(getString(R.string.profile_edit_user_state), tempUser)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable(getString(R.string.profile_edit_user_state))?.also { tempUser = it as User }
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
                // TODO improve
                if (tempUser.name == "" || tempUser.email == "" || tempUser.nickname == "" || tempUser.location == "" || tempUser.phone == "") {
                    for (field in setOf(profile_edit_name, profile_edit_email, profile_edit_nickname, profile_edit_location, profile_edit_phone)) {
                        if (field.text.toString() == "") {
                            field.error = getString(R.string.message_error_field_required)
                        }
                    }
                    false
                } else {
                    userVM.updateUser(tempUser)
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                    findNavController().navigate(EditProfileFragmentDirections.actionSaveProfile())
                    true
                }
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_CAPTURE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        updateFields()
                        displayMessage(root_edit_profile, getString(R.string.message_taken_photo))
                    }
                    else -> {
                        tempUser.also {
                            File(it.photo).delete()
                        }
                        displayMessage(root_edit_profile, getString(R.string.message_error_intent))
                    }
                }
            }
            RC_GALLERY -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.data?.also {
                            tempUser.apply { photo = it.toString() }
                            updateFields()
                            displayMessage(root_edit_profile, getString(R.string.message_chosen_photo))
                        } ?: displayMessage(root_edit_profile, getString(R.string.message_error_intent))
                    }
                    else -> displayMessage(root_edit_profile, getString(R.string.message_error_intent))
                }
            }
            //else -> displayMessage(requireContext(), "Request cancelled or something went wrong.")
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
        if (tempUser.photo != "") {
            profile_edit_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(tempUser.photo))!!)
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
                translationY = if (tempUser.photo == "") {
                    measuredHeight / 6F
                } else {
                    0F
                }
            }

            // Visibility
            cardView.visibility = View.VISIBLE
        }
    }
}