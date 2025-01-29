package com.unipi.mobdev.unipiplishopping.data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.unipi.mobdev.unipiplishopping.data.model.Product
import com.unipi.mobdev.unipiplishopping.databinding.CartItemBinding

class CartAdapter(private val onRemoveClick: (String) -> Unit) :
    ListAdapter<String, CartAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val productId = getItem(position)
        holder.bind(productId)
    }

    inner class ViewHolder(private val binding: CartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(productId: String) {
            // Fetch product details from Firestore
            FirebaseFirestore.getInstance().collection("products").document(productId)
                .get()
                .addOnSuccessListener { document ->
                    //document.toObject(Product::class.java)?.let { product ->
                    val product = Product(
                        document.id,
                        document["name"] as String,
                        document["price"] as Double,
                        document["description"] as String,
                        document["release_date"] as Timestamp,
                        document["store_location"] as GeoPoint,
                        (document["store_id"] as Long).toInt()
                    )

                    product?.let {
                        binding.productTitle.text = product.name
                        binding.productPrice.text = "$${product.price}"
                    }
                }
                .addOnFailureListener {
                    binding.productTitle.text = "Product unavailable"
                    binding.productPrice.text = ""
                }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(productId)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}