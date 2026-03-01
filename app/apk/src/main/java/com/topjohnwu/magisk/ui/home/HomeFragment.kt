package com.topjohnwu.magisk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.arch.BaseFragment
import com.topjohnwu.magisk.arch.viewModel
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.databinding.FragmentHomeMd2Binding
import com.topjohnwu.magisk.ui.MainActivity
import com.topjohnwu.magisk.core.R as CoreR

class HomeFragment : BaseFragment<FragmentHomeMd2Binding>(), MenuProvider {

    override val layoutRes = R.layout.fragment_home_md2
    override val viewModel by viewModel<HomeViewModel>()

    override fun onStart() {
        super.onStart()
        // 1. LSPosed 风格通常不显示多余的顶部 Title，因为 AppBar 已经处理了
        // 但为了兼容性，我们保留更新 activity 标题的逻辑
        activity?.setTitle(CoreR.string.section_home)
        DownloadEngine.observeProgress(this, viewModel::onProgressUpdate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 2. 使用新版 MenuProvider API (对标 LSPosed/M3 现代架构)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // 3. 确保进入首页时，底栏一定是显示的 (调用 MainActivity 重写的函数)
        (activity as? MainActivity)?.requestNavigationHidden(false)
    }

    private fun checkTitle(text: TextView, icon: ImageView) {
        text.post {
            // Material 3 布局通常更宽绰，如果标题还是溢出，直接隐藏图标以保持简洁
            if (text.layout?.getEllipsisCount(0) ?: 0 != 0) {
                icon.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        // 4. 对标 LSPosed 首页卡片样式：移除不必要的图标挤压检查，如果布局已经够宽
        with(binding.homeMagiskWrapper) {
            checkTitle(homeMagiskTitle, homeMagiskIcon)
        }
        with(binding.homeManagerWrapper) {
            checkTitle(homeManagerTitle, homeManagerIcon)
        }

        return root
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        // 5. 换用 M3 风格的菜单文件
        inflater.inflate(R.menu.menu_home_md2, menu)
        if (!Info.isRooted)
            menu.removeItem(R.id.action_reboot)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val action = HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                view?.findNavController()?.navigate(action)
                true
            }
            R.id.action_reboot -> {
                activity?.let { RebootMenu.inflate(it).show() }
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.stateManagerProgress = 0
        // 再次确保底栏状态正确
        (activity as? MainActivity)?.requestNavigationHidden(false)
    }
}
