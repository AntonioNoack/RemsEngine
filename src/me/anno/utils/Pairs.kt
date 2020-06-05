package me.anno.utils



operator fun <A> Pair<A, A>.get(isFirst: Boolean) = if(isFirst) first else second