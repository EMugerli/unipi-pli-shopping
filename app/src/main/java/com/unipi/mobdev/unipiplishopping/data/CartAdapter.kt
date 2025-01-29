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
    ListAdapter<Pair<String, Int>, CartAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (productId, quantity) = getItem(position)
        holder.bind(productId, quantity)
    }

    inner class ViewHolder(private val binding: CartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(productId: String, quantity: Int) {
            // Fetch product details from Firestore
            FirebaseFirestore.getInstance().collection("products").document(productId)
                .get()
                .addOnSuccessListener { document ->
                    //document.toObject(Product::class.java)?.let { product ->
                    val product = Product(
                        document.id,
                        document["name"] as String,
                        document.getDouble("price") as Double,
                        document["description"] as String,
                        document["release_date"] as Timestamp,
                        document["store_location"] as GeoPoint,
                        (document["store_id"] as Long).toInt()
                    )

                    product?.let {
                        binding.productTitle.text = product.name
                        binding.productQuantity.text = "x${quantity}"
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

    class DiffCallback : DiffUtil.ItemCallback<Pair<String, Int>>() {
        override fun areItemsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>) =
            oldItem.first == newItem.first

        override fun areContentsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>) =
            oldItem == newItem
    }
}