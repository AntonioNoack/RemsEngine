package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache.getPrefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.io.zip.InnerTmpFile
import me.anno.utils.structures.StartsWith.startsWith
import org.apache.logging.log4j.LogManager

object Hierarchy {

    private val LOGGER = LogManager.getLogger(Hierarchy::class)

    fun extractPrefab(element: PrefabSaveable, copyPasteRoot: Boolean): Prefab {
        val prefab = Prefab(element.className)
        val elPrefab = element.prefab2
        if (!copyPasteRoot) prefab.prefab = elPrefab?.prefab?.nullIfUndefined() ?: elPrefab?.source ?: InvalidRef
        prefab.createLists()
        val adders = prefab.adds as ArrayList
        val setters = prefab.sets as ArrayList
        val path = element.pathInRoot2()
        LOGGER.info("for copy path: $path")
        // collect changes from this element going upwards
        var someParent = element
        val collDepth = element.depthInHierarchy
        setters.add(CSet(Path.ROOT_PATH, "name", element.name))
        if (!copyPasteRoot) someParent = someParent.parent!!
        val startIndex = if (copyPasteRoot) collDepth else collDepth - 1
        for (depth in startIndex downTo 0) {// from element to root
            LOGGER.info("checking depth $depth/$collDepth, ${someParent.name}")
            var someRelatedParent = someParent.prefab2
            while (someRelatedParent != null) {// follow the chain of prefab-inheritance
                val adds = someRelatedParent.adds
                val sets = someRelatedParent.sets
                LOGGER.info("changes from $depth/${someRelatedParent.getPrefabOrSource()}: ${adds?.size} + ${sets?.size}")
                // get all changes
                // filter them & short them by their filter
                if (adds != null) for (change in adds.mapNotNull { path.getSubPathIfMatching(it, depth) }) {
                    adders.add(change)
                }
                if (sets != null) for (change in sets.mapNotNull { path.getSubPathIfMatching(it, depth) }) {
                    // don't apply changes twice, especially, because the order is reversed
                    // this would cause errors
                    if (setters.none { it.path == change.path && it.name == change.name }) {
                        setters.add(change)
                    }
                }
                someRelatedParent = getPrefab(someRelatedParent.prefab) ?: break
            }
            someParent = someParent.parent ?: break
        }
        LOGGER.info("found: ${prefab.prefab}, prefab: ${elPrefab?.prefab}, own file: ${elPrefab?.source}, has prefab: ${elPrefab != null}")
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
                        throw RuntimeException()
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
        println("trying to add ${srcPrefab.source}/$srcPath to ${dstPrefab.source}/$dstParentPath")
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
                val clazz = srcPrefab.clazzName!!
                val prefab0 = srcSample.prefab2?.prefab ?: InvalidRef
                val add0 = CAdd(dstParentPath, type, clazz, name, prefab0)
                val dstPath = dstPrefab.add(add0, index)
                LOGGER.info("adding element to path $dstPath")
                val adds = srcPrefab.adds
                val sets = srcPrefab.sets
                if (adds === dstPrefab.adds) throw IllegalStateException()
                if (adds != null) for (change in adds) {
                    dstPrefab.add(change.withPath(Path(dstPath, change.path)))
                }
                if (sets != null) for (change in sets) {
                    dstPrefab.add(change.withPath(Path(dstPath, change.path)))
                }
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
        val type = dstPath.types.last()
        val name = child.name.ifEmpty { child.className }
        dstPrefab.add(CAdd(dstPath.getParent(), type, child.className, name, child.prefab2?.source ?: InvalidRef))
        val sample = ISaveable.getSample(child.className)!!
        for (pName in child.getReflections().allProperties.keys) {
            val value = child[pName]
            if (value != sample[pName]) {
                dstPrefab.add(CSet(dstPath, pName, value))
            }
        }
        // val index = dstPath.indices.last()
        // parent.addChildByType(index, type, child)
    }

    fun remove(
        prefab: Prefab,
        path: Path
    ) {

        // remove the instance at this path completely
        // if this is not possible, go as far as possible, and disable the instance

        // remove all properties
        val sets = prefab.sets
        if (sets != null) {
            sets as MutableList
            LOGGER.info("Removing ${sets.count { it.path.startsWith(path) }} sets")
            sets.removeIf { it.path.startsWith(path) }
            prefab.isValid = false
        }

        val parentPath = path.getParent()

        val adds = prefab.adds ?: emptyList()
        val matches = adds.filter { it.path == parentPath }
        when (matches.size) {
            0 -> {
                LOGGER.info("did not find add @$parentPath, prefab: ${prefab.source}:${prefab.prefab}, ${prefab.adds}, ${prefab.sets}")
                prefab.add(CSet(path, "isEnabled", false))
            }
            1 -> {
                LOGGER.info("Removing single add")
                adds as MutableList
                adds.removeIf { it.path.startsWith(parentPath) }
                prefab.isValid = false
            }
            else -> {
                // todo find the correct index...
                // todo how many elements where there in the parent for this element

                // todo also renumber all following adds & changes

                TODO()
            }
        }
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

    @JvmStatic
    fun main(args: Array<String>) {
        // test
        ECSRegistry.initNoGFX()
        ISaveable.create("CAdd")
        val scene = Prefab("Entity", ScenePrefab)
        scene.source = InnerTmpFile.InnerTmpPrefabFile(scene)
        val sample0 = scene.getSampleInstance() as Entity
        val size0 = sample0.sizeOfHierarchy
        val added = add(scene, Path.ROOT_PATH, scene, Path.ROOT_PATH)!!
        val sample1 = scene.getSampleInstance() as Entity
        val size1 = sample1.sizeOfHierarchy
        if (size0 * 2 != size1) {
            println(sample0)
            println(sample1)
            throw RuntimeException("Sizes don't match: $size0*2 vs $size1")
        }
        remove(scene, added)
        val sample2 = scene.getSampleInstance() as Entity
        val size2 = sample2.sizeOfHierarchy
        if (size0 != size2){
            println(sample0)
            println(sample2)
            throw RuntimeException("Removal failed: $size0 vs $size2")
        }
    }

}