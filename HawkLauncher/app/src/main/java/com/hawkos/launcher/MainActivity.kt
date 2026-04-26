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
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvRam: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etSearch: EditText
    private lateinit var lvApps: ListView
    private lateinit var tvPrompt: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var adapter: AppAdapter

    data class AppInfo(val label: String, val packageName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        tvTime = findViewById(R.id.tv_time)
        tvDate = findViewById(R.id.tv_date)
        tvBattery = findViewById(R.id.tv_battery)
        tvRam = findViewById(R.id.tv_ram)
        tvStatus = findViewById(R.id.tv_status)
        etSearch = findViewById(R.id.et_search)
        lvApps = findViewById(R.id.lv_apps)
        tvPrompt = findViewById(R.id.tv_prompt)

        loadApps()
        startClock()
        setupSearch()
        setupAppClick()
        updateSystemStats()
    }

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

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = if (query.isEmpty()) allApps
                else allApps.filter { it.label.lowercase().contains(query) }
                adapter.updateList(filtered)
                tvPrompt.text = if (query.isEmpty()) "hawk@os:~# _"
                else "hawk@os:~# $query _"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                val query = etSearch.text.toString().lowercase().trim()
                handleCommand(query)
                true
            } else false
        }
    }

    private fun handleCommand(cmd: String) {
        when {
            cmd == "neofetch" -> showToast("[HAWK OS v1.0] [Exynos 9611] [Mali-G72]")
            cmd == "top" -> showToast("[CPU: ${(10..40).random()}%] [RAM: ${(30..70).random()}%]")
            cmd == "clear" -> { etSearch.setText(""); adapter.updateList(allApps) }
            cmd == "apps" -> { etSearch.setText(""); adapter.updateList(allApps) }
            cmd == "set launcher" -> setDefaultLauncher()
            cmd == "help" -> showHelp()
            cmd.startsWith("open ") -> launchBestMatch(cmd.removePrefix("open ").trim())
            else -> {
                val match = findBestMatch(cmd)
                if (match != null) launchPackage(match.packageName)
                else showToast("[ERROR] not found: $cmd — type 'help'")
            }
        }
        etSearch.setText("")
    }

    private fun setDefaultLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    startActivityForResult(intent, 1)
                }
            } else {
                val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // fallback
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        }
    }

    private fun showHelp() {
        val help = """
[HAWK OS COMMANDS]
> open <app>     launch app
> apps           list all apps
> set launcher   set as default
> neofetch       system info
> top            cpu/ram stats
> clear          clear prompt
> help           show this
        """.trimIndent()
        showToast(help)
    }

    private fun findBestMatch(query: String): AppInfo? {
        val exact = allApps.firstOrNull {
            it.label.lowercase() == query.lowercase()
        }
        if (exact != null) return exact
        val startsWith = allApps.firstOrNull {
            it.label.lowercase().startsWith(query.lowercase())
        }
        if (startsWith != null) return startsWith
        return allApps.firstOrNull {
            it.label.lowercase().contains(query.lowercase())
        }
    }

    private fun launchBestMatch(name: String) {
        val match = findBestMatch(name)
        if (match != null) launchPackage(match.packageName)
        else showToast("[ERROR] app not found: $name")
    }

    private fun setupAppClick() {
        lvApps.setOnItemClickListener { _, _, position, _ ->
            val app = adapter.getItem(position) as AppInfo
            launchPackage(app.packageName)
        }
    }

    private fun launchPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
        else showToast("[ERROR] cannot launch $packageName")
    }

    private fun startClock() {
        handler.post(object : Runnable {
            override fun run() {
                val cal = java.util.Calendar.getInstance()
                val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
                val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
                val s = cal.get(java.util.Calendar.SECOND).toString().padStart(2, '0')
                tvTime.text = "$h:$m:$s"
                val days = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
                val day = days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
                val mon = months[cal.get(java.util.Calendar.MONTH)]
                val y = cal.get(java.util.Calendar.YEAR)
                tvDate.text = "$day $d $mon $y"
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun updateSystemStats() {
        handler.post(object : Runnable {
            override fun run() {
                val mi = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
                val usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024)
                val totalMb = mi.totalMem / (1024 * 1024)
                tvRam.text = "[RAM: ${usedMb}MB/${totalMb}MB]"
                tvStatus.text = "[SYS:OK] [HAWK:ACTIVE]"
                handler.postDelayed(this, 5000)
            }
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {}
}
