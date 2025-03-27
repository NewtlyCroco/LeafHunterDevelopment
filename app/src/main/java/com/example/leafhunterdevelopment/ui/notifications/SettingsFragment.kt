package com.example.leafhunterdevelopment.ui.notifications

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.example.leafhunterdevelopment.R
import com.example.leafhunterdevelopment.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.widget.Toast

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(context, R.animator.fade_in)
        val bounce = AnimationUtils.loadAnimation(context, R.animator.bounce)

        // Apply animations to elements
        binding.userBannerCard.startAnimation(fadeIn)
        binding.signOutButton.startAnimation(fadeIn)
        binding.clearCacheButton.startAnimation(fadeIn)

        // Set current user data
        val currentUser = auth.currentUser
        currentUser?.let {
            binding.userName.text = it.displayName ?: "Leaf Hunter"
            binding.userEmail.text = it.email ?: "No email available"
        }

        // Sign Out Button
        binding.signOutButton.setOnClickListener {
            it.startAnimation(bounce)
            auth.signOut()
            // Navigate to login screen or handle sign out
        }

        // Clear Cache Button
        binding.clearCacheButton.setOnClickListener {
            it.startAnimation(bounce)
            clearCache()
        }
    }

    private fun clearCache() {
        try {
            val context = requireContext()
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.isDirectory) {
                cacheDir.deleteRecursively()
                // Show success message
                showToast("Cache cleared successfully")
            }
        } catch (e: Exception) {
            showToast("Failed to clear cache")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}