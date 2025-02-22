data class Booking(
    val description: String,  // Example: "Captured By K Package: Solo Package"
    val appointmentDate: String,
    val totalAmount: String,
    val imageUrl: String = ""  // New field for image URL (optional)
)
