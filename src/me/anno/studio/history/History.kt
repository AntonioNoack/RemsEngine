package me.anno.studio.history

import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.math.max

class History: Saveable(){

    // history states instead of history entries

    // todo history with branches, commits, etc?
    // todo hierarchic history, so ending of editing a field only saves the final state??
    // logarithmic history? -> no, just make it long enough?

    // limit the size of elements? -> nah, disk space is cheap ^^
    // save them on the disk, because they are relatively rarely changed anyways?
    // nah, they are changed every step...

    private var maxHistoryElements = max(1, DefaultConfig["maxHistoryElements", 256])
    val elements = ArrayList<HistoryState<*>>(maxHistoryElements)
    var nextInsertIndex = 0

    override fun getClassName() = "History"
    override fun getApproxSize(): Int = 1_000_000

    fun clear(){
        synchronized(elements){
            elements.clear()
        }
    }

    fun undo(){
        LOGGER.info("undo")
        synchronized(elements){
            // if the next insert index was 1,
            // then there were only 1 entry,
            // so there would be nothing to undo
            if(nextInsertIndex > 1){
                elements[nextInsertIndex-- - 2].apply()
            }
        }
    }

    fun redo(){
        LOGGER.info("redo")
        synchronized(elements){
            if(nextInsertIndex < elements.size){
                elements[nextInsertIndex++].apply()
            }
        }
    }

    /**
     * put the old state here, so it can be reversed
     * */
    fun put(element: HistoryState<*>){
        if(nextInsertIndex > 0 && element == elements[nextInsertIndex-1]){
            // don't push, if they are the same
            return
        }
        synchronized(elements){
            while(elements.size > nextInsertIndex && elements.isNotEmpty()){
                elements.removeAt(elements.lastIndex)
            }
            if(elements.size+1 > maxHistoryElements){
                elements.removeAt(0)
            }
            elements.add(element)
            nextInsertIndex = elements.size
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

    companion object {
        private val LOGGER = LogManager.getLogger(History::class)
    }

}