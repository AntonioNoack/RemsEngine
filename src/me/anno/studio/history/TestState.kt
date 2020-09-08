package me.anno.studio.history

import me.anno.io.base.BaseWriter

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

    fun check(title: String, action: () -> Unit){
        println("\nGoal: $title, (from ${History.nextInsertIndex})")
        action()
        println("Result: ${History.elements.map { (it as TestState).state }}, ${History.nextInsertIndex}")
    }

    History.put(TestState("A"))
    History.put(TestState("B"))

    check("A") {
        History.undo()
    }

    check("B") {
        History.redo()
    }

    check("A"){
        History.undo()
    }

    check("C"){
        History.put(TestState("C"))
    }

    check("A"){
        History.undo()
    }

    check("nothing happens, still A"){
        History.undo()
    }

    check("D"){
        History.put(TestState("D"))
    }

    check("E"){
        History.put(TestState("E"))
    }

    check("D"){
        History.undo()
    }

    check("A"){
        History.undo()
    }

    check("D"){
        History.redo()
    }

    check("E"){
        History.redo()
    }

    check("nothing, still E"){
        History.redo()
    }

    check("D"){
        History.undo()
    }

}