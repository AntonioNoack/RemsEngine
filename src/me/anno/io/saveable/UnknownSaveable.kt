package me.anno.io.saveable

import me.anno.io.base.BaseWriter

/**
 * When a single class cannot be found, we might still be fine ->
 *  load stuff as far as possible, and just create stubs like this when sth couldn't be loaded properly
 * */
class UnknownSaveable() : Saveable() {
    override var className = "UnknownSaveable"

    constructor(className: String) : this() {
        this.className = className
    }

    private val properties = LinkedHashMap<String, Any?>()
    override fun setProperty(name: String, value: Any?) {
        properties[name] = value
    }

    override fun get(propertyName: String): Any? {
        return properties[propertyName]
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        for ((name, value) in properties) {
            writer.writeSomething(this, name, value, true)
        }
    }
}