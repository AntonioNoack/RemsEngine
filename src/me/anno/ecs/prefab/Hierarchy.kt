package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import org.apache.logging.log4j.LogManager

object Hierarchy {

    private val LOGGER = LogManager.getLogger(Hierarchy::class)

    private fun findAdd(prefab: Prefab, srcSetPath: Path, maxRecursiveDepth: Int = 100): CAdd? {
        val parent = srcSetPath.parent ?: Path.ROOT_PATH
        for (add in prefab.adds) {
            if (add.path == parent && add.nameId == srcSetPath.nameId) {
                return add
            }
        }
        val newRecursionDepth = maxRecursiveDepth - 1
        if (newRecursionDepth < 0) {
            LOGGER.warn("Too many iterations for findAdd()")
            return null
        }
        val prefab1 = PrefabCache[prefab.prefab]
        if (prefab1 != null) {
            val className = findAdd(prefab1, srcSetPath, newRecursionDepth)
            if (className != null) return className
        }
        // check sub paths prefabs
        for (add in prefab.adds) {
            val prefabPath = add.prefab
            if (prefabPath != InvalidRef) {
                val prefab2 = PrefabCache[prefabPath]
                if (prefab2 != null) {
                    val addPath = add.getSetterPath(0)
                    val newSearchPath = srcSetPath.startsWith1(addPath)
                    if (newSearchPath != null) {
                        val className = findAdd(prefab2, newSearchPath, newRecursionDepth)
                        if (className != null) return className
                    }
                }
            }
        }
        // not found
        return null
    }

    private fun extractPrefab(srcPrefab: Prefab, srcPath: Path, srcAdd: CAdd?): Prefab {

        val className = when {
            srcAdd != null -> srcAdd.clazzName
            srcPath == Path.ROOT_PATH -> srcPrefab.clazzName
            else -> findAdd(srcPrefab, srcPath)?.clazzName ?: throw RuntimeException("Instance was not found")
        }

        val dstPrefab = Prefab(className)
        dstPrefab.ensureMutableLists()
        dstPrefab.isValid = false

        val isRoot = srcPath == Path.ROOT_PATH && srcAdd == null
        if (isRoot) {
            // simple copy-paste
            dstPrefab.prefab = srcPrefab.prefab
            srcPrefab.adds.forEach {
                dstPrefab.add(it.clone(), -1)
            }
            srcPrefab.sets.forEach { k1, k2, v ->
                dstPrefab[k1, k2] = v
            }
            return dstPrefab
        }

        val srcSetPath = if (srcAdd == null) srcPath else
            srcPath.added(srcAdd.nameId, 0, srcAdd.type)

        LOGGER.info("For copy path: $srcSetPath")

        fun processPrefab(prefab: Prefab, prefabRootPath: Path) {
            prefab.adds.forEach { add ->
                val herePath = prefabRootPath + add.getSetterPath(0)
                val startsWithPath = herePath.startsWith1(srcSetPath)
                if (startsWithPath != null) {
                    if (startsWithPath != Path.ROOT_PATH) {
                        // can simply reference it, and we're done
                        dstPrefab.add(startsWithPath.parent!!, add.type, add.clazzName, add.nameId, add.prefab)
                    }// else done: this was already added via Prefab(className)
                } else if (add.prefab != InvalidRef) {
                    val prefab2 = PrefabCache[add.prefab]
                    if (prefab2 != null) {
                        val startsWithPath2 = srcSetPath.startsWith1(herePath)
                        if (startsWithPath2 != null) {
                            // check out all properties from the prefab
                            processPrefab(prefab, herePath)
                        }
                    }
                }
            }
            prefab.sets.forEach { path, name, value ->
                // todo there is such a thing as relative paths (Rigidbody)... can they be copied?
                // check if set is applicable
                val herePath = prefabRootPath + path
                val startsWithPath = herePath.startsWith1(srcSetPath)
                if (startsWithPath != null) {
                    dstPrefab[startsWithPath, name] = value
                }
            }
        }

        processPrefab(srcPrefab, Path.ROOT_PATH)

        // collect changes from this element going upwards
        /*var someParent = element
        val collDepth = element.depthInHierarchy
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
                        dstPrefab.setIfNotExisting(change, k2, v)
                        /*if (setters.none { it.path == change.path && it.name == change.name }) {
                            setters.add(change)
                        }*/
                    }
                }
                someRelatedParent = PrefabCache[someRelatedParent.prefab] ?: break
            }
            someParent = someParent.parent ?: break
        }
        */

        LOGGER.info("Found: ${dstPrefab.prefab}, prefab: ${srcPrefab.prefab}, own file: ${srcPrefab.source}")
        LOGGER.info("check start")
        dstPrefab.getSampleInstance()
        LOGGER.info("check end")
        return dstPrefab
    }

    fun stringify(element: PrefabSaveable): String {
        return TextWriter.toText(
            extractPrefab(element.prefab!!, element.prefabPath!!, null),
            StudioBase.workspace
        )
    }

    fun getInstanceAt(instance0: PrefabSaveable, path: Path): PrefabSaveable? {

        if (path == Path.ROOT_PATH) return instance0

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
                    // bingo; easiest way: path is matching
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

    fun add(srcPrefab: Prefab, srcPath: Path, dst: PrefabSaveable, insertIndex: Int = -1) =
        add(srcPrefab, srcPath, dst.root.prefab!!, dst.prefabPath!!, dst, insertIndex)

    fun add(
        srcPrefab: Prefab,
        srcPath: Path,
        dstPrefab: Prefab,
        dstParentPath: Path,
        insertIndex: Int = -1
    ): Path? {
        val dstParentInstance = getInstanceAt(dstPrefab.getSampleInstance(), dstParentPath)!!
        return add(srcPrefab, srcPath, dstPrefab, dstParentPath, dstParentInstance, insertIndex)
    }

    fun add(
        srcPrefab: Prefab,
        srcPath: Path,
        dstPrefab: Prefab,
        dstParentPath: Path,
        dstParentInstance: PrefabSaveable,
        insertIndex: Int = -1
    ): Path? {
        if (!dstPrefab.isWritable) throw ImmutablePrefabException(dstPrefab.source)
        LOGGER.debug("Trying to add ${srcPrefab.source}/$srcPath to ${dstPrefab.source}/$dstParentPath")
        if (srcPrefab == dstPrefab || (srcPrefab.source == dstPrefab.source && srcPrefab.source != InvalidRef)) {
            LOGGER.debug("src == dst, so trying extraction")
            return add(
                extractPrefab(srcPrefab, srcPath, null),
                Path.ROOT_PATH,
                dstPrefab,
                dstParentPath,
                dstParentInstance,
                insertIndex
            )
        } else {
            // find all necessary changes
            if (srcPath.isEmpty()) {
                LOGGER.debug("Path is empty")
                // find correct type and insert index
                val srcSample = getInstanceAt(srcPrefab.getSampleInstance(), srcPath)!!
                val type = dstParentInstance.getTypeOf(srcSample)
                val nameId = Path.generateRandomId()
                val clazz = srcPrefab.clazzName
                val allowLink = srcPrefab.source != InvalidRef
                if (allowLink) {
                    val srcPrefabSource = srcPrefab.source
                    if (type == ' ') LOGGER.warn("Adding type '$type' (${dstParentInstance.className} += $clazz), might not be supported")
                    val dstPath = dstPrefab.add(dstParentPath, type, clazz, nameId, srcPrefabSource, insertIndex)
                    LOGGER.debug("Adding element '$nameId' of class $clazz, type '$type' to path '$dstPath'")
                    ECSSceneTabs.updatePrefab(dstPrefab)
                    return dstPath
                } else {
                    val srcPrefabSource = srcPrefab.prefab
                    if (type == ' ') LOGGER.warn("Adding type '$type' (${dstParentInstance.className} += $clazz), might not be supported")
                    val dstPath = dstPrefab.add(dstParentPath, type, clazz, nameId, srcPrefabSource, insertIndex)
                    LOGGER.debug("Adding element '$nameId' of class $clazz, type '$type' to path '$dstPath'")
                    val adds = srcPrefab.adds
                    assert(adds !== dstPrefab.adds)
                    for (index1 in adds.indices) {
                        val change = adds[index1]
                        dstPrefab.add(change.withPath(Path(dstPath, change.path), true), -1)
                    }
                    val sets = srcPrefab.sets
                    sets.forEach { k1, k2, v ->
                        dstPrefab[Path(dstPath, k1), k2] = v
                    }
                    ECSSceneTabs.updatePrefab(dstPrefab)
                    return dstPath
                }
            } else {
                LOGGER.debug("Extraction")
                return add(
                    extractPrefab(srcPrefab, srcPath, null),
                    Path.ROOT_PATH,
                    dstPrefab,
                    dstParentPath,
                    dstParentInstance,
                    insertIndex
                )
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

        LOGGER.info("Removing ${sets.count { k1, _, _ -> k1.startsWith0(path) }}, " +
                "sets: ${sets.filterMajor { k1 -> k1.startsWith0(path) }.entries.joinToString { it.key.toString() }}, " +
                "all start with $path"
        )
        sets.removeMajorIf { it.startsWith0(path) }
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
                adds.removeIf { it.path.startsWith0(path) }
                // todo renumber stuff
                // val t = HashSet<IntArray>()
                // renumber(path.lastIndex(), -1, path, adds, t)
                // renumber(path.lastIndex(), -1, path, sets, t)
                prefab.invalidateInstance()
            }
        }

        ECSSceneTabs.updatePrefab(prefab)

    }


    fun assert(boolean: Boolean) {
        if (!boolean) throw RuntimeException()
    }

    fun assert(a: Any?, b: Any?) {
        if (a != b) throw RuntimeException("$a != $b")
    }


}