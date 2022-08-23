package me.anno.parser

fun lerpAny(a: Any, b: Any, f: Double): Any {
    return addAny(
        mulAny(1.0-f, a),
        mulAny(f, b)
    )
}

fun mulAny(a: Any, b: Any): Any {
    if(a is Double){
        return when(b){
            is Double -> a * b
            is Vector -> b.map { mulAny(a, it) }
            else -> throw RuntimeException("todo implement $a * $b")
        }
    }
    if(b is Double) return mulAny(b, a)
    throw RuntimeException("todo implement $a * $b")
}

fun divAny(a: Any, b: Any): Any {
    if(a is Double){
        return when(b){
            is Double -> a / b
            is Vector -> b.map { divAny(a, it) }
            else -> throw RuntimeException("todo implement $a / $b")
        }
    }
    if(b is Double) return mulAny(1.0/b, a)
    throw RuntimeException("todo implement $a / $b")
}

fun modAny(a: Any, b: Any): Any {
    if(a is Double){
        return when(b){
            is Double -> a % b
            is Vector -> b.map { modAny(a, it) }
            else -> throw RuntimeException("todo implement $a % $b")
        }
    }
    if(b is Double){
        return when(a){
            is Vector -> a.map { modAny(it, b) }
            else -> throw RuntimeException("todo implement $a % $b")
        }
    }
    throw RuntimeException("todo implement $a % $b")
}


fun crossAny(a: Any, b: Any): Any {
    throw RuntimeException("todo implement $a cross $b")
}

fun addAny(a: Any, b: Any): Any {
    if(a is Double){
        return when(b){
            is Double -> a + b
            is Vector -> b.map { addAny(a, it) }
            else -> throw RuntimeException()
        }
    }
    if(b is Double) return addAny(b, a)
    throw RuntimeException("todo implement $a + $b")
}

fun subAny(a: Any, b: Any): Any {
    if(a is Double){
        return when(b){
            is Double -> a - b
            is Vector -> b.map { subAny(a, it) }
            else -> throw RuntimeException()
        }
    }
    if(b is Double) return addAny(-b, a)
    throw RuntimeException("todo implement $a - $b")
}