package it.polito.mad.madmax.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import it.polito.mad.madmax.R
import it.polito.mad.madmax.displayMessage
import it.polito.mad.madmax.getAddressFromLocation
import it.polito.mad.madmax.getLocationFromAddress
import kotlinx.android.synthetic.main.map_dialog.*
import java.io.IOException

class MapDialog : DialogFragment(), OnMapReadyCallback {

    private var isEditMode: Boolean = true
    private lateinit var location: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Parse arguments
        location = requireArguments().getString("location")!!
        isEditMode = requireArguments().getBoolean("editMode")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_dialog, container, false).also {
            (childFragmentManager.findFragmentById(R.id.map_dialog_fragment_container) as SupportMapFragment).getMapAsync(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isEditMode) {
            map_dialog_button.setOnClickListener {
                setFragmentResult("MAP_DIALOG_REQUEST", bundleOf("address" to location))
                dismiss()
            }
        } else {
            map_dialog_button.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isEditMode) {
            map_dialog_button.setOnClickListener(null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(getString(R.string.map_dialog_state), location)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getString(getString(R.string.map_dialog_state), location)?.also {
            location = it
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap?) {
        p0?.apply {
            uiSettings.apply {
                setAllGesturesEnabled(true)
                isZoomControlsEnabled = true
            }
        }?.also { map ->
            // As soon as the map is ready, request to access current location
            // If granted, show the current location on the map
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation.addOnSuccessListener { myLoc ->
                        map.isMyLocationEnabled = true
                        //map.isMyLocationButtonEnabled = true
                        if (!isEditMode) {
                            try {
                                getLocationFromAddress(requireContext(), location)?.also { loc ->
                                    map.addPolyline(PolylineOptions().apply {
                                        add(LatLng(myLoc.latitude, myLoc.longitude), loc)
                                        color(Color.GREEN)
                                        width(10F)
                                        geodesic(false)
                                    })
                                }
                            } catch (e: IOException) {
                                Log.d(TAG, "Couldn't retrieve location")
                                displayMessage(requireContext(), "Cannot access geocoder service")
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.d(TAG, "Failed to get fused location.", e)
                    }
                } else {
                    Log.d(TAG, "User negated LOCATION permission.")
                }
            }.also { permission -> permission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

            // In any case, add a marker with the current chosen location
            if (location != "") {
                map.apply {
                    clear()
                    try {
                        getLocationFromAddress(requireContext(), location)?.also { loc ->
                            addMarker(MarkerOptions().position(loc).title(location)).showInfoWindow()
                            animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15F))
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "Couldn't retrieve location")
                        displayMessage(
                            requireContext(),
                            "Cannot access geocoder service"
                        )
                    }
                }
            } else {
                map.clear()
            }

            // On click, change current selected location
            map.setOnMapClickListener { newLoc ->
                try {
                    location = getAddressFromLocation(requireContext(), newLoc)
                    map.apply {
                        clear()
                        addMarker(MarkerOptions().position(newLoc).title(location)).showInfoWindow()
                        animateCamera(CameraUpdateFactory.newLatLngZoom(newLoc, 15F))
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "Couldn't retrieve address")
                    displayMessage(requireContext(), "Cannot access geocoder service")
                }
            }
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_MAP_DIALOG"
    }
}