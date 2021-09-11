package me.anno.ecs.prefab.change2

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

class Prefab2 : Saveable {

    constructor() : super()

    constructor(clazzName: String) : super() {
        resources.add(CRes(clazzName, this))
    }

    constructor(clazzName: String, name: String) : super() {
        resources.add(CRes(clazzName, name, this))
    }

    var source: FileReference = InvalidRef
    var parent: Prefab2? = null

    val resources = ArrayList<CRes>()
    val changes = ArrayList<CSet2>()

    private var sampleInstance: Saveable? = null
    private var isValid: Boolean = false
    fun getSampleInstance(): Saveable {
        if (!isValid) synchronized(this) {
            if (!isValid) {
                isValid = true
                for (res in resources) res.apply()
                for (change in changes) change.apply()
            }
        }
        return sampleInstance!!
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // writer.writeFile("source", source) defined by where it was loaded from

        writer.writeObjectList(this, "resources", resources)
        writer.writeObjectList(this, "changes", changes)
    }

}