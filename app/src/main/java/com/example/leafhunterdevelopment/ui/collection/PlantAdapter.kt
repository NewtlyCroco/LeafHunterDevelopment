package com.example.leafhunterdevelopment.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.leafhunterdevelopment.R

data class Plant( //class to hold our individual plant objects, and their data
    val commonName: String = "",
    val genusName: String = "",
    val familyName: String = "",
    val imageUrl: String = "",
    val timestamp: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

class PlantAdapter(private val plants: List<Plant>) :
    RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {//what is actually displayed in our collection to our user, could add different fields for more information
        val nameText: TextView = itemView.findViewById(R.id.plantName)
        val descText: TextView = itemView.findViewById(R.id.plantDescription)
        val imageView: ImageView = itemView.findViewById(R.id.plantImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)//holds and places our whole layout
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]//simply using the commonname and genus as the display text
        holder.nameText.text = plant.commonName
        holder.descText.text = "${plant.genusName} (${plant.familyName})"

        Glide.with(holder.itemView.context)//allows the placement of the image with text to make everything fit
            .load(plant.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imageView)
    }

    override fun getItemCount() = plants.size//size of our whole plant collection gathered from the length of the planet list from users
}
