# CarryBot

Android 控制端（Java + Views）用于连接并操作 CarryBot 小车，支持：
- 设备添加/删除
- 多语言界面（法语/英语/中文）
- TTS 按钮播报
- 高对比度模式
- 方向与升降控制
- 实时视频流（MJPEG）

## 1. 技术栈

- Android SDK: `minSdk 24`, `targetSdk 36`, `compileSdk 36`
- 语言: Java 11
- UI: XML + View 系统（非 Compose）
- 主要依赖: AppCompat, Material, ConstraintLayout, RecyclerView

## 2. 页面流程

- `SplashActivity`：开场动画（默认重置为法语，TTS 默认关闭）
- `DeviceSelectActivity`：设备列表、语言切换、TTS 开关、对比度开关
- `ConnectActivity`：输入 IP 添加设备
- `MainActivity`：机器人控制页（状态、方向、急停、升降、视频）

## 3. 接口对接（当前 App 逻辑）

> 设备 IP 由用户输入，例如 `10.42.0.1`（App 内会自动补成 `http://<IP>`）。

当前代码默认按 `8080` 端口通信：

- 连接检测（任一成功即视为在线）
  - `GET /health`
  - `GET /status`
  - `GET /ping`
- 运动控制
  - `POST /drive`，Body: `{"action":"forward|backward|left|right|up|down"}`
  - `POST /stop`
- 视频流（开关 `Vidéo` 打开后）
  - `GET /video_feed`
  - 类型: `multipart/x-mixed-replace`（MJPEG）

如果你的机器人控制服务在 `8090`，可按需要调整：
- `MainActivity` 中 `CONTROL_PORT` 与 `CONTROL_PORT_CANDIDATES`

## 4. 本地运行

### Android Studio

1. 打开项目根目录：`carrybot3`
2. `Sync Gradle`
3. 选择设备（真机或模拟器）
4. 运行 `app`

### 命令行构建

```bash
./gradlew assembleDebug
```

## 5. TTS 说明

- App 启动后，TTS 默认是关闭状态，需要在设备选择页手动打开。
- 模拟器常见问题：法语/中文无声音，通常是系统未安装对应语音包。
- 在系统设置 `Text-to-speech output` 中安装语言语音数据后可恢复。

## 6. 关键目录

- 代码：`app/src/main/java/net/chezxinqi/carrybot3/`
- 布局：`app/src/main/res/layout/`
- 图片/形状资源：`app/src/main/res/drawable/`
- 清单：`app/src/main/AndroidManifest.xml`
- 接口说明：`README_APP_API.md`

## 7. 已知行为

- `DÉCONNECTER` 时会自动关闭视频开关并停止视频流。
- `Vidéo` 开启时控制模块会自动切换为紧凑布局；关闭后恢复常规布局。
- 添加重复 IP 时会提示：`Déjà ajouté : <设备名>`。

---
如需联调机器人端 API 细节，请先看项目内 `README_APP_API.md`。
