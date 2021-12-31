package me.anno.objects.documents

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.utils.types.Strings.isBlank2

object SiteSelection : CacheSection("SiteSelection") {

    fun parseSites(sites: String): List<IntRange> {
        if (sites.isBlank2()) return listOf(0 until maxSite)
        val cacheData = getEntry(sites, timeout, false) {
            val delta = -1
            val list = sites
                .replace('+', ',')
                .replace(';', ',')
                .split(',')
                .mapNotNull {
                    val fi = it.indexOf('-')
                    if (fi < 0) {
                        it.trim().toIntOrNull()?.run { (this + delta)..(this + delta) }
                    } else {
                        it.substring(0, fi).trim().toIntOrNull()?.run {
                            val a = this
                            val b = it.substring(fi + 1).trim().toIntOrNull() ?: maxSite
                            (a + delta)..(b + delta)
                        }
                    }
                }
                .filter { !it.isEmpty() }
            CacheData(list)
        } as CacheData<*>
        @Suppress("UNCHECKED_CAST")
        return cacheData.value as List<IntRange>
    }

    private val timeout = 1000L
    private val maxSite = 100_000_000

}