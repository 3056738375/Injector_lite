package com.injector.loader.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.injector.loader.core.AppManager
import com.injector.loader.core.FileManager
import com.injector.loader.core.LogManager
import com.injector.loader.injection.SOInjector
import com.injector.loader.permission.PermissionLevel
import com.injector.loader.permission.PermissionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val pm = PermissionManager(this)
    private val am = AppManager(this)
    private val si = SOInjector(this)
    private val fm = FileManager()
    private val lm = LogManager.getInstance()

    private lateinit var tvStatus: TextView
    private lateinit var tvApp: TextView
    private lateinit var tvSo: TextView
    private lateinit var tvLog: TextView
    private lateinit var rv: RecyclerView

    private var permLv = PermissionLevel.NONE
    private var selApp: String? = null
    private var selSo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(12, 12, 12, 12)
        }

        main.addView(TextView(this).apply {
            text = "Injector"
            textSize = 22f
            layoutParams = lpWrap().apply { bottomMargin = 8 }
        })

        tvStatus = TextView(this).apply {
            text = "初始化中..."
            textSize = 12f
            layoutParams = lpWrap().apply { bottomMargin = 8 }
        }
        main.addView(tvStatus)

        tvApp = TextView(this).apply {
            text = "应用: 未选择"
            textSize = 11f
            layoutParams = lpWrap().apply { bottomMargin = 4 }
        }
        main.addView(tvApp)

        tvSo = TextView(this).apply {
            text = "SO: 未选择"
            textSize = 11f
            layoutParams = lpWrap().apply { bottomMargin = 12 }
        }
        main.addView(tvSo)

        rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 280
            ).apply { bottomMargin = 12 }
        }
        main.addView(rv)

        tvLog = TextView(this).apply {
            text = "[日志]\n"
            textSize = 9f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 180
            ).apply { bottomMargin = 12 }
            setBackgroundColor(android.graphics.Color.parseColor("#efefef"))
            setPadding(6, 6, 6, 6)
        }
        main.addView(tvLog)

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = lpMatch().apply { bottomMargin = 8 }
        }
        row1.addView(Button(this).apply {
            text = "刷新"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f).apply { marginEnd = 4 }
            setOnClickListener { lifecycleScope.launch { loadApps() } }
        })
        row1.addView(Button(this).apply {
            text = "选SO"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f).apply { marginEnd = 4; marginStart = 4 }
            setOnClickListener {
                FilePickerDialog { path ->
                    selSo = path
                    tvSo.text = "SO: ${path.substringAfterLast('/')}"
                    lm.info("选择: $path")
                }.show(supportFragmentManager, "fp")
            }
        })
        row1.addView(Button(this).apply {
            text = "清日志"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f).apply { marginStart = 4 }
            setOnClickListener {
                lm.clearLogs()
                tvLog.text = "[日志]\n"
            }
        })
        main.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = lpMatch()
        }
        row2.addView(Button(this).apply {
            text = "开始注入"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f)
            setOnClickListener {
                if (selApp == null || selSo == null) {
                    Toast.makeText(this@MainActivity, "选择应用和SO", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch { inject() }
            }
        })
        main.addView(row2)

        scroll.addView(main)
        setContentView(scroll)

        lm.addListener { entry ->
            runOnUiThread {
                tvLog.append("[${entry.timestamp}] ${entry.level}: ${entry.message}\n")
            }
        }

        lifecycleScope.launch {
            checkPerms()
            loadApps()
        }
    }

    private suspend fun checkPerms() {
        lm.info("检查权限...")
        try {
            if (!pm.checkAdbPermission()) {
                pm.requestAdbPermission()
            }
        } catch (e: Exception) {
            lm.debug("ADB请求异常")
        }

        permLv = pm.getEffectivePermission()
        val txt = when (permLv) {
            PermissionLevel.ROOT -> "✓ Root"
            PermissionLevel.ADB -> "✓ ADB"
            PermissionLevel.NONE -> "✗ 无权限"
        }
        tvStatus.text = txt
        lm.success(txt)
    }

    private suspend fun loadApps() {
        lm.info("加载应用...")
        val apps = am.getInstalledApps(false)
        lm.success("${apps.size} 个应用")
        rv.adapter = AppAdapter(apps) { pkg ->
            selApp = pkg.packageName
            tvApp.text = "应用: ${pkg.appName}"
            lm.info("选择: ${pkg.appName}")
        }
    }

    private suspend fun inject() {
        lm.info("注入开始: $selApp")
        if (!fm.exists(selSo!!)) {
            lm.error("SO文件不存在")
            return
        }
        lm.info("SO大小: ${fm.formatSize(fm.getSize(selSo!!))}")

        val result = si.injectSO(selApp!!, selSo!!, permLv)
        when (result) {
            is com.injector.loader.injection.InjectionResult.SUCCESS -> {
                lm.success(result.message)
                Toast.makeText(this, "成功", Toast.LENGTH_SHORT).show()
            }
            is com.injector.loader.injection.InjectionResult.FAILED -> {
                lm.error(result.reason)
                Toast.makeText(this, "失败: ${result.reason}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun lpWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun lpMatch() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}

private class AppAdapter(val items: List<com.injector.loader.core.AppItem>, val cb: (com.injector.loader.core.AppItem) -> Unit) :
    RecyclerView.Adapter<AppAdapter.VH>() {
    inner class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        fun bind(item: com.injector.loader.core.AppItem) {
            v.findViewById<ImageView>(android.R.id.icon).setImageDrawable(item.icon)
            v.findViewById<TextView>(android.R.id.text1).text = item.appName
            v.findViewById<TextView>(android.R.id.text2).text = item.packageName +
                    if (item.isDebug) " [D]" else ""
            v.setOnClickListener { cb(item) }
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, t: Int): VH {
        val v = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 70
            )
            setPadding(8, 8, 8, 8)
        }
        val iv = ImageView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
        }
        val tc = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                .apply { marginStart = 12 }
        }
        val t1 = TextView(parent.context).apply {
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val t2 = TextView(parent.context).apply {
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }
        tc.addView(t1)
        tc.addView(t2)
        v.addView(iv)
        v.addView(tc)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(vh: VH, i: Int) = vh.bind(items[i])
}
