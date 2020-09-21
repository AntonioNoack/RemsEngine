package me.anno.studio.history

import me.anno.io.base.BaseWriter
import me.anno.studio.Studio
import me.anno.studio.Studio.history

class TestState(name: String): HistoryState<String>(){
    init {
        state = name
    }
    override fun apply(state: String) {
        println("current: $state")
    }
    override fun writeState(writer: BaseWriter, name: String, v: String) {
        writer.writeString(name, state)
    }
    override fun getClassName() = ""
}

fun main(){

    val history = history

    fun check(title: String, action: () -> Unit){
        println("\nGoal: $title, (from ${Studio.history.nextInsertIndex})")
        action()
        println("Result: ${Studio.history.elements.map { (it as TestState).state }}, ${Studio.history.nextInsertIndex}")
    }

    Studio.history.put(TestState("A"))
    Studio.history.put(TestState("B"))

    check("A") {
        history.undo()
    }

    check("B") {
        history.redo()
    }

    check("A"){
        history.undo()
    }

    check("C"){
        history.put(TestState("C"))
    }

    check("A"){
        history.undo()
    }

    check("nothing happens, still A"){
        history.undo()
    }

    check("D"){
        history.put(TestState("D"))
    }

    check("E"){
        history.put(TestState("E"))
    }

    check("D"){
        history.undo()
    }

    check("A"){
        history.undo()
    }

    check("D"){
        history.redo()
    }

    check("E"){
        history.redo()
    }

    check("nothing, still E"){
        history.redo()
    }

    check("D"){
        history.undo()
    }

}