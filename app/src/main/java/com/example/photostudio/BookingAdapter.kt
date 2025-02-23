import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.photostudio.R
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookingAdapter(private var bookings: MutableList<Booking>, private val listener: OnBookingClickListener) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    interface OnBookingClickListener {
        fun onRebookClicked(booking: Booking)
        fun onCancelClicked(booking: Booking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.packageName.text = "Package: ${booking.description}"
        holder.bookingDate.text = "Date: ${booking.appointmentDate}"
        holder.appointmentTime.text = "Time: ${booking.appointmentTime}"
        holder.bookingAmount.text = "Amount: ₱${booking.totalAmount}"


        holder.paymentStatus.text = if (booking.paymentType == "Full Payment") {
            "Payment Status: Fully Paid"
        } else {
            "Payment Status: Downpayment"
        }


        holder.paymentStatus.setTextColor(
            if (booking.paymentType == "Full Payment") Color.parseColor("#388E3C")
            else Color.parseColor("#D32F2F")
        )

        holder.rebookButton.setOnClickListener { listener.onRebookClicked(booking) }
        holder.cancelButton.setOnClickListener { listener.onCancelClicked(booking) }
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
    }
}
