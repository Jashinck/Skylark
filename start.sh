#!/bin/bash

echo "╔════════════════════════════════════════╗"
echo "║  百聆 (Bailing) 语音助手启动脚本      ║"
echo "╚════════════════════════════════════════╝"

if [ ! -f .env ]; then
    echo "⚠️ 未找到.env文件，从模板创建..."
    cp .env.example .env
    echo "请编辑.env文件，填入API密钥"
    exit 1
fi

export $(cat .env | grep -v '^#' | xargs)

mkdir -p tmp/asr tmp/tts tmp/vad logs models config

echo ""
echo "🚀 启动服务..."
echo ""

docker-compose up --build -d

echo ""
echo "⏳ 等待服务启动..."
sleep 10

echo ""
echo "📊 服务状态检查:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

services=("asr:8001" "vad:8002" "tts:8003" "java:8080")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    
    if [ "$name" == "java" ]; then
        url="http://localhost:$port/actuator/health"
    else
        url="http://localhost:$port/health"
    fi
    
    if curl -s "$url" > /dev/null 2>&1; then
        echo "✅ $name-service (port $port): 运行中"
    else
        echo "❌ $name-service (port $port): 未就绪"
    fi
done

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
