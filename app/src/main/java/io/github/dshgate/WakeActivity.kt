package io.github.dshgate

import android.app.Activity
import android.os.Bundle

/** 存在的唯一目的：让系统把屏幕点亮，并盖在锁屏上。它不做任何操作。 */
class WakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        runCatching {
            val km = getSystemService(android.app.KeyguardManager::class.java)
            if (km != null && km.isKeyguardLocked) km.requestDismissKeyguard(this, null)
        }
        window?.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
        android.os.Handler(mainLooper).postDelayed({ finish() }, 1500)
    }
}
