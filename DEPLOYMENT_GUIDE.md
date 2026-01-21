# 纯Java部署指南 (Pure Java Deployment Guide)

## 目标 (Objective)

本指南说明如何部署纯Java版本的Voice Agent系统，无需Python依赖。

This guide explains how to deploy the pure Java version of the Voice Agent system without Python dependencies.

## 系统要求 (System Requirements)

- Java 17 或更高版本
- Maven 3.6 或更高版本
- 4GB RAM (最小)
- 10GB 磁盘空间

## 部署步骤 (Deployment Steps)

### 1. 编译Java服务 (Build Java Service)

```bash
cd java-service
mvn clean package -DskipTests
```

构建成功后，将在 `target/` 目录下生成 `bailing-java.jar` 文件。

After successful build, the `bailing-java.jar` file will be generated in the `target/` directory.

### 2. 配置服务 (Configure Services)

使用纯Java配置文件：

Use the pure Java configuration file:

```bash
cp config/config-java-only.yaml config/my-config.yaml
```

编辑配置文件，设置API密钥：

Edit the configuration file to set API keys:

```yaml
llm:
  api_key: your-api-key-here
```

### 3. 启动服务 (Start Service)

```bash
java -jar java-service/target/bailing-java.jar config/my-config.yaml
```

服务将在以下端口启动：
- 主服务端口: 8080
- ASR API: http://localhost:8080/asr
- TTS API: http://localhost:8080/tts
- VAD API: http://localhost:8080/vad

### 4. 验证服务 (Verify Services)

检查所有服务是否正常运行：

Check if all services are running properly:

```bash
# ASR健康检查
curl http://localhost:8080/asr/health

# TTS健康检查
curl http://localhost:8080/tts/health

# VAD健康检查
curl http://localhost:8080/vad/health
```

预期响应：

Expected response:

```json
{"status":"healthy","service":"asr"}
{"status":"healthy","service":"tts"}
{"status":"healthy","service":"vad"}
```

### 5. 测试功能 (Test Functionality)

#### 测试TTS (Test TTS)

```bash
curl -X POST http://localhost:8080/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"你好，世界"}' \
  --output test.wav
```

#### 测试VAD (Test VAD)

```bash
curl -X POST http://localhost:8080/vad \
  -H "Content-Type: application/json" \
  -d '{"audio_data":"AAAAAAAAAA==","session_id":"test"}'
```

#### 查询可用语音 (List Available Voices)

```bash
curl http://localhost:8080/tts/voices
```

## 生产环境部署 (Production Deployment)

### 使用systemd服务 (Using systemd service)

创建服务文件 `/etc/systemd/system/voice-agent.service`:

```ini
[Unit]
Description=Voice Agent Service
After=network.target

[Service]
Type=simple
User=voice-agent
WorkingDirectory=/opt/voice-agent
ExecStart=/usr/bin/java -jar /opt/voice-agent/java-service/target/bailing-java.jar /opt/voice-agent/config/config-java-only.yaml
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=voice-agent

[Install]
WantedBy=multi-user.target
```

启用并启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable voice-agent
sudo systemctl start voice-agent
sudo systemctl status voice-agent
```

### 使用Docker部署 (Docker Deployment)

创建 `Dockerfile-java-only`:

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

# 复制构建产物
COPY java-service/target/bailing-java.jar /app/
COPY config/ /app/config/

# 创建必要的目录
RUN mkdir -p /app/tmp /app/logs

# 暴露端口
EXPOSE 8080

# 启动服务
CMD ["java", "-jar", "bailing-java.jar", "config/config-java-only.yaml"]
```

构建并运行：

```bash
# 构建镜像
docker build -f Dockerfile-java-only -t voice-agent-java:latest .

# 运行容器
docker run -d \
  --name voice-agent \
  -p 8080:8080 \
  -e DEEPSEEK_API_KEY=your-api-key \
  -v $(pwd)/config:/app/config \
  -v $(pwd)/tmp:/app/tmp \
  -v $(pwd)/logs:/app/logs \
  voice-agent-java:latest
```

## 性能优化 (Performance Optimization)

### JVM参数调优 (JVM Tuning)

```bash
java -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heap_dump.hprof \
  -jar bailing-java.jar config/config-java-only.yaml
```

### 配置文件优化 (Configuration Optimization)

```yaml
# 调整线程池大小
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10

# 调整超时时间
asr:
  timeout: 60  # 秒

tts:
  timeout: 120  # 秒

vad:
  timeout: 30  # 秒
```

## 监控和日志 (Monitoring and Logging)

### 日志配置 (Logging Configuration)

在 `application.yml` 中配置日志：

```yaml
logging:
  level:
    root: INFO
    com.bailing: DEBUG
  file:
    name: logs/voice-agent.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 健康检查端点 (Health Check Endpoints)

```bash
# 检查ASR服务
curl http://localhost:8080/asr/health

# 检查TTS服务
curl http://localhost:8080/tts/health

# 检查VAD服务
curl http://localhost:8080/vad/health
```

## 故障排查 (Troubleshooting)

### 服务无法启动 (Service Won't Start)

1. 检查Java版本：`java -version`
2. 检查端口是否被占用：`netstat -tlnp | grep 8080`
3. 查看日志：`tail -f logs/voice-agent.log`

### API调用失败 (API Calls Failing)

1. 确认服务正在运行：`curl http://localhost:8080/asr/health`
2. 检查请求格式是否正确
3. 查看服务日志获取详细错误信息

### 性能问题 (Performance Issues)

1. 增加JVM堆内存：`-Xmx4g`
2. 调整线程池大小
3. 检查磁盘IO和网络延迟

## 下一步集成 (Next Steps for Integration)

当前实现使用占位符代码。要实现完整功能，需要集成以下库：

The current implementation uses placeholder code. For full functionality, integrate these libraries:

### ASR集成选项 (ASR Integration Options)

1. **Vosk** - 离线语音识别
   ```xml
   <dependency>
       <groupId>com.alphacephei</groupId>
       <artifactId>vosk</artifactId>
       <version>0.3.45</version>
   </dependency>
   ```

2. **Google Cloud Speech**
   ```xml
   <dependency>
       <groupId>com.google.cloud</groupId>
       <artifactId>google-cloud-speech</artifactId>
       <version>4.25.0</version>
   </dependency>
   ```

### TTS集成选项 (TTS Integration Options)

1. **MaryTTS**
   ```xml
   <dependency>
       <groupId>de.dfki.mary</groupId>
       <artifactId>marytts</artifactId>
       <version>5.2</version>
   </dependency>
   ```

2. **Google Cloud TTS**
   ```xml
   <dependency>
       <groupId>com.google.cloud</groupId>
       <artifactId>google-cloud-texttospeech</artifactId>
       <version>2.32.0</version>
   </dependency>
   ```

### VAD集成选项 (VAD Integration Options)

1. **ONNX Runtime** (用于Silero VAD)
   ```xml
   <dependency>
       <groupId>com.microsoft.onnxruntime</groupId>
       <artifactId>onnxruntime</artifactId>
       <version>1.16.3</version>
   </dependency>
   ```

## 支持和反馈 (Support and Feedback)

如有问题或建议，请在GitHub上提交issue。

For questions or suggestions, please submit an issue on GitHub.
