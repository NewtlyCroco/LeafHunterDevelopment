package com.example.leafhunterdevelopment.ui.collection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leafhunterdevelopment.R

class CollectionFragment : Fragment() {

    private val viewModel: CollectionViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlantAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.tab_collections, container, false)

        recyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Then observe and update data
        viewModel.plants.observe(viewLifecycleOwner, Observer { plantList ->
            Log.d("CollectionsFragment", "Observed ${plantList.size} plants")
            adapter = PlantAdapter(plantList) // OR better: use a mutable list and call adapter.notifyDataSetChanged()
            recyclerView.adapter = adapter
        })

        return root
    }
}
