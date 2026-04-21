package com.fcl.plugin.mobileglues.settings

import android.content.Context
import com.fcl.plugin.mobileglues.utils.Constants
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MGConfig private constructor(val context: Context, private var isInitializing: Boolean) {

    constructor(context: Context) : this(context, false)

    var enableANGLE: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var enableNoError: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var enableExtTimerQuery: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var enableExtComputeShader: Int = 1  // VinzzFix: ON untuk Iris compute shaders
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var enableExtDirectStateAccess: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var maxGlslCacheSize: Int = 32
        set(value) {
            if (field != value) {
                field = value
                if (value == -1) clearCacheFile()
                saveIfReady()
            }
        }
    var multidrawMode: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var angleDepthClearFixMode: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var customGLVersion: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var fsr1Setting: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }

    // ===== VinzzRenderer Optimizations =====
    var vinzzNoThrottle: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzFastHints: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzDisableDither: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzSkipSmallDraws: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzStateCache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzFboCache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzSmartInvalidate: Int = 0  // VinzzFix: OFF default
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzColorInvalidate: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzQcomTiling: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzMultidrawSodium: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzShaderCacheAggressive: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzFencePool: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzDisjointTimerOff: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzSodiumMode: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzTexCache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzPersistentVbo: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzIndexReuse: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzBatchUniforms: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzEarlyZ: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzLrz: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVertexMediump: Int = 1  // FIX: was vinzzVertexMediaump (typo)
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzInvariantStrip: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzPreciseStrip: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzFp16Varyings: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    // ===== VulkanMod Mode =====
    var vinzzVulkanMode: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanLwjglPatch: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanAsyncCompute: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanVmaDefrag: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanDisableValidation: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanMemoryBudget: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanSpirvOpt: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzVulkanFrameOverlap: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    // ===== VinzzRenderer New 5 Features =====
    var vinzzBufferStreaming: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzCpuPreprep: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzDenoiser: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzAsyncShader: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzPipelineCache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    // ===== Shader Protection =====
    var vinzzShaderComplexityGate: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzComputeProtect: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }

    // ===== Distant Horizons Support =====
    var vinzzDistantHorizonsSupport: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }

    var vinzzGlslPragmaOpt: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzReducePrecision: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzMediumpFragment: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzAstcPrefer: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzAnisotropicLevel: Int = 4
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzzMipBiasX10: Int = -5
        set(value) { if (field != value) { field = value; saveIfReady() } }

    private fun saveIfReady() {
        if (!isInitializing) save()
    }

    fun save() {
        runCatching {
            val configFile = File(Constants.CONFIG_FILE_PATH)
            configFile.parentFile?.mkdirs()
            configFile.writeText(Gson().toJson(buildConfigMap()))
        }
    }

    fun saveToCachePath() {
        if (cacheConfigPath == null) {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            cacheMGDir = File(cacheDir, "MG").apply { mkdirs() }
            cacheConfigPath = File(cacheMGDir, "config.json").absolutePath
        }
        runCatching {
            File(cacheConfigPath!!).writeText(Gson().toJson(buildConfigMap()))
        }
    }

    private fun clearCacheFile() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { File(Constants.GLSL_CACHE_FILE_PATH).delete() }
        }
    }

    private fun buildConfigMap(): Map<String, Int> = mapOf(
        "enableANGLE" to enableANGLE,
        "enableNoError" to enableNoError,
        "enableExtTimerQuery" to enableExtTimerQuery,
        "enableExtComputeShader" to enableExtComputeShader,
        "enableExtDirectStateAccess" to enableExtDirectStateAccess,
        "maxGlslCacheSize" to maxGlslCacheSize,
        "multidrawMode" to multidrawMode,
        "angleDepthClearFixMode" to angleDepthClearFixMode,
        "customGLVersion" to customGLVersion,
        "fsr1Setting" to fsr1Setting,
        "vinzz_no_throttle" to vinzzNoThrottle,
        "vinzz_fast_hints" to vinzzFastHints,
        "vinzz_disable_dither" to vinzzDisableDither,
        "vinzz_skip_small_draws" to vinzzSkipSmallDraws,
        "vinzz_state_cache" to vinzzStateCache,
        "vinzz_fbo_cache" to vinzzFboCache,
        "vinzz_smart_invalidate" to vinzzSmartInvalidate,
        "vinzz_color_invalidate" to vinzzColorInvalidate,
        "vinzz_qcom_tiling" to vinzzQcomTiling,
        "vinzz_multidraw_sodium" to vinzzMultidrawSodium,
        "vinzz_shader_cache_aggressive" to vinzzShaderCacheAggressive,
        "vinzz_fence_pool" to vinzzFencePool,
        "vinzz_disjoint_timer_off" to vinzzDisjointTimerOff,
        "vinzz_sodium_mode" to vinzzSodiumMode,
        "vinzz_tex_cache" to vinzzTexCache,
        "vinzz_persistent_vbo" to vinzzPersistentVbo,
        "vinzz_index_reuse" to vinzzIndexReuse,
        "vinzz_batch_uniforms" to vinzzBatchUniforms,
        "vinzz_early_z" to vinzzEarlyZ,
        "vinzz_lrz" to vinzzLrz,
        "vinzz_vertex_mediump" to vinzzVertexMediump,
        "vinzz_invariant_strip" to vinzzInvariantStrip,
        "vinzz_precise_strip" to vinzzPreciseStrip,
        "vinzz_fp16_varyings" to vinzzFp16Varyings,
        "vinzz_vulkan_mode" to vinzzVulkanMode,
        "vinzz_vulkan_lwjgl_patch" to vinzzVulkanLwjglPatch,
        "vinzz_vulkan_async_compute" to vinzzVulkanAsyncCompute,
        "vinzz_vulkan_vma_defrag" to vinzzVulkanVmaDefrag,
        "vinzz_vulkan_disable_validation" to vinzzVulkanDisableValidation,
        "vinzz_vulkan_memory_budget" to vinzzVulkanMemoryBudget,
        "vinzz_vulkan_spirv_opt" to vinzzVulkanSpirvOpt,
        "vinzz_vulkan_frame_overlap" to vinzzVulkanFrameOverlap,
        "vinzz_buffer_streaming" to vinzzBufferStreaming,
        "vinzz_cpu_preprep"      to vinzzCpuPreprep,
        "vinzz_denoiser"         to vinzzDenoiser,
        "vinzz_async_shader"     to vinzzAsyncShader,
        "vinzz_pipeline_cache"   to vinzzPipelineCache,
        "vinzz_shader_complexity_gate" to vinzzShaderComplexityGate,
        "vinzz_compute_protect"         to vinzzComputeProtect, // FIX: missing comma
        "vinzz_distant_horizons_support" to vinzzDistantHorizonsSupport,
        "vinzz_glsl_pragma_opt" to vinzzGlslPragmaOpt,
        "vinzz_reduce_precision" to vinzzReducePrecision,
        "vinzz_mediump_fragment" to vinzzMediumpFragment,
        "vinzz_astc_prefer" to vinzzAstcPrefer,
        "vinzz_anisotropic_level" to vinzzAnisotropicLevel,
        "vinzz_mip_bias_x10" to vinzzMipBiasX10
    )

    companion object {
        var cacheConfigPath: String? = null
        var cacheMGDir: File = File("")

        fun loadConfig(context: Context): MGConfig? {
            val configFile = File(Constants.CONFIG_FILE_PATH)
            if (!configFile.exists()) return null
            val configStr = runCatching { configFile.readText() }.getOrNull() ?: return null
            return runCatching {
                val obj: JsonObject = JsonParser.parseString(configStr).asJsonObject
                val config = MGConfig(context, isInitializing = true)
                config.applyFromJson(obj)
                config.isInitializing = false
                config
            }.getOrNull()
        }

        private fun MGConfig.applyFromJson(obj: JsonObject) {
            fun JsonObject.int(key: String, default: Int) = get(key)?.asInt ?: default
            enableANGLE = obj.int("enableANGLE", 1)
            enableNoError = obj.int("enableNoError", 0)
            enableExtTimerQuery = obj.int("enableExtTimerQuery", 1)
            enableExtComputeShader = obj.int("enableExtComputeShader", 0)
            enableExtDirectStateAccess = obj.int("enableExtDirectStateAccess", 0)
            maxGlslCacheSize = obj.int("maxGlslCacheSize", 32)
            multidrawMode = obj.int("multidrawMode", 0)
            angleDepthClearFixMode = obj.int("angleDepthClearFixMode", 0)
            customGLVersion = obj.int("customGLVersion", 0)
            fsr1Setting = obj.int("fsr1Setting", 0)
            vinzzNoThrottle = obj.int("vinzz_no_throttle", 0)
            vinzzFastHints = obj.int("vinzz_fast_hints", 1)
            vinzzDisableDither = obj.int("vinzz_disable_dither", 1)
            vinzzSkipSmallDraws = obj.int("vinzz_skip_small_draws", 0)
            vinzzStateCache = obj.int("vinzz_state_cache", 1)
            vinzzFboCache = obj.int("vinzz_fbo_cache", 1)
            vinzzSmartInvalidate = obj.int("vinzz_smart_invalidate", 1)
            vinzzColorInvalidate = obj.int("vinzz_color_invalidate", 0)
            vinzzQcomTiling = obj.int("vinzz_qcom_tiling", 1)
            vinzzMultidrawSodium = obj.int("vinzz_multidraw_sodium", 1)
            vinzzShaderCacheAggressive = obj.int("vinzz_shader_cache_aggressive", 1)
            vinzzFencePool = obj.int("vinzz_fence_pool", 1)
            vinzzDisjointTimerOff = obj.int("vinzz_disjoint_timer_off", 1)
            vinzzSodiumMode = obj.int("vinzz_sodium_mode", 1)
            vinzzTexCache = obj.int("vinzz_tex_cache", 1)
            vinzzPersistentVbo = obj.int("vinzz_persistent_vbo", 1)
            vinzzIndexReuse = obj.int("vinzz_index_reuse", 1)
            vinzzBatchUniforms = obj.int("vinzz_batch_uniforms", 1)
            vinzzEarlyZ = obj.int("vinzz_early_z", 1)
            vinzzLrz = obj.int("vinzz_lrz", 1)
            vinzzVertexMediump = obj.int("vinzz_vertex_mediump", 1)
            vinzzInvariantStrip = obj.int("vinzz_invariant_strip", 1)
            vinzzPreciseStrip = obj.int("vinzz_precise_strip", 1)
            vinzzFp16Varyings = obj.int("vinzz_fp16_varyings", 0)
            vinzzVulkanMode = obj.int("vinzz_vulkan_mode", 0)
            vinzzVulkanLwjglPatch = obj.int("vinzz_vulkan_lwjgl_patch", 1)
            vinzzVulkanAsyncCompute = obj.int("vinzz_vulkan_async_compute", 1)
            vinzzVulkanVmaDefrag = obj.int("vinzz_vulkan_vma_defrag", 1)
            vinzzVulkanDisableValidation = obj.int("vinzz_vulkan_disable_validation", 1)
            vinzzVulkanMemoryBudget = obj.int("vinzz_vulkan_memory_budget", 1)
            vinzzVulkanSpirvOpt = obj.int("vinzz_vulkan_spirv_opt", 1)
            vinzzVulkanFrameOverlap = obj.int("vinzz_vulkan_frame_overlap", 1)
            vinzzBufferStreaming = obj.int("vinzz_buffer_streaming", 1)
            vinzzCpuPreprep      = obj.int("vinzz_cpu_preprep", 1)
            vinzzDenoiser        = obj.int("vinzz_denoiser", 1)
            vinzzAsyncShader     = obj.int("vinzz_async_shader", 1)
            vinzzPipelineCache   = obj.int("vinzz_pipeline_cache", 1)
            vinzzShaderComplexityGate = obj.int("vinzz_shader_complexity_gate", 1)
            vinzzComputeProtect        = obj.int("vinzz_compute_protect", 1)
            vinzzDistantHorizonsSupport = obj.int("vinzz_distant_horizons_support", 0)
            vinzzGlslPragmaOpt = obj.int("vinzz_glsl_pragma_opt", 0)
            vinzzReducePrecision = obj.int("vinzz_reduce_precision", 0)
            vinzzMediumpFragment = obj.int("vinzz_mediump_fragment", 0)
            vinzzAstcPrefer = obj.int("vinzz_astc_prefer", 1)
            vinzzAnisotropicLevel = obj.int("vinzz_anisotropic_level", 4)
            vinzzMipBiasX10 = obj.int("vinzz_mip_bias_x10", -5)
        }
    }
}
