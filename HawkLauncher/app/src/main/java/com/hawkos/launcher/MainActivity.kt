import android.net.TrafficStats
import android.provider.ContactsContract
import android.provider.MediaStore

class MainActivity : Activity() {
    private var lastRxBytes: Long = 0
    private var lastTimestamp: Long = 0

    // Call this in onCreate()
    private fun setupUniversalSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    adapter.updateList(allApps)
                    return
                }
                
                // 1. Search Apps (Existing)
                val filteredApps = allApps.filter { it.label.contains(query, ignoreCase = true) }
                
                // 2. Placeholder for Contact/File Search
                // Note: Requires READ_CONTACTS and READ_EXTERNAL_STORAGE permissions
                searchExtraResources(query)
                
                adapter.updateList(filteredApps)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateSystemStats() {
        handler.post(object : Runnable {
            override fun run() {
                // RAM & CPU
                val mi = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem) / (1024 * 1024)
                
                // Network Speed Logic
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTime = System.currentTimeMillis()
                val speed = if (lastTimestamp != 0L) {
                    (currentRxBytes - lastRxBytes) / (currentTime - lastTimestamp) // KB/s
                } else 0
                
                lastRxBytes = currentRxBytes
                lastTimestamp = currentTime

                tvRam.text = "[RAM: ${used}MB] [NET: ${speed}KB/s]"
                handler.postDelayed(this, 1000) 
            }
        })
    }
}
