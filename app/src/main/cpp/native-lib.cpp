#include <jni.h>
#include <string>
#include <sstream>
#include "gpu_diagnostic_probe.h"
#include "dsp/audio_dsp_engine.h"

using namespace svc::diagnostics;
using namespace svc::dsp;

static std::string vulkanInfoToJson(const VulkanDeviceInfo& v) {
    std::ostringstream ss;
    ss << "{"
       << "\"is_available\":" << (v.is_available ? "true" : "false") << ","
       << "\"device_name\":\"" << v.device_name << "\","
       << "\"api_version\":" << v.api_version << ","
       << "\"driver_version\":" << v.driver_version << ","
       << "\"driver_name\":\"" << v.driver_name << "\","
       << "\"has_compute_queue\":" << (v.has_compute_queue ? "true" : "false") << ","
       << "\"supports_fp16\":" << (v.supports_fp16 ? "true" : "false") << ","
       << "\"subgroup_size\":" << v.subgroup_size << ","
       << "\"diagnostic_message\":\"" << v.diagnostic_message << "\""
       << "}";
    return ss.str();
}

static std::string openclInfoToJson(const OpenCLDeviceInfo& c) {
    std::ostringstream ss;
    ss << "{"
       << "\"is_available\":" << (c.is_available ? "true" : "false") << ","
       << "\"platform_name\":\"" << c.platform_name << "\","
       << "\"platform_vendor\":\"" << c.platform_vendor << "\","
       << "\"platform_version\":\"" << c.platform_version << "\","
       << "\"device_name\":\"" << c.device_name << "\","
       << "\"device_version\":\"" << c.device_version << "\","
       << "\"driver_version\":\"" << c.driver_version << "\","
       << "\"supports_fp16\":" << (c.supports_fp16 ? "true" : "false") << ","
       << "\"max_compute_units\":" << c.max_compute_units << ","
       << "\"max_work_group_size\":" << c.max_work_group_size << ","
       << "\"diagnostic_message\":\"" << c.diagnostic_message << "\""
       << "}";
    return ss.str();
}

static std::string benchmarkResultToJson(const ComputeBenchmarkResult& b) {
    std::ostringstream ss;
    ss << "{"
       << "\"success\":" << (b.success ? "true" : "false") << ","
       << "\"backend_name\":\"" << b.backend_name << "\","
       << "\"execution_time_ms\":" << b.execution_time_ms << ","
       << "\"max_absolute_error\":" << b.max_absolute_error << ","
       << "\"correctness_verified\":" << (b.correctness_verified ? "true" : "false") << ","
       << "\"status_message\":\"" << b.status_message << "\""
       << "}";
    return ss.str();
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_smartvoicechanger_app_NativeBridge_getNativeBuildInfo(
    JNIEnv* env,
    jobject /* this */) {
    std::string build_info = "Smart Voice Changer Native Engine (arm64-v8a, C++20, NDK 28)";
    return env->NewStringUTF(build_info.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_smartvoicechanger_app_NativeBridge_probeVulkanNative(
    JNIEnv* env,
    jobject /* this */) {
    VulkanDeviceInfo info = GpuDiagnosticProbe::probeVulkan();
    std::string json = vulkanInfoToJson(info);
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_smartvoicechanger_app_NativeBridge_probeOpenCLNative(
    JNIEnv* env,
    jobject /* this */) {
    OpenCLDeviceInfo info = GpuDiagnosticProbe::probeOpenCL();
    std::string json = openclInfoToJson(info);
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_smartvoicechanger_app_NativeBridge_runCpuReferenceBenchmarkNative(
    JNIEnv* env,
    jobject /* this */,
    jint element_count) {
    ComputeBenchmarkResult res = GpuDiagnosticProbe::runCpuReferenceBenchmark(static_cast<size_t>(element_count));
    std::string json = benchmarkResultToJson(res);
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_smartvoicechanger_app_NativeBridge_runOpenCLComputeBenchmarkNative(
    JNIEnv* env,
    jobject /* this */,
    jint element_count) {
    ComputeBenchmarkResult res = GpuDiagnosticProbe::runOpenCLComputeBenchmark(static_cast<size_t>(element_count));
    std::string json = benchmarkResultToJson(res);
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_smartvoicechanger_app_NativeBridge_runVulkanComputeBenchmarkNative(
    JNIEnv* env,
    jobject /* this */,
    jint element_count) {
    ComputeBenchmarkResult res = GpuDiagnosticProbe::runVulkanComputeBenchmark(static_cast<size_t>(element_count));
    std::string json = benchmarkResultToJson(res);
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jfloatArray JNICALL
Java_com_smartvoicechanger_app_NativeBridge_pitchShiftNative(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray samples_arr,
    jint sample_rate,
    jfloat semitones) {
    jsize len = env->GetArrayLength(samples_arr);
    jfloat* data = env->GetFloatArrayElements(samples_arr, nullptr);

    AudioPcm inPcm;
    inPcm.samples.assign(data, data + len);
    inPcm.sample_rate = static_cast<uint32_t>(sample_rate);
    inPcm.channels = 1;

    env->ReleaseFloatArrayElements(samples_arr, data, JNI_ABORT);

    AudioPcm outPcm = AudioDspEngine::pitchShift(inPcm, semitones);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(outPcm.samples.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(outPcm.samples.size()), outPcm.samples.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_smartvoicechanger_app_NativeBridge_resampleNative(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray samples_arr,
    jint src_rate,
    jint target_rate) {
    jsize len = env->GetArrayLength(samples_arr);
    jfloat* data = env->GetFloatArrayElements(samples_arr, nullptr);

    AudioPcm inPcm;
    inPcm.samples.assign(data, data + len);
    inPcm.sample_rate = static_cast<uint32_t>(src_rate);
    inPcm.channels = 1;

    env->ReleaseFloatArrayElements(samples_arr, data, JNI_ABORT);

    AudioPcm outPcm = AudioDspEngine::resample(inPcm, static_cast<uint32_t>(target_rate));

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(outPcm.samples.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(outPcm.samples.size()), outPcm.samples.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_smartvoicechanger_app_NativeBridge_computeMdxStftNative(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray samples_arr,
    jint n_fft,
    jint hop_length,
    jint dim_f,
    jint dim_t) {
    jsize len = env->GetArrayLength(samples_arr);
    jfloat* data = env->GetFloatArrayElements(samples_arr, nullptr);

    std::vector<float> planes = AudioDspEngine::computeMdxStft(
        data,
        static_cast<size_t>(len),
        n_fft,
        hop_length,
        dim_f,
        dim_t
    );

    env->ReleaseFloatArrayElements(samples_arr, data, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(planes.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(planes.size()), planes.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_smartvoicechanger_app_NativeBridge_computeMdxIstftNative(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray stft_planes_arr,
    jint total_output_samples,
    jint n_fft,
    jint hop_length,
    jint dim_f,
    jint dim_t) {
    jsize len = env->GetArrayLength(stft_planes_arr);
    jfloat* data = env->GetFloatArrayElements(stft_planes_arr, nullptr);

    std::vector<float> audio = AudioDspEngine::computeMdxIstft(
        data,
        static_cast<size_t>(total_output_samples),
        n_fft,
        hop_length,
        dim_f,
        dim_t
    );

    env->ReleaseFloatArrayElements(stft_planes_arr, data, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(audio.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(audio.size()), audio.data());
    return result;
}

}
