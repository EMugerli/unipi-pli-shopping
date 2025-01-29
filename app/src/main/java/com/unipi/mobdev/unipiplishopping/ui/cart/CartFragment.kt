package com.unipi.mobdev.unipiplishopping.ui.cart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unipi.mobdev.unipiplishopping.data.CartAdapter
import com.unipi.mobdev.unipiplishopping.data.CartManager
import com.unipi.mobdev.unipiplishopping.data.model.Receipt
import com.unipi.mobdev.unipiplishopping.databinding.FragmentCartBinding
import java.util.UUID

class CartFragment : Fragment() {
    private lateinit var adapter: CartAdapter

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setupRecyclerView()
        setupCheckoutButton()
        return root
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter { removedProductId ->
            CartManager.removeFromCart(removedProductId)
            // Refresh the list after removal
            adapter.submitList(CartManager.cartItems.toList())

            // Check number of items in the cart
            binding.checkoutButton.isEnabled = CartManager.cartItems.isNotEmpty()
        }
        binding.cartRecyclerView.adapter = adapter
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter.submitList(CartManager.cartItems)
    }

    private fun setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener {
            createOrder()
        }
    }

    private fun createOrder() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val receipt = Receipt(
            id = UUID.randomUUID().toString(),
            customerId = currentUser?.uid ?: "",
            customerName = currentUser?.displayName ?: "",
            products = CartManager.cartItems,
            createdAt = Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("receipts")
            .add(receipt)
            .addOnSuccessListener {
                CartManager.clearCart()
                adapter.submitList(emptyList())
                Toast.makeText(requireContext(), "Order placed successfully", Toast.LENGTH_SHORT).show()
            }
    }

    // Enable order button only if there are items in the cart
    override fun onResume() {
        super.onResume()
        binding.checkoutButton.isEnabled = CartManager.cartItems.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}