import android.net.TrafficStats
import android.provider.ContactsContract
import android.provider.MediaStore

// ... inside MainActivity ...
private var lastRxBytes: Long = 0
private var lastTimestamp: Long = 0

private fun startSystemLoop() {
    handler.post(object : Runnable {
        override fun run() {
            // 1. Update RAM
            val mi = android.app.ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
            val used = (mi.totalMem - mi.availMem) / (1024 * 1024)
            tvRam.text = "[RAM: ${used}MB / ${(mi.totalMem / 1024 / 1024)}MB]"

            // 2. Update Network Speed (Real-time)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTime = System.currentTimeMillis()
            if (lastTimestamp != 0L) {
                val speed = (currentRx - lastRxBytes) / (currentTime - lastTimestamp) // KB/s
                findViewById<TextView>(R.id.tv_net).text = "[NET: ${speed} KB/s]"
            }
            lastRxBytes = currentRx
            lastTimestamp = currentTime

            handler.postDelayed(this, 1000)
        }
    })
}

private fun performUniversalSearch(query: String) {
    val q = query.lowercase()
    
    // Combine Results
    val results = mutableListOf<AppInfo>()
    
    // 1. Filter Apps
    results.addAll(allApps.filter { it.label.lowercase().contains(q) })
    
    // 2. Add System Commands
    if (q == "launcher" || q == "config") {
        results.add(AppInfo("SYSTEM: CHANGE LAUNCHER", "com.android.settings"))
    }
    
    // 3. Contact/File placeholders (requires permissions)
    // searchContacts(q)
    // searchFiles(q)

    adapter.updateList(results)
}
