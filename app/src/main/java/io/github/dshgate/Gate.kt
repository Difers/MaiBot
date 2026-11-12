package io.github.dshgate

import android.content.Context
import org.json.JSONObject
import java.io.File

data class Event(val at: Long, val pkg: String, val title: String, val text: String, val key: String)

/** 全部状态都在这里：收到的通知、关注哪些应用、落盘文件。 */
object Gate {
    private const val MAX = 800
    private val events = ArrayDeque<Event>()
    private var prefs: android.content.SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.getSharedPreferences("gate", Context.MODE_PRIVATE)
        runCatching {
            File(ctx.filesDir, "events.jsonl").readLines().takeLast(200).forEach { line ->
                val o = JSONObject(line)
                events.addLast(
                    Event(
                        o.optLong("at"),
                        o.optString("pkg"),
                        o.optString("title"),
                        o.optString("text"),
                        o.optString("key"),
                    ),
                )
            }
        }
    }

    val packages: List<String>
        get() = prefs?.getString("packages", null)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("com.tencent.mobileqq")

    fun setPackages(csv: String) {
        prefs?.edit()?.putString("packages", csv)?.apply()
    }

    @Synchronized
    fun record(ctx: Context, e: Event) {
        events.addLast(e)
        while (events.size > MAX) events.removeFirst()
        runCatching {
            File(ctx.filesDir, "events.jsonl").appendText(
                JSONObject().apply {
                    put("at", e.at); put("pkg", e.pkg); put("title", e.title)
                    put("text", e.text); put("key", e.key)
                }.toString() + "\n",
            )
        }
    }

    @Synchronized
    fun since(ts: Long): List<Event> = events.filter { it.at > ts }

    @Synchronized
    fun all(): List<Event> = events.toList()
}
