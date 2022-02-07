package me.anno.video

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.min

class ImageSequenceMeta(file: FileReference) {

    var matches: List<Pair<FileReference, Double>>

    init {
        val key = file.name
        val identifier = imageSequenceIdentifier
        val i0 = key.indexOf(identifier)
        val i1 = i0 + identifier.length
        val prefix = key.substring(0, i0)
        val suffix = key.substring(i1)
        // LOGGER.info("'$prefix' $i0-$i1 '$suffix'")
        // val file2 = file.unsafeFile
        matches = (file.getParent()?.listChildren() ?: emptyList())
            .mapNotNull { child ->
                val name = child.name
                if (name.startsWith(prefix) && name.endsWith(suffix)) {
                    val time = name.substring(i0, name.length - suffix.length).toDoubleOrNull()
                    // LOGGER.info("$child, $name, ${name.substring(i0, i1)}: $time")
                    if (time == null) null
                    else child to time
                } else null
            }
            .sortedBy { it.second }
    }


    private val startTime = matches.firstOrNull()?.second ?: 0.0
    val duration = (matches.lastOrNull()?.second ?: 1.0) - startTime + 1.0 // last frame for one second
    val frameCount = matches.size

    val isValid = matches.isNotEmpty()

    fun getIndex(time: Double): Int {
        val localTime = time - startTime
        var index = matches.binarySearch { it.second.compareTo(localTime) }
        if (index < 0) index = -1 - index
        return min(index, matches.lastIndex)
    }

    fun getImage(index: Int): FileReference {
        return matches[index].first
    }

    fun getImage(time: Double): FileReference {
        return matches[getIndex(time)].first
    }

    override fun toString() = "$duration: $matches"

    companion object {

        val imageSequenceIdentifier get() = DefaultConfig["video.imageSequence.identifier", "%"]

        private val LOGGER = LogManager.getLogger(ImageSequenceMeta::class)

        @JvmStatic
        fun main(args: Array<String>) {
            val file = File("C:\\Users\\Antonio\\Documents\\Blender\\Image Sequence\\%.jpg")
            val meta = ImageSequenceMeta(getReference(file))
            LOGGER.info(meta.toString())
            /*meta.matches.forEach { (file, _) ->
                val src = ImageIO.read(file)
                val dst = src.withoutAlpha()
                val out = File(file.parentFile, file.nameWithoutExtension + ".jpg")
                ImageIO.write(dst, "jpg", out)
            }*/
        }
    }

}