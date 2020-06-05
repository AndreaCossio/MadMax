package it.polito.mad.madmax.data.model

import com.google.gson.annotations.SerializedName

data class PlaceInfo (
	@SerializedName("geometry") val geometry : Geometry,
	@SerializedName("type") val type : String,
	@SerializedName("properties") val properties : Properties
)