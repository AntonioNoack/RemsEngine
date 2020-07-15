package me.anno.io.json

abstract class JsonNode {
    abstract fun get(key: String): JsonNode?
    fun asText() = toString()
}