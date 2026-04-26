package com.hawkos.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvRam: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvBattery: TextView
    private lateinit var etSearch: EditText
    private lateinit var lvApps: ListView
    private lateinit var tvPrompt: TextView
    private lateinit var tvTerminal: TextView
    private lateinit var scrollTerminal: ScrollView

    private val handler = Handler(Looper.getMainLooper())
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var adapter: AppAdapter
    private val terminalLines = mutableListOf<String>()

    data class AppInfo(val label: String, val packageName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        tvTime     = findViewById(R.id.tv_time)
        tvDate     = findViewById(R.id.tv_date)
        tvBattery  = findViewById(R.id.tv_battery)
        tvRam      = findViewById(R.id.tv_ram)
        tvStatus   = findViewById(R.id.tv_status)
        etSearch   = findViewById(R.id.et_search)
        lvApps     = findViewById(R.id.lv_apps)
        tvPrompt   = findViewById(R.id.tv_prompt)
        tvTerminal = findViewById(R.id.tv_terminal)
        scrollTerminal = findViewById(R.id.scroll_terminal)

        loadApps()
        startClock()
        setupSearch()
        setupAppClick()
        updateSystemStats()

        // show app list by default
        showAppList()
        print("HAWK OS v1.2 — HUNT BEGINS")
        print("type 'help' for commands")
    }

    // ── Terminal output ──────────────────────────────
    private fun print(text: String) {
        terminalLines.add(text)
        if (terminalLines.size > 6) terminalLines.removeAt(0)
        tvTerminal.text = terminalLines.joinToString("\n")
    }

    // ── App list ─────────────────────────────────────
    private fun showAppList() {
        lvApps.visibility = View.VISIBLE
        adapter.updateList(allApps)
    }

    private fun showFilteredList(query: String) {
        val filtered = allApps.filter {
            it.label.lowercase().contains(query.lowercase())
        }
        adapter.updateList(filtered)
        lvApps.visibility = View.VISIBLE
    }

    // ── Load apps ────────────────────────────────────
    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        allApps = resolveInfos
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != packageName }
            .sortedBy { it.label.lowercase() }

        adapter = AppAdapter(this, allApps.toMutableList())
        lvApps.adapter = adapter
    }

    // ── Search & commands ────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                tvPrompt.text = if (query.isEmpty()) "hawk@os:~# _"
                                else "hawk@os:~# $query _"
                if (query.isEmpty()) showAppList()
                else showFilteredList(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_UP
                && etSearch.text.isNotEmpty()) {
                val query = etSearch.text.toString().trim()
                print("hawk@os:~# $query")
                handleCommand(query.lowercase())
                etSearch.setText("")
                true
            } else false
        }
    }

    private fun handleCommand(cmd: String) {
        when {
            cmd == "help" -> {
                print("open <app> | apps | neofetch")
                print("top | set launcher | clear")
            }
            cmd == "neofetch" -> {
                val mi = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem) / (1024*1024)
                val total = mi.totalMem / (1024*1024)
                print("os: Hawk OS v1.2")
                print("device: ${Build.MANUFACTURER} ${Build.MODEL}")
                print("ram: ${used}MB / ${total}MB")
                print("apps: ${allApps.size} installed")
            }
            cmd == "top" -> {
                val mi = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem) / (1024*1024)
                val total = mi.totalMem / (1024*1024)
                print("RAM: ${used}MB / ${total}MB")
                print("FREE: ${mi.availMem/(1024*1024)}MB")
            }
            cmd == "apps" -> {
                showAppList()
                print("${allApps.size} packages installed")
            }
            cmd == "clear" -> {
                terminalLines.clear()
                tvTerminal.text = ""
                showAppList()
            }
            cmd == "set launcher" -> {
                print("opening launcher settings...")
                setDefaultLauncher()
            }
            cmd.startsWith("open ") -> {
                val name = cmd.removePrefix("open ").trim()
                launchBestMatch(name)
            }
            else -> {
                val match = findBestMatch(cmd)
                if (match != null) {
                    print("launching ${match.label}...")
                    launchPackage(match.packageName)
                } else {
                    print("[ERROR] not found: $cmd")
                }
            }
        }
    }

    private fun setDefaultLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 1)
                }
            } else {
                startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
            }
        } catch (e: Exception) {
            startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun findBestMatch(query: String): AppInfo? {
        return allApps.firstOrNull { it.label.lowercase() == query }
            ?: allApps.firstOrNull { it.label.lowercase().startsWith(query) }
            ?: allApps.firstOrNull { it.label.lowercase().contains(query) }
    }

    private fun launchBestMatch(name: String) {
        val match = findBestMatch(name)
        if (match != null) { print("launching ${match.label}..."); launchPackage(match.packageName) }
        else print("[ERROR] not found: $name")
    }

    private fun setupAppClick() {
        lvApps.setOnItemClickListener { _, _, position, _ ->
            val app = adapter.getItem(position) as AppInfo
            print("launching ${app.label}...")
            launchPackage(app.packageName)
        }
    }

    private fun launchPackage(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) startActivity(intent)
        else print("[ERROR] cannot launch")
    }

    private fun startClock() {
        handler.post(object : Runnable {
            override fun run() {
                val cal = java.util.Calendar.getInstance()
                val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2,'0')
                val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2,'0')
                val s = cal.get(java.util.Calendar.SECOND).toString().padStart(2,'0')
                tvTime.text = "$h:$m:$s"
                val days = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
                val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
                val day = days[cal.get(java.util.Calendar.DAY_OF_WEEK)-1]
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2,'0')
                val mon = months[cal.get(java.util.Calendar.MONTH)]
                tvDate.text = "$day $d $mon ${cal.get(java.util.Calendar.YEAR)}"
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun updateSystemStats() {
        handler.post(object : Runnable {
            override fun run() {
                val mi = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
                val used = (mi.totalMem - mi.availMem)/(1024*1024)
                val total = mi.totalMem/(1024*1024)
                tvRam.text = "[RAM:${used}/${total}MB]"
                tvStatus.text = "[SYS:OK]"
                handler.postDelayed(this, 5000)
            }
        })
    }

    override fun onBackPressed() {}
}
