package com.unipi.mobdev.unipiplishopping.ui.product

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.unipi.mobdev.unipiplishopping.R
import com.unipi.mobdev.unipiplishopping.data.CartManager
import com.unipi.mobdev.unipiplishopping.data.model.Product
import com.unipi.mobdev.unipiplishopping.databinding.ActivityProductDetailBinding

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId = intent.getStringExtra("PRODUCT_CODE") ?: return
        loadProductDetails(productId)

        binding.addToCartButton.setOnClickListener {
            CartManager.addToCart(productId)
            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadProductDetails(productId: String) {
        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                //val product = document.toObject(Product::class.java)
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
                    binding.productNameTextView.text = it.name
                    binding.productDescriptionTextView.text = it.description
                    binding.productPriceTextView.text = "$${it.price}"
                }
            }
    }
}