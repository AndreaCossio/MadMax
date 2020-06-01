package it.polito.mad.madmax

import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_maps.*
import java.util.*

class MapsFragment : DialogFragment(),OnMapReadyCallback {

    private lateinit var googleMap:GoogleMap
    private lateinit var markerOptions: MarkerOptions
    //private var location: Location? = null
    private var location = LatLng(45.0708043,7.674188)
    //TODO get real location

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_maps, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)
        /*dialog?.window?.apply {
            setLayout(getFragmentSpaceSize(requireContext()).x, getFragmentSpaceSize(requireContext()).y)
            setBackgroundDrawableResource(android.R.color.transparent)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }*/
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        confirm_location_btn.setOnClickListener{
            setFragmentResult("MAP_ADDRESS", bundleOf("address" to getAddressFromLocation()))
            dismiss()
        }
    }

    private fun getAddressFromLocation(): String{
        val geocoder= Geocoder(requireContext(), Locale.getDefault())
        val address =  geocoder.getFromLocation(location.latitude, location.longitude,1)[0].getAddressLine(0)
        return address
    }

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0!!
        googleMap.uiSettings.apply {
            this.setAllGesturesEnabled(true)
        }
        googleMap.setOnMapClickListener {
            clickOnMap(it)
        }
        addMarker()
        animateCamera()
    }

    private fun addMarker(){
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(location).title(getAddressFromLocation())).showInfoWindow()
    }
    private fun animateCamera(){
        val cameraUpdateFactory =CameraUpdateFactory.newLatLngZoom(location, 15.0F)
        googleMap.animateCamera(cameraUpdateFactory)
    }

    private fun clickOnMap(latLng: LatLng){
        location = latLng
        addMarker()
        animateCamera()
    }


    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        // register the permissions callback. This can be done as a class val, or a
// lateinit var in onAttach() or onCreate()
        val requestPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            // the result from RequestMultiplePermissions is a map linking each
            // request permission to a boolean of whether it is GRANTED

            // check if the permission is granted
            if (result[ACCESS_COARSE_LOCATION]!!) {
                // it was granted
            } else {
                // it was not granted

            }
        }
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                    location = it
                }
            }
            shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION) ->{

            }

            else -> {
                // We can request the permission by launching the ActivityResultLauncher
                requestPermissions.launch(arrayOf(ACCESS_COARSE_LOCATION))
                // The registered ActivityResultCallback gets the result of the request.
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {

        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener {
                if(it.isSuccessful && it.result != null){
                    location = LatLng(it.result!!.latitude,it.result!!.longitude)
                }else{
                }
            }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(requireContext(),ACCESS_COARSE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(requireActivity(),
            arrayOf(ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),ACCESS_COARSE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {


            Toast.makeText(requireContext(),"Require permission",Toast.LENGTH_LONG).show()
        } else {
            Log.i(TAG, "Requesting permission")
            startLocationPermissionRequest()
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation()
            } else {
                displayMessage(requireContext(),"Unable to get location!")
            }
        }
    }

    companion object {

        private val TAG = "LocationProvider"

        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
*/

}