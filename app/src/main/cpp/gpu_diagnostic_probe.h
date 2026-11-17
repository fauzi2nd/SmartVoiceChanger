#pragma once

#include <string>

namespace svc::diagnostics {

struct VulkanDeviceInfo {
    bool is_available{false};
    std::string device_name;
    uint32_t api_version{0};
    uint32_t driver_version{0};
    std::string driver_name;
    std::string driver_info;
    bool has_compute_queue{false};
    bool supports_fp16{false};
    uint32_t subgroup_size{0};
    std::string diagnostic_message;
};

struct OpenCLDeviceInfo {
    bool is_available{false};
    std::string platform_name;
    std::string platform_vendor;
    std::string platform_version;
    std::string device_name;
    std::string device_version;
    std::string driver_version;
    bool supports_fp16{false};
    uint32_t max_compute_units{0};
    uint64_t max_work_group_size{0};
    std::string diagnostic_message;
};

struct ComputeBenchmarkResult {
    bool success{false};
    std::string backend_name;
    double execution_time_ms{0.0};
    float max_absolute_error{0.0f};
    bool correctness_verified{false};
    std::string status_message;
};

class GpuDiagnosticProbe {
public:
    static VulkanDeviceInfo probeVulkan();
    static OpenCLDeviceInfo probeOpenCL();
    static ComputeBenchmarkResult runVulkanComputeBenchmark(size_t element_count);
    static ComputeBenchmarkResult runOpenCLComputeBenchmark(size_t element_count);
    static ComputeBenchmarkResult runCpuReferenceBenchmark(size_t element_count);
};

} // namespace svc::diagnostics
