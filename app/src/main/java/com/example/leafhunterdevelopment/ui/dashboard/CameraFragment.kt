package com.example.leafhunterdevelopment.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import android.net.Uri
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

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
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
      requestLocationPermission()

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

        // Request camera permissions
        if (allPermissionsGranted()) {
            initializeCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
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
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed: ${e.message}")
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    //need to find a better way to do this method
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeCamera()
            } else {
                Toast.makeText(context, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationPermission() {
        val hasCoarseLocation = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    
        if (hasFineLocation) {
            // Fine location permission is granted
            fetchCurrentLocation()
        } else if (hasCoarseLocation) {
            // Only coarse location permission is granted
            Toast.makeText(
                context,
                "Fine location is required for accurate location. Please enable it in settings.",
                Toast.LENGTH_LONG
            ).show()
            openAppSettings()
        } else {
            // Request both fine and coarse location permissions
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }



    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
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
                        // Optionally navigate back or cancel the flow
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Re-check coarse/fine permission
            val hasCoarseLocation = ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasFineLocation = ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            when {
                hasFineLocation -> {
                    // Permission granted for fine location
                    fetchCurrentLocation()
                }
                hasCoarseLocation -> {
                    // Only approximate location permission granted
                    Toast.makeText(
                        context,
                        "We need your fine location. Please enable it in Settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    openAppSettings()
                }
                else -> {
                    // No location permission at all
                    Toast.makeText(context, "Location permission denied. Cannot proceed!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.navigationHome)
                }
            }
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
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }
}