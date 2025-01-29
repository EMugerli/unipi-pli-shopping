package com.unipi.mobdev.unipiplishopping.data.model

import com.google.firebase.Timestamp

data class Receipt(
    var id: String,
    val customerId: String,
    val customerName: String,
    val products: List<String> = emptyList(),
    val createdAt: Timestamp
)
