package com.unipi.mobdev.unipiplishopping.data

/**
 * A simple class that manages the cart items.
 * It provides methods to add, remove and clear items from the cart.
 * It also provides a read-only property to access the cart items.
 */
object CartManager {
    private val _cartItems = mutableMapOf<String, Int>()
    val cartItems: Map<String, Int> get() = _cartItems

    fun addToCart(productId: String) {
        _cartItems[productId] = _cartItems[productId]?.plus(1) ?: 1
    }

    fun removeFromCart(productId: String) {
        val currentQuantity = _cartItems[productId] ?: 0
        if (currentQuantity > 1) {
            _cartItems[productId] = currentQuantity - 1
        } else {
            _cartItems.remove(productId)
        }
    }

    fun clearCart() {
        _cartItems.clear()
    }
}