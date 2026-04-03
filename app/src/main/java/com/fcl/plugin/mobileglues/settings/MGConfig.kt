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

/**
 * MobileGlues 配置类。
 */
class MGConfig private constructor(val context: Context, private var isInitializing: Boolean) {

    // 默认构造函数，供正常实例化使用
    constructor(context: Context) : this(context, false)

    // ---- 配置字段（UI 触发变更后自动保存） ----

    var enableANGLE: Int = 1
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var enableNoError: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var enableExtTimerQuery: Int = 1
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var enableExtComputeShader: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var enableExtDirectStateAccess: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var maxGlslCacheSize: Int = 32
        set(value) {
            if (field != value) {
                field = value
                if (value == -1) clearCacheFile()
                saveIfReady()
            }
        }

    var multidrawMode: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var angleDepthClearFixMode: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var customGLVersion: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    var fsr1Setting: Int = 0
        set(value) {
            if (field != value) {
                field = value; saveIfReady()
            }
        }

    // ===== VinzzRenderer Optimizations =====
    var vinzz_no_throttle: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_fast_hints: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_disable_dither: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_skip_small_draws: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_state_cache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_fbo_cache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_smart_invalidate: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_color_invalidate: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_qcom_tiling: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_multidraw_sodium: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_shader_cache_aggressive: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_fence_pool: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_disjoint_timer_off: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_sodium_mode: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_tex_cache: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_persistent_vbo: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_index_reuse: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_batch_uniforms: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_early_z: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_glsl_pragma_opt: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_reduce_precision: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_mediump_fragment: Int = 0
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_astc_prefer: Int = 1
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_anisotropic_level: Int = 4
        set(value) { if (field != value) { field = value; saveIfReady() } }
    var vinzz_mip_bias_x10: Int = -5
        set(value) { if (field != value) { field = value; saveIfReady() } }

    // ---- 对外操作 ----

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

    // ---- 私有辅助 ----

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
        "vinzz_no_throttle" to vinzz_no_throttle,
        "vinzz_fast_hints" to vinzz_fast_hints,
        "vinzz_disable_dither" to vinzz_disable_dither,
        "vinzz_skip_small_draws" to vinzz_skip_small_draws,
        "vinzz_state_cache" to vinzz_state_cache,
        "vinzz_fbo_cache" to vinzz_fbo_cache,
        "vinzz_smart_invalidate" to vinzz_smart_invalidate,
        "vinzz_color_invalidate" to vinzz_color_invalidate,
        "vinzz_qcom_tiling" to vinzz_qcom_tiling,
        "vinzz_multidraw_sodium" to vinzz_multidraw_sodium,
        "vinzz_shader_cache_aggressive" to vinzz_shader_cache_aggressive,
        "vinzz_fence_pool" to vinzz_fence_pool,
        "vinzz_disjoint_timer_off" to vinzz_disjoint_timer_off,
        "vinzz_sodium_mode" to vinzz_sodium_mode,
        "vinzz_tex_cache" to vinzz_tex_cache,
        "vinzz_persistent_vbo" to vinzz_persistent_vbo,
        "vinzz_index_reuse" to vinzz_index_reuse,
        "vinzz_batch_uniforms" to vinzz_batch_uniforms,
        "vinzz_early_z" to vinzz_early_z,
        "vinzz_glsl_pragma_opt" to vinzz_glsl_pragma_opt,
        "vinzz_reduce_precision" to vinzz_reduce_precision,
        "vinzz_mediump_fragment" to vinzz_mediump_fragment,
        "vinzz_astc_prefer" to vinzz_astc_prefer,
        "vinzz_anisotropic_level" to vinzz_anisotropic_level,
        "vinzz_mip_bias_x10" to vinzz_mip_bias_x10
    )

    companion object {
        var cacheConfigPath: String? = null
        var cacheMGDir: File = File("")

        /**
         * 从磁盘加载配置，文件不存在或解析失败时返回 null。
         */
        fun loadConfig(context: Context): MGConfig? {
            val configFile = File(Constants.CONFIG_FILE_PATH)
            if (!configFile.exists()) return null

            val configStr = runCatching { configFile.readText() }.getOrNull() ?: return null

            return runCatching {
                val obj: JsonObject = JsonParser.parseString(configStr).asJsonObject
                // 开启 isInitializing 拦截，防止在读取 JSON 赋值时触发大量冗余的 save() 磁盘 I/O
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
            vinzz_no_throttle = obj.int("vinzz_no_throttle", 0)
            vinzz_fast_hints = obj.int("vinzz_fast_hints", 1)
            vinzz_disable_dither = obj.int("vinzz_disable_dither", 1)
            vinzz_skip_small_draws = obj.int("vinzz_skip_small_draws", 0)
            vinzz_state_cache = obj.int("vinzz_state_cache", 1)
            vinzz_fbo_cache = obj.int("vinzz_fbo_cache", 1)
            vinzz_smart_invalidate = obj.int("vinzz_smart_invalidate", 1)
            vinzz_color_invalidate = obj.int("vinzz_color_invalidate", 0)
            vinzz_qcom_tiling = obj.int("vinzz_qcom_tiling", 1)
            vinzz_multidraw_sodium = obj.int("vinzz_multidraw_sodium", 1)
            vinzz_shader_cache_aggressive = obj.int("vinzz_shader_cache_aggressive", 1)
            vinzz_fence_pool = obj.int("vinzz_fence_pool", 1)
            vinzz_disjoint_timer_off = obj.int("vinzz_disjoint_timer_off", 1)
            vinzz_sodium_mode = obj.int("vinzz_sodium_mode", 1)
            vinzz_tex_cache = obj.int("vinzz_tex_cache", 1)
            vinzz_persistent_vbo = obj.int("vinzz_persistent_vbo", 1)
            vinzz_index_reuse = obj.int("vinzz_index_reuse", 1)
            vinzz_batch_uniforms = obj.int("vinzz_batch_uniforms", 1)
            vinzz_early_z = obj.int("vinzz_early_z", 1)
            vinzz_glsl_pragma_opt = obj.int("vinzz_glsl_pragma_opt", 0)
            vinzz_reduce_precision = obj.int("vinzz_reduce_precision", 0)
            vinzz_mediump_fragment = obj.int("vinzz_mediump_fragment", 0)
            vinzz_astc_prefer = obj.int("vinzz_astc_prefer", 1)
            vinzz_anisotropic_level = obj.int("vinzz_anisotropic_level", 4)
            vinzz_mip_bias_x10 = obj.int("vinzz_mip_bias_x10", -5)
        }
    }
}