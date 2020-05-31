package it.polito.mad.madmax

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_maps.*

class MapsFragment : Fragment(),OnMapReadyCallback {

    private lateinit var googleMap:GoogleMap
    private lateinit var markerOptions: MarkerOptions


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


    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0!!
        googleMap.uiSettings.apply {
            this.setAllGesturesEnabled(true)
        }
        googleMap.setOnMapClickListener {
            clickOnMap(it)
        }

    }


    private fun clickOnMap(latLng: LatLng){
        markerOptions = MarkerOptions().position(latLng).draggable(true)
        googleMap.addMarker(markerOptions)
    }
}