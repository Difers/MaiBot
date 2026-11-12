package io.github.dshgate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * 只监听 127.0.0.1，不对外网开放。
 * GET  /health            → 状态
 * GET  /events?since=ms   → 这段时间之后收到的通知
 * POST /wake              → 点亮屏幕
 * POST /config            → body: packages=com.tencent.mobileqq,xxx
 */
class GateServer(private val ctx: Context, private val port: Int = 8747) {
    @Volatile private var running = false
    private var server: ServerSocket? = null

    fun start() {
        if (running) return
        running = true
        thread(isDaemon = true, name = "gate-server") {
            runCatching {
                val s = ServerSocket()
                s.reuseAddress = true
                s.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port))
                server = s
                while (running) {
                    val socket = runCatching { s.accept() }.getOrNull() ?: continue
                    thread(isDaemon = true) { runCatching { handle(socket) } }
                }
            }
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
    }

    private fun handle(socket: Socket) = socket.use {
        it.soTimeout = 5000
        val reader = BufferedReader(InputStreamReader(it.getInputStream()))
        val requestLine = reader.readLine() ?: return@use
        val parts = requestLine.split(" ")
        val method = parts.getOrNull(0) ?: "GET"
        val path = parts.getOrNull(1) ?: "/"
        var length = 0
        while (true) {
            val header = reader.readLine() ?: break
            if (header.isEmpty()) break
            if (header.startsWith("Content-Length:", true)) {
                length = header.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }
        val body = if (length in 1..65536) {
            val buffer = CharArray(length)
            var read = 0
            while (read < length) {
                val n = reader.read(buffer, read, length - read)
                if (n <= 0) break
                read += n
            }
            String(buffer, 0, read)
        } else {
            ""
        }

        val (code, payload) = route(method, path, body)
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val out: OutputStream = it.getOutputStream()
        val header = buildString {
            append("HTTP/1.1 $code\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun route(method: String, path: String, body: String): Pair<Int, String> {
        val clean = path.substringBefore('?')
        val query = path.substringAfter('?', "")
        return when {
            clean == "/health" -> 200 to JSONObject().apply {
                put("ok", true)
                put("listening", NotificationAccess.enabled(ctx))
                put("packages", JSONArray(Gate.packages))
                put("port", port)
            }.toString()

            clean == "/events" -> {
                val since = query.split("&")
                    .firstOrNull { it.startsWith("since=") }
                    ?.substringAfter("=")?.toLongOrNull() ?: 0L
                val arr = JSONArray()
                Gate.since(since).forEach { e ->
                    arr.put(JSONObject().apply {
                        put("at", e.at); put("pkg", e.pkg); put("title", e.title); put("text", e.text)
                    })
                }
                200 to JSONObject().put("events", arr).toString()
            }

            clean == "/wake" && method == "POST" -> {
                val pm = ctx.getSystemService(android.os.PowerManager::class.java)
                val before = pm?.isInteractive == true
                val how = Waker.wake(ctx)
                android.os.SystemClock.sleep(if (before) 0 else 900)
                val after = pm?.isInteractive == true
                200 to JSONObject().apply {
                    put("ok", after)
                    put("strategy", how)
                    put("screenOn", after)
                }.toString()
            }

            clean == "/config" && method == "POST" -> {
                val csv = body.substringAfter("packages=", "").trim()
                if (csv.isNotEmpty()) Gate.setPackages(csv)
                200 to JSONObject().put("ok", true).put("packages", JSONArray(Gate.packages)).toString()
            }

            else -> 404 to JSONObject().put("ok", false).put("error", "no route").toString()
        }
    }
}

/** 通知使用权是否已授予。 */
object NotificationAccess {
    fun enabled(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        val expected = ComponentName(ctx, NotifyService::class.java)
        return flat.split(":").any { it.equals(expected.flattenToString(), ignoreCase = true) }
    }
}
