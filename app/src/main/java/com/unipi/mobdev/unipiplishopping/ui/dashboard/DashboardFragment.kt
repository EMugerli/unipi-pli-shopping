package com.unipi.mobdev.unipiplishopping.ui.dashboard

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.unipi.mobdev.unipiplishopping.R
import com.unipi.mobdev.unipiplishopping.data.ProductsAdapter
import com.unipi.mobdev.unipiplishopping.data.model.Product
import com.unipi.mobdev.unipiplishopping.databinding.FragmentDashboardBinding
import com.unipi.mobdev.unipiplishopping.ui.product.ProductDetailActivity
import java.util.jar.Manifest

/**
 * A simple [Fragment] subclass.
 * It displays a list of products in a RecyclerView and a map with markers for each product.
 * It also shows the user's location on the map.
 * It uses the Firestore database to fetch the products.
 * It uses the FusedLocationProviderClient to get the user's location.
 * It uses the Google Maps API to display the map.
 */
class DashboardFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var products = mutableListOf<Product>()
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap -> onMapReady(googleMap) }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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

    fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        checkLocationPermission()
        //loadProductsOnMap()
    }

    private fun loadProductsOnMap() {
        products.forEach { product ->
            val productLatLng = LatLng(product.storeLocation.latitude, product.storeLocation.longitude)
            Log.d("DashboardFragment", "Adding marker for product: ${product.name}, at: $productLatLng")
            addProductMarker(productLatLng, product)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showUserLocation()
        } else {
            /*requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )*/
        }
    }

    /**
     * Shows the user's location on the map.
     * It uses the FusedLocationProviderClient to get the last known location.
     * If the location is available, it adds a marker on the map.
     */
    private fun showUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(userLatLng)
                        .title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
            }
        }
    }

    /**
     * Adds a marker on the map for the given product.
     */
    private fun addProductMarker(latLng: LatLng, product: Product) {
        googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(product.name)
                .snippet("Price: $${product.price}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    /**
     * Fetches the products from the Firestore database.
     * It adds the products to the products list and sets up the RecyclerView.
     * It also calls loadProductsOnMap() to add markers on the map for each product.
     */
    private fun loadProducts() {
        try {

            db.collection("products")
                .get()
                .addOnSuccessListener { result ->
                    //products = result.toObjects(Product::class.java)
                    products.clear()
                    for (document in result.documents) {
                        Log.d("DashboardFragment", "Document: ${document.id} => ${document.data}")

                        val product = Product(
                            document.id,
                            document["name"] as String,
                            document.getDouble("price") as Double,
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

                    loadProductsOnMap()

                }
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error fetching products", e)
        }
    }

    /**
     * Handles the result of the location permission request.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}