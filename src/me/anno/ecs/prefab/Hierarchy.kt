package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache.getPrefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Change
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.io.zip.InnerTmpFile
import me.anno.utils.structures.StartsWith.startsWith
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

object Hierarchy {

    // todo to switch the order, renumber things
    // but also we could just implement this operation as add+remove of all following children
    // typically it won't be THAT many

    private val LOGGER = LogManager.getLogger(Hierarchy::class)

    private fun extractPrefab(element: PrefabSaveable, copyPasteRoot: Boolean): Prefab {
        val prefab = Prefab(element.className)
        val elPrefab = element.prefab2
        if (!copyPasteRoot) prefab.prefab = elPrefab?.prefab?.nullIfUndefined() ?: elPrefab?.source ?: InvalidRef
        prefab.ensureMutableLists()
        val adders = prefab.adds as ArrayList
        val setters = prefab.sets //as ArrayList
        val path = element.pathInRoot2()
        LOGGER.info("For copy path: $path")
        // collect changes from this element going upwards
        var someParent = element
        val collDepth = element.depthInHierarchy
        prefab.set(Path.ROOT_PATH, "name", element.name)
        // setters.add(CSet(Path.ROOT_PATH, "name", element.name))
        if (!copyPasteRoot) someParent = someParent.parent!!
        val startIndex = if (copyPasteRoot) collDepth else collDepth - 1
        for (depth in startIndex downTo 0) {// from element to root
            LOGGER.info("Checking depth $depth/$collDepth, ${someParent.name}")
            var someRelatedParent = someParent.prefab2
            while (someRelatedParent != null) {// follow the chain of prefab-inheritance
                val adds = someRelatedParent.adds
                val sets = someRelatedParent.sets
                LOGGER.info("Changes from $depth/${someRelatedParent.getPrefabOrSource()}: ${adds.size} + ${sets.size}")
                // get all changes
                // filter them & short them by their filter
                for (change in adds.mapNotNull { path.getSubPathIfMatching(it, depth) }) {
                    adders.add(change)
                }
                sets.forEach { k1, k2, v ->
                    val change = path.getSubPathIfMatching(k1, depth)
                    if (change != null) {
                        // don't apply changes twice, especially, because the order is reversed
                        // this would cause errors
                        prefab.setIfNotExisting(change, k2, v)
                        /*if (setters.none { it.path == change.path && it.name == change.name }) {
                            setters.add(change)
                        }*/
                    }
                }
                someRelatedParent = getPrefab(someRelatedParent.prefab) ?: break
            }
            someParent = someParent.parent ?: break
        }
        prefab.isValid = false
        LOGGER.info("Found: ${prefab.prefab}, prefab: ${elPrefab?.prefab}, own file: ${elPrefab?.source}, has prefab: ${elPrefab != null}")
        return prefab
    }

    fun stringify(element: PrefabSaveable): String {
        return TextWriter.toText(extractPrefab(element, false))
    }

    fun getInstanceAt(instance0: PrefabSaveable, path: Path): PrefabSaveable? {

        var instance = instance0
        for (pathIndex in 0 until path.size) {

            // we can go deeper :)
            val chars = instance.listChildTypes()
            val childName = path.getName(pathIndex)
            val childIndex = path.getIndex(pathIndex)
            val childType = path.getType(pathIndex, chars[0])

            val components = instance.getChildListByType(childType)

            instance = if (components.getOrNull(childIndex)?.name == childName) {
                // bingo, easiest way: name and index are matching
                components[childIndex]
            } else {
                val matchesName = components.firstOrNull { it.name == childName }
                when {
                    matchesName != null -> matchesName
                    childIndex in components.indices -> components[childIndex]
                    else -> {
                        LOGGER.warn(
                            "Missing path at index $pathIndex, '$childName','$childType',${childIndex} in $this, " +
                                    "only ${components.size} $childType available ${components.joinToString { "'${it["name"]}'" }}"
                        )
                        return null
                    }
                }
            }
        }
        return instance
    }

    fun add(
        srcPrefab: Prefab,
        srcPath: Path,
        dstPrefab: Prefab,
        dstParentPath: Path,
        dstParentInstance: PrefabSaveable = getInstanceAt(dstPrefab.getSampleInstance(), dstParentPath)!!
    ): Path? {
        if (!dstPrefab.isWritable) throw ImmutablePrefabException(dstPrefab.source)
        LOGGER.debug("Trying to add ${srcPrefab.source}/$srcPath to ${dstPrefab.source}/$dstParentPath")
        if (srcPrefab == dstPrefab || (srcPrefab.source == dstPrefab.source && srcPrefab.source != InvalidRef)) {
            val element = srcPrefab.getSampleInstance()
            return add(extractPrefab(element, true), Path.ROOT_PATH, dstPrefab, dstParentPath, dstParentInstance)
        } else {
            // find all necessary changes
            if (srcPath.isEmpty()) {
                // find correct type and insert index
                val srcSample = getInstanceAt(srcPrefab.getSampleInstance(), srcPath)!!
                val type = dstParentInstance.getTypeOf(srcSample)
                val index = dstParentInstance.getChildListByType(type).size
                val name = srcSample.name
                val clazz = srcPrefab.clazzName
                val prefab0 = srcSample.prefab2?.prefab ?: InvalidRef
                val add0 = CAdd(dstParentPath, type, clazz, name, prefab0)
                val dstPath = dstPrefab.add(add0, index)
                LOGGER.debug("Adding element to path $dstPath")
                val adds = srcPrefab.adds
                if (adds === dstPrefab.adds) throw IllegalStateException()
                for (change in adds) {
                    dstPrefab.add(change.withPath(Path(dstPath, change.path)))
                }
                val sets = srcPrefab.sets
                sets.forEach { k1, k2, v ->
                    dstPrefab.set(Path(dstPath, k1), k2, v)
                }
                ECSSceneTabs.updatePrefab(dstPrefab)
                return dstPath
            } else {
                val element = getInstanceAt(srcPrefab.getSampleInstance(), srcPath) ?: return null
                return add(extractPrefab(element, false), Path.ROOT_PATH, dstPrefab, dstParentPath, dstParentInstance)
            }
        }
    }

    fun add(
        dstPrefab: Prefab,
        dstPath: Path,
        parent: PrefabSaveable,
        child: PrefabSaveable,
    ) {
        if (!dstPrefab.isWritable) throw ImmutablePrefabException(dstPrefab.source)
        val type = dstPath.types.last()
        val name = child.name.ifEmpty { child.className }
        dstPrefab.add(CAdd(dstPath.getParent(), type, child.className, name, child.prefab2?.source ?: InvalidRef))
        val sample = ISaveable.getSample(child.className)!!
        for ((pName, field) in child.getReflections().allProperties) {
            if (field.serialize) {
                val value = child[pName]
                if (value != sample[pName]) {
                    // we can do it unsafe, because we just added the path,
                    // and know that there is nothing inside it
                    dstPrefab.setUnsafe(dstPath, pName, value)
                }
            }
        }
        ECSSceneTabs.updatePrefab(dstPrefab)
        // val index = dstPath.indices.last()
        // parent.addChildByType(index, type, child)
    }

    fun removePathFromPrefab(
        prefab: Prefab,
        element: PrefabSaveable
    ) = removePathFromPrefab(prefab, element.pathInRoot2(), element.className)

    fun removePathFromPrefab(
        prefab: Prefab,
        path: Path,
        clazzName: String
    ) {

        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)

        if (path.isEmpty()) {
            LOGGER.warn("Cannot remove root!")
            prefab.set(path, "isEnabled", false)
            return
        }

        // remove the instance at this path completely
        // if this is not possible, go as far as possible, and disable the instance

        // remove all properties
        prefab.ensureMutableLists()
        val sets = prefab.sets
        // sets as MutableList

        LOGGER.info("Removing ${sets.count { k1, _, _ -> k1.startsWith(path) }} sets")
        sets.removeMajorIf { it.startsWith(path) }
        // sets.removeIf { it.path.startsWith(path) }
        prefab.isValid = false

        val parentPath = path.getParent()

        val adds = prefab.adds

        // val parentPrefab = loadPrefab(prefab.prefab)
        val sample = prefab.getSampleInstance()
        val child = getInstanceAt(sample, path)

        if (child == null) {
            LOGGER.warn("Could not find path '$path' in sample!")
            return
        }

        val parent = child.parent!!
        val type = path.types.last()
        val indexInParent = parent.getChildListByType(type).indexOf(child)
        if (indexInParent < 0) {
            LOGGER.warn("Could not find child in parent! Internal error!!")
            return
        }

        val name = path.names.last()
        val lambda = { it: CAdd ->
            it.path == parentPath && it.type == type &&
                    it.clazzName == clazzName && it.name == name
        }
        val matches = adds.filter(lambda)
        when (matches.size) {
            0 -> {
                LOGGER.info("did not find add @$parentPath, prefab: ${prefab.source}:${prefab.prefab}, ${prefab.adds}, ${prefab.sets}")
                prefab.set(path, "isEnabled", false)
            }
            else -> {
                if (matches.size == 1) {
                    LOGGER.info("Removing single add")
                } else {
                    LOGGER.warn("There were two items with the same name: illegal! Removing one of them")
                }
                adds as MutableList
                // remove all following things
                adds.removeAt(adds.indexOfFirst(lambda))
                adds.removeIf { it.path.startsWith(path) }
                val t = HashSet<IntArray>()
                renumber(path.indices.last(), -1, path, adds, t)
                renumber(path.indices.last(), -1, path, sets, t)
                prefab.invalidate()
            }
        }

        ECSSceneTabs.updatePrefab(prefab)

    }

    private fun testAdd() {
        // test
        val scene = Prefab("Entity", ScenePrefab)
        scene.source = InnerTmpFile.InnerTmpPrefabFile(scene)
        val sample0 = scene.getSampleInstance() as Entity
        val size0 = sample0.sizeOfHierarchy
        val added = add(scene, Path.ROOT_PATH, scene, Path.ROOT_PATH)!!
        val sample1 = scene.getSampleInstance() as Entity
        val size1 = sample1.sizeOfHierarchy
        if (size0 * 2 != size1) {
            LOGGER.warn(sample0)
            LOGGER.warn(sample1)
            throw RuntimeException("Sizes don't match: $size0*2 vs $size1")
        }
        removePathFromPrefab(scene, added, "Entity")
        val sample2 = scene.getSampleInstance() as Entity
        val size2 = sample2.sizeOfHierarchy
        if (size0 != size2) {
            LOGGER.warn(sample0)
            LOGGER.warn(sample2)
            throw RuntimeException("Removal failed: $size0 vs $size2")
        }
    }

    private fun testRenumberRemove() {
        val prefab = Prefab("Entity")
        val elementA = prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "A"))
        val elementB = prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "B"))
        val elementC = prefab.add(CAdd(elementB.getChildPath(1), 'e', "Entity", "C"))
        val pathC = elementC.getChildPath(0)
        prefab.set(pathC, "position", Vector3d())
        // Root
        // - A
        // - B
        // - - C
        val sample0 = prefab.getSampleInstance() as Entity
        val numElements = sample0.sizeOfHierarchy
        if (numElements != 4) throw IllegalStateException()
        removePathFromPrefab(prefab, elementA.getChildPath(0), "Entity")
        val sample1 = prefab.getSampleInstance() as Entity
        val numElements2 = sample1.sizeOfHierarchy
        if (numElements2 != 3) throw IllegalStateException("number of elements: $numElements2")
        if (prefab.adds.any { it.path.isNotEmpty() && it.path.indices[0] > 0 }) {
            LOGGER.warn(sample1)
            LOGGER.warn(prefab.adds)
            LOGGER.warn(prefab.sets)
            throw IllegalStateException()
        }
    }

    /**
     * renumber all changes, which are relevant to the components
     * */
    private fun renumber(
        from: Int, delta: Int, path0: Path, changes: List<Change>,
        changedArrays: HashSet<IntArray> = HashSet()
    ): HashSet<IntArray> {
        val targetSize = path0.indices.size
        val targetIndex = targetSize - 1
        for (change in changes) {
            val path = change.path
            val indices = path.indices
            val types = path.types
            if (path.size >= targetSize &&
                indices[targetIndex] >= from &&
                indices !in changedArrays &&
                indices.startsWith(path0.indices, 0, targetIndex) &&
                types.startsWith(path0.types, 0, targetIndex)
            ) {
                val str0 = path.toString()
                indices[targetIndex] += delta
                changedArrays.add(indices)
                LOGGER.info("Renumbered $str0 to $path")
            }
        }
        return changedArrays
    }

    private fun renumber(
        from: Int, delta: Int, path0: Path, changes: KeyPairMap<Path, String, Any?>,
        changedArrays: HashSet<IntArray> = HashSet()
    ): HashSet<IntArray> {
        val targetSize = path0.indices.size
        val targetIndex = targetSize - 1
        changes.forEach { path, _, _ ->
            val indices = path.indices
            val types = path.types
            if (path.size >= targetSize &&
                indices[targetIndex] >= from &&
                indices !in changedArrays &&
                indices.startsWith(path0.indices, 0, targetIndex) &&
                types.startsWith(path0.types, 0, targetIndex)
            ) {
                val str0 = path.toString()
                indices[targetIndex] += delta
                changedArrays.add(indices)
                LOGGER.info("Renumbered $str0 to $path")
            }
        }
        return changedArrays
    }

    @JvmStatic
    fun main(args: Array<String>) {
        ECSRegistry.initNoGFX()
        // testAdd()
        testRenumberRemove()
    }

}