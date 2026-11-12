# 怎么把它变成 APK

这台手机上没有 JDK 和 Android SDK，编译不了。任选一条路。

## A. 用 GitHub Actions（推荐，零安装）

1. 在 GitHub 新建一个空仓库，比如 `dsh-gate`
2. 把本目录（`dsh-gate/` 里的全部内容）作为仓库根目录推上去：

```bash
cd dsh-gate
git init
git add .
git commit -m "init"
git branch -M main
git remote add origin https://github.com/<你的用户名>/dsh-gate.git
git push -u origin main
```

3. 仓库页面 → Actions → 等 `build` 跑完（约 3 分钟）
4. 点进那次运行 → 页面底部 `Artifacts` → 下载 `dsh-gate-debug`
5. 解压得到 `app-debug.apk`，传到手机安装

## B. 用 Windows 电脑（Android Studio）

1. 安装 Android Studio（自带 JDK 和 SDK）
2. `File → Open`，选 `dsh-gate` 目录
3. 等 Gradle 同步完
4. `Build → Build Bundle(s)/APK(s) → Build APK(s)`
5. 产物在 `app/build/outputs/apk/debug/app-debug.apk`

## 装好之后

```
① 授予通知使用权   → 系统列表勾上 DSH Gate
② 启动本地桥       → 常驻通知出现
③ 测试点亮屏幕     → 手机会亮
```

③ 成功的话，`POST /wake` 会返回 `strategy` 字段，告诉你用的是哪条：

- `wakeUp` —— 官方 API 生效（少见）
- `wakelock` —— **最可能**，ACQUIRE_CAUSES_WAKEUP 生效
- `activity` —— 靠透明 Activity 点亮
- `failed` —— 三种都不行，这台机器只能走常亮或协议侧

## 如果 ③ 没反应

去系统设置给 DSH Gate 打开：

- 后台弹出界面 / 显示在其他应用上层
- 自启动
- 省电策略改为「无限制」
