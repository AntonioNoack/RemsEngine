package me.anno.studio.dependencies

import me.anno.cache.CacheSection

class Resource(val cache: CacheSection, val key: Any, val generator: () -> Unit)
