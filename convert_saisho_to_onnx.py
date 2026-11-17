# @title 🚀 1-Click RVC to ONNX Converter: Saisho Utane
# @markdown Skrip ini mengunduh RVC WebUI exporter dan model resmi Saisho Utane (CC BY 4.0),
# @markdown lalu mengekspornya menjadi format ONNX mobile yang siap dipakai di SmartVoiceChanger.

import os
import subprocess

print("==> 1. Menginstal dependensi & cloning RVC Exporter...")
!git clone --depth 1 https://github.com/RVC-Project/Retrieval-based-Voice-Conversion-WebUI.git rvc_repo
%cd rvc_repo
!pip install -q onnx onnxruntime soundfile torchaudio

print("\n==> 2. Mengunduh model PyTorch Saisho Utane (57.6 MB)...")
model_url = "https://huggingface.co/Saisho-Utane/Saisho-Model-RVC/resolve/main/Saisho%20Utane/model.pth"
!mkdir -p weights
!wget -q -O weights/saisho_utane.pth "$model_url"

print("Model Saisho Utane berhasil diunduh. Memeriksa file...")
!ls -lh weights/saisho_utane.pth

print("\n==> 3. Mengekspor PyTorch (.pth) -> ONNX (.onnx)...")
import torch
from infer.lib.infer_pack.models_onnx import SynthesizerTrnMsNSFsidM

# Load checkpoint
cpt = torch.load("weights/saisho_utane.pth", map_location="cpu", weights_only=False)
target_sr = cpt.get("sr", "40k")
if isinstance(target_sr, int):
    target_sr = f"{target_sr // 1000}k"

version = cpt.get("version", "v2")
print(f"Versi model: {version}, Target Sample Rate: {target_sr}")

# Ekspor langsung menggunakan script resmi RVC
export_cmd = f"python infer/modules/onnx/export.py --model_path weights/saisho_utane.pth --output_path ../saisho_utane.onnx"
res = subprocess.run(export_cmd, shell=True, capture_output=True, text=True)

if os.path.exists("../saisho_utane.onnx"):
    print("\n✅ BERHASIL! Model saisho_utane.onnx telah dibuat:")
    !ls -lh ../saisho_utane.onnx
else:
    print("Mencoba alternatif ekspor fallback...")
    from infer.modules.onnx.export import export_onnx
    export_onnx("weights/saisho_utane.pth", "../saisho_utane.onnx")
    !ls -lh ../saisho_utane.onnx

print("\n==> 4. Mengunduh hasil saisho_utane.onnx ke komputer lokal...")
from google.colab import files
if os.path.exists("../saisho_utane.onnx"):
    files.download("../saisho_utane.onnx")
    print("Download popup telah muncul di browsermu!")
else:
    print("File output belum ditemukan.")
