package it.polito.mad.madmax.ui.profile

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.PlaceInfo
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.MapDialog
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import java.io.IOException
import java.util.*

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

        // Address text listener
        profile_edit_location.addTextChangedListener(locationListener)

        // Map listener
        profile_edit_location_button.setOnClickListener {
            MapDialog().apply {
                setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MadMax_Dialog)
                arguments = bundleOf("location" to this@EditProfileFragment.profile_edit_location.text.toString(), "editMode" to true)
            }.show(requireFragmentManager(), TAG)
        }

        // Dialog listener
        setFragmentResultListener("MAP_DIALOG_REQUEST") { _, bundle ->
            profile_edit_location.setText(bundle.getString("address"))
        }

        // Display user data
        updateFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listeners
        profile_edit_location_button.setOnClickListener(null)
        profile_edit_location.removeTextChangedListener(locationListener)
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
                    "Sure" ->
                        openSureDialog(requireContext(), requireActivity(), {
                            showProgress(requireActivity())
                            findNavController().navigateUp()
                        }, { a: String -> openDialog = a})
                    "Change" ->
                        openPhotoDialog(requireContext(), requireActivity(), {
                            a: String -> openDialog = a
                        }, {captureImage()}, {getImageFromGallery()}, {removeImage()})
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
                // Validate fields
                if (validateFields()) {
                    // Show progress before uploading user data to the db
                    showProgress(requireActivity())
                    userVM.updateUser(tempUser.copy()).addOnCompleteListener {
                        deletePhoto(requireContext(), tempUser.photo)
                        findNavController().navigate(EditProfileFragmentDirections.actionSaveProfile())
                    }
                    true
                } else false
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun validateFields(): Boolean {
        // Load modified user data
        updateUser()

        var valid = true
        for (field in setOf(profile_edit_email, profile_edit_name, profile_edit_location)) {
            if (field.text.toString() == "") {
                valid = false
                field.error = getString(R.string.message_error_field_required)
                field.requestFocus()
            } else {
                when (field) {
                    profile_edit_email -> {
                        if (!isEmailValid(field.text.toString())) {
                            valid = false
                            field.error = getString(R.string.message_error_field_invalid_email)
                        }
                    }
                    profile_edit_location -> {
                        try {
                            if (getLocationFromAddress(requireContext(), field.text.toString()) == null) {
                                valid = false
                                field.error = getString(R.string.message_error_field_invalid_location)
                                field.requestFocus()
                            }
                        } catch (e: IOException) {
                            Log.d(TAG, "Couldn't retrieve location")
                            displayMessage(requireContext(), "Cannot access geocoder service")
                        }
                    }
                }
            }
        }
        return valid
    }

    private fun captureImage() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                createImageFile(requireContext()).also { file ->
                    val photoUri = FileProvider.getUriForFile(requireContext(), getString(R.string.file_provider), file)
                    tempUser.apply { photo = photoUri.toString() }
                    registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
                        if (taken) {
                            tempUser.apply { photo = compressImage(requireContext(), tempUser.photo).toString() }
                            updateFields()
                            displayMessage(requireContext(), getString(R.string.message_taken_photo))
                        } else {
                            deletePhoto(requireContext(), tempUser.photo)
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
        profile_edit_photo.post {
            Picasso.get().load(Uri.parse(tempUser.photo)).into(profile_edit_photo, object : Callback {
                override fun onSuccess() {
                    profile_edit_photo.translationY = 0F
                    hideProgress(requireActivity())
                }

                override fun onError(e: Exception?) {
                    profile_edit_photo.apply {
                        translationY = measuredHeight / 6F
                        setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile))
                    }
                    hideProgress(requireActivity())
                }
            })
        }
    }

    private val locationListener = object : TextWatcher {

        private lateinit var timer: Timer

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (this::timer.isInitialized) {
                timer.cancel()
            }
        }

        override fun afterTextChanged(s: Editable?) {
            profile_edit_location.dismissDropDown()
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    if (s?.length!! > 3) {
                        val jsonObjectRequest = JsonObjectRequest(
                            Request.Method.GET,
                            "https://photon.komoot.de/api/?q=${s}&limit=5".replace(" ", "%20"),
                            null,
                            Response.Listener { response ->
                                context?.also { context ->
                                    val cities = (Gson().fromJson(response["features"].toString(), object : TypeToken<ArrayList<PlaceInfo?>?>() {}.type) as ArrayList<PlaceInfo>).map { pi ->
                                        "${pi.properties.name} ${pi.properties.city} ${pi.properties.state} ${pi.properties.country}"
                                    }.toTypedArray()
                                    profile_edit_location.setAdapter(ArrayAdapter(context, com.google.android.material.R.layout.support_simple_spinner_dropdown_item, cities))
                                    if (profile_edit_location.isFocused) {
                                        profile_edit_location.showDropDown()
                                    }
                                }
                            },
                            Response.ErrorListener { error ->
                                Log.d(TAG, error.toString())
                            }
                        )
                        Volley.newRequestQueue(context).add(jsonObjectRequest)
                    }
                }
            }, 500)
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_EDIT_PROFILE"
    }
}