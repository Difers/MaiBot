# DSH Gate

一个只有两个功能的小 App：**读通知**（屏幕黑着也能读）和 **点亮屏幕**。
给手机上的自动化当桥用。所有接口只监听 `127.0.0.1:8747`，不联网、不上传。

## 为什么需要它

无障碍服务在锁屏时完全失明：读不到屏、点不了、连自己 App 都打不开。
这个 App 用系统允许的两条路绕过去：

| 能力 | 原理 | 锁屏时可用 |
|---|---|---|
| 读通知 | `NotificationListenerService` | ✅ |
| 点亮屏幕 | `setTurnScreenOn(true)` 的透明 Activity | ✅ |

它**不做任何操作**——不点按、不发送、不读屏幕内容。动作仍然交给 DSH 的无障碍服务，
所以「锁屏时不许偷偷操作」这条规矩没被破坏。

## 怎么拿到 APK（不用装任何开发环境）

1. 把 `dsh-gate/` 整个目录 push 到一个 GitHub 仓库
2. 仓库的 Actions 会自动跑（也可以手动点 Run workflow）
3. 跑完在 Actions 页面下载 `dsh-gate-debug` 这个 artifact，里面是 `app-debug.apk`
4. 传到手机安装（允许“安装未知来源应用”）

有电脑的话，Android Studio 打开这个目录直接 Run 也行。

## 装好之后

1. 打开 App → 点 **① 授予通知使用权** → 在系统列表里勾上 **DSH Gate**
2. 回到 App → 点 **② 启动本地桥**（会常驻一个低优先级通知）
3. 点 **③ 测试点亮屏幕**：手机会亮起来

如果 ③ 没反应，去系统设置给它开「后台弹出界面 / 显示在其他应用上层」——国产 ROM 常需要这个。

## 接口

```text
GET  /health                → { ok, listening, packages, port }
GET  /events?since=<毫秒>    → { events: [{ at, pkg, title, text }] }
POST /wake                  → { ok, strategy, screenOn }  点亮屏幕
POST /config  packages=a,b  → { ok, packages }
```

默认只收 `com.tencent.mobileqq` 的通知，别的应用要收就调 `/config`。

## 点亮屏幕试了三种策略

`Waker.kt` 依次尝试，哪个成功就返回哪个名字：

| 顺序 | 策略 | 说明 |
|---|---|---|
| 1 | `PowerManager.wakeUp()` | 官方 API，需要 DEVICE_POWER 签名权限，多数应用会被拒 |
| 2 | `WakeLock(FULL_WAKE_LOCK \| ACQUIRE_CAUSES_WAKEUP)` | 官方已废弃，但很多 ROM 仍然照做——**最可能成立的一条** |
| 3 | 透明 Activity + `setTurnScreenOn(true)` | 兜底 |

`POST /wake` 返回 `strategy` 字段，第一次跑就知道这台机器吃哪一套。
无密码锁屏下，`WakeActivity` 会顺带 `requestDismissKeyguard()` 把锁屏收起来；有密码时它什么也不做。

## 已知限制

- 群消息通知可能被 QQ 折叠成「N 条新消息」，拿不到全文
- 通知里的图片/表情只有占位，没有内容
- 点亮屏幕后如果设了密码，仍然需要人解锁（这台机器是无密码，所以够用）
- 部分 ROM 会杀后台，需要把 App 加进「自启动 / 后台运行」白名单

## 它不是什么

不是机器人、不接 QQ 协议、不碰账号密码。
它只把“屏幕上本来就会弹出的通知”交出来，别的什么都不做。
