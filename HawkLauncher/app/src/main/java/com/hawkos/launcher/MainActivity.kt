package com.hawkos.launcher

import android.app.*
import android.app.role.RoleManager
import android.content.*
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.TrafficStats
import android.os.*
import android.provider.ContactsContract
import android.text.*
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.core.view.GestureDetectorCompat

class MainActivity : Activity() {

    // Views
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvRam: TextView
    private lateinit var tvCpu: TextView
    private lateinit var tvNet: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etSearch: EditText
    private lateinit var tvPrompt: TextView
    private lateinit var tvOutput: TextView
    private lateinit var drawerLayout: FrameLayout
    private lateinit var appDrawer: LinearLayout
    private lateinit var lvApps: ListView
    private lateinit var etDrawerSearch: EditText

    private val handler = Handler(Looper.getMainLooper())
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var drawerAdapter: AppAdapter
    private var isDrawerOpen = false

    // Network tracking
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L

    data class AppInfo(val label: String, val packageName: String)
    data class SearchResult(val label: String, val type: String, val data: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        bindViews()
        loadApps()
        setupDrawer()
        setupSearch()
        startClock()
        startStats()
    }

    private fun bindViews() {
        tvTime        = findViewById(R.id.tv_time)
        tvDate        = findViewById(R.id.tv_date)
        tvRam         = findViewById(R.id.tv_ram)
        tvCpu         = findViewById(R.id.tv_cpu)
        tvNet         = findViewById(R.id.tv_net)
        tvStatus      = findViewById(R.id.tv_status)
        etSearch      = findViewById(R.id.et_search)
        tvPrompt      = findViewById(R.id.tv_prompt)
        tvOutput      = findViewById(R.id.tv_output)
        drawerLayout  = findViewById(R.id.drawer_layout)
        appDrawer     = findViewById(R.id.app_drawer)
        lvApps        = findViewById(R.id.lv_apps)
        etDrawerSearch= findViewById(R.id.et_drawer_search)
    }

    // ── App Drawer ───────────────────────────────────

    private fun setupDrawer() {
        drawerAdapter = AppAdapter(this, allApps.toMutableList())
        lvApps.adapter = drawerAdapter

        // swipe gesture on main layout
        val gestureDetector = GestureDetectorCompat(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val dx = e2.x - e1.x
                    val dy = e2.y - e1.y
                    // swipe right from left edge
                    if (e1.x < 80f && dx > 100f && Math.abs(dy) < 200f) {
                        openDrawer(); return true
                    }
                    // swipe left to close
                    if (isDrawerOpen && dx < -100f && Math.abs(dy) < 200f) {
                        closeDrawer(); return true
                    }
                    return false
                }
            })

        findViewById<View>(R.id.main_content).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        // close drawer on overlay tap
        drawerLayout.setOnClickListener {
            if (isDrawerOpen) closeDrawer()
        }

        // drawer search
        etDrawerSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.lowercase() ?: ""
                val filtered = if (q.isEmpty()) allApps
                else allApps.filter { it.label.lowercase().contains(q) }
                drawerAdapter.updateList(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        lvApps.setOnItemClickListener { _, _, position, _ ->
            val app = drawerAdapter.getItem(position) as AppInfo
            closeDrawer()
            launchPackage(app.packageName)
        }
    }

    private fun openDrawer() {
        isDrawerOpen = true
        drawerLayout.visibility = View.VISIBLE
        appDrawer.visibility = View.VISIBLE
        val anim = TranslateAnimation(-appDrawer.width.toFloat(), 0f, 0f, 0f)
        anim.duration = 250
        anim.fillAfter = true
        appDrawer.startAnimation(anim)
    }

    private fun closeDrawer() {
        isDrawerOpen = false
        val anim = TranslateAnimation(0f, -appDrawer.width.toFloat(), 0f, 0f)
        anim.duration = 200
        anim.fillAfter = true
        appDrawer.startAnimation(anim)
        handler.postDelayed({
            drawerLayout.visibility = View.GONE
        }, 200)
        etDrawerSearch.setText("")
        drawerAdapter.updateList(allApps)
    }

    // ── Universal Search ─────────────────────────────

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                tvPrompt.text = if (q.isEmpty()) "hawk@os:~# _" else "hawk@os:~# $q _"
                if (q.isEmpty()) tvOutput.text = ""
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_UP
                && etSearch.text.isNotEmpty()) {
                val query = etSearch.text.toString().trim()
                handleInput(query)
                etSearch.setText("")
                true
            } else false
        }
    }

    private fun handleInput(input: String) {
        val cmd = input.lowercase().trim()
        when {
            cmd == "help" -> showOutput("""
── HAWK OS COMMANDS ──────────────
open <app>     launch app
call <name>    call contact
neofetch       system info
top            ram & cpu stats
settings       launcher settings
clear          clear output
help           this menu
──────────────────────────────────
tip: just type app/contact name
            """.trimIndent())

            cmd == "neofetch" -> {
                val mi = ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem) / (1024 * 1024)
                val total = mi.totalMem / (1024 * 1024)
                showOutput("""
os       Hawk OS v1.3
device   ${Build.MANUFACTURER} ${Build.MODEL}
android  ${Build.VERSION.RELEASE}
ram      ${used}MB / ${total}MB
apps     ${allApps.size} installed
kernel   HawkKernel-balanced
theme    Cyberdeck Dark
                """.trimIndent())
            }

            cmd == "top" -> {
                val mi = ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem) / (1024 * 1024)
                val total = mi.totalMem / (1024 * 1024)
                val free = mi.availMem / (1024 * 1024)
                showOutput("RAM total : ${total}MB\nRAM used  : ${used}MB\nRAM free  : ${free}MB")
            }

            cmd == "clear" -> tvOutput.text = ""

            cmd == "settings" -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            cmd.startsWith("open ") -> {
                val name = cmd.removePrefix("open ").trim()
                val match = findBestApp(name)
                if (match != null) { showOutput("launching ${match.label}..."); launchPackage(match.packageName) }
                else showOutput("[ERROR] app not found: $name")
            }

            cmd.startsWith("call ") -> {
                val name = cmd.removePrefix("call ").trim()
                showOutput("searching contacts: $name...")
                callContact(name)
            }

            else -> {
                // universal search — apps + contacts
                val results = universalSearch(input)
                if (results.isEmpty()) {
                    showOutput("[ERROR] nothing found: $input\ntype 'help' for commands")
                } else if (results.size == 1) {
                    val r = results[0]
                    showOutput("launching ${r.label}...")
                    when (r.type) {
                        "app" -> launchPackage(r.data)
                        "contact" -> dialNumber(r.data)
                    }
                } else {
                    val sb = StringBuilder("// RESULTS\n")
                    results.take(5).forEach { sb.append("[${it.type}] ${it.label}\n") }
                    showOutput(sb.toString())
                }
            }
        }
    }

    private fun universalSearch(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        // apps
        allApps.filter { it.label.lowercase().contains(query.lowercase()) }
            .take(3)
            .forEach { results.add(SearchResult(it.label, "app", it.packageName)) }
        // contacts
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"), null
            )
            cursor?.use {
                while (it.moveToNext() && results.size < 6) {
                    val name = it.getString(0) ?: continue
                    val number = it.getString(1) ?: continue
                    results.add(SearchResult(name, "contact", number))
                }
            }
        } catch (e: Exception) { }
        return results
    }

    private fun showOutput(text: String) {
        tvOutput.text = text
    }

    // ── App utils ─────────────────────────────────────

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        allApps = pm.queryIntentActivities(intent, 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun findBestApp(query: String): AppInfo? {
        return allApps.firstOrNull { it.label.lowercase() == query }
            ?: allApps.firstOrNull { it.label.lowercase().startsWith(query) }
            ?: allApps.firstOrNull { it.label.lowercase().contains(query) }
    }

    private fun launchPackage(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            ?: showOutput("[ERROR] cannot launch")
    }

    private fun callContact(name: String) {
        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"), null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(1)
                    dialNumber(number)
                } else showOutput("[ERROR] contact not found: $name")
            }
        } catch (e: Exception) { showOutput("[ERROR] contacts unavailable") }
    }

    private fun dialNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$number")
        }
        startActivity(intent)
    }

    // ── Clock ─────────────────────────────────────────

    private fun startClock() {
        handler.post(object : Runnable {
            override fun run() {
                val cal = java.util.Calendar.getInstance()
                val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
                val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
                val s = cal.get(java.util.Calendar.SECOND).toString().padStart(2, '0')
                tvTime.text = "$h:$m:$s"
                val days = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
                val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
                    "JUL","AUG","SEP","OCT","NOV","DEC")
                val day = days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                val mon = months[cal.get(java.util.Calendar.MONTH)]
                tvDate.text = "$day $d $mon ${cal.get(java.util.Calendar.YEAR)}"
                handler.postDelayed(this, 1000)
            }
        })
    }

    // ── Live Stats ────────────────────────────────────

    private fun startStats() {
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        handler.post(object : Runnable {
            override fun run() {
                // RAM
                val mi = ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem) / (1024 * 1024)
                val total = mi.totalMem / (1024 * 1024)
                tvRam.text = "RAM:${used}/${total}MB"

                // Network speed
                val rx = TrafficStats.getTotalRxBytes()
                val tx = TrafficStats.getTotalTxBytes()
                val downKb = (rx - lastRxBytes) / 1024
                val upKb = (tx - lastTxBytes) / 1024
                lastRxBytes = rx
                lastTxBytes = tx
                tvNet.text = "↓${downKb}KB/s ↑${upKb}KB/s"
                tvStatus.text = "[SYS:OK]"

                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onBackPressed() {
        if (isDrawerOpen) closeDrawer()
    }
}
