# libs/ — 本地 SDK JAR 目录

本目录存放无法通过 Maven 中央仓库获取的原生 SDK JAR 包。

## 声网 Agora RTC Server SDK

**当前文件**：`agora-linux-sdk-4.4.31.4.jar`（编译用 Stub）

项目默认包含一个 **Stub JAR**，仅提供类签名以保证编译通过。
要启用完整的声网 RTC 服务端功能，请用真实 SDK 替换此文件：

1. 访问 [声网 RTC Server SDK 下载页](https://doc.shengwang.cn/doc/rtc-server-sdk/java/resources)
2. 下载 Linux 版本 Java Server SDK（v4.4.31.4 或兼容版本）
3. 将下载的 JAR 文件重命名为 `agora-linux-sdk-4.4.31.4.jar` 并放置到此目录
4. 同时将 SDK 附带的 `.so` 原生库文件放到系统 `LD_LIBRARY_PATH` 可访问的路径

## 参考

- [声网官方文档](https://doc.shengwang.cn/doc/rtc-server-sdk/java/get-started/quick-start)
- [Skylark PAAS RTC 集成规格](../share/PAAS_RTC_INTEGRATION_SPEC.md)
