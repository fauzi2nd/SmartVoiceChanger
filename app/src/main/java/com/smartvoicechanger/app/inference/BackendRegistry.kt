package com.smartvoicechanger.app.inference

class BackendRegistry(private val capabilities: DeviceCapabilities) {

    private val backends = mutableMapOf<BackendType, InferenceBackend>()

    init {
        registerDefaultBackends()
    }

    private fun registerDefaultBackends() {
        // 1. Mandatory CPU Backend (Auto-thread allocation based on CPU cores)
        val optimalThreads = when {
            capabilities.cpuCores >= 8 -> 4
            capabilities.cpuCores >= 4 -> 3
            else -> 2
        }
        backends[BackendType.CPU] = CpuBackend(threadCount = optimalThreads)

        // 2. NNAPI Backend (if OS level supports it and device is not entry-level)
        if (capabilities.nnapiAvailable) {
            backends[BackendType.NNAPI] = NnapiBackend()
        }

        // 3. Vulkan Backend (if Vulkan compute queue is confirmed)
        if (capabilities.vulkanCapabilities.isSupported && capabilities.vulkanCapabilities.hasComputeQueue) {
            backends[BackendType.VULKAN] = VulkanBackend(
                hasComputeQueue = capabilities.vulkanCapabilities.hasComputeQueue,
                supportsFp16 = capabilities.vulkanCapabilities.supportsFp16
            )
        }
    }

    fun getBackend(type: BackendType): InferenceBackend? = backends[type]

    fun getAllBackends(): List<InferenceBackend> = backends.values.toList()

    fun getPrioritizedBackends(): List<InferenceBackend> {
        // Priority order: Vulkan (when fully ready) -> NNAPI -> CPU (always guaranteed fallback)
        val list = mutableListOf<InferenceBackend>()
        backends[BackendType.VULKAN]?.let { list.add(it) }
        backends[BackendType.NNAPI]?.let { list.add(it) }
        backends[BackendType.CPU]?.let { list.add(it) }
        return list
    }
}
