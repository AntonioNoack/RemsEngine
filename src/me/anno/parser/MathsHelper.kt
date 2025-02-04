package me.anno.parser

fun lerpAny(a: Any?, b: Any?, f: Double): Any {
    return addAny(
        mulAny(1.0 - f, a),
        mulAny(f, b)
    )
}

fun mulAny(a: Any?, b: Any?): Any {
    if (a is Exception) return a
    if (b is Exception) return b
    if (a is Double) {
        return when (b) {
            is Double -> a * b
            is Vector -> b.map { mulAny(a, it) }
            else -> RuntimeException("todo implement $a * $b")
        }
    }
    return if (b is Double) mulAny(b, a)
    else RuntimeException("todo implement $a * $b")
}

fun divAny(a: Any?, b: Any?): Any {
    if (a is Exception) return a
    if (b is Exception) return b
    if (a is Double) {
        return when (b) {
            is Double -> a / b
            is Vector -> b.map { divAny(a, it) }
            else -> RuntimeException("todo implement $a / $b")
        }
    }
    return if (b is Double) mulAny(1.0 / b, a)
    else RuntimeException("todo implement $a / $b")
}

fun modAny(a: Any?, b: Any?): Any {
    if (a is Exception) return a
    if (b is Exception) return b
    if (a is Double) {
        return when (b) {
            is Double -> a % b
            is Vector -> b.map { modAny(a, it) }
            else -> RuntimeException("todo implement $a % $b")
        }
    }
    if (b is Double) {
        return when (a) {
            is Vector -> a.map { modAny(it, b) }
            else -> RuntimeException("todo implement $a % $b")
        }
    }
    return RuntimeException("todo implement $a % $b")
}


fun crossAny(a: Any?, b: Any?): Any {
    if (a is Exception) return a
    if (b is Exception) return b
    return RuntimeException("todo implement $a cross $b")
}

fun addAny(a: Any?, b: Any?): Any {
    if (a is Exception) return a
    if (b is Exception) return b
    if (a is Double) {
        return when (b) {
            is Double -> a + b
            is Vector -> b.map { addAny(a, it) }
            else -> RuntimeException()
        }
    }
    return if (b is Double) addAny(b, a)
    else RuntimeException("todo implement $a + $b")
}

fun subAny(a: Any?, b: Any?): Any {
    if (a is Exception) return a
    if (b is Exception) return b
    if (a is Double) {
        return when (b) {
            is Double -> a - b
            is Vector -> b.map { subAny(a, it) }
            else -> throw RuntimeException()
        }
    }
    if (b is Double) return addAny(-b, a)
    throw RuntimeException("todo implement $a - $b")
}