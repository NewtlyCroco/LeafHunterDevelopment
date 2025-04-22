package com.example.leafhunterdevelopment.utils

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.util.*

class FirebaseHelper(useEmulator: Boolean = false) {

    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")

    init {
        if(useEmulator) {
            functions.useEmulator("10.0.2.2", 5001)
        }
    }

    fun uploadBitmapToFirebase(imageData: Bitmap, onSuccess: (Uri) -> Unit, onFailure: (Exception) -> Unit) {
        val photoRef = storageRef.child("photos/${UUID.randomUUID()}.png")

        val stream = ByteArrayOutputStream()
        imageData.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val imageData = stream.toByteArray()

        photoRef.putBytes(imageData)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "Image uploaded to: $uri")
                    onSuccess(uri)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed: ${e.message}")
                onFailure(e)
            }
    }

    fun callFirebaseFunction(functionName: String, data: Map<String, Any>, onSuccess: (Any?) -> Unit, onFailure: (Exception) -> Unit) {
        Log.d(TAG, "Calling function: $functionName with data: $data")
        Log.d(TAG, "Current user: ${getCurrentUserId()}")

        functions
              .getHttpsCallable(functionName)
              .call(data)
              .addOnSuccessListener { result ->
                  Log.d(TAG, "Function call succeeded: ${result.data}")
                  onSuccess(result.data)
              }
              .addOnFailureListener { e ->
                  Log.e(TAG, "Function call failed for ${functionName}: ${e.message}")
                  onFailure(e)
              }
    }

    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    companion object {
        private const val TAG = "FirebaseHelper"
    }
}