package com.example.photostudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LandingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var imageItemList: List<ImageItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_landing_page, container, false)

        recyclerView = view.findViewById(R.id.imageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Make sure these drawable resources exist
        imageItemList = listOf(
            ImageItem(R.drawable.image1),
            ImageItem(R.drawable.image2),
            ImageItem(R.drawable.image3),
            ImageItem(R.drawable.image4),
            ImageItem(R.drawable.image5),
            ImageItem(R.drawable.group),
            ImageItem(R.drawable.fiveyrearsold),
            ImageItem(R.drawable.prebirthday)
        )

        imageAdapter = ImageAdapter(imageItemList)
        recyclerView.adapter = imageAdapter

        return view
    }
}
