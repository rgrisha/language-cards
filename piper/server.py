import os
import subprocess
import tempfile

from flask import Flask, after_this_request, jsonify, request, send_file

MODEL_PATH = os.environ.get("PIPER_MODEL", "/app/voices/fr_FR-siwis-medium.onnx")

app = Flask(__name__)


@app.route("/synthesize", methods=["POST"])
def synthesize():
    data = request.get_json(force=True)
    text = (data or {}).get("text", "").strip()
    if not text:
        return jsonify({"error": "text is required"}), 400

    fd, output_path = tempfile.mkstemp(suffix=".wav")
    os.close(fd)

    try:
        subprocess.run(
            ["piper", "--model", MODEL_PATH, "--output_file", output_path],
            input=text.encode("utf-8"),
            check=True,
            capture_output=True,
        )
    except subprocess.CalledProcessError as e:
        os.remove(output_path)
        return jsonify({"error": e.stderr.decode("utf-8", errors="replace")}), 500

    @after_this_request
    def cleanup(response):
        try:
            os.remove(output_path)
        except OSError:
            pass
        return response

    return send_file(output_path, mimetype="audio/wav")


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
