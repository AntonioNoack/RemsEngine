package me.anno.cache.debug

import me.anno.Time
import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.DualCacheSection
import me.anno.gpu.GFX
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.roundDiv
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption

object DebugCaches {

    fun openMenu() {
        Menu.openMenu(
            GFX.someWindow.windowStack, CacheSection.caches.map { section ->
                MenuOption(formatName(section)) {
                    openMenu(section)
                }
            } + DualCacheSection.caches.map { section ->
                MenuOption(formatName(section)) {
                    openMenu(section)
                }
            }
        )
    }

    private fun formatName(section: CacheSection<*, *>): NameDesc {
        return NameDesc("${section.name} (${section.cache.size})")
    }

    private fun formatName(section: DualCacheSection<*, *, *>): NameDesc {
        return NameDesc("${section.name} (${section.dualCache.size})")
    }

    private fun openMenu(section: CacheSection<*, *>) {
        // list all keys, maybe their timeouts, and maybe their values
        // todo remove common suffix (by group?) and show it separately
        val entries = section.cache.map { (k, v) -> createName1(k, v) }
        Menu.openMenu(GFX.someWindow.windowStack, entries.sorted().map { MenuOption(NameDesc(it)) {} })
    }

    private fun openMenu(section: DualCacheSection<*, *, *>) {
        // list all keys, maybe their timeouts, and maybe their values
        // todo remove common suffix (by group?) and show it separately
        val entries = section.dualCache.map { k1, k2, v -> createName2(k1, k2, v) }
        Menu.openMenu(GFX.someWindow.windowStack, entries.sorted().map { MenuOption(NameDesc(it)) {} })
    }

    private fun collectValueInfo(value: AsyncCacheData<*>): String {
        return formatTime(value.timeoutNanoTime - Time.nanoTime)
    }

    private fun formatTime(v: Long): String {
        return when {
            v < 0 -> "-${formatTime(-v)}"
            v < 10_000 -> "${v}ns"
            v < 10_000_000 -> "${roundDiv(v, 1000)}µs"
            v < 10_000_000_000 -> "${roundDiv(v, 1000_000)}ms"
            else -> "${roundDiv(v, 1000_000_000)}s"
        }
    }

    private fun createName1(key: Any?, value: AsyncCacheData<*>): String {
        return "$key, ${collectValueInfo(value)}"
    }

    private fun createName2(k1: Any?, k2: Any?, value: AsyncCacheData<*>): String {
        return "($k1, $k2), ${collectValueInfo(value)}"
    }
}