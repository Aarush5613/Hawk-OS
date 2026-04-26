// Add these imports to your existing ones
import android.net.TrafficStats
import android.provider.ContactsContract

class MainActivity : Activity() {
    private var lastRxBytes: Long = 0
    private var lastTimestamp: Long = 0

    // Update the existing updateSystemStats function:
    private fun updateSystemStats() {
        handler.post(object : Runnable {
            override fun run() {
                // 1. RAM info
                val mi = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
                val usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024)
                val totalMb = mi.totalMem / (1024 * 1024)

                // 2. Network Speed
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTimestamp = System.currentTimeMillis()
                val speed = if (lastTimestamp != 0L) {
                    (currentRxBytes - lastRxBytes) * 1000 / (currentTimestamp - lastTimestamp) / 1024
                } else 0
                
                lastRxBytes = currentRxBytes
                lastTimestamp = currentTimestamp

                tvRam.text = "[RAM: ${usedMb}/${totalMb}MB] [CPU: --]"
                tvNetSpeed.text = "[NET: ${speed} KB/s]"
                
                handler.postDelayed(this, 2000) // Update every 2s for efficiency
            }
        })
    }

    // New Universal Search Function
    private fun searchUniversal(query: String) {
        val filteredApps = allApps.filter { it.label.contains(query, ignoreCase = true) }
        
        // Search Contacts (Brief example)
        if (query.length > 2) {
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, null,
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?", 
                arrayOf("%$query%"), null
            )
            // Handle cursor to add contact names to the list...
            cursor?.close()
        }
        
        adapter.updateList(filteredApps)
    }
}
