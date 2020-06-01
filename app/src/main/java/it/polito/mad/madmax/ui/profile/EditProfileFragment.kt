package it.polito.mad.madmax.ui.profile

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.item.OnSaleListFragment
import kotlinx.android.synthetic.main.fragment_edit_profile.*

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
        showProgress(requireActivity())

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Real 0.33 guideline
        guidelineConstrain(requireContext(), profile_edit_guideline)

        // Card radius
        profile_edit_card.addOnLayoutChangeListener(cardRadiusConstrain)

        // Attach change photo listener
        profile_edit_change_photo.setOnClickListener {
            openPhotoDialog(requireContext(), requireActivity(), { a: String -> openDialog = a}, {captureImage()}, {getImageFromGallery()}, {removeImage()})
        }

        show_map.setOnClickListener {
            val filterDialog = MapsFragment().apply {
                setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MadMax_Dialog)
            }
            filterDialog.show(requireFragmentManager(), OnSaleListFragment.TAG)
        }

        setFragmentResultListener("MAP_ADDRESS") { key, bundle ->
            // We use a String here, but any type that can be put in a Bundle is supported
            val result = bundle.getString("address")
            profile_edit_location.setText(result)
            // Do something with the result...
        }

        // Display user data
        updateFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listener
        profile_edit_change_photo.setOnClickListener(null)
        profile_edit_card.removeOnLayoutChangeListener(cardRadiusConstrain)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Custom back navigation to alert the user that any unsaved changes will be discarded
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                updateUser()
                if (tempUser != userVM.getCurrentUserData().value) {
                    openSureDialog(requireContext(), requireActivity(), {
                        showProgress(requireActivity())
                        findNavController().navigateUp()
                    }, { a: String -> openDialog = a})
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
                updateFields()
            }
            state.getString(getString(R.string.edit_profile_dialog_state))?.also {
                openDialog = it
                when (openDialog) {
                    "Sure" -> openSureDialog(requireContext(), requireActivity(), {
                        showProgress(requireActivity())
                        findNavController().navigateUp()
                    }, { a: String -> openDialog = a})
                    "Change" -> openPhotoDialog(requireContext(), requireActivity(), { a: String -> openDialog = a}, {captureImage()}, {getImageFromGallery()}, {removeImage()})
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_save, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Custom back navigation to check unsaved changes
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

                    // Load user data to the db and go back
                    userVM.updateUser(tempUser.copy()).addOnCompleteListener {
                        deletePhoto(requireContext(), tempUser.photo)
                        findNavController().navigate(EditProfileFragmentDirections.actionSaveProfile())
                    }
                    true
                } else false
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun captureImage() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                createImageFile(requireContext()).also { file ->
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        getString(R.string.file_provider),
                        file
                    )
                    tempUser.apply { photo = photoUri.toString() }
                    registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
                        if (taken) {
                            tempUser.apply { photo = compressImage(requireContext(), tempUser.photo).toString() }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_taken_photo))
                        } else {
                            // Delete destination file
                            deletePhoto(requireContext(), tempUser.photo)
                            // Restore tempItem field
                            tempUser.apply { photo = userVM.getCurrentUserData().value!!.photo }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_error_intent))
                        }
                    }.also { picture -> picture.launch(photoUri) }
                }
            }
        }.also { permission -> permission.launch(Manifest.permission.CAMERA) }
    }

    private fun getImageFromGallery() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    uri?.also {
                        tempUser.apply { photo = compressImage(requireContext(), it.toString()).toString() }
                        updateFields()
                        displayMessage(requireContext(), getString(R.string.message_chosen_photo))
                    } ?: displayMessage(requireContext(), getString(R.string.message_error_intent))
                }.also { content -> content.launch("image/*") }
            }
        }.also { permission -> permission.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
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
        profile_edit_photo.apply {
            if (tempUser.photo != "") {
                Picasso.get().load(Uri.parse(tempUser.photo)).into(profile_edit_photo, object : Callback {
                    override fun onSuccess() {
                        translationY = 0F
                        hideProgress(requireActivity())
                    }

                    override fun onError(e: Exception?) {
                        translationY = measuredHeight / 6F
                        setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile))
                        hideProgress(requireActivity())
                    }
                })
            } else {
                translationY = measuredHeight / 6F
                setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile))
                hideProgress(requireActivity())
            }
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_EDIT_PROFILE"
    }
}