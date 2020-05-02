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
import androidx.cardview.widget.CardView
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
import java.io.IOException

class EditProfileFragment : Fragment() {

    // User
    private var user: User? = null
    private var tempUri: String? = null

    // Destination arguments
    private val args: EditProfileFragmentArgs by navArgs()

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    // Intent codes
    private val capturePermissionRequest = 0
    private val galleryPermissionRequest = 1
    private val captureIntentRequest = 2
    private val galleryIntentRequest = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initListeners()
        user = args.user
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
        outState.putSerializable("profile_edit_user_state", user)
        outState.putString("profile_edit_newPhotoUri_state", tempUri)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also { state ->
            state.getSerializable("profile_edit_user_state")?.also { user = it as User }
            state.getString("profile_edit_newPhotoUri_state")?.also { tempUri = it }
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
                if (user!!.name == "" || user!!.email == "" || user!!.nickname == "" || user!!.location == "" || user!!.phone == "") {
                    for (field in setOf(profile_edit_name, profile_edit_email, profile_edit_nickname, profile_edit_location, profile_edit_phone)) {
                        if (field.text.toString() == "") {
                            field.error = getString(R.string.message_error_field_required)
                        }
                    }
                    false
                } else {
                    // Save user data to shared pref
                    val prefs = activity?.getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE) ?: return false
                    with (prefs.edit()) {
                        putString(getString(R.string.preference_user), Gson().toJson(user))
                        apply()
                    }

                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                    findNavController().navigate(EditProfileFragmentDirections.actionSaveProfile(user?.copy()))
                    true
                }
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            captureIntentRequest -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        user = user?.apply { photo = tempUri } ?: User(photo = tempUri)
                        tempUri = null
                        updateFields()
                        displayMessage(requireContext(), "Picture taken correctly")
                    }
                    else -> displayMessage(requireContext(), "Request cancelled or something went wrong.")
                }
            }
            galleryIntentRequest -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.data?.also {
                            user = user?.apply { photo = it.toString() } ?: User(photo = it.toString())
                            updateFields()
                            displayMessage(requireContext(), "Picture loaded correctly")
                        } ?: displayMessage(requireContext(), "Request cancelled or something went wrong.")
                    }
                    else -> displayMessage(requireContext(), "Request cancelled or something went wrong.")
                }
            }
            else -> displayMessage(requireContext(), "Request cancelled or something went wrong.")
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
            requestPermissions(arrayOf(Manifest.permission.CAMERA), capturePermissionRequest)
        } else {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                activity?.packageManager?.also { pm ->
                    takePictureIntent.resolveActivity(pm)?.also {
                        // Create the File where the photo should go
                        try {
                            createImageFile(requireContext()).also { file ->
                                val photoUri = FileProvider.getUriForFile(requireContext(), getString(R.string.file_provider), file)
                                tempUri = photoUri.toString()
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                                startActivityForResult(takePictureIntent, captureIntentRequest)
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
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), galleryPermissionRequest)
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickPhoto ->
                pickPhoto.type = "image/*"
                if (activity?.packageManager?.queryIntentActivities(pickPhoto, 0)?.isNotEmpty() == true) {
                    startActivityForResult(pickPhoto, galleryIntentRequest)
                } else {
                    displayMessage(requireContext(), "Sorry, no gallery applications")
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
        user?.also {
            profile_edit_name.setText(it.name)
            profile_edit_nickname.setText(it.nickname)
            profile_edit_email.setText(it.email)
            profile_edit_location.setText(it.location)
            profile_edit_phone.setText(it.phone)
            it.photo?.also { uri ->
                profile_edit_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(uri))!!)
            }
        }
    }

    // Initialize listeners
    private fun initListeners() {
        // This listener is necessary to make sure that the cardView has always 50% radius (circle)
        // and that if the image is the icon, it is translated down
        cardListener = View.OnLayoutChangeListener {v, _, _, _, _, _, _, _, _ ->
            (v as CardView).apply {
                radius = measuredHeight / 2F
            }
            v.visibility = View.VISIBLE
            val photoView = (v.getChildAt(0) as ViewGroup).getChildAt(0)
            user?.photo.also {
                photoView.apply {
                    translationY = 0F
                }
            } ?: photoView.apply {
                translationY = v.measuredHeight / 6F
            }
        }
    }
}