package me.anno.io.base

import me.anno.io.Saveable
import me.anno.io.utils.StringMap
import me.anno.objects.Image
import me.anno.objects.Text
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Keyframe
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import java.io.File
import java.lang.RuntimeException

abstract class BaseReader {

    val content = HashMap<Long, Saveable>()
    val missingReferences = HashMap<Long, ArrayList<Pair<Saveable, String>>>()

    fun getNewClassInstance(clazz: String): Saveable {
        return when(clazz){
            "SMap" -> StringMap()
            "Transform" -> Transform(null)
            "Text" -> Text("", null)
            "Circle" -> Circle(null)
            "Polygon" -> Polygon(null)
            "Image" -> Image(File(""), null)
            "Video" -> Video(File(""), null)
            "AnimatedProperty<float>" -> AnimatedProperty.float()
            "AnimatedProperty<pos>" -> AnimatedProperty.pos()
            "AnimatedProperty<scale>" -> AnimatedProperty.scale()
            "AnimatedProperty<rotYXZ>" -> AnimatedProperty.rotYXZ()
            "AnimatedProperty<skew2D>" -> AnimatedProperty.skew()
            "AnimatedProperty<color>" -> AnimatedProperty.color()
            "AnimatedProperty<quaternion>" -> AnimatedProperty.quat()
            "Keyframe" -> Keyframe<Any>(0f, 0f)
            else -> throw RuntimeException("Unknown class $clazz")
        }
    }

    fun register(value: Saveable){
        val uuid = value.uuid
        if(uuid != 0L){
            content[uuid] = value
            missingReferences[uuid]?.forEach { (obj, name) ->
                obj.readObject(name, value)
            }
        } else println("got object with uuid 0: $value, it will be ignored")
    }

    fun addMissingReference(owner: Saveable, name: String, childPtr: Long){
        val list = missingReferences[childPtr]
        val entry = owner to name
        if(list != null){
            list += entry
        } else {
            missingReferences[childPtr] = arrayListOf(entry)
        }
    }

    fun assert(b: Boolean, msg: String){
        if(!b) throw RuntimeException(msg)
    }

    fun assertChar(isValue: Char, shallValue: Char){
        if(isValue != shallValue) throw RuntimeException("Expected $shallValue but got $isValue")
    }

    fun error(msg: String): Nothing = throw RuntimeException(msg)

}