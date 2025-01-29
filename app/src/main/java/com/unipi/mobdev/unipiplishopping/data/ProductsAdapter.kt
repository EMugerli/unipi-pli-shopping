package com.unipi.mobdev.unipiplishopping.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.unipi.mobdev.unipiplishopping.R
import com.unipi.mobdev.unipiplishopping.data.model.Product

class ProductsAdapter(
    private val products: List<Product>,
    private val listener: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productTitle: TextView = view.findViewById(R.id.item_product_title)
        val productPrice: TextView = view.findViewById(R.id.item_product_price)
        val productDescription: TextView = view.findViewById(R.id.item_product_description)

        fun bind(product: Product) {
            productTitle.text = product.name
            productPrice.text = "€${product.price}"
            productDescription.text = product.description
            itemView.setOnClickListener() {
                listener(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.code == newItem.code
        override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
    }
}