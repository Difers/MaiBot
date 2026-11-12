package io.github.dshgate

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log

/**
 * 点亮屏幕，三种策略依次试，返回哪种生效。
 *
 * 1. PowerManager.wakeUp()：官方 API，需要 DEVICE_POWER 签名权限，普通应用一般会抛异常
 * 2. WakeLock(ACQUIRE_CAUSES_WAKEUP)：官方已废弃，但很多 ROM 仍然照做——这一条最可能成立
 * 3. 透明 Activity(setTurnScreenOn)：最后兜底
 */
object Waker {
    private const val TAG = "DshGate"

    fun wake(ctx: Context): String {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isInteractive) return "already-on"

        // 1
        runCatching { pm.wakeUp(SystemClock.uptimeMillis()) }
            .onFailure { Log.d(TAG, "wakeUp() 被拒: ${it::class.java.simpleName}") }
        if (pm.isInteractive) {
            dismiss(ctx)
            return "wakeUp"
        }

        // 2
        runCatching {
            @Suppress("DEPRECATION")
            val flags = PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE
            @Suppress("DEPRECATION")
            val lock = pm.newWakeLock(flags, "dsh-gate:wake")
            lock.acquire(4000)
        }.onFailure { Log.d(TAG, "wakelock 失败: ${it::class.java.simpleName}") }
        if (pm.isInteractive) {
            dismiss(ctx)
            return "wakelock"
        }

        // 3
        runCatching {
            ctx.startActivity(
                Intent(ctx, WakeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
            )
        }
        return if (pm.isInteractive) "activity" else "failed"
    }

    /** 熄屏后再拉一次透明 Activity，让它在 setShowWhenLocked 下把锁屏收起来。 */
    private fun dismiss(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(ctx, WakeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
            )
        }
    }
}
