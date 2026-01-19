from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import edge_tts
import asyncio
import tempfile
import uuid
import os
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

DEFAULT_VOICE = os.getenv("TTS_VOICE", "zh-CN-XiaoxiaoNeural")

logger.info(f"✅ TTS服务初始化完成，默认语音: {DEFAULT_VOICE}")

async def generate_tts(text: str, voice: str) -> str:
    temp_dir = tempfile.gettempdir()
    output_file = os.path.join(temp_dir, f"tts_{uuid.uuid4().hex}.wav")
    
    communicate = edge_tts.Communicate(text, voice)
    await communicate.save(output_file)
    
    return output_file

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy", "service": "tts"})

@app.route('/tts', methods=['POST'])
def text_to_speech():
    try:
        data = request.get_json()
        
        if 'text' not in data:
            return jsonify({"error": "缺少text字段"}), 400
        
        text = data['text']
        voice = data.get('voice', DEFAULT_VOICE)
        
        if not text.strip():
            return jsonify({"error": "文本为空"}), 400
        
        logger.info(f"TTS请求: {text[:50]}... (voice: {voice})")
        
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        audio_file = loop.run_until_complete(generate_tts(text, voice))
        loop.close()
        
        logger.info(f"TTS生成完成: {audio_file}")
        
        response = send_file(
            audio_file,
            mimetype='audio/wav',
            as_attachment=False,
            download_name=f"tts_{uuid.uuid4().hex}.wav"
        )
        
        @response.call_on_close
        def cleanup():
            try:
                os.remove(audio_file)
            except:
                pass
        
        return response
        
    except Exception as e:
        logger.error(f"TTS生成错误: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8003, debug=False)
