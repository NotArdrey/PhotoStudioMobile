import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.photostudio.R

class BookingAdapter(
    private var bookings: MutableList<Booking>,
    private val listener: OnBookingClickListener
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    interface OnBookingClickListener {
        fun onRebookClicked(booking: Booking)
        fun onCancelClicked(booking: Booking)
        fun onPayRemainingClicked(booking: Booking)  // New method
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.packageName.text = "Package: ${booking.packageType}"
        holder.bookingDate.text = "Date: ${booking.appointmentDate}"
        holder.appointmentTime.text = "Time: ${booking.appointmentTime}"
        holder.bookingAmount.text = "Amount: ₱${booking.totalAmount}"

        if (booking.paymentType.equals("Full Payment", ignoreCase = true)) {
            holder.paymentStatus.text = "Payment Status: Fully Paid"
            holder.paymentStatus.setTextColor(Color.parseColor("#388E3C"))
            holder.payRemainingButton.visibility = View.GONE  // Hide the button
        } else {
            holder.paymentStatus.text = "Payment Status: Downpayment. Remaining: ₱${booking.remainingAmount}"
            holder.paymentStatus.setTextColor(Color.parseColor("#D32F2F"))
            // Show the "Pay Remaining" button only if remainingAmount is greater than 0
            if (booking.remainingAmount > 0) {
                holder.payRemainingButton.visibility = View.VISIBLE
            } else {
                holder.payRemainingButton.visibility = View.GONE
            }
        }

        val packageType = booking.packageType.lowercase()
        val imageRes = when {
            "Solo Package" in packageType -> R.drawable.solopackageimg
            "Pair Package" in packageType -> R.drawable.pair
            "Group Package" in packageType -> R.drawable.group
            "1-2 Years Old Package" in packageType -> R.drawable.preoneyearold
            "3-4 Years Old Package" in packageType -> R.drawable.image5
            "5-9 Years Old Package" in packageType -> R.drawable.tenyearsold
            else -> R.drawable.group
        }
        holder.packageImage.setImageResource(imageRes)

        holder.rebookButton.setOnClickListener { listener.onRebookClicked(booking) }
        holder.cancelButton.setOnClickListener { listener.onCancelClicked(booking) }
        holder.payRemainingButton.setOnClickListener { listener.onPayRemainingClicked(booking) }  // New click event
    }

    fun updateBookings(newBookings: List<Booking>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = bookings.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val bookingDate: TextView = itemView.findViewById(R.id.bookingDate)
        val appointmentTime: TextView = itemView.findViewById(R.id.appointmentTime)
        val bookingAmount: TextView = itemView.findViewById(R.id.bookingAmount)
        val paymentStatus: TextView = itemView.findViewById(R.id.paymentStatus)
        val rebookButton: Button = itemView.findViewById(R.id.rebookButton)
        val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
        val packageImage: ImageView = itemView.findViewById(R.id.packageImage)
        val payRemainingButton: Button = itemView.findViewById(R.id.payRemainingButton)  // New button
    }
}
