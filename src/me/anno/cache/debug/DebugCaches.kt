package me.anno.cache.debug

import me.anno.Time
import me.anno.cache.CacheEntry
import me.anno.cache.CacheSection
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
            }
        )
    }

    private fun formatName(section: CacheSection): NameDesc {
        return NameDesc("${section.name} (${section.cache.size},${section.dualCache.size})")
    }

    private fun openMenu(section: CacheSection) {
        // list all keys, maybe their timeouts, and maybe their values
        // todo remove common suffix and show it separately
        Menu.openMenu(
            GFX.someWindow.windowStack,
            (section.cache.map { (k, v) -> MenuOption(createName1(k, v)) {} } +
                    section.dualCache.map { k1, k2, v -> MenuOption(createName2(k1, k2, v)) {} })
                .sortedBy { it.title }
        )
    }

    private fun collectValueInfo(value: CacheEntry): String {
        return "${formatTime(value.timeoutNanoTime - Time.nanoTime)}, \"${value.generatorThread.name}\""
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

    private fun createName1(key: Any?, value: CacheEntry): NameDesc {
        return NameDesc("$key, ${collectValueInfo(value)}")
    }

    private fun createName2(k1: Any?, k2: Any?, value: CacheEntry): NameDesc {
        return NameDesc("($k1, $k2), ${collectValueInfo(value)}")
    }
}