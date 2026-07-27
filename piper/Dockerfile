FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

RUN mkdir -p /app/voices \
    && wget -q -O /app/voices/fr_FR-siwis-medium.onnx \
       https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium.onnx \
    && wget -q -O /app/voices/fr_FR-siwis-medium.onnx.json \
       https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium.onnx.json

COPY server.py .

EXPOSE 5000
CMD ["python", "server.py"]
