package me.anno.studio.history

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

abstract class HistoryState<V>(): Saveable(){

    var state: V? = null

    fun apply(){
        apply(state as V)
    }

    abstract fun apply(state: V)

    abstract fun writeState(writer: BaseWriter, name: String, v: V)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        if(state != null){
            writeState(writer, "state", state!!)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when(name){
            "state" -> state = value as V?
            else -> super.readSomething(name, value)
        }
    }

    override fun getApproxSize() = 10_000
    override fun isDefaultValue() = false

}