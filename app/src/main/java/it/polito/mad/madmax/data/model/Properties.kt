package it.polito.mad.madmax.data.model

import com.google.gson.annotations.SerializedName

data class Properties (
	@SerializedName("osm_id") val osm_id : Long,
	@SerializedName("osm_type") val osm_type : String,
	@SerializedName("extent") val extent : List<Double>,
	@SerializedName("country") val country : String,
	@SerializedName("osm_key") val osm_osm_key : String,
	@SerializedName("osm_value") val osm_value : String,
	@SerializedName("city") val city: String?,
	@SerializedName("name") val name : String,
	@SerializedName("state") val state : String
)