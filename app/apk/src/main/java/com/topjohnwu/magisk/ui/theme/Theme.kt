package com.topjohnwu.magisk.ui.theme

import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.core.Config

enum class Theme(
    val themeName: String,
    val themeRes: Int
) {

    // 默认浅色主题 (LSPosed 风格)
    Piplup(
        themeName = "Material You (Light)",
        themeRes = R.style.Theme_Foundation_Light
    ),
    
    // 默认深色/AMOLED主题
    PiplupAmoled(
        themeName = "Material You (Dark)",
        themeRes = R.style.Theme_Foundation
    );

    // 下面这些原本的主题可以暂时保留定义以防止其他地方引用报错，
    // 但我们将它们全部指向你的新主题。
    
    // Rayquaza(themeName = "Rayquaza", themeRes = R.style.Theme_Foundation),
    // ...以此类推
    
    val isSelected get() = Config.themeOrdinal == ordinal

    companion object {
        // 这里的逻辑保证了无论用户之前选了哪个主题，
        // 都会跳转到我们定义的 LSP 风格主题上
        val selected get() = values().getOrNull(Config.themeOrdinal) ?: Piplup
    }
}
