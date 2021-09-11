package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Change
import me.anno.ecs.prefab.change.Path
import me.anno.io.ISaveable
import me.anno.utils.structures.StartsWith.startsWith

object Hierarchy {


    fun add(
        src: Prefab,
        srcPath: Path,
        dst: Prefab,
        dstParentPath: Path,
        index: Int
    ) {

    }


    // todo completely rework these functions

    fun add(
        dst: Prefab,
        dstPath: Path,
        child: Prefab,
        index: Int
    ) {
        if (child.src == dst.src) {
            // this ofc won't work, because it would cause infinite recursion on instantiation
            // -> extract changes and try again
            add(
                dst, dstPath, index,
                child.clazzName!!,
                ' ',// todo get the type in parent...
                child.adds!! + child.sets!!
            )
        } else {
            // todo add the item with all its own changes
            // todo get the name somehow
            val childPath = dst.add(CAdd(dstPath, ' ', child.clazzName!!, child.name, child.getPrefabOrSource()))

            TODO()
        }
    }

    fun add(
        dst: Prefab,
        dstPath: Path,
        index: Int,
        childClass: String,
        childType: Char,
        changes: List<Change>
    ) {
        // todo all later items, that we defined, must be renumbered
        TODO()
    }

    fun remove(dst: Prefab, path: Path) {
        // todo remove the instance at this path completely
        // todo if this is not possible, go as far as possible, and disable the instance

    }


    /**
     * renumber all changes, which are relevant to the components
     * */
    private fun renumber(from: Int, delta: Int, path: Path, sets: List<CSet>) {
        val targetSize = path.indices.size
        val changedArrays = HashSet<IntArray>()
        for (change in sets) {
            val path2 = change.path
            val indices = path2.indices
            val types = path2.types
            if (indices.size == targetSize &&
                indices[targetSize - 1] >= from &&
                indices !in changedArrays &&
                indices.startsWith(path.indices) &&
                types.startsWith(path.types)
            ) {
                indices[targetSize - 1] += delta
                changedArrays.add(indices)
            }
        }
    }

    fun addExistingChild(
        root: PrefabSaveable,
        adds: MutableList<CAdd>, sets: MutableList<CSet>,
        parent: PrefabSaveable, child: PrefabSaveable, index: Int, type: Char
    ) {

        val hierarchyPrefabs = parent.listOfHierarchy.mapNotNull { it.prefab2 }.map { it.getPrefabOrSource() }

        // todo add all changes
        // todo if there are prefabs, which describe the existing changes, use them, as long, as they are not part of this hierarchy
        // todo actually add a copy of it


    }

    fun addNewChild(
        root: PrefabSaveable,
        adds: MutableList<CAdd>, sets: MutableList<CSet>,
        parent: PrefabSaveable, child: PrefabSaveable, index: Int, type: Char
    ) {

        val parentPath = parent.pathInRoot2(root, false)

        val prefab = parent.prefab

        child.parent = parent

        val prefabComponents = prefab?.getChildListByType(type)
        if (prefab != null && index < prefabComponents!!.size) {
            // if index < prefab.size, then disallow
            throw RuntimeException("Cannot insert between prefab components!")
        }

        val parentComponents = parent.getChildListByType(type)
        if (index < parentComponents.size) {
            renumber(index, +1, parentPath, sets)
        }

        parent.addChildByType(index, type, child)

        // just append it :)
        adds.add(CAdd(parentPath, 'c', child.className, child.name))

        // if it contains any changes, we need to apply them
        val sample = ISaveable.getSample(child.className)!!
        val compPath = parentPath.added(child.name, index, type)

        for (name in child.getReflections().allProperties.keys) {
            val value = child[name]
            if (value != sample[name]) {
                sets.add(CSet(compPath, name, value))
            }
        }

    }

    fun removeChild(
        root: PrefabSaveable,
        adds: MutableList<CAdd>, sets: MutableList<CSet>,
        parent: PrefabSaveable, child: PrefabSaveable, type: Char
    ) {

        val parentPath = parent.pathInRoot2(root, false)
        val childPath = child.pathInRoot2(root, false)

        val prefab = parent.prefab

        val components = parent.getChildListByType(type)
        if (child !in components) return // done

        val index = components.indexOf(child)
        val prefabComponents = prefab?.getChildListByType(type)
        if (prefab != null && index < prefabComponents!!.size) {

            // original component, cannot be removed
            // remove all the changes, that we applied? yes :)
            adds.removeIf { it.path.startsWith(parentPath) }
            sets.removeIf { it.path.startsWith(parentPath) }
            child.isEnabled = false

        } else {

            // when a component is deleted, its changes need to be deleted as well
            sets.removeIf { it.path == childPath }

            if (index + 1 < components.size) {
                // not the last one
                renumber(index + 1, -1, parentPath, sets)
            }

            // it's ok, and fine
            // remove the respective change
            parent.deleteChild(child)
            // not very elegant, but should work...
            // correct?

            // todo only remove first one
            adds.removeIf { it.path == parentPath }
            val prefabList = prefab?.getChildListByType(type)
            val i0 = (prefabList?.size ?: 0)
            for (i in i0 until components.size) {
                val componentI = components[i]
                adds.add(i - i0, CAdd(parentPath, type, componentI.className, componentI.name))
            }

        }

    }

}