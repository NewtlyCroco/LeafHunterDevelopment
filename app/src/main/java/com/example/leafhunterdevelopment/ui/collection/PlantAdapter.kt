package com.example.leafhunterdevelopment.ui.collection
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.leafhunterdevelopment.R

data class Plant(
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: String = ""
)

class PlantAdapter(private val plants: List<Plant>) :
    RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.plantName)
        val descText: TextView = itemView.findViewById(R.id.plantDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.nameText.text = plant.name
        holder.descText.text = plant.description
    }

    override fun getItemCount() = plants.size
}
