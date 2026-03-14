#!/bin/bash
set -euo pipefail

echo "╔════════════════════════════════════════╗"
echo "║  云雀 (Skylark) 语音助手启动脚本      ║"
echo "╚════════════════════════════════════════╝"

# 启动模式：local（默认）或 docker
MODE="${1:-local}"

# ===== 可通过环境变量覆盖的路径配置 =====
AGORA_NATIVE_DIR="${AGORA_NATIVE_DIR:-$(pwd)/native/agora/linux/x86_64}"
JAR_PATH="${JAR_PATH:-$(pwd)/target/skylark.jar}"
CONFIG_PATH="${CONFIG_PATH:-$(pwd)/src/main/resources/config/config.yaml}"

# ===== 加载 .env =====
if [ -f .env ]; then
    # 使用 set -a/source 安全加载，避免 xargs 对特殊字符的处理问题
    set -a
    # shellcheck source=.env
    source .env
    set +a
else
    echo "⚠️ 未找到 .env 文件，如需配置 API 密钥请先创建（可参考 .env.example）"
fi

mkdir -p tmp/asr tmp/tts tmp/vad logs models

# ===== 检查 Agora native .so 目录 =====
check_agora_native() {
    if [ ! -d "$AGORA_NATIVE_DIR" ]; then
        echo "⚠️  未找到 Agora native 目录: $AGORA_NATIVE_DIR"
        echo "   请创建该目录并放入 Agora Linux SDK 的所有 .so 文件"
        echo "   参考: native/README.md"
        return 1
    fi
    if ! ls "$AGORA_NATIVE_DIR"/*.so >/dev/null 2>&1; then
        echo "⚠️  Agora native 目录下未发现 .so 文件: $AGORA_NATIVE_DIR"
        echo "   请将 Agora Linux SDK 的 .so 文件放入该目录"
        echo "   参考: native/README.md"
        return 1
    fi
    echo "✅ Agora native 目录就绪: $AGORA_NATIVE_DIR"
    return 0
}

# ===== 本地 java -jar 启动模式 =====
if [ "$MODE" = "local" ]; then
    echo ""
    echo "🚀 本地启动模式 (java -jar)"
    echo ""

    # 检查 JAR
    if [ ! -f "$JAR_PATH" ]; then
        echo "❌ 未找到 JAR 文件: $JAR_PATH"
        echo "   请先执行: mvn clean package -DskipTests"
        exit 1
    fi

    # 检查配置文件
    if [ ! -f "$CONFIG_PATH" ]; then
        echo "❌ 未找到配置文件: $CONFIG_PATH"
        exit 1
    fi

    # 检查 Agora native .so，允许缺失（Java 侧会降级）
    JAVA_NATIVE_OPTS=""
    if check_agora_native; then
        export LD_LIBRARY_PATH="${AGORA_NATIVE_DIR}${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
        JAVA_NATIVE_OPTS="-Djava.library.path=${AGORA_NATIVE_DIR}"
        echo "   LD_LIBRARY_PATH 已设置: $LD_LIBRARY_PATH"
    else
        echo "ℹ️  将以「无 native .so」模式启动：Agora RTC join/send/recv 不可用，Token 仍可正常生成"
        echo "   若需要完整 RTC 能力，请参考 native/README.md 放置 .so 后重新启动"
    fi

    echo ""
    echo "JAR:    $JAR_PATH"
    echo "CONFIG: $CONFIG_PATH"
    [ -n "$JAVA_NATIVE_OPTS" ] && echo "JVM:    $JAVA_NATIVE_OPTS"
    echo ""
    echo "🚀 正在启动服务..."
    echo ""

    # 若要打开 JNI 加载调试，取消注释下行：
    # export LD_DEBUG=libs

    exec java ${JAVA_NATIVE_OPTS} -jar "$JAR_PATH" "$CONFIG_PATH"
fi

# ===== Docker Compose 启动模式 =====
if [ "$MODE" = "docker" ]; then
    echo ""
    echo "🚀 Docker Compose 启动模式"
    echo ""

    docker-compose up --build -d

    echo ""
    echo "⏳ 等待服务启动..."
    sleep 10

    echo ""
    echo "📊 服务状态检查:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if curl -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
        echo "✅ skylark (port 8080): 运行中"
    else
        echo "❌ skylark (port 8080): 未就绪（可执行 docker-compose logs 查看详情）"
    fi

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "✨ 启动完成！"
    echo ""
    echo "📝 访问地址:"
    echo "   - WebSocket: ws://localhost:8080/ws/audio"
    echo "   - Web客户端: 打开 web/index.html"
    echo ""
    echo "📖 查看日志: docker-compose logs -f"
    echo "🛑 停止服务: ./stop.sh"
    echo ""
    exit 0
fi

echo "❌ 未知启动模式: $MODE"
echo "   用法: ./start.sh [local|docker]"
echo "   local  - 直接在宿主机以 java -jar 运行（默认）"
echo "   docker - 使用 docker-compose 启动"
exit 1
