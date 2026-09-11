package egehanvx.portscanner

data class ScanResult(
    val port: Int,
    val isOpen: Boolean,
    val service: String?
)