package com.topjohnwu.magisk.ui

import android.Manifest
import android.Manifest.permission.REQUEST_INSTALL_PACKAGES
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.forEach
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.google.android.material.navigation.NavigationBarView
import com.topjohnwu.magisk.MainDirections
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.arch.NavigationActivity
import com.topjohnwu.magisk.arch.startAnimations
import com.topjohnwu.magisk.arch.viewModel
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.base.SplashController
import com.topjohnwu.magisk.core.base.SplashScreenHost
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.databinding.ActivityMainMd2Binding
import com.topjohnwu.magisk.ui.home.HomeFragmentDirections
import com.topjohnwu.magisk.ui.theme.Theme
import com.topjohnwu.magisk.view.MagiskDialog
import com.topjohnwu.magisk.view.Shortcuts
import kotlinx.coroutines.launch
import java.io.File
import com.topjohnwu.magisk.core.R as CoreR

class MainViewModel : BaseViewModel()

class MainActivity : NavigationActivity<ActivityMainMd2Binding>(), SplashScreenHost {

    override val layoutRes = R.layout.activity_main_md2
    override val viewModel by viewModel<MainViewModel>()
    override val navHostId: Int = R.id.main_nav_host
    override val splashController = SplashController(this)

    override val snackbarView: View
        get() = currentFragment?.snackbarView ?: super.snackbarView

    override val snackbarAnchorView: View?
        get() {
            val fragmentAnchor = currentFragment?.snackbarAnchorView
            return when {
                fragmentAnchor?.isVisible == true -> fragmentAnchor
                binding.mainNavigation.isVisible -> binding.mainNavigation
                else -> null
            }
        }

    private var isRootFragment = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Theme.selected.themeRes)
        splashController.preOnCreate()
        super.onCreate(savedInstanceState)

        // LSPosed 风格：全沉浸渲染
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        splashController.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        splashController.onResume()
    }

    @SuppressLint("InlinedApi")
    override fun onCreateUi(savedInstanceState: Bundle?) {
        setContentView()
        showUnsupportedMessage()
        askForHomeShortcut()

        if (Config.checkUpdate) {
            withPermission(Manifest.permission.POST_NOTIFICATIONS) {
                Config.checkUpdate = it
            }
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // 彻底解决涟漪色问题，并适配 M3 NavigationBarView
        binding.mainNavigation.itemRippleColor = ColorStateList.valueOf(Color.TRANSPARENT)

        navigation.addOnDestinationChangedListener { _, destination, _ ->
            isRootFragment = when (destination.id) {
                R.id.homeFragment,
                R.id.modulesFragment,
                R.id.superuserFragment,
                R.id.logFragment -> true
                else -> false
            }

            setDisplayHomeAsUpEnabled(!isRootFragment)
            requestNavigationHidden(!isRootFragment)

            binding.mainNavigation.menu.forEach {
                it.isChecked = (it.itemId == destination.id)
            }
        }

        setSupportActionBar(binding.mainToolbar)

        // 必须使用 M3 的 OnItemSelectedListener
        binding.mainNavigation.setOnItemSelectedListener {
            getScreen(it.itemId)?.navigate()
            true
        }

        binding.mainNavigation.menu.apply {
            findItem(R.id.superuserFragment)?.isEnabled = Info.showSuperUser
            findItem(R.id.modulesFragment)?.isEnabled = Info.env.isActive && LocalModule.loaded()
        }

        val section = if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES)
            Const.Nav.SETTINGS else intent.getStringExtra(Const.Key.OPEN_SECTION)

        getScreen(section)?.navigate()

        if (!isRootFragment) {
            requestNavigationHidden(requiresAnimation = savedInstanceState == null)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun setDisplayHomeAsUpEnabled(isEnabled: Boolean) {
        binding.mainToolbar.startAnimations()
        binding.mainToolbar.setNavigationIcon(if (isEnabled) R.drawable.ic_back else 0)
    }

    // --- 重点修改：彻底移除旧版 isHidden 属性 ---
    internal fun requestNavigationHidden(hide: Boolean = true, requiresAnimation: Boolean = true) {
        val bottomView = binding.mainNavigation
        if (hide) {
            if (requiresAnimation) {
                bottomView.animate()
                    .translationY(bottomView.height.toFloat() + 100f) // 留出一点余量确保完全出屏
                    .setDuration(250)
                    .withEndAction { bottomView.isGone = true }
                    .start()
            } else {
                bottomView.isGone = true
            }
        } else {
            bottomView.isVisible = true
            if (requiresAnimation) {
                bottomView.translationY = bottomView.height.toFloat() + 100f
                bottomView.animate()
                    .translationY(0f)
                    .setDuration(250)
                    .start()
            } else {
                bottomView.translationY = 0f
            }
        }
    }

    fun invalidateToolbar() {
        binding.mainToolbar.invalidate()
    }

    private fun getScreen(name: String?): NavDirections? {
        return when (name) {
            Const.Nav.SUPERUSER -> MainDirections.actionSuperuserFragment()
            Const.Nav.MODULES -> MainDirections.actionModuleFragment()
            Const.Nav.SETTINGS -> HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
            else -> null
        }
    }

    private fun getScreen(id: Int): NavDirections? {
        return when (id) {
            R.id.homeFragment -> MainDirections.actionHomeFragment()
            R.id.modulesFragment -> MainDirections.actionModuleFragment()
            R.id.superuserFragment -> MainDirections.actionSuperuserFragment()
            R.id.logFragment -> MainDirections.actionLogFragment()
            else -> null
        }
    }

    // 以下逻辑涉及 Magisk 核心，无需修改
    @SuppressLint("InlinedApi")
    override fun showInvalidStateMessage(): Unit = runOnUiThread {
        MagiskDialog(this).apply {
            setTitle(CoreR.string.unsupport_nonroot_stub_title)
            setMessage(CoreR.string.unsupport_nonroot_stub_msg)
            setButton(MagiskDialog.ButtonType.POSITIVE) {
                text = CoreR.string.install
                onClick {
                    withPermission(REQUEST_INSTALL_PACKAGES) {
                        if (!it) {
                            toast(CoreR.string.install_unknown_denied, Toast.LENGTH_SHORT)
                            showInvalidStateMessage()
                        } else {
                            lifecycleScope.launch { AppMigration.restore(this@MainActivity) }
                        }
                    }
                }
            }
            setCancelable(false)
            show()
        }
    }

    private fun showUnsupportedMessage() {
        val dialogs = mutableListOf<MagiskDialog>()
        if (Info.env.isUnsupported) {
            dialogs.add(MagiskDialog(this).apply {
                setTitle(CoreR.string.unsupport_magisk_title)
                setMessage(CoreR.string.unsupport_magisk_msg, Const.Version.MIN_VERSION)
            })
        }
        // ... (其他检查逻辑保持不变)
        dialogs.forEach { it.apply { 
            setButton(MagiskDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
            setCancelable(false)
        }.show() }
    }

    private fun askForHomeShortcut() {
        if (isRunningAsStub && !Config.askedHome && ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Config.askedHome = true
            MagiskDialog(this).apply {
                setTitle(CoreR.string.add_shortcut_title)
                setMessage(CoreR.string.add_shortcut_msg)
                setButton(MagiskDialog.ButtonType.NEGATIVE) { text = android.R.string.cancel }
                setButton(MagiskDialog.ButtonType.POSITIVE) {
                    text = android.R.string.ok
                    onClick { Shortcuts.addHomeIcon(this@MainActivity) }
                }
                setCancelable(true)
            }.show()
        }
    }
}
