package me.anno.io.serialization

/**
 * use this to mark a property for serialization
 * it needs to be ISerializable
 *
 * public properties are serializable by default
 * */
annotation class SerializedProperty(val name: String = "", val forceSaving: Boolean = false)