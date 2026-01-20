from flask import Flask, request, jsonify
from flask_cors import CORS
from funasr import AutoModel
from funasr.utils.postprocess_utils import rich_transcription_postprocess
import os
import logging
import tempfile
import uuid

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

MODEL_DIR = os.getenv("MODEL_DIR", "iic/SenseVoiceSmall")
DEVICE = os.getenv("DEVICE", "cpu")

logger.info(f"正在加载ASR模型: {MODEL_DIR}")
model = AutoModel(
    model=MODEL_DIR,
    vad_kwargs={"max_single_segment_time": 30000},
    disable_update=True,
    hub="hf",
    device=DEVICE
)
logger.info("✅ ASR模型加载完成")

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy", "service": "asr"})

@app.route('/recognize', methods=['POST'])
def recognize():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "没有上传文件"}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({"error": "文件名为空"}), 400
        
        temp_dir = tempfile.gettempdir()
        temp_file = os.path.join(temp_dir, f"asr_{uuid.uuid4().hex}.wav")
        file.save(temp_file)
        
        logger.info(f"处理音频文件: {temp_file}")
        
        res = model.generate(
            input=temp_file,
            cache={},
            language="auto",
            use_itn=True,
            batch_size_s=60,
        )
        
        text = rich_transcription_postprocess(res[0]["text"])
        
        try:
            os.remove(temp_file)
        except:
            pass
        
        logger.info(f"识别结果: {text}")
        
        return jsonify({
            "text": text,
            "language": res[0].get("language", "unknown")
        })
        
    except Exception as e:
        logger.error(f"ASR识别错误: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8001, debug=False)
