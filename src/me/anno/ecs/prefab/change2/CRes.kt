package me.anno.ecs.prefab.change2

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import javax.print.attribute.standard.MediaSize
import kotlin.test.assertIsNot

class CRes : Saveable {

    // todo each prefab defines resources, and their positions in the hierarchy
    // todo these resources are defined by index,name,id
    // todo when an old resource is missing, indicate it somehow, or do automatic matching by index, name, className, etc...

    constructor() : super()

    constructor(clazzName: String, prefab: Prefab2) : this() {
        this.clazzName = clazzName
        this.owner = prefab
    }

    constructor(clazzName: String, name: String, prefab: Prefab2) : this(clazzName, prefab) {
        this.name = name
    }

    // where the resource is coming from
    // invalidRef = self
    var origin: FileReference = InvalidRef
    var owner: Prefab2? = null // our owner
    var clazzName: String = ""
    var name: String = ""
    var id: String = ""

    var sourceIndex: Int = 0 // where in the source it was coming from

    // placement in hierarchy
    var parent: CRes? = null
    var insertIndex: Int = 0 // -1/0 = at the start, int.max = at the end


    // not saved
    var instance: ISaveable? = null

    fun apply() {
        val instance = if (origin == InvalidRef) {
            // self
            val instance = ISaveable.create(clazzName)
            if(instance is NamedSaveable){
                instance.name = name
            }
            if(instance is PrefabSaveable){
                instance.prefab3 = owner
                instance.res = this
            }
            instance
        } else {
            // find the instance definition from our parent prefab

            TODO()
        }
        // todo add to parent

    }

}