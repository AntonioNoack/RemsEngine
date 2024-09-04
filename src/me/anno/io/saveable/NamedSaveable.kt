package me.anno.io.saveable

import me.anno.io.base.BaseWriter
import me.anno.utils.InternalAPI

/**
 * something that should be saveable, but also nameable by editors or users
 * */
open class NamedSaveable : Saveable() {

    var name = ""
        set(value) {
            if (field != value) {
                field = value
                onChangeNameDesc()
            }
        }

    // probably rarely used, so it could become a WeakHashMap-entry...
    @InternalAPI
    var description = ""
        set(value) {
            if (field != value) {
                field = value
                onChangeNameDesc()
            }
        }

    open fun onChangeNameDesc() {}

    override val approxSize get() = 10

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("name", name)
        writer.writeString("desc", description)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "name" -> this.name = value as? String ?: return
            "desc", "description" -> this.description = value as? String ?: return
            else -> super.setProperty(name, value)
        }
    }
}