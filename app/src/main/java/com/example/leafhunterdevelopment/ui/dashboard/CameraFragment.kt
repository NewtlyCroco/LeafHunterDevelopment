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
import com.example.leafhunterdevelopment.utils.FirebaseHelper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.card.MaterialCardView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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

    private lateinit var firebaseHelper: FirebaseHelper

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

        firebaseHelper = FirebaseHelper(useEmulator = true)

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

                    // DEBUGGING PURPORSES TODO: REMOVE
                    // val bitmap = BitmapFactory.decodeResource(resources, R.drawable.variegata)
                    // image.close()

                    // Convert ImageProxy to Bitmap
                    val bitmap = image.toBitmap()

                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
                        // TODO: some sort of yes/no choice
                        firebaseHelper.uploadBitmapToFirebase(
                            bitmap,
                            onSuccess = { uri ->
                                Log.d(TAG, "Before")
                                storePlantToFirebase(uri.toString())
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Upload failed: ${e.message}")
                                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        )
    }

    private fun storePlantToFirebase(uri: String) {
        val data = mapOf("imageUrl" to uri.toString())
        firebaseHelper.callFirebaseFunction(
            functionName = LOCAL_GET_PLANT,
            data = data,
            onSuccess = { result -> // Plant recognition succeeded
                // Fetch the user's current location
                fetchImmediateLocation(
                    onLocationFetched = { latitude, longitude ->
                        // Combine plant recognition result with location data
                        val plantData = mapOf(
                            "imageUrl" to uri.toString(),
                            "userId" to (firebaseHelper.getCurrentUserId() ?: ""),
                            "lat" to latitude,
                            "lon" to longitude,
                            "plantData" to (result ?: "unknown_result")
                        )

                        // Send the combined data to the database
                        firebaseHelper.callFirebaseFunction(
                            functionName = LOCAL_STORE_PLANT,
                            data = plantData,
                            onSuccess = {
                                Toast.makeText(context, "Plant data stored successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Failed to store plant data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onFailure = {
                        Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.navigationHome)
                    }
                )
            },
            onFailure = { e ->
                if(e.message == "NOT_FOUND") {
                    Log.d(TAG, "NOT_RECOGNIZED")
                    Toast.makeText(context, "The plant was not recognized, try again.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Function call failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun fetchImmediateLocation(
        onLocationFetched: (Double, Double) -> Unit,
        onFailure: () -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(listener: OnTokenCanceledListener): CancellationToken = this
                override fun isCancellationRequested(): Boolean = false
            }
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationFetched(location.latitude, location.longitude)
            } else {
                onFailure()
            }
        }.addOnFailureListener {
            onFailure()
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

    // TODO: is location requesting needed here?
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
            fetchImmediateLocation(
                onLocationFetched = { latitude, longitude ->
                    // TODO: send to firebase
                    Log.d(TAG, "Lat: $latitude, Lon: $longitude")
                },
                onFailure = {
                    Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.navigationHome)
                }
            )
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
                fetchImmediateLocation(
                    onLocationFetched = { latitude, longitude ->
                        // TODO: send to firebase
                        Log.d(TAG, "Lat: $latitude, Lon: $longitude")
                        Toast.makeText(context, "Lat: $latitude, Lon: $longitude", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.navigationHome)
                    }
                )
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
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        private const val LOCAL_GET_PLANT = "get_plant_families"
        private const val LOCAL_STORE_PLANT = "store_plant_data"
    }
}
