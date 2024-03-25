package me.anno.engine.serialization

/**
 * use this to mark a property for serialization
 *
 * public properties are serializable by default
 * */
annotation class SerializedProperty(val name: String = "", val forceSaving: Boolean = false)