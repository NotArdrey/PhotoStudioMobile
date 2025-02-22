import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.photostudio.R

class BookingAdapter(private var bookings: MutableList<Booking>) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newList: List<Booking>) {
        bookings.clear()
        bookings.addAll(newList)
        notifyDataSetChanged()
    }

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val packageImage: ImageView =
            itemView.findViewById(R.id.packageImage) // Matches XML
        private val title: TextView = itemView.findViewById(R.id.packageName) // Matches XML
        private val date: TextView = itemView.findViewById(R.id.bookingDate) // Matches XML
        private val price: TextView = itemView.findViewById(R.id.bookingAmount) // Matches XML
        private val rebookButton: Button = itemView.findViewById(R.id.rebookButton) // Matches XML

        fun bind(booking: Booking) {
            title.text = booking.description
            date.text = "Date: ${booking.appointmentDate}"
            price.text = "Amount: ₱${booking.totalAmount}"

            // Dynamically set the image based on package type
            val packageType = booking.description.lowercase()
            val imageRes = when {
                "solo" in packageType -> R.drawable.solopackageimg
                "pair" in packageType -> R.drawable.pair
                "group" in packageType -> R.drawable.group
                else -> R.drawable.group // Fallback image
            }
            packageImage.setImageResource(imageRes)

            // Handle rebook button click (optional)
            rebookButton.setOnClickListener {
                // Implement your rebooking logic here
            }
        }
    }
}
