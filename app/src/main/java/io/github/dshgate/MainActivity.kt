package io.github.dshgate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            gravity = Gravity.START
        }

        root.addView(TextView(this).apply { text = "DSH Gate"; textSize = 24f })
        root.addView(TextView(this).apply {
            text = "只做两件事：读通知、点亮屏幕。所有接口只在 127.0.0.1:8747，不联网。"
            textSize = 14f
            setPadding(0, pad / 2, 0, pad)
        })

        status = TextView(this).apply { textSize = 15f }
        root.addView(status)

        fun button(label: String, onClick: () -> Unit) {
            root.addView(Button(this).apply { text = label; setOnClickListener { onClick() } })
        }

        button("① 授予通知使用权") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        button("② 启动本地桥") {
            startService(Intent(this, GateService::class.java))
            refresh()
        }
        button("③ 测试点亮屏幕") {
            startActivity(Intent(this, WakeActivity::class.java))
        }
        button("刷新状态") { refresh() }

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val access = if (NotificationAccess.enabled(this)) "已授予" else "未授予"
        val running = if (isServiceRunning()) "运行中" else "未启动"
        status.text = "通知使用权：$access\n本地桥：$running\n监听：com.tencent.mobileqq"
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(android.app.ActivityManager::class.java)
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == GateService::class.java.name }
    }
}
