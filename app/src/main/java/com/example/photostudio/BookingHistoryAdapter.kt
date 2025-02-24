package com.example.photostudio

import Booking
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BookingHistoryAdapter(private var bookingList: List<Booking>) :
    RecyclerView.Adapter<BookingHistoryAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val packageImage: ImageView = itemView.findViewById(R.id.packageImage)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val bookingDate: TextView = itemView.findViewById(R.id.bookingDate)
        val bookingAmount: TextView = itemView.findViewById(R.id.bookingAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_history, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookingList[position]
        holder.packageName.text = "Package: ${booking.description}"
        holder.bookingDate.text = "Date: ${booking.appointmentDate}"
        holder.bookingAmount.text = "Amount: ₱${booking.totalAmount
        }"

        // Load image using Glide (if available)
        if (booking.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(booking.imageUrl)
                .placeholder(R.drawable.solopackageimg)
                .into(holder.packageImage)
        } else {
            holder.packageImage.setImageResource(R.drawable.solopackageimg)
        }
    }

    override fun getItemCount(): Int {
        return bookingList.size
    }

    fun updateBookings(newBookings: List<Booking>) {
        bookingList = newBookings
        notifyDataSetChanged()
    }
}
