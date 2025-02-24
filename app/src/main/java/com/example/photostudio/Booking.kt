data class Booking(
    val id: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val archive: String = "no",
    val complete: String = "no",
    val defaultBackdrop: String = "",
    val imageUrl: String = "",
    val packageType: String = "",
    val extraPersonQty: Int = 0,
    val paymentLinkId: String = "",
    val paymentType: String = "",
    val paymongoName: String = "",
    val selectedExtraBackdrop: String = "",
    val softCopyQty: Int = 0,
    val totalAmount: Int = 0,
    val remainingAmount: Int = 0,
    val uid: String = ""
)
