package me.anno.io.serialization

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class CachedProperty(
    val forceSaving: Boolean?,
    val getter: KProperty1.Getter<*, *>,
    val setter: KMutableProperty1.Setter<*, *>
)
