package me.anno.utils.test

import me.anno.objects.Video
import me.anno.utils.LOGGER
import java.io.File
import kotlin.math.min

fun main(){
    val file = File("C:\\Users\\Antonio\\Documents\\Blender\\Image Sequence\\%.png")
    val meta = ImageSequenceMeta(file)
    LOGGER.info(meta.toString())
}

class ImageSequenceMeta(file: File){

    var matches: List<Pair<File, Double>>

    init {
        val key = file.name
        val identifier = Video.imageSequenceIdentifier
        val i0 = key.indexOf(identifier)
        val i1 = i0 + identifier.length
        val prefix = key.substring(0, i0)
        val suffix = key.substring(i1)
        // LOGGER.info("'$prefix' $i0-$i1 '$suffix'")
        matches = (file.parentFile.listFiles() ?: emptyArray())
            .mapNotNull { child ->
                val name = child.name
                if(name.startsWith(prefix) && name.endsWith(suffix)){
                    val time = name.substring(i0, name.length - suffix.length).toDoubleOrNull()
                    // LOGGER.info("$child, $name, ${name.substring(i0, i1)}: $time")
                    if(time == null) null
                    else {
                        child to time
                    }
                } else null
            }
            .sortedBy { it.second }
    }


    private val startTime = matches.firstOrNull()?.second ?: 0.0
    val duration = (matches.lastOrNull()?.second ?: 1.0) - startTime

    val isValid = matches.isNotEmpty()

    fun getIndex(time: Double): Int {
        val localTime = time - startTime
        var index = matches.binarySearch { it.second.compareTo(localTime) }
        if(index < 0) index = -1-index
        return min(index, matches.lastIndex)
    }

    fun getImage(index: Int): File {
        return matches[index].first
    }

    fun getImage(time: Double): File {
        return matches[getIndex(time)].first
    }

    override fun toString() = "$duration: $matches"

}