package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Prefab
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.structures.maps.KeyPairMap

class PrefabChanges(
    val adds: HashMap<Path, ArrayList<CAdd>>,
    val sets: KeyPairMap<Path, String, Any?>
) : Saveable() {

    constructor() : this(HashMap(), KeyPairMap())
    constructor(prefab: Prefab) : this(prefab.adds, prefab.sets)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(null, "adds", adds.values.flatten())
        writer.writeObjectList(null, "sets", sets.map { k1, k2, v -> CSet(k1, k2, v) })
    }

    override fun setProperty(name: String, value: Any?) {
        if (value !is List<*>) return
        when (name) {
            "adds" -> {
                adds.clear()
                for (v in value) {
                    v as? CAdd ?: continue
                    adds.getOrPut(v.path, ::ArrayList).add(v)
                }
            }
            "sets" -> {
                sets.clear()
                for (v in value) {
                    v as? CSet ?: continue
                    sets[v.path, v.name] = v.value
                }
            }
            else -> super.setProperty(name, value)
        }
    }
}