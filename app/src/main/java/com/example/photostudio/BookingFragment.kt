package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class BookingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout
        val view = inflater.inflate(R.layout.fragment_booking_page, container, false)

        // Handle Image Clicks
        view.findViewById<ImageView>(R.id.arrow).setOnClickListener {
            startActivity(Intent(requireContext(), SoloPackagePage::class.java))
        }

        view.findViewById<ImageView>(R.id.pairArrow).setOnClickListener {
            startActivity(Intent(requireContext(), PairPackageActivity::class.java))
        }

        view.findViewById<ImageView>(R.id.GroupArrow).setOnClickListener {
            startActivity(Intent(requireContext(), GroupPackage::class.java))
        }

        view.findViewById<ImageView>(R.id.BirthdayArrow).setOnClickListener {
            startActivity(Intent(requireContext(), BirthdayPage::class.java))
        }

        return view
    }
}
