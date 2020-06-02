package it.polito.mad.madmax.ui

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.database.collection.LLRBNode
import it.polito.mad.madmax.R
import java.util.*

class MapsFragment : DialogFragment(),OnMapReadyCallback {

    private val LOCATION_REQUEST_CODE = 10001;
    private lateinit var googleMap:GoogleMap
    private var location = LatLng(45.0708043,7.674188)

    private var location2 = LatLng(42.0708043,8.674188)
    private lateinit var myLocation: MutableLiveData<LatLng>

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        myLocation = MutableLiveData()

        /*if (arguments?.containsKey("locationArg") == true){
            val locationString = requireArguments().getString("locationArg")
            val address = Geocoder(requireContext(), Locale.getDefault()).getFromLocationName(locationString,1)[0]
            location  = LatLng(address.latitude,address.longitude)
        }*/

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_maps, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return layout
    }

    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        confirm_location_btn.setOnClickListener{
            setFragmentResult("MAP_ADDRESS", bundleOf("address" to getAddressFromLocation()))
            dismiss()
        }
    }
*/
    private fun getAddressFromLocation(): String{
        val geocoder= Geocoder(requireContext(), Locale.getDefault())
        try {
            val address =  geocoder.getFromLocation(location.latitude, location.longitude,1)[0].getAddressLine(0)
            return address
        }catch (e:Exception){
            Toast.makeText(requireContext(),e.message,Toast.LENGTH_LONG).show()
            return "Unavailable"
        }


    }

    private fun drawRoute(){
        val route =  PolylineOptions().add(location,location2)
            .color(Color.GREEN)
            .width(10F)
            .geodesic(true)

        googleMap.addPolyline(route)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0!!

        googleMap.uiSettings.apply {
            this.setAllGesturesEnabled(true)
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true

        }
        if (myLocation.value!=null){
            googleMap.isMyLocationEnabled = true
        }else {
            myLocation.observe(requireActivity(), androidx.lifecycle.Observer {
                googleMap.isMyLocationEnabled = true
            })
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

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            askLocationPermission();
        }

    }

    private fun isLocationPermissionGranted(): Boolean{
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun askLocationPermission() {
        if (!isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST_CODE
                )
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val locationTask: Task<Location> =
            fusedLocationProviderClient.lastLocation
        locationTask.addOnSuccessListener { loc ->
            if (loc != null) {
                //We have a location
                Log.d("XXX", "onSuccess: $loc")
                myLocation.value = LatLng(loc.latitude,loc.longitude)
            } else {
                Log.d("XXX", "onSuccess: Location was null...")
            }
        }
        locationTask.addOnFailureListener { e -> Log.e("XXX", "onFailure: " + e.localizedMessage) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getLastLocation();
            } else {
                //Permission not granted
            }
        }
    }


}