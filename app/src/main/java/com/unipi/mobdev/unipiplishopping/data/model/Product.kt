package com.unipi.mobdev.unipiplishopping.data.model

import com.google.firebase.firestore.GeoPoint

data class Product(
    var code: String,
    val name: String,
    val price: Double,
    val description: String,
    val releaseDate: com.google.firebase.Timestamp,
    val storeLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val storeId: Int
)
