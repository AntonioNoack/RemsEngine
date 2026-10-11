package speiger.primitivecollections

object ToStringHelper {
    fun StringBuilder.nextKV(): StringBuilder {
        return append(if (isEmpty()) "{" else ", ")
    }

    fun StringBuilder.eqKV(): StringBuilder {
        return append('=')
    }

    fun StringBuilder.finishKVMap(): String {
        if (isEmpty()) return "{}"
        append("}")
        return toString()
    }
}