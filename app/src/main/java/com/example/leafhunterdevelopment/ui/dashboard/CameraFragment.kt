package com.example.leafhunterdevelopment.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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