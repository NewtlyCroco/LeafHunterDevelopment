package com.example.leafhunterdevelopment.ui.collection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.leafhunterdevelopment.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.functions.functions
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class PlantListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val plantList = mutableListOf<Plant>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_plant_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerViewPlants)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = PlantAdapter(plantList)

        fetchPlants()
    }

    private fun fetchPlants() {
        progressBar.visibility = View.VISIBLE

        val userId = Firebase.auth.currentUser?.uid ?: return
        val url = URL("https://us-central1-leafhunter-daf07.cloudfunctions.net/get_user_data")
        val jsonBody = JSONObject().put("userId", userId)

        Thread {
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
                    val plant = Plant(
                        name = obj.optString("name"),
                        description = obj.optString("description"),
                        imageUrl = obj.optString("imageUrl"),
                        timestamp = obj.optString("timestamp")
                    )
                    newPlants.add(plant)
                }

                requireActivity().runOnUiThread {
                    plantList.clear()
                    plantList.addAll(newPlants)
                    recyclerView.adapter?.notifyDataSetChanged()
                    progressBar.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("PlantListFragment", "Error fetching plants", e)
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }
}




