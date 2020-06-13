package me.anno.studio.history

import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import java.util.*
import kotlin.math.max

object History: Saveable(){

    // history states instead of history entries

    // todo history with branches, commits, etc?
    // logarithmic history? -> no, just make it long enough?

    // limit the size of elements? -> nah, disk space is cheap ^^
    // save them on the disk, because they are relatively rarely changed anyways?
    // nah, they are changed every step...

    private var maxHistoryElements = max(1, DefaultConfig["maxHistoryElements", 256])
    private val elements = LinkedList<HistoryState<*>>()
    private var nextInsertIndex = 0

    override fun getClassName() = "History"
    override fun getApproxSize(): Int = 1_000_000

    fun clear(){
        synchronized(elements){
            elements.clear()
        }
    }

    fun undo(){
        synchronized(elements){
            // if the next insert index was 1,
            // then there were only 1 entry,
            // so there would be nothing to undo
            if(nextInsertIndex > 1){
                nextInsertIndex--
                elements[nextInsertIndex-1].apply()
            }
        }
    }

    fun redo(){
        synchronized(elements){
            if(nextInsertIndex < elements.size){
                elements[nextInsertIndex++].apply()
            }
        }
    }

    fun put(element: HistoryState<*>){
        synchronized(elements){
            while(nextInsertIndex > elements.size && elements.isNotEmpty()){
                elements.pop()
            }
            elements.push(element)
            nextInsertIndex = elements.size
            popIfFull()
        }
    }

    fun popIfFull(){
        synchronized(elements){
            while(elements.size > maxHistoryElements){
                elements.pollFirst()
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("nextInsertIndex", nextInsertIndex)
        writer.writeList(this, "elements", elements)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "nextInsertIndex" -> nextInsertIndex = max(0, value)
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "elements" -> {
                if(value is HistoryState<*>){
                    elements.add(value)
                }
            }
            else -> super.readObject(name, value)
        }
    }

    override fun isDefaultValue() = false

}