package me.anno.io

import me.anno.io.base.BaseWriter

abstract class AutoSaveable : Saveable() {
    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }
    override fun setProperty(name: String, value: Any?) {
        setSerializableProperty(name, value)
    }
}