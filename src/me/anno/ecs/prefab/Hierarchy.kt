package me.anno.ecs.prefab

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.sdf.modifiers.SDFHalfSpace
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.JsonFormatter
import me.anno.io.text.TextWriter
import me.anno.io.zip.InnerTmpFile
import me.anno.studio.StudioBase
import me.anno.utils.OS.documents
import org.apache.logging.log4j.LogManager
import org.joml.Planef
import org.joml.Vector3d

object Hierarchy {

    // todo to switch the order, renumber things
    // but also we could just implement this operation as add+remove of all following children
    // typically it won't be THAT many

    private val LOGGER = LogManager.getLogger(Hierarchy::class)

    private fun extractPrefab(element: PrefabSaveable, copyPasteRoot: Boolean): Prefab {
        val prefab = Prefab(element.className)
        val elPrefab = element.prefab
        if (!copyPasteRoot) prefab.prefab = elPrefab?.prefab?.nullIfUndefined() ?: elPrefab?.source ?: InvalidRef
        prefab.ensureMutableLists()
        val adders = prefab.adds as ArrayList
        val path = element.prefabPath!!
        LOGGER.info("For copy path: $path")
        // collect changes from this element going upwards
        var someParent = element
        val collDepth = element.depthInHierarchy
        prefab[Path.ROOT_PATH, "name"] = element.name
        // setters.add(CSet(Path.ROOT_PATH, "name", element.name))
        // todo why can its parent be null?
        if (!copyPasteRoot && someParent.parent != null) someParent = someParent.parent!!
        val startIndex = if (copyPasteRoot) collDepth else collDepth - 1
        for (depth in startIndex downTo 0) {// from element to root
            LOGGER.info("Checking depth $depth/$collDepth, ${someParent.name}")
            var someRelatedParent = someParent.prefab
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
                someRelatedParent = PrefabCache[someRelatedParent.prefab] ?: break
            }
            someParent = someParent.parent ?: break
        }
        prefab.isValid = false
        LOGGER.info("Found: ${prefab.prefab}, prefab: ${elPrefab?.prefab}, own file: ${elPrefab?.source}, has prefab: ${elPrefab != null}")
        return prefab
    }

    fun stringify(element: PrefabSaveable): String {
        return TextWriter.toText(extractPrefab(element, false), StudioBase.workspace)
    }

    fun getInstanceAt(instance0: PrefabSaveable, path: Path): PrefabSaveable? {

        var instance = instance0
        try {
            path.fromRootToThis(false) { pathIndex, pathI ->

                val childType = pathI.type
                val components = instance.getChildListByType(childType)

                val childIndex = pathI.index
                if (
                    childIndex in components.indices &&
                    components[childIndex].prefabPath == pathI
                ) {
                    // bingo, easiest way: path is matching
                    instance = components[childIndex]
                } else {
                    val match = components.firstOrNull { it.prefabPath == pathI }
                    if (match != null) instance = match
                    else {
                        var foundMatch = false
                        for (type in instance.listChildTypes()) {
                            val match2 = instance.getChildListByType(type).firstOrNull { it.prefabPath == pathI }
                            if (match2 != null) {
                                LOGGER.warn("Child $pathI had incorrect type '$childType', actual type was '$type'")
                                foundMatch = true
                                instance = match2
                                break
                            }
                        }
                        if (!foundMatch) {
                            LOGGER.warn(
                                "Missing path (${Thread.currentThread().name}) $path[$pathIndex] (${path.getNames()}, ${path.getTypes()}, ${path.getIndices()}) in $instance, " +
                                        "only ${components.size} $childType available ${components.joinToString { "'${it.name}':${it.prefabPath}" }}"
                            )
                            throw Path.EXIT
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            if (e === Path.EXIT) return null
            throw e
        }
        return instance
    }

    fun add(srcPrefab: Prefab, srcPath: Path, dst: PrefabSaveable) =
        add(srcPrefab, srcPath, dst.root.prefab!!, dst.prefabPath!!, dst)

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
            LOGGER.debug("src == dst, so trying extraction")
            val element = srcPrefab.getSampleInstance()
            return add(extractPrefab(element, true), Path.ROOT_PATH, dstPrefab, dstParentPath, dstParentInstance)
        } else {
            // find all necessary changes
            if (srcPath.isEmpty()) {
                LOGGER.debug("Path is empty")
                // find correct type and insert index
                val srcSample = getInstanceAt(srcPrefab.getSampleInstance(), srcPath)!!
                val type = dstParentInstance.getTypeOf(srcSample)
                val nameId = Path.generateRandomId()
                val clazz = srcPrefab.clazzName
                val srcPrefabSource = srcSample.prefab?.prefab ?: InvalidRef
                if (type == ' ') LOGGER.warn("Adding type '$type' (${dstParentInstance.className} += $clazz), might not be supported")
                val dstPath = dstPrefab.add(dstParentPath, type, clazz, nameId, srcPrefabSource)
                LOGGER.debug("Adding element '$nameId' of class $clazz, type '$type' to path '$dstPath'")
                val adds = srcPrefab.adds
                assert(adds !== dstPrefab.adds)
                for (index1 in adds.indices) {
                    val change = adds[index1]
                    dstPrefab.add(change.withPath(Path(dstPath, change.path), true))
                }
                val sets = srcPrefab.sets
                sets.forEach { k1, k2, v ->
                    dstPrefab[Path(dstPath, k1), k2] = v
                }
                ECSSceneTabs.updatePrefab(dstPrefab)
                return dstPath
            } else {
                LOGGER.debug("Extraction")
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
        val type = dstPath.lastType()
        val nameId = dstPath.lastNameId()
        val dstPath2 = dstPrefab.add(
            dstPath.parent ?: Path.ROOT_PATH,
            type, child.className, nameId,
            child.prefab?.source ?: InvalidRef
        )
        if (dstPath2 != dstPath) throw IllegalStateException("Could not add child at index, $dstPath vs $dstPath2")
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
        // hopefully correct...
        parent.addChildByType(dstPath2.lastIndex(), type, child)
        ECSSceneTabs.updatePrefab(dstPrefab)
        // val index = dstPath.indices.last()
        // parent.addChildByType(index, type, child)
    }

    fun removePathFromPrefab(
        prefab: Prefab,
        saveable: PrefabSaveable
    ) = removePathFromPrefab(
        prefab,
        saveable.prefabPath ?: throw RuntimeException("Saveable is missing prefab path"),
        saveable.className
    )

    fun removePathFromPrefab(
        prefab: Prefab,
        path: Path,
        clazzName: String
    ) {

        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)

        if (path.isEmpty()) {
            LOGGER.warn("Cannot remove root!")
            prefab[path, "isEnabled"] = false
            return
        }

        // remove the instance at this path completely
        // if this is not possible, go as far as possible, and disable the instance

        // remove all properties
        prefab.ensureMutableLists()
        val sets = prefab.sets
        // sets as MutableList

        LOGGER.info("Removing ${sets.count { k1, _, _ -> k1.startsWith(path) }}, " +
                "sets: ${sets.filterMajor { k1 -> k1.startsWith(path) }.entries.joinToString { it.key.toString() }}, " +
                "all start with $path"
        )
        sets.removeMajorIf { it.startsWith(path) }
        // sets.removeIf { it.path.startsWith(path) }
        prefab.isValid = false

        val parentPath = path.parent

        val adds = prefab.adds

        // val parentPrefab = loadPrefab(prefab.prefab)
        val sample = prefab.getSampleInstance()
        val child = getInstanceAt(sample, path)

        if (child == null) {
            fun printAvailablePaths(instance: PrefabSaveable) {
                LOGGER.info(instance.prefabPath)
                for (childType in instance.listChildTypes()) {
                    for (childI in instance.getChildListByType(childType)) {
                        printAvailablePaths(childI)
                    }
                }
            }
            LOGGER.warn("Could not find path '$path' in sample!")
            LOGGER.info(path)
            printAvailablePaths(sample)
            return
        }

        val parent = child.parent!!
        val type = path.lastType()
        var indexInParent = parent.getChildListByType(type).indexOf(child)
        if (indexInParent < 0) {
            for (childType in parent.listChildTypes()) {
                if (childType != type) {
                    indexInParent = parent.getChildListByType(childType).indexOf(child)
                    if (indexInParent >= 0) {
                        LOGGER.warn("Path had incorrect type '$type', found child in '$childType'")
                        break
                    }
                }
            }
            if (indexInParent < 0) {
                LOGGER.warn("Could not find child in parent! Internal error!!")
                return
            }
        }

        val name = path.lastNameId()
        val lambda = { it: CAdd ->
            it.path == parentPath && it.type == type &&
                    it.clazzName == clazzName && it.nameId == name
        }
        val matches = adds.filter(lambda)
        when (matches.size) {
            0 -> {
                LOGGER.info("did not find add @$parentPath, prefab: ${prefab.source}:${prefab.prefab}, ${prefab.adds}, ${prefab.sets}")
                prefab[path, "isEnabled"] = false
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
                // todo renumber stuff
                // val t = HashSet<IntArray>()
                // renumber(path.lastIndex(), -1, path, adds, t)
                // renumber(path.lastIndex(), -1, path, sets, t)
                prefab.invalidateInstance()
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
        val elementA = prefab.add(Path.ROOT_PATH, 'e', "Entity", "A")
        val elementB = prefab.add(Path.ROOT_PATH, 'e', "Entity", "B")
        val elementC = prefab.add(elementB, 'e', "Entity", "C")
        prefab[elementC, "position"] = Vector3d()
        // Root
        // - A
        // - B
        // - - C
        val sample0 = prefab.getSampleInstance() as Entity
        val numElements1 = sample0.sizeOfHierarchy
        if (numElements1 != 4) throw IllegalStateException("incorrect number of elements: $numElements1")
        removePathFromPrefab(prefab, elementA, "Entity")
        val sample1 = prefab.getSampleInstance() as Entity
        val numElements2 = sample1.sizeOfHierarchy
        if (numElements2 != 3) throw IllegalStateException("incorrect number of elements: $numElements2")
        // renumbering is currently disabled, because it only is a hint
        /*if (prefab.adds.any { it.path.isNotEmpty() && it.path.firstIndex() > 0 }) {
            LOGGER.warn(JsonFormatter.format(sample1.toString()))
            for (add in prefab.adds) {
                LOGGER.warn(add)
            }
            LOGGER.warn(prefab.sets)
            throw IllegalStateException("there still is adds, which are non-empty and the firstIndex > 0")
        }*/
    }

    fun assert(boolean: Boolean) {
        if (!boolean) throw RuntimeException()
    }

    fun assert(a: Any?, b: Any?) {
        if (a != b) throw RuntimeException("$a != $b")
    }

    fun testRemoval2() {
        val prefab = Prefab("Entity")
        val clazz = "PointLight"
        val n = 10
        val names = Array(n) { "child$it" }
        for (i in 0 until n) {
            val child = prefab.add(Path.ROOT_PATH, 'c', clazz, names[i])
            prefab.setProperty(child, "description", "desc$i")
            prefab.setProperty(child, "lightSize", i.toDouble())
        }
        assert(prefab.adds.size, n)
        assert(prefab.sets.size, 2 * n)
        val tested = intArrayOf(1, 2, 3, 5, 7)
        for (i in tested.sortedDescending()) {
            val sample = prefab.getSampleInstance() as Entity
            removePathFromPrefab(prefab, sample.components[i])
        }
        // test prefab
        assert(prefab.adds.size, n - tested.size)
        assert(prefab.sets.size, 2 * (n - tested.size))
        // test result
        val sample = prefab.getSampleInstance() as Entity
        assert(sample.components.size, n - tested.size)
        for (i in 0 until n) {
            // test that exactly those still exist, that we didn't remove
            assert(sample.components.count { it.name == names[i] }, if (i in tested) 0 else 1)
        }
        Engine.requestShutdown()
    }

    private fun testJsonFormatter() {
        val ref = getReference(documents, "RemsEngine/SampleProject/Scene.json")
        val prefab = PrefabCache[ref]
        println(JsonFormatter.format(prefab.toString()))
    }

    private fun testPrefab() {
        val prefab = Prefab("Entity")
        val sample1 = prefab.getSampleInstance()
        assert(sample1 is Entity)
        val child = prefab.add(Path.ROOT_PATH, 'c', "PointLight")
        prefab[child, "lightSize"] = Math.PI
        val sample2 = prefab.getSampleInstance()
        assert(sample2 is Entity)
        sample2 as Entity
        assert(sample2.components.count { it is PointLight }, 1)
        val light1 = getInstanceAt(sample2, child)
        println("found ${light1?.prefabPath} at $child")
        assert(light1 is PointLight)
        assert((light1 as PointLight).lightSize == Math.PI)
    }

    private fun testMultiAdd() {
        val prefab = Prefab("SDFBox")
        val count = 3
        for (i in 0 until count) {
            val child = Prefab("SDFHalfSpace")
            child[Path.ROOT_PATH, "plane"] = Planef(0f, 1f, 0f, i.toFloat())
            add(child, Path.ROOT_PATH, prefab, Path.ROOT_PATH)
        }
        println(prefab.adds)
        println(prefab.sets)
        val inst = prefab.getSampleInstance() as SDFBox
        for (i in 0 until count) {
            val dist = inst.distanceMappers[i] as SDFHalfSpace
            assert(dist.plane.d, i.toFloat())
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        ECSRegistry.initNoGFX()
        println("----------------------")
        testMultiAdd()
        println("----------------------")
        testPrefab()
        println("----------------------")
        testRemoval2()
        println("----------------------")
        testAdd()
        println("----------------------")
        testRenumberRemove()
        println("----------------------")
        // testJsonFormatter()
        println("----------------------")
        Engine.requestShutdown()
    }

}