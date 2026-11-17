package com.smartvoicechanger.app.inference

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.smartvoicechanger.app.NativeBridge
import org.json.JSONObject

enum class DeviceProfile {
    ENTRY,       // Low RAM (< 4GB) or <= 4 cores: small chunks, conservative threading
    STANDARD,    // 4GB - 8GB RAM: CPU multi-threaded / NNAPI eligible
    PERFORMANCE  // > 8GB RAM + verified GPU acceleration
}

data class VulkanCapabilities(
    val isSupported: Boolean,
    val deviceName: String,
    val apiVersion: String,
    val driverName: String,
    val hasComputeQueue: Boolean,
    val supportsFp16: Boolean,
    val subgroupSize: Int
)

data class DeviceCapabilities(
    val manufacturer: String,
    val model: String,
    val socModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val abiList: List<String>,
    val cpuCores: Int,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val isThermalThrottling: Boolean,
    val vulkanCapabilities: VulkanCapabilities,
    val nnapiAvailable: Boolean,
    val recommendedProfile: DeviceProfile
)

interface HardwareInspector {
    fun inspect(): DeviceCapabilities
}

class AndroidHardwareInspector(private val context: Context) : HardwareInspector {

    override fun inspect(): DeviceCapabilities {
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // RAM Inspection via ActivityManager
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem

        // Thermal State
        var isThrottling = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val thermalStatus = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            isThrottling = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        }

        // Native Vulkan Subsystem Inspection
        val vkJsonStr = NativeBridge.probeVulkanNative()
        val vkJson = try {
            JSONObject(vkJsonStr)
        } catch (_: Exception) {
            JSONObject()
        }

        val vkSupported = vkJson.optBoolean("is_available", false)
        val vkCaps = VulkanCapabilities(
            isSupported = vkSupported,
            deviceName = vkJson.optString("device_name", "Unknown"),
            apiVersion = if (vkSupported) "1.2.192" else "None",
            driverName = vkJson.optString("driver_name", "Unknown"),
            hasComputeQueue = vkJson.optBoolean("has_compute_queue", false),
            supportsFp16 = vkJson.optBoolean("supports_fp16", false),
            subgroupSize = vkJson.optInt("subgroup_size", 0)
        )

        // Device Profile Decision
        val totalRamGb = totalRam / (1024.0 * 1024.0 * 1024.0)
        val profile = when {
            totalRamGb < 3.8 || cpuCores <= 4 -> DeviceProfile.ENTRY
            totalRamGb >= 7.5 && vkCaps.isSupported && vkCaps.hasComputeQueue -> DeviceProfile.PERFORMANCE
            else -> DeviceProfile.STANDARD
        }

        return DeviceCapabilities(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            socModel = Build.SOC_MODEL.ifEmpty { Build.HARDWARE },
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            abiList = Build.SUPPORTED_ABIS.toList(),
            cpuCores = cpuCores,
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            isThermalThrottling = isThrottling,
            vulkanCapabilities = vkCaps,
            nnapiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1,
            recommendedProfile = profile
        )
    }
}
