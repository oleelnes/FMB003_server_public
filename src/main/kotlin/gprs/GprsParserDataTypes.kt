package gprs


data class ResponseCodec12 (
    val dataSize: Int,
    val responseQuantity: Int,
    val responseType: Int,
    val data: String
)