package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.joml.Quaterniond
import org.joml.Vector3d

class ChangeSetEntityAttribute : ChangeSetAttribute {
    constructor(value: Any?) : super(value, 3)
    constructor() : super(null, 3)
    constructor(path: Path, value: Any?) : this(value) {
        this.path = path
    }

    override fun applyChange(element: Any?, name: String?) {
        element as Entity
        when (name) {
            "name" -> element.name = value.toString()
            "description", "desc" -> element.description = value.toString()
            "isEnabled" -> element.isEnabled = value as? Boolean ?: return
            "position" -> element.transform.localPosition = value as? Vector3d ?: return
            "rotation" -> element.transform.localRotation = value as? Quaterniond ?: return
            "scale" -> element.transform.localScale = value as? Vector3d ?: return
            "prefab" -> {
                element.prefabPath = value as? FileReference ?: return
                element.prefab = if (element.prefabPath == InvalidRef) null
                else TODO()//PrefabInspector.currentInspector!!.getPrefab(element.prefabPath)
            }
            null -> {
            }
            else -> element[name] = value
        }
    }

    override val className: String = "ChangeSetEntityAttribute"
}
