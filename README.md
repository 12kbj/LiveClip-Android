# 拾影 LiveClip（Android）

独立编写的抖音分享链接媒体保存实验项目。支持普通视频、图文原图，以及分享页提供的实况静态原图与短片。操作：粘贴或从抖音分享文本到本 App → 解析 → 预览 → 分项或批量保存到 Downloads/LiveClip。

## 当前状态

**预览版，未经真机与真实抖音链接验证。** 抖音分享页可能因改版、验证码、登录、地区或反爬措施无法提供可解析数据。浏览器模式也无法保证能获取作品数据。实况短片不一定暴露在分享页，缺少地址时只会显示静态图片。本项目不声称稳定支持所有最新版抖音作品。

未使用 InvertGeek/TikDown 的源码、资源或构建产物。交互流程仅参考常见的粘贴、解析、保存操作。请只保存有权保存的内容。

## 构建

用 Android Studio 打开仓库根目录，JDK 17、Android SDK 35，Gradle 8.11.1，执行 `gradle :app:assembleDebug`。GitHub Actions 的 `Build Android APK` 在 main 推送或手动触发后构建调试 APK，从 Actions 的 artifact 下载。

调试 APK 有效期有限，不等于正式签名版；需要实际安装和验证视频、图文、实况后再考虑建立 Release。下载内容放在系统 Downloads/LiveClip。动态照片分别保存原图和 MP4，不保证系统相册将其合成 Live Photo。

## 维护

解析逻辑集中在 `app/src/main/java/com/example/liveclip/MediaParser.java`，界面与保存操作在 `MainActivity.java`。出现分享页格式变化时先更新解析与样本测试。不得提交私人的抖音 Cookie 或作品链接。
