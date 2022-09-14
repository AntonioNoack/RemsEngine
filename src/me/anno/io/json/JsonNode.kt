package me.anno.io.json

abstract class JsonNode {

    abstract fun get(key: String): JsonNode?

    open fun asText() = toString()
    open fun getString(key: String) = get(key)?.asText()

    open fun asBool(default: Boolean = false) = asInt(if (default) 1 else 0) != 0
    open fun asInt(default: Int = 0) = JsonValue.asInt(asText(), default)
    open fun asLong(default: Long = 0L) = JsonValue.asLong(asText(), default)
    open fun asFloat(default: Float = 0f) = JsonValue.asFloat(asText(), default)
    open fun asDouble(default: Double = 0.0) = JsonValue.asDouble(asText(), default)

    open fun getInt(key: String, default: Int = 0) = JsonValue.asInt(get(key), default)
    open fun getLong(key: String, default: Long = 0L) = JsonValue.asLong(get(key), default)
    open fun getFloat(key: String, default: Float = 0f) = JsonValue.asFloat(get(key), default)
    open fun getDouble(key: String, default: Double = 0.0) = JsonValue.asDouble(get(key), default)
    open fun getBool(key: String, default: Boolean = false) = getInt(key, if (default) 1 else 0) != 0

    companion object {
        @Suppress("unused")
        fun Any?.toJsonNode(): JsonNode {
            val value = this
            if (value is JsonNode) return value
            return JsonValue(value)
        }
    }

}