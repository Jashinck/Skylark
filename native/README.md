# Native 库目录说明

本目录用于存放第三方 SDK 提供的 native 动态链接库（`.so` 文件）。

> ⚠️ **重要**：所有 `.so` 二进制文件均已加入 `.gitignore`，**不要将其提交到仓库**。

---

## Agora Linux Java Server SDK（x86_64）

### 1. 放置路径

```
native/agora/linux/x86_64/
```

将 Agora Linux SDK 压缩包中的**所有 `.so` 文件**（包括依赖库）解压到此目录。

### 2. 获取 SDK

从声网官网下载 Agora Linux Java Server SDK：

- 官方下载地址：https://doc.shengwang.cn/doc/rtc-server-sdk/java/resources
- Maven 坐标：`io.agora.rtc:linux-java-sdk`（JAR 内含 `.so`，可从 JAR 中提取）

### 3. 需要放置的文件

Agora SDK 压缩包通常包含以下类型的 `.so` 文件（以版本 4.x 为例）：

```
libagora_rtc_sdk.so          # 主 SDK 库
libagora_fdkaac.so           # AAC 编解码
libagora_ffmpeg.so           # 音视频处理
libagora_soundtouch.so       # 音效处理
libagora_ai_echo_cancellation.so  # AI 回声消除
libagora_uap_aed.so          # 音频增强
... 及其他依赖 .so
```

> ⚠️ **必须放置所有 `.so`**，缺少任何一个依赖都会导致 `dlopen` 在二级依赖处失败。

### 4. 设置文件权限

```bash
chmod 755 native/agora/linux/x86_64/*.so
```

### 5. 启动服务

放置好 `.so` 后，使用以下命令启动（本地 `java -jar` 模式）：

```bash
./start.sh local
```

脚本会自动：
- 检测 `.so` 目录是否存在
- 设置 `LD_LIBRARY_PATH=native/agora/linux/x86_64`
- 添加 JVM 参数 `-Djava.library.path=native/agora/linux/x86_64`

若 `.so` 不存在，服务仍可启动，但 Agora RTC 功能（join/send/recv）将降级为 no-op，Token 生成仍正常。

---

## 排查指南

### 验证 .so 文件是否齐全

```bash
ldd native/agora/linux/x86_64/*.so | grep "not found"
```

若有输出，说明缺少系统运行库（如 `libstdc++`、`libgcc_s` 等），需安装对应系统包。

### 打开动态链接器加载日志（最精准的排查方式）

```bash
LD_DEBUG=libs ./start.sh local 2>&1 | grep agora
```

### 验证 JVM 是否找到 .so

在服务启动日志中搜索：

```
[AgoraClientAdapterImpl] Agora native .so libraries loaded successfully
```

若出现 `UnsatisfiedLinkError`，说明 `.so` 未被正确加载，请按以下步骤排查：

1. 确认 `LD_LIBRARY_PATH` 包含 `.so` 目录（`./start.sh local` 启动时会打印）
2. 运行 `ldd` 检查依赖是否齐全
3. 确认系统架构为 `x86_64`（`uname -m`）
4. 确认 glibc 版本兼容（`ldd --version`）

### 常见问题

| 错误信息 | 原因 | 解决方法 |
|---------|------|---------|
| `cannot open shared object file: No such file or directory` | `.so` 路径未加入 `LD_LIBRARY_PATH` | 使用 `./start.sh local` 启动 |
| `version GLIBC_2.xx not found` | glibc 版本不匹配 | 升级系统或使用兼容版本的 SDK |
| `wrong ELF class: ELFCLASS32` | 使用了 32-bit `.so` | 确保使用 x86_64 版本的 SDK |
| `undefined symbol: xxx` | 缺少某个依赖 `.so` | 将所有 SDK `.so` 都放入目录 |
