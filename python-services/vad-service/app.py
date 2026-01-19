from flask import Flask, request, jsonify
from flask_cors import CORS
import torch
import numpy as np
from silero_vad import load_silero_vad, VADIterator
import base64
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

logger.info("正在加载VAD模型")
vad_model = load_silero_vad()

SAMPLING_RATE = 16000
THRESHOLD = 0.5
MIN_SILENCE_DURATION_MS = 500

vad_iterators = {}

logger.info("✅ VAD模型加载完成")

def get_vad_iterator(session_id="default"):
    if session_id not in vad_iterators:
        vad_iterators[session_id] = VADIterator(
            vad_model,
            threshold=THRESHOLD,
            sampling_rate=SAMPLING_RATE,
            min_silence_duration_ms=MIN_SILENCE_DURATION_MS
        )
    return vad_iterators[session_id]

def int2float(sound):
    sound = sound.astype(np.float32) / 32768.0
    return sound

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy", "service": "vad"})

@app.route('/vad', methods=['POST'])
def vad_detect():
    try:
        data = request.get_json()
        
        if 'audio_data' not in data:
            return jsonify({"error": "缺少audio_data字段"}), 400
        
        audio_bytes = base64.b64decode(data['audio_data'])
        audio_int16 = np.frombuffer(audio_bytes, dtype=np.int16)
        audio_float32 = int2float(audio_int16)
        
        session_id = data.get('session_id', 'default')
        vad_iterator = get_vad_iterator(session_id)
        
        vad_output = vad_iterator(torch.from_numpy(audio_float32))
        
        if vad_output is not None:
            if 'start' in vad_output:
                return jsonify({"status": "start", "timestamp": vad_output['start']})
            elif 'end' in vad_output:
                return jsonify({"status": "end", "timestamp": vad_output['end']})
        
        return jsonify({"status": None})
        
    except Exception as e:
        logger.error(f"VAD检测错误: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/reset', methods=['POST'])
def reset_vad():
    try:
        data = request.get_json() or {}
        session_id = data.get('session_id', 'default')
        
        if session_id in vad_iterators:
            vad_iterators[session_id].reset_states()
            logger.info(f"VAD状态已重置: {session_id}")
        
        return jsonify({"status": "reset"})
        
    except Exception as e:
        logger.error(f"VAD重置错误: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8002, debug=False)
