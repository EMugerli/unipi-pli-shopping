package com.unipi.mobdev.unipiplishopping.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.unipi.mobdev.unipiplishopping.data.ProductsAdapter
import com.unipi.mobdev.unipiplishopping.data.model.Product
import com.unipi.mobdev.unipiplishopping.databinding.FragmentDashboardBinding
import com.unipi.mobdev.unipiplishopping.ui.product.ProductDetailActivity

class DashboardFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var products = mutableListOf<Product>()

    private var _binding: FragmentDashboardBinding? = null
    private val db = Firebase.firestore

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.productsRecyclerView

        loadProducts()

        return root
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ProductsAdapter(products) { product ->
            val intent = Intent(context, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_CODE", product.code)
            }
            startActivity(intent)
        }
    }

    private fun loadProducts() {
        try {

            db.collection("products")
                .get()
                .addOnSuccessListener { result ->
                    //products = result.toObjects(Product::class.java)
                    products.clear()
                    for (document in result.documents) {
                        val product = Product(
                            document.id,
                            document["name"] as String,
                            document["price"] as Double,
                            document["description"] as String,
                            document["release_date"] as Timestamp,
                            document["store_location"] as GeoPoint,
                            (document["store_id"] as Long).toInt()
                        )

                        products.add(product)
                        Log.d("DashboardFragment", "Fetched product: ${product.name}")
                    }
                    setupRecyclerView()
                    recyclerView.adapter?.notifyDataSetChanged()

                }
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error fetching products", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadProductsSampleData() {
        products.addAll(
            listOf(
                Product("1", "Product A", 10.0, "Description A", Timestamp.now(), GeoPoint(0.0, 0.0), 1),
                Product("2", "Product B", 15.0, "Description B", Timestamp.now(), GeoPoint(0.0, 0.0), 2),
            )
        )
        recyclerView.adapter?.notifyDataSetChanged()
    }
}