# 拾影 LiveClip（Android）

## 📲 安卓版下载

**[点击下载最新安装包：拾影 v0.3.0 APK](https://github.com/12kbj/LiveClip-Android/releases/download/v0.3.0/LiveClip-v0.3.0-debug.apk)**

若点击后没有开始下载，请打开 [v0.3.0 发布页面](https://github.com/12kbj/LiveClip-Android/releases/tag/v0.3.0)，展开 **Assets**，选择 `LiveClip-v0.3.0-debug.apk`。APK 仅适用于安卓手机，iPhone 无法安装。该版本为预览版，实况 MP4 功能仍需按具体作品验证。

独立编写的抖音分享链接媒体保存实验项目。支持普通视频、图文原图，以及作品详情提供的实况静态原图与短片。打开 App 时若前台剪贴板有有效抖音分享链接，会自动填入并解析；也可手动粘贴。解析后可分别保存原图与动态 MP4 到 Downloads/LiveClip。

## 当前状态

**预览版。** 已经在真机上验证部分普通视频与图文的解析和下载。实况视频需要抖音作品详情返回动态地址；分享页通常只提供静态图片。若界面只显示静态图，可点“打开抖音网页登录”，在抖音官网完成登录后返回并重新解析。网页登录不保证所有实况作品都可获取 MP4；私密、删除、地区限制或平台验证仍可能导致失败。应用不会在输入框收集密码。

v0.3.0：图文和实况作品优先查询完整详情中的动态视频，失败后回退分享页或移动端数据。额外兼容更多动态视频字段及 `modal_id` 分享链接。详情请求失败时显示原因，避免把静态原图误称为 MP4。前台首次打开或返回时识别新复制的抖音链接，同一链接不重复自动解析；Android 10+ 的系统剪贴板限制要求应用在前台有焦点。仅有静态帧而没有上游动态视频时无法还原原始实况。

未使用 InvertGeek/TikDown 的源码、资源或构建产物。部分详情接口与实况 URI 处理参考了 MIT 授权的 ucmao/media-parser，详见 `THIRD_PARTY_NOTICES.md`。请只保存有权保存的内容。

## 构建

用 Android Studio 打开仓库根目录，JDK 17、Android SDK 35，Gradle 8.11.1，执行 `gradle :app:assembleDebug`。GitHub Actions 的 `Build Android APK` 在 main 推送或手动触发后构建调试 APK，从 Actions 的 artifact 下载。

下载内容放在系统 Downloads/LiveClip。动态照片分别保存原图和 MP4，不保证系统相册将其合成 Live Photo。当前 GitHub Release 提供的是调试签名 APK；不同构建间签名可能不同，更新安装若提示签名不一致需要先卸载旧版。

## 维护

解析逻辑集中在 `app/src/main/java/com/example/liveclip/MediaParser.java`，界面与保存操作在 `MainActivity.java`。出现分享页格式变化时先更新解析与样本测试。不得提交私人的抖音 Cookie 或作品链接。
