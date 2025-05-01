package com.example.leafhunterdevelopment.ui.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.leafhunterdevelopment.R
import com.example.leafhunterdevelopment.databinding.FragmentCollectionBinding
import com.google.android.material.tabs.TabLayoutMediator

class CollectionFragment : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewPager with tabs
        binding.viewPager.adapter = CollectionPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> getString(R.string.tab_all_plants)
                1 -> getString(R.string.tab_collections)
                else -> ""
            }
        }.attach()

        // FAB click listener
        binding.fabAddPlant.setOnClickListener {
            // Handle add new plant action
        }
    }

    private inner class CollectionPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> PlantListFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
