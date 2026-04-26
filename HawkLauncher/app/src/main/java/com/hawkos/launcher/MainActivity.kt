import android.net.TrafficStats
import android.provider.ContactsContract

// Inside MainActivity class
private var lastRxBytes: Long = 0
private var lastTimestamp: Long = 0

private fun startNetworkMonitor() {
    handler.post(object : Runnable {
        override fun run() {
            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTime = System.currentTimeMillis()
            
            if (lastTimestamp != 0L) {
                val bytesReceived = currentRxBytes - lastRxBytes
                val timeDiff = currentTime - lastTimestamp
                // Calculate speed in KB/s
                val speed = (bytesReceived * 1000) / timeDiff / 1024
                findViewById<TextView>(R.id.tv_net).text = "[NET: ${speed}KB/s]"
            }
            
            lastRxBytes = currentRxBytes
            lastTimestamp = currentTime
            handler.postDelayed(this, 2000) // Update every 2 seconds
        }
    })
}

private fun performUniversalSearch(query: String) {
    // 1. Filter Apps
    val filteredApps = allApps.filter { it.label.contains(query, ignoreCase = true) }
    
    // 2. Search Contacts (Optional: Requires READ_CONTACTS permission)
    if (query.length > 2) {
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, null,
            "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?", arrayOf("%$query%"), null
        )
        // Add results to your adapter here
        cursor?.close()
    }
    
    adapter.updateList(filteredApps)
}
