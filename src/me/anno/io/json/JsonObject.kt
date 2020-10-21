package me.anno.io.json

import me.anno.io.json.ObjectMapper.toJsonNode

class JsonObject : JsonNode() {

    val map = HashMap<String, Any>()

    operator fun set(key: String, value: Any) {
        map[key] = value
    }

    override operator fun get(key: String): JsonNode? {
        return map[key]?.toJsonNode()
    }

    fun getValue(key: String) = map[key]

    override fun toString() = map.toString()

}