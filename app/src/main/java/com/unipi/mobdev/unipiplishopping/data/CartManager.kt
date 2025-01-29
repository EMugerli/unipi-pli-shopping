package com.unipi.mobdev.unipiplishopping.data

object CartManager {
    private val _cartItems = mutableListOf<String>()
    val cartItems: List<String> get() = _cartItems.toList()

    fun addToCart(productId: String) {
        _cartItems.add(productId)
    }

    fun removeFromCart(productId: String) {
        _cartItems.remove(productId)
    }

    fun clearCart() {
        _cartItems.clear()
    }
}