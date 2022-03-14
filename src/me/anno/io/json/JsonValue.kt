package me.anno.io.json

class JsonValue(val value: Any?) : JsonNode() {

    override fun get(key: String): JsonNode? = null
    override fun asInt(default: Int) = asInt(value, default)
    override fun asLong(default: Long) = asLong(value, default)
    override fun asFloat(default: Float) = asFloat(value, default)
    override fun asDouble(default: Double) = asDouble(value, default)
    override fun asText() = value.toString()

    override fun toString() = value.toString()

    companion object {

        fun asInt(value: Any?, default: Int = 0): Int {
            return when (value) {
                null -> default
                false -> 0
                true -> 1
                is Int -> value
                is Long -> value.toInt()
                is Float -> value.toInt()
                is Double -> value.toInt()
                else -> value.toString().toIntOrNull() ?: default
            }
        }

        fun asLong(value: Any?, default: Long = 0L): Long {
            return when (value) {
                null -> default
                false -> 0
                true -> 1
                is Int -> value.toLong()
                is Long -> value
                is Float -> value.toLong()
                is Double -> value.toLong()
                else -> value.toString().toLongOrNull() ?: default
            }
        }

        fun asFloat(value: Any?, default: Float = 0f): Float {
            return when (value) {
                null -> default
                false -> 0f
                true -> 1f
                is Int -> value.toFloat()
                is Long -> value.toFloat()
                is Float -> value
                is Double -> value.toFloat()
                else -> value.toString().toFloatOrNull() ?: default
            }
        }

        fun asDouble(value: Any?, default: Double = 0.0): Double {
            return when (value) {
                null -> default
                false -> 0.0
                true -> 1.0
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                is Float -> value.toDouble()
                is Double -> value
                else -> value.toString().toDoubleOrNull() ?: default
            }
        }

        fun asBool(value: Any?, default: Boolean = false): Boolean {
            return asInt(value, if (default) 1 else 0) != 0
        }

    }
}