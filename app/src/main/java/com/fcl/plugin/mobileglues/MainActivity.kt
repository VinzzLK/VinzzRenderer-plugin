package com.fcl.plugin.mobileglues

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.fcl.plugin.mobileglues.databinding.ActivityMainBinding
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.utils.Constants
import com.fcl.plugin.mobileglues.utils.toast
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess
import androidx.appcompat.R as AppcompatR

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    CompoundButton.OnCheckedChangeListener {

    // ---- GL 版本映射 ----
    private val glVersionMap: Map<String, Int> by lazy {
        linkedMapOf(
            getString(R.string.option_angle_disable) to 0,
            "OpenGL 4.6" to 46, "OpenGL 4.5" to 45, "OpenGL 4.4" to 44,
            "OpenGL 4.3" to 43, "OpenGL 4.2" to 42, "OpenGL 4.1" to 41,
            "OpenGL 4.0" to 40, "OpenGL 3.3" to 33, "OpenGL 3.2" to 32
        )
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var config: MGConfig? = null
    private var isSpinnerInitialized = false

    // ---- Activity Result Launchers ----
    private val manageAllFilesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handlePermissionResult(hasMgDirectoryAccess())
        }

    private val appSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handlePermissionResult(hasLegacyPermissions())
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            when {
                isGranted -> handlePermissionResult(true)
                !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> showGoToSettingsDialog()
                else -> handlePermissionResult(false)
            }
        }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            MGConfig(this).save()
            showOptions()
        } else {
            snackbar(getString(R.string.permission_failed))
            hideOptions()
        }
    }

    // ---- 生命周期 ----
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLogger.install(this)  // VinzzRenderer crash logger
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(binding.root)
        setSupportActionBar(binding.appBar)
        setupSpinners()

        binding.openOptions.setOnClickListener {
            if (hasMgDirectoryAccess()) {
                // 如果已经授权，但因没有 MG 文件夹而停留在首屏，直接触发创建并进入设置
                handlePermissionResult(true)
            } else {
                // 如果未授权，走正常的授权流程
                checkPermission()
            }
        }
        setupWindowInsets()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionSilently()
    }

    // ---- 菜单 ----
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_remove)?.isEnabled =
            hasMgDirectoryAccess() && File(Constants.CONFIG_FILE_PATH).exists()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_about -> {
            showAppInfoDialog(this, config); true
        }

        R.id.action_remove -> {
            showRemoveConfirmationDialog(); true
        }

        else -> super.onOptionsItemSelected(item)
    }

    // ---- UI 初始化 ----
    private fun setupWindowInsets() {
        val optionLayoutParams = binding.optionLayout.layoutParams as ViewGroup.MarginLayoutParams
        window.decorView.setOnApplyWindowInsetsListener { _, insets ->
            @Suppress("DEPRECATION")
            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }
            optionLayoutParams.setMargins(0, 0, 0, bottomInset)
            insets
        }
    }

    private fun setupSpinners() {
        fun bindSpinner(spinner: Spinner, arrayRes: Int) {
            ArrayAdapter.createFromResource(this, arrayRes, R.layout.spinner).also { adapter ->
                adapter.setDropDownViewResource(R.layout.spinner)
                spinner.adapter = adapter
            }
        }

        bindSpinner(binding.spinnerAngle, R.array.angle_options)
        bindSpinner(binding.spinnerNoError, R.array.no_error_options)
        bindSpinner(binding.spinnerMultidrawMode, R.array.multidraw_mode_options)
        bindSpinner(binding.angleClearWorkaround, R.array.angle_clear_workaround_options)

        binding.spinnerCustomGlVersion.adapter =
            ArrayAdapter(this, R.layout.spinner, ArrayList(glVersionMap.keys))
    }

    // ---- 权限检查 ----
    private fun hasMgDirectoryAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasLegacyPermissions()
        }

    private fun hasLegacyPermissions(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

    private fun checkPermissionSilently() {
        // 关键逻辑：只有在已经授权且配置文件/MG文件夹存在时，才自动进入设置
        val isConfigured = File(Constants.CONFIG_FILE_PATH).exists()

        if (hasMgDirectoryAccess() && isConfigured) {
            (MGConfig.loadConfig(this) ?: MGConfig(this)).save()
            showOptions()
        } else {
            // 否则（未授权，或已授权但未点击启用），停留在启动页并显示 openOptions 按钮
            hideOptions()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showManageAllFilesDialog()
        } else {
            if (hasLegacyPermissions()) showOptions()
            else requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // ---- 权限相关对话框 ----
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showManageAllFilesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_permission_title)
            .setMessage(getString(R.string.dialog_permission_msg_android_Q, Constants.MG_DIRECTORY))
            .setPositiveButton(R.string.dialog_positive) { _, _ -> launchManageAllFilesSettings() }
            .setNegativeButton(R.string.dialog_negative, null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchManageAllFilesSettings() {
        try {
            manageAllFilesLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:$packageName".toUri()
                )
            )
        } catch (_: Exception) {
            manageAllFilesLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun showGoToSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_permission_title)
            .setMessage(R.string.dialog_permission_msg)
            .setPositiveButton(R.string.dialog_positive) { _, _ ->
                appSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            }
            .setNegativeButton(R.string.dialog_negative, null)
            .show()
    }

    // ---- 选项面板显示/隐藏 ----
    private fun showOptions() {
        isSpinnerInitialized = false
        setAllListeners(null)

        config = (MGConfig.loadConfig(this) ?: MGConfig(this)).apply {
            if (enableANGLE !in 0..3) enableANGLE = 0
            if (enableNoError !in 0..3) enableNoError = 0
            if (maxGlslCacheSize <= 0 && maxGlslCacheSize != -1) maxGlslCacheSize = 32

            binding.inputMaxGlslCacheSize.setText(maxGlslCacheSize.toString())
            binding.spinnerAngle.setSelection(enableANGLE)
            binding.spinnerNoError.setSelection(enableNoError)
            binding.spinnerMultidrawMode.setSelection(multidrawMode)
            binding.angleClearWorkaround.setSelection(angleDepthClearFixMode)

            binding.switchExtTimerQuery.isChecked = enableExtTimerQuery == 0
            binding.switchExtDirectStateAccess.isChecked = enableExtDirectStateAccess == 1
            binding.switchExtCs.isChecked = enableExtComputeShader == 1
            binding.switchEnableFsr1.isChecked = fsr1Setting == 1

            binding.spinnerCustomGlVersion.setSelection(getSpinnerIndexByGLVersion(customGLVersion))
        }

        setAllListeners(this)
        setupGlslCacheSizeWatcher()

        binding.openOptions.visibility = View.GONE
        binding.scrollLayout.visibility = View.VISIBLE

        binding.root.post { isSpinnerInitialized = true }
        setupVinzzSwitches()
    }

    private fun hideOptions() {
        binding.openOptions.visibility = View.VISIBLE
        binding.scrollLayout.visibility = View.GONE
        invalidateOptionsMenu()
    }

    private fun setAllListeners(listener: Any?) {
        val itemListener = listener as? AdapterView.OnItemSelectedListener
        val checkedListener = listener as? CompoundButton.OnCheckedChangeListener

        invalidateOptionsMenu()

        binding.apply {
            spinnerAngle.onItemSelectedListener = itemListener
            spinnerNoError.onItemSelectedListener = itemListener
            spinnerMultidrawMode.onItemSelectedListener = itemListener
            spinnerCustomGlVersion.onItemSelectedListener = itemListener
            angleClearWorkaround.onItemSelectedListener = itemListener

            switchExtCs.setOnCheckedChangeListener(checkedListener)
            switchExtTimerQuery.setOnCheckedChangeListener(checkedListener)
            switchExtDirectStateAccess.setOnCheckedChangeListener(checkedListener)
            switchEnableFsr1.setOnCheckedChangeListener(checkedListener)
        }
    }

    private fun setupGlslCacheSizeWatcher() {
        binding.inputMaxGlslCacheSize.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                if (text.isEmpty()) {
                    binding.inputMaxGlslCacheSizeLayout.error = null
                    config?.maxGlslCacheSize = 32
                    return
                }
                text.toIntOrNull()?.let { number ->
                    if (number < -1 || number == 0) {
                        binding.inputMaxGlslCacheSizeLayout.error =
                            getString(R.string.option_glsl_cache_error_range)
                    } else {
                        binding.inputMaxGlslCacheSizeLayout.error = null
                        config?.maxGlslCacheSize = number
                    }
                } ?: run {
                    binding.inputMaxGlslCacheSizeLayout.error =
                        getString(R.string.option_glsl_cache_error_invalid)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ---- Spinner 回调 ----
    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
        if (!isSpinnerInitialized || config == null) return

        when (adapterView.id) {
            R.id.spinner_angle -> handleAngleSelection(position)
            R.id.spinner_no_error -> config?.enableNoError = position
            R.id.spinner_multidraw_mode -> config?.multidrawMode = position
            R.id.spinner_custom_gl_version -> handleCustomGLVersionSelection(position)
            R.id.angle_clear_workaround -> handleAngleClearWorkaroundSelection(position)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    private fun handleAngleSelection(position: Int) {
        val previous = config?.enableANGLE ?: return
        if (position == previous) return

        if (position == 3 && isAdreno740()) {
            showWarningDialog(
                messageRes = R.string.warning_adreno_740_angle,
                onConfirm = { config?.enableANGLE = position },
                onCancel = { revertSpinner(binding.spinnerAngle, previous) }
            )
        } else {
            config?.enableANGLE = position
        }
    }

    private fun handleAngleClearWorkaroundSelection(position: Int) {
        val previous = config?.angleDepthClearFixMode ?: return
        if (position == previous) return

        if (position >= 1) {
            showWarningDialog(
                messageRes = R.string.warning_enabling_angle_clear_workaround,
                onConfirm = { config?.angleDepthClearFixMode = position },
                onCancel = { revertSpinner(binding.angleClearWorkaround, previous) }
            )
        } else {
            config?.angleDepthClearFixMode = position
        }
    }

    private fun handleCustomGLVersionSelection(position: Int) {
        val previous = config?.customGLVersion ?: return
        val newValue = getGLVersionBySpinnerIndex(position)
        if (newValue == previous) return

        if (previous == 0) {
            showCountdownWarningDialog(
                messageRes = R.string.warning_enabling_custom_gl_version,
                cooldownSeconds = 41,
                onConfirm = { config?.customGLVersion = newValue },
                onCancel = {
                    revertSpinner(
                        binding.spinnerCustomGlVersion,
                        getSpinnerIndexByGLVersion(previous)
                    )
                }
            )
        } else {
            config?.customGLVersion = newValue
        }
    }

    // ---- Switch 回调 ----
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (config == null) return

        when (buttonView.id) {
            R.id.switch_ext_cs -> handleSwitchWithWarning(
                isChecked = isChecked,
                warningMsgRes = R.string.warning_ext_cs_enable,
                onConfirm = { config?.enableExtComputeShader = 1 },
                onCancel = { config?.enableExtComputeShader = 0 },
                button = buttonView
            )

            R.id.switch_enable_fsr1 -> handleSwitchWithWarning(
                isChecked = isChecked,
                warningMsgRes = R.string.warning_fsr1_enable,
                onConfirm = { config?.fsr1Setting = 1 },
                onCancel = { config?.fsr1Setting = 0 },
                button = buttonView
            )

            R.id.switch_ext_timer_query -> config?.enableExtTimerQuery = if (isChecked) 0 else 1
            R.id.switch_ext_direct_state_access -> config?.enableExtDirectStateAccess =
                if (isChecked) 1 else 0

            // ── VinzzRenderer switches ──
            R.id.switch_vinzz_no_throttle          -> config?.vinzzNoThrottle          = if (isChecked) 1 else 0
            R.id.switch_vinzz_fast_hints           -> config?.vinzzFastHints           = if (isChecked) 1 else 0
            R.id.switch_vinzz_disable_dither       -> config?.vinzzDisableDither       = if (isChecked) 1 else 0
            R.id.switch_vinzz_state_cache          -> config?.vinzzStateCache          = if (isChecked) 1 else 0
            R.id.switch_vinzz_fbo_cache            -> config?.vinzzFboCache            = if (isChecked) 1 else 0
            R.id.switch_vinzz_skip_small_draws     -> config?.vinzzSkipSmallDraws      = if (isChecked) 1 else 0
            R.id.switch_vinzz_batch_uniforms       -> config?.vinzzBatchUniforms       = if (isChecked) 1 else 0
            R.id.switch_vinzz_multidraw_sodium     -> config?.vinzzMultidrawSodium     = if (isChecked) 1 else 0
            R.id.switch_vinzz_qcom_tiling          -> config?.vinzzQcomTiling          = if (isChecked) 1 else 0
            R.id.switch_vinzz_fence_pool           -> config?.vinzzFencePool           = if (isChecked) 1 else 0
            R.id.switch_vinzz_disjoint_timer_off   -> config?.vinzzDisjointTimerOff   = if (isChecked) 1 else 0
            R.id.switch_vinzz_smart_invalidate     -> config?.vinzzSmartInvalidate     = if (isChecked) 1 else 0
            R.id.switch_vinzz_color_invalidate     -> config?.vinzzColorInvalidate     = if (isChecked) 1 else 0
            R.id.switch_vinzz_sodium_mode -> {
                config?.vinzzSodiumMode = if (isChecked) 1 else 0
                if (isChecked && (config?.vinzzVulkanMode ?: 0) == 1) {
                    config?.vinzzVulkanMode = 0
                    binding.switchVinzzVulkanMode.isChecked = false
                    setVulkanSubOptionsVisible(false)
                    writeVulkanJvmArgs()
                }
            }
            R.id.switch_vinzz_persistent_vbo       -> config?.vinzzPersistentVbo       = if (isChecked) 1 else 0
            R.id.switch_vinzz_index_reuse          -> config?.vinzzIndexReuse          = if (isChecked) 1 else 0
            R.id.switch_vinzz_tex_cache            -> config?.vinzzTexCache            = if (isChecked) 1 else 0
            R.id.switch_vinzz_astc_prefer          -> config?.vinzzAstcPrefer          = if (isChecked) 1 else 0
            R.id.switch_vinzz_shader_cache_aggressive -> config?.vinzzShaderCacheAggressive = if (isChecked) 1 else 0
            R.id.switch_vinzz_glsl_pragma_opt      -> config?.vinzzGlslPragmaOpt       = if (isChecked) 1 else 0
            R.id.switch_vinzz_reduce_precision     -> config?.vinzzReducePrecision     = if (isChecked) 1 else 0
            R.id.switch_vinzz_mediump_fragment     -> config?.vinzzMediumpFragment     = if (isChecked) 1 else 0
            R.id.switch_vinzz_early_z              -> config?.vinzzEarlyZ              = if (isChecked) 1 else 0
            R.id.switch_vinzz_lrz                  -> config?.vinzzLrz                 = if (isChecked) 1 else 0
            
            R.id.switch_vinzz_vulkan_mode -> {
                config?.vinzzVulkanMode = if (isChecked) 1 else 0
                setVulkanSubOptionsVisible(isChecked)
                if (isChecked) {
                    // Matikan Sodium Mode (mutual exclusivity)
                    config?.vinzzSodiumMode = 0
                    binding.switchVinzzSodiumMode.isChecked = false
                    // Tulis JVM flags untuk LWJGL Android fix
                    writeVulkanJvmArgs()
                } else {
                    writeVulkanJvmArgs() // hapus flags file
                }
            }
            R.id.switch_vinzz_vulkan_lwjgl_patch -> {
                config?.vinzzVulkanLwjglPatch = if (isChecked) 1 else 0
                writeVulkanJvmArgs() // update flags file
            }
            R.id.switch_vinzz_vulkan_async_compute    -> config?.vinzzVulkanAsyncCompute    = if (isChecked) 1 else 0
            R.id.switch_vinzz_vulkan_vma_defrag       -> config?.vinzzVulkanVmaDefrag       = if (isChecked) 1 else 0
            R.id.switch_vinzz_vulkan_disable_validation -> config?.vinzzVulkanDisableValidation = if (isChecked) 1 else 0
            R.id.switch_vinzz_vulkan_memory_budget    -> config?.vinzzVulkanMemoryBudget    = if (isChecked) 1 else 0
            R.id.switch_vinzz_vulkan_spirv_opt        -> config?.vinzzVulkanSpirvOpt        = if (isChecked) 1 else 0
            R.id.switch_vinzz_vulkan_frame_overlap    -> config?.vinzzVulkanFrameOverlap    = if (isChecked) 1 else 0
            R.id.switch_vinzz_buffer_streaming -> config?.vinzzBufferStreaming = if (isChecked) 1 else 0
            R.id.switch_vinzz_cpu_preprep      -> config?.vinzzCpuPreprep      = if (isChecked) 1 else 0
            R.id.switch_vinzz_denoiser         -> config?.vinzzDenoiser        = if (isChecked) 1 else 0
            R.id.switch_vinzz_async_shader     -> config?.vinzzAsyncShader     = if (isChecked) 1 else 0
            R.id.switch_vinzz_pipeline_cache   -> config?.vinzzPipelineCache   = if (isChecked) 1 else 0
            R.id.switch_vinzz_distant_horizons_support -> {
                config?.vinzzDistantHorizonsSupport = if (isChecked) 1 else 0
                // DH Support: aktifkan compute shader + matikan depth invalidation
                if (isChecked) {
                    config?.enableExtComputeShader = 1
                    config?.vinzzSmartInvalidate   = 0
                    binding.switchVinzzSmartInvalidate.isChecked = false
                }
            }
            R.id.switch_vinzz_shader_complexity_gate -> config?.vinzzShaderComplexityGate = if (isChecked) 1 else 0
            R.id.switch_vinzz_compute_protect        -> config?.vinzzComputeProtect        = if (isChecked) 1 else 0
            
            R.id.switch_vinzz_vertex_mediump    -> config?.vinzzVertexMediump    = if (isChecked) 1 else 0
            R.id.switch_vinzz_invariant_strip   -> config?.vinzzInvariantStrip    = if (isChecked) 1 else 0
            R.id.switch_vinzz_precise_strip     -> config?.vinzzPreciseStrip      = if (isChecked) 1 else 0
            R.id.switch_vinzz_fp16_varyings     -> config?.vinzzFp16Varyings      = if (isChecked) 1 else 0
        }
    }

    private fun handleSwitchWithWarning(
        isChecked: Boolean,
        @StringRes warningMsgRes: Int,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
        button: CompoundButton
    ) {
        if (isChecked) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(warningMsgRes)
                .setCancelable(false)
                .setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
                .setPositiveButton(R.string.dialog_positive) { _, _ -> onConfirm() }
                .setNegativeButton(R.string.dialog_negative) { _, _ ->
                    button.isChecked = false
                    onCancel()
                }
                .show()
        } else {
            onCancel()
        }
    }

    // ---- 对话框辅助 ----
    private fun showWarningDialog(
        @StringRes messageRes: Int,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_warning)
            .setMessage(getString(messageRes))
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_positive) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.dialog_negative) { _, _ -> onCancel() }
            .show()
    }

    private fun showCountdownWarningDialog(
        @StringRes messageRes: Int,
        cooldownSeconds: Int,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_warning)
            .setMessage(getStyledMessage(messageRes))
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.dialog_negative) { _, _ -> onCancel() }
            .show()

        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.isEnabled = false
        positiveButton.setOnClickListener { onConfirm(); dialog.dismiss() }

        object : CountDownTimer(cooldownSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                positiveButton.text =
                    getString(R.string.ok_with_countdown, (millisUntilFinished / 1000).toInt())
            }

            override fun onFinish() {
                positiveButton.apply {
                    text = getString(R.string.ok)
                    setTextColor(
                        MaterialColors.getColor(
                            context,
                            AppcompatR.attr.colorError,
                            Color.RED
                        )
                    )
                    isEnabled = true
                }
            }
        }.start()
    }

    // ---- 删除 MobileGlues ----
    private fun showRemoveConfirmationDialog() {
        showCountdownWarningDialog(
            R.string.remove_mg_files_message,
            10,
            { removeMobileGluesCompletely() },
            {})
    }

    private fun removeMobileGluesCompletely() {
        val view = LayoutInflater.from(this).inflate(R.layout.progress_dialog_md3, null)

        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.removing_mobileglues)
            .setView(view)
            .setCancelable(false)
            .show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                File(Constants.MG_DIRECTORY).deleteRecursively()

                withContext(Dispatchers.Main) {
                    config = null
                    hideOptions()
                    progressDialog.dismiss()
                    showFinalDialog()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    toast(getString(R.string.remove_failed, e.message))
                }
            }
        }
    }

    private fun showFinalDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remove_complete_title)
            .setMessage(R.string.remove_complete_message)
            .setCancelable(false)
            .setPositiveButton(R.string.exit) { _, _ -> finishAffinity(); exitProcess(0) }
            .show()
    }

    // ---- GPU 检测 ----
    private fun getGPUName(): String? {
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(
                eglDisplay,
                IntArray(2),
                0,
                IntArray(2),
                1
            )
        ) return null

        var renderer: String? = null
        val configAttributes =
            intArrayOf(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE)
        val eglConfigs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        if (EGL14.eglChooseConfig(
                eglDisplay,
                configAttributes,
                0,
                eglConfigs,
                0,
                1,
                numConfigs,
                0
            )
        ) {
            val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfigs[0],
                EGL14.EGL_NO_CONTEXT,
                contextAttributes,
                0
            )

            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                val pbufferAttributes =
                    intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                val eglSurface =
                    EGL14.eglCreatePbufferSurface(eglDisplay, eglConfigs[0], pbufferAttributes, 0)
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    if (EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                        renderer = GLES20.glGetString(GLES20.GL_RENDERER)
                    }
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
        }
        EGL14.eglTerminate(eglDisplay)
        return renderer
    }

    private fun isAdreno740(): Boolean {
        return getGPUName()?.let {
            it.contains(
                "adreno",
                ignoreCase = true
            ) && it.contains("740")
        } == true
    }

    // ---- GL Version Spinner 辅助 ----
    private fun getGLVersionBySpinnerIndex(index: Int): Int {
        val selected =
            binding.spinnerCustomGlVersion.getItemAtPosition(index) as? String ?: return 0
        return glVersionMap[selected] ?: 0
    }

    private fun getSpinnerIndexByGLVersion(glVersion: Int): Int {
        val targetDisplay = glVersionMap.entries.firstOrNull { it.value == glVersion }?.key
            ?: getString(R.string.option_angle_disable)
        return glVersionMap.keys.indexOf(targetDisplay).coerceAtLeast(0)
    }

    private fun revertSpinner(spinner: Spinner, position: Int) {
        isSpinnerInitialized = false
        spinner.setSelection(position)
        spinner.post { isSpinnerInitialized = true }
    }

    // ---- 样式化消息 ----
    private fun getStyledMessage(@StringRes id: Int): Spanned {
        val errorColorHex = String.format(
            "#%06X",
            0xFFFFFF and MaterialColors.getColor(this, AppcompatR.attr.colorError, Color.RED)
        )
        return Html.fromHtml(
            getString(id).replace("@colorError", errorColorHex),
            Html.FROM_HTML_MODE_LEGACY
        )
    }

    // ---- Snackbar ----
    fun snackbar(text: CharSequence, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, text, duration).show()
    }
    // ──────────────────────────────────────────────────────
    // VinzzRenderer: bind semua switch + slider ke MGConfig
    // ──────────────────────────────────────────────────────
    private fun setupVinzzSwitches() {
        val cfg = config ?: return
        try {

        // ── Performance ──
        binding.switchVinzzNoThrottle.isChecked        = cfg.vinzzNoThrottle == 1
        binding.switchVinzzFastHints.isChecked         = cfg.vinzzFastHints == 1
        binding.switchVinzzDisableDither.isChecked     = cfg.vinzzDisableDither == 1
        binding.switchVinzzStateCache.isChecked        = cfg.vinzzStateCache == 1
        binding.switchVinzzFboCache.isChecked          = cfg.vinzzFboCache == 1
        binding.switchVinzzSkipSmallDraws.isChecked    = cfg.vinzzSkipSmallDraws == 1
        binding.switchVinzzBatchUniforms.isChecked     = cfg.vinzzBatchUniforms == 1
        binding.switchVinzzMultidrawSodium.isChecked   = cfg.vinzzMultidrawSodium == 1
        binding.switchVinzzQcomTiling.isChecked        = cfg.vinzzQcomTiling == 1
        binding.switchVinzzFencePool.isChecked         = cfg.vinzzFencePool == 1
        binding.switchVinzzDisjointTimerOff.isChecked  = cfg.vinzzDisjointTimerOff == 1
        binding.switchVinzzSmartInvalidate.isChecked   = cfg.vinzzSmartInvalidate == 1
        binding.switchVinzzColorInvalidate.isChecked   = cfg.vinzzColorInvalidate == 1
        binding.switchVinzzSodiumMode.isChecked        = cfg.vinzzSodiumMode == 1
        binding.switchVinzzPersistentVbo.isChecked     = cfg.vinzzPersistentVbo == 1
        binding.switchVinzzIndexReuse.isChecked        = cfg.vinzzIndexReuse == 1

        // ── Texture ──
        binding.switchVinzzTexCache.isChecked          = cfg.vinzzTexCache == 1
        binding.switchVinzzAstcPrefer.isChecked        = cfg.vinzzAstcPrefer == 1

        // Anisotropic slider: 1→1x 2→2x 3→4x 4→8x 5→16x (log2 mapping)
        val anisoSliderVal = when (cfg.vinzzAnisotropicLevel) {
            1  -> 1f; 2 -> 2f; 4 -> 3f; 8 -> 4f; 16 -> 5f; else -> 3f
        }
        binding.sliderAnisotropic.value = anisoSliderVal.coerceIn(1f, 5f).coerceIn(1f, 5f)
        binding.textAnisotropicValue.text = "${cfg.vinzzAnisotropicLevel}x"
        binding.sliderAnisotropic.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val level = when (value.toInt()) { 1->1; 2->2; 3->4; 4->8; 5->16; else->1 }
            config?.vinzzAnisotropicLevel = level
            binding.textAnisotropicValue.text = "${level}x"
        }

        // Mip bias slider: stored as x10, so -5 = -0.5
        binding.sliderMipBias.value = cfg.vinzzMipBiasX10.toFloat().coerceIn(-10f, 10f).coerceIn(-10f, 10f)
        binding.textMipBiasValue.text = "%.1f".format(cfg.vinzzMipBiasX10 / 10.0)
        binding.sliderMipBias.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            config?.vinzzMipBiasX10 = value.toInt()
            binding.textMipBiasValue.text = "%.1f".format(value / 10.0)
        }

        // ── Shader ──
        binding.switchVinzzShaderCacheAggressive.isChecked = cfg.vinzzShaderCacheAggressive == 1
        binding.switchVinzzGlslPragmaOpt.isChecked         = cfg.vinzzGlslPragmaOpt == 1
        binding.switchVinzzReducePrecision.isChecked       = cfg.vinzzReducePrecision == 1
        binding.switchVinzzMediumpFragment.isChecked       = cfg.vinzzMediumpFragment == 1
        binding.switchVinzzEarlyZ.isChecked                = cfg.vinzzEarlyZ == 1
        binding.switchVinzzLrz.isChecked                   = cfg.vinzzLrz == 1
        binding.switchVinzzVertexMediump.isChecked   = cfg.vinzzVertexMediump == 1
        binding.switchVinzzInvariantStrip.isChecked   = cfg.vinzzInvariantStrip == 1
        binding.switchVinzzPreciseStrip.isChecked     = cfg.vinzzPreciseStrip == 1
        binding.switchVinzzFp16Varyings.isChecked     = cfg.vinzzFp16Varyings == 1
        // ── New 5 Features ──
        binding.switchVinzzBufferStreaming.isChecked = cfg.vinzzBufferStreaming == 1
        binding.switchVinzzCpuPreprep.isChecked      = cfg.vinzzCpuPreprep      == 1
        binding.switchVinzzDenoiser.isChecked        = cfg.vinzzDenoiser        == 1
        binding.switchVinzzAsyncShader.isChecked     = cfg.vinzzAsyncShader     == 1
        binding.switchVinzzPipelineCache.isChecked   = cfg.vinzzPipelineCache   == 1
        runCatching { binding.switchVinzzDistantHorizonsSupport.isChecked = cfg.vinzzDistantHorizonsSupport == 1 }
        binding.switchVinzzShaderComplexityGate.isChecked = cfg.vinzzShaderComplexityGate == 1
        binding.switchVinzzComputeProtect.isChecked        = cfg.vinzzComputeProtect        == 1

        // Register semua ke checkedChangeListener
        listOf(
            binding.switchVinzzNoThrottle,
            binding.switchVinzzFastHints,
            binding.switchVinzzDisableDither,
            binding.switchVinzzStateCache,
            binding.switchVinzzFboCache,
            binding.switchVinzzSkipSmallDraws,
            binding.switchVinzzBatchUniforms,
            binding.switchVinzzMultidrawSodium,
            binding.switchVinzzQcomTiling,
            binding.switchVinzzFencePool,
            binding.switchVinzzDisjointTimerOff,
            binding.switchVinzzSmartInvalidate,
            binding.switchVinzzColorInvalidate,
            binding.switchVinzzSodiumMode,
            binding.switchVinzzPersistentVbo,
            binding.switchVinzzIndexReuse,
            binding.switchVinzzTexCache,
            binding.switchVinzzAstcPrefer,
            binding.switchVinzzShaderCacheAggressive,
            binding.switchVinzzGlslPragmaOpt,
            binding.switchVinzzReducePrecision,
            binding.switchVinzzMediumpFragment,
            binding.switchVinzzEarlyZ,
            binding.switchVinzzLrz,
            binding.switchVinzzVertexMediump,
            binding.switchVinzzInvariantStrip,
            binding.switchVinzzPreciseStrip,
            binding.switchVinzzFp16Varyings,
            binding.switchVinzzBufferStreaming,
            binding.switchVinzzCpuPreprep,
            binding.switchVinzzDenoiser,
            binding.switchVinzzAsyncShader,
            binding.switchVinzzPipelineCache,
            binding.switchVinzzShaderComplexityGate,
            binding.switchVinzzComputeProtect,
            binding.switchVinzzVulkanMode,
            binding.switchVinzzVulkanLwjglPatch,
            binding.switchVinzzVulkanAsyncCompute,
            binding.switchVinzzVulkanVmaDefrag,
            binding.switchVinzzVulkanDisableValidation,
            binding.switchVinzzVulkanMemoryBudget,
            binding.switchVinzzVulkanSpirvOpt,
            binding.switchVinzzVulkanFrameOverlap,
            binding.switchVinzzDistantHorizonsSupport,
        ).forEach { it.setOnCheckedChangeListener(this) }
        } catch (e: Exception) {
            // VinzzRenderer: catch any NPE/IllegalStateException in switch setup
            android.util.Log.e("VinzzRenderer", "setupVinzzSwitches crash", e)
            try {
                val logDir = getExternalFilesDir(null) ?: filesDir
                val f = java.io.File(logDir, "crash.txt")
                f.appendText("[setupVinzzSwitches] ${e}\n${e.stackTraceToString()}\n\n")
            } catch (_: Exception) {}
        }
    }

    // ── Vulkan Mode: tulis JVM flags untuk fix LWJGL Android crash ──
    // Fix: NoSuchFieldError: VkAndroidSurfaceCreateInfoKHR 'sun.misc.Unsafe UNSAFE'
    // Root cause: LWJGL 3.3.x pakai reflection ke sun.misc.Unsafe tapi
    //             Android Java 21 perlu explicit --add-opens
    private fun writeVulkanJvmArgs() {
        runCatching {
            val mgDir = File(com.fcl.plugin.mobileglues.utils.Constants.MG_DIRECTORY)
            mgDir.mkdirs()
            val flagsFile = File(mgDir, "vulkan_jvmargs.txt")
            val vulkanMode = config?.vinzzVulkanMode ?: 0
            val lwjglPatch = config?.vinzzVulkanLwjglPatch ?: 1
            if (vulkanMode == 1 && lwjglPatch == 1) {
                flagsFile.writeText(
                    "--add-opens java.base/sun.misc=ALL-UNNAMED\n" +
                    "--add-opens java.base/java.nio=ALL-UNNAMED\n" +
                    "--add-opens java.base/java.lang=ALL-UNNAMED\n" +
                    "-Dorg.lwjgl.system.SharedLibraryExtractPath=/data/local/tmp\n" +
                    "-Dorg.lwjgl.vulkan.libname=libvulkan.so\n"
                )
                android.widget.Toast.makeText(
                    this,
                    "✓ vulkan_jvmargs.txt ditulis ke /sdcard/MG/\nSalin flags ke Zalith: JVM Args",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                flagsFile.delete()
            }
        }
    }

    private fun setVulkanSubOptionsVisible(visible: Boolean) {
        binding.layoutVulkanOptions.visibility =
            if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }


}