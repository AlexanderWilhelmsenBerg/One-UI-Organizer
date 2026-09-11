package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.emulators

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRule
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppSelector

object EmulatorKnownAppRules {
    val entries: List<KnownAppRule> =
        listOf(
            packageRule("com.dsemu.drastic"),
            packageRule("com.flycast.emulator"),
            packageRule("com.github.stenzek.duckstation"),
            packageRule("com.retroarch"),
            packageRule("info.cemu.cemu"),
            packageRule("io.github.lime3ds.android"),
            packageRule("net.rpcsx"),
            packageRule("org.citra.citra_emu"),
            packageRule("org.citron.citron_emu"),
            packageRule("org.dolphinemu.dolphinemu"),
            packageRule("org.ppsspp.ppsspp"),
            packageRule("org.scummvm.scummvm"),
            packageRule("org.sudachi.sudachi_emu"),
            packageRule("xyz.aethersx2.android"),
            componentRule(
                packageName = "com.miHoYo.Yuanshen",
                className = "org.yuzu.yuzu_emu.ui.main.MainActivity"
            )
        )

    private fun packageRule(packageName: String): KnownAppRule = KnownAppRule(
        selector = KnownAppSelector.ExactPackage(AppId(packageName)),
        category = AppCategory.EMULATORS
    )

    private fun componentRule(packageName: String, className: String): KnownAppRule = KnownAppRule(
        selector = KnownAppSelector.ExactComponent(LaunchTargetId(packageName, className)),
        category = AppCategory.EMULATORS
    )
}
