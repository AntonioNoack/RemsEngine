package me.anno.io.json

class JsonObject : JsonNode() {

    val map = HashMap<String, Any?>()

    operator fun contains(key: String) =
        map.containsKey(key)

    operator fun set(key: String, value: Any?) {
        map[key] = value
    }

    override operator fun get(key: String): JsonNode? {
        return map[key]?.toJsonNode()
    }

    // all those cannot be parsed
    override fun asBool(default: Boolean) = default
    override fun asInt(default: Int) = default
    override fun asLong(default: Long) = default
    override fun asFloat(default: Float) = default
    override fun asDouble(default: Double) = default
    override fun getString(key: String): String? = map[key]?.toString()

    // we can skip the conversion to a JsonValue
    override fun getBool(key: String, default: Boolean) = JsonValue.asBool(map[key], default)
    override fun getInt(key: String, default: Int) = JsonValue.asInt(map[key], default)
    override fun getLong(key: String, default: Long) = JsonValue.asLong(map[key], default)
    override fun getFloat(key: String, default: Float) = JsonValue.asFloat(map[key], default)
    override fun getDouble(key: String, default: Double) = JsonValue.asDouble(map[key], default)

    fun getValue(key: String) = map[key]

    override fun toString() = map.toString()

}