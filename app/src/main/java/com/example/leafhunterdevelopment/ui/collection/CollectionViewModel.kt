package com.example.leafhunterdevelopment.ui.collection

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class CollectionViewModel : ViewModel() {

    private val _plants = MutableLiveData<List<Plant>>()
    val plants: LiveData<List<Plant>> = _plants

    init {
        fetchPlantsFromCloudRun()
    }

    private fun fetchPlantsFromCloudRun() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val url = URL("https://get-user-data-32w73mgs3q-uc.a.run.app")
        val jsonBody = JSONObject().put("userId", userId)

        Executors.newSingleThreadExecutor().execute {
            try {
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.bufferedWriter().use { it.write(jsonBody.toString()) }

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val plantsJson = json.getJSONObject("data").getJSONArray("plants")

                val newPlants = mutableListOf<Plant>()
                for (i in 0 until plantsJson.length()) {
                    val obj = plantsJson.getJSONObject(i)
                    val plantData = obj.optJSONObject("plantData")

                    val plant = Plant(
                        commonName = plantData?.optString("commonName") ?: "",
                        genusName = plantData?.optString("genusName") ?: "",
                        familyName = plantData?.optString("familyName") ?: "",
                        imageUrl = obj.optString("imageUrl"),
                        timestamp = obj.optString("timestamp"),
                        lat = obj.optDouble("lat", 0.0),
                        lon = obj.optDouble("lon", 0.0)
                    )
                    newPlants.add(plant)
                }

                _plants.postValue(newPlants)

            } catch (e: Exception) {
                Log.e("CollectionViewModel", "Failed to fetch from Cloud Run", e)
                _plants.postValue(emptyList())
            }
        }
    }
}
