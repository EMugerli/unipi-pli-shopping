package com.unipi.mobdev.unipiplishopping.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unipi.mobdev.unipiplishopping.BaseActivity
import com.unipi.mobdev.unipiplishopping.R
import com.unipi.mobdev.unipiplishopping.data.LanguageHelper
import com.unipi.mobdev.unipiplishopping.data.LoginDataSource
import com.unipi.mobdev.unipiplishopping.data.LoginRepository
import com.unipi.mobdev.unipiplishopping.ui.login.LoginActivity
import java.util.jar.Manifest

class SettingsFragment : PreferenceFragmentCompat() {

    private val db = FirebaseFirestore.getInstance()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permissions granted, start service
            (requireActivity() as BaseActivity).startLocationService()
        } else {
            // Disable switch if permissions denied
            findPreference<SwitchPreferenceCompat>("location_notifications")?.isChecked = false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        setupUserDetails()
        findPreference<SwitchPreferenceCompat>("night_mode")?.setOnPreferenceChangeListener { _, _ ->
            (requireActivity() as BaseActivity).updateTheme()
            true
        }

        findPreference<ListPreference>("language_preference")?.setOnPreferenceChangeListener { _, newValue ->
            val newLanguage = newValue as String
            LanguageHelper.setLocale(requireContext(), newLanguage)
            requireActivity().recreate()
            true
        }

        findPreference<SwitchPreferenceCompat>("location_notifications")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    checkLocationPermissions()
                } else {
                    (requireActivity() as BaseActivity).stopLocationService()
                }
                true
            }
        }

        findPreference<ListPreference>("text_size")?.setOnPreferenceChangeListener { _, newValue ->
            (requireActivity() as BaseActivity).apply {
                updateTextSize()
                recreate()
            }
            true
        }

        findPreference<Preference>("logout")?.setOnPreferenceClickListener {
            logoutUser()
            true
        }
    }

    private fun setupUserDetails() {
        refreshUserDetails()
    }

    private fun refreshUserDetails() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        db.collection("users").document(currentUser?.uid ?: "")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.data
                    findPreference<Preference>("user_name")?.summary = user?.get("name") as String? ?: "Not set"
                    findPreference<Preference>("user_email")?.summary = currentUser?.email ?: "Not set"
                    findPreference<Preference>("user_surname")?.summary = user?.get("surname") as String? ?: "Not set"
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun checkLocationPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (requiredPermissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
            (requireActivity() as BaseActivity).startLocationService()
        } else {
            locationPermissionRequest.launch(requiredPermissions)
        }
    }

    private fun logoutUser() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                val loginRepository = LoginRepository(LoginDataSource())
                loginRepository.logout()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshUserDetails()
    }
}