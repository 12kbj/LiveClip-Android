# 拾影 LiveClip（Android）

独立编写的抖音分享链接媒体保存实验项目。支持普通视频、图文原图，以及分享页提供的实况静态原图与短片。操作：粘贴或从抖音分享文本到本 App → 解析 → 预览 → 分项或批量保存到 Downloads/LiveClip。

## 当前状态

**预览版。** 已经在真机上验证部分普通视频与图文的解析和下载。实况视频需要抖音作品详情返回动态地址；分享页通常只提供静态图片。若界面只显示静态图，可点“打开抖音网页登录”，在抖音官网完成登录后返回并重新解析。网页登录不保证所有实况作品都可获取 MP4；私密、删除、地区限制或平台验证仍可能导致失败。应用不会在输入框收集密码。

未使用 InvertGeek/TikDown 的源码、资源或构建产物。部分详情接口与实况 URI 处理参考了 MIT 授权的 ucmao/media-parser，详见 `THIRD_PARTY_NOTICES.md`。请只保存有权保存的内容。

## 构建

用 Android Studio 打开仓库根目录，JDK 17、Android SDK 35，Gradle 8.11.1，执行 `gradle :app:assembleDebug`。GitHub Actions 的 `Build Android APK` 在 main 推送或手动触发后构建调试 APK，从 Actions 的 artifact 下载。

下载内容放在系统 Downloads/LiveClip。动态照片分别保存原图和 MP4，不保证系统相册将其合成 Live Photo。当前 GitHub Release 提供的是调试签名 APK；不同构建间签名可能不同，更新安装若提示签名不一致需要先卸载旧版。

## 维护

解析逻辑集中在 `app/src/main/java/com/example/liveclip/MediaParser.java`，界面与保存操作在 `MainActivity.java`。出现分享页格式变化时先更新解析与样本测试。不得提交私人的抖音 Cookie 或作品链接。
