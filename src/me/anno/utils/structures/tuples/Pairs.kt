package me.anno.utils.structures.tuples

operator fun <V> Pair<V, V>.get(isFirst: Boolean): V = if (isFirst) first else second