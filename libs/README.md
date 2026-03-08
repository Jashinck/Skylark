# libs/ — 本地 SDK JAR 目录

本目录存放无法通过 Maven 中央仓库获取的原生 SDK JAR 包。

> **注意**：声网 Agora RTC Server SDK（`io.agora.rtc:linux-sdk`）已通过 Maven 中央仓库直接引入，
> 无需放置在此目录。详见 `pom.xml` 和 `share/PAAS_RTC_INTEGRATION_SPEC.md` §2.1。

## 阿里云 AliRTC Linux SDK（如需使用）

阿里云 ARTC Linux SDK 目前**不支持 Maven 中央仓库**，需手动下载后以本地 JAR 引入。

1. 联系阿里云商务申请 ARTC Linux SDK
2. 下载后将 JAR 文件放置到此目录
3. 在 `pom.xml` 中以 `system` scope 引入：
   ```xml
   <dependency>
       <groupId>com.aliyun.artc</groupId>
       <artifactId>alirtc-linux-sdk</artifactId>
       <version>2.0.0</version>
       <scope>system</scope>
       <systemPath>${project.basedir}/libs/alirtc-linux-sdk-2.0.0.jar</systemPath>
   </dependency>
   ```
4. 将 SDK 附带的 `.so` 原生库文件放到系统 `LD_LIBRARY_PATH` 可访问的路径

## 参考

- [声网官方文档](https://doc.shengwang.cn/doc/rtc-server-sdk/java/get-started/quick-start)
- [阿里云 ARTC SDK 下载](https://help.aliyun.com/zh/live/artc-download-the-sdk)
- [Skylark PAAS RTC 集成规格](../share/PAAS_RTC_INTEGRATION_SPEC.md)
