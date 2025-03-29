package com.example.leafhunterdevelopment.ui.dashboard

import android.Manifest
import android.graphics.Bitmap
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.leafhunterdevelopment.databinding.FragmentCameraBinding
import com.example.leafhunterdevelopment.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.os.Looper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!


    private lateinit var cameraViewModel: CameraViewModel

    private lateinit var cameraButtonCard: MaterialCardView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var storageRef: StorageReference

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cameraViewModel = ViewModelProvider(this).get(CameraViewModel::class.java)

        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize Firebase Storage
        storageRef = FirebaseStorage.getInstance().reference

        // Set up camera button with bounce animation
        cameraButtonCard = binding.cameraButtonCard
        cameraButtonCard.setOnClickListener {
            it.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    takePhoto()
                }
                .start()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request permissions
        requestCameraPermission()
    }

    private fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview: Preview = Preview.Builder().build()
                preview.surfaceProvider = binding.previewView.surfaceProvider

                // Only setup ImageCapture (no Preview needed)
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Bind only ImageCapture use case
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview
                )
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    imageCapture
                )
            } catch(exc: Exception) {
                Log.e(TAG, "Camera initialization failed", exc)
                Toast.makeText(context, "Failed to initialize camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}")
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Photo capture failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.d(TAG, "Image captured successfully")
                    Log.d(TAG, "  Size ${image.width}x${image.height} format ${image.format}")
                    Log.d(TAG, "  Info ${image.imageInfo}")

                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
                        uploadBitmapToFirebase(image)
                    }
                }
            }
        )
    }

    private fun uploadBitmapToFirebase(image: ImageProxy) {
        val photoRef = storageRef.child("photos/${UUID.randomUUID()}.png")

        val stream = ByteArrayOutputStream()
        image.toBitmap().compress(Bitmap.CompressFormat.PNG, 90, stream)

        photoRef.putBytes(stream.toByteArray())
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "Image uploaded to: $uri")
                    // TODO: Save the download URL to Firestore if needed
                    // TODO: change to prod version of the app
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed: ${e.message}")
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun requestCameraPermission() {
        val hasCameraPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            // Camera permission is granted
            initializeCamera()
        } else {
            // Check if the user denied the permission previously
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                // Show rationale and re-request permission
                Toast.makeText(context, "Camera permission is required to use this feature.", Toast.LENGTH_LONG).show()
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission is permanently denied, guide user to app settings
                Toast.makeText(context, "Please enable camera permission in settings.", Toast.LENGTH_LONG).show()
                openAppSettings()
            }
        }
    }

    private fun requestLocationPermission() {
        // Check what permissions are granted
        val hasCoarseLocation = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    
        if (hasFineLocation || hasCoarseLocation) {
            // Either fine or coarse location permission is granted
            fetchCurrentLocation()
        } else {
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show rationale and re-request permission
                Toast.makeText(context, "Location permission is required to use this feature.", Toast.LENGTH_LONG).show()
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission is permanently denied, guide user to app settings
                Toast.makeText(context, "Please enable location permission in settings.", Toast.LENGTH_LONG).show()
                openAppSettings()
            }
        }
    }



    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, // Use balanced accuracy for coarse location
            1000L
        ).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this) // Stop updates after getting location
                    val location = locationResult.lastLocation
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.d("CameraFragment", "Lat: $latitude, Lon: $longitude")
                        Toast.makeText(context, "Lat: $latitude, Lon: $longitude", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.navigationHome)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                val cameraGranted = handleCameraPermissions(grantResults)
                if (cameraGranted) {
                    // request location
                    requestLocationPermission()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                val locationGranted = handleLocationPermissions(grantResults)

            }
        }
    }

    private fun handleLocationPermissions(grantResults: IntArray) {

        val hasFineLocation = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED

        when {
            hasFineLocation || hasCoarseLocation -> {
                // Either fine or coarse location permission is granted
                fetchCurrentLocation()
            }
            else -> {
                // No location permission at all
                Toast.makeText(context, "Location permission denied. Cannot proceed!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.navigationHome)
            }
        }
    }

    private fun handleCameraPermissions(grantResults: IntArray): Boolean {
        val isCameraPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (isCameraPermissionGranted) {
            initializeCamera()
            return true
        } else {
            Toast.makeText(context, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.navigationHome)
            return false
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        _binding = null
    }

    companion object {
        private const val TAG = "CameraFragment"
        private val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val CAMERA_PERMISSION_REQUEST_CODE = 1002
    }
}