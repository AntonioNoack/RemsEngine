package me.anno.utils.structures.tuples

operator fun <A> Pair<A, A>.get(isFirst: Boolean) = if(isFirst) first else second