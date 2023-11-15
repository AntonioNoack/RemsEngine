package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.studio.StudioBase
import org.apache.logging.log4j.LogManager
import kotlin.test.assertTrue

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

    private fun extractPrefab(srcPrefab: Prefab, srcPath: Path): Prefab {

        val className = when (srcPath) {
            Path.ROOT_PATH -> srcPrefab.clazzName
            else -> findAdd(srcPrefab, srcPath)?.clazzName ?: throw IllegalStateException("Instance was not found")
        }

        val dstPrefab = Prefab(className)
        dstPrefab.ensureMutableLists()
        dstPrefab.isValid = false

        val isRoot = srcPath == Path.ROOT_PATH
        if (isRoot) {

            // simple copy-paste
            dstPrefab.prefab = srcPrefab.prefab
            for (it in srcPrefab.adds) {
                dstPrefab.add(it.clone(), -1)
            }
            srcPrefab.sets.forEach { k1, k2, v ->
                dstPrefab[k1, k2] = v
            }
            return dstPrefab
        } else {

            LOGGER.info("For copy path: $srcPath")

            fun processPrefab(prefab: Prefab, prefabRootPath: Path) {
                for (add in prefab.adds) {
                    val herePath = prefabRootPath + add.getSetterPath(0)
                    val startsWithPath = herePath.startsWith1(srcPath)
                    if (startsWithPath != null) {
                        if (startsWithPath != Path.ROOT_PATH) {
                            // can simply reference it, and we're done
                            dstPrefab.add(startsWithPath.parent!!, add.type, add.clazzName, add.nameId, add.prefab)
                        }// else done: this was already added via Prefab(className)
                    } else if (add.prefab != InvalidRef) {
                        val prefab2 = PrefabCache[add.prefab]
                        if (prefab2 != null) {
                            val startsWithPath2 = srcPath.startsWith1(herePath)
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
                    val startsWithPath = herePath.startsWith1(srcPath)
                    if (startsWithPath != null) {
                        dstPrefab[startsWithPath, name] = value
                    }
                }
            }

            processPrefab(srcPrefab, Path.ROOT_PATH)

            LOGGER.info("Found: ${dstPrefab.prefab}, prefab: ${srcPrefab.prefab}, own file: ${srcPrefab.source}")
            LOGGER.info("check start")
            dstPrefab.getSampleInstance()
            LOGGER.info("check end")
            return dstPrefab
        }
    }

    fun stringify(element: PrefabSaveable): String {
        return JsonStringWriter.toText(
            extractPrefab(element.prefab!!, element.prefabPath),
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
                if (components.getOrNull(childIndex)?.prefabPath == pathI) {
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
                                LOGGER.warn("Child $pathI had incorrect type '$childType', actual type was '$type' in ${instance0.prefab?.source}")
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

    fun add(srcPrefab: Prefab, srcPath: Path, dst: PrefabSaveable, type: Char, insertIndex: Int = -1) =
        add(srcPrefab, srcPath, dst.root.prefab!!, dst.prefabPath, dst, type, insertIndex)

    fun add(
        srcPrefab: Prefab,
        srcPath: Path,
        dstPrefab: Prefab,
        dstParentPath: Path,
        type: Char,
        insertIndex: Int = -1
    ): Path? {
        val dstParentInstance = getInstanceAt(dstPrefab.getSampleInstance(), dstParentPath)!!
        return add(srcPrefab, srcPath, dstPrefab, dstParentPath, dstParentInstance, type, insertIndex)
    }

    fun add(
        srcPrefab: Prefab,
        srcPath: Path,
        dstPrefab: Prefab,
        dstParentPath: Path,
        dstParentInstance: PrefabSaveable,
        type: Char,
        insertIndex: Int = -1
    ): Path? {
        if (!dstPrefab.isWritable) throw ImmutablePrefabException(dstPrefab.source)
        LOGGER.debug(
            "Trying to add " +
                    "'${srcPrefab.source}'/'$srcPath'@${System.identityHashCode(srcPrefab)},${srcPrefab.adds.size}+${srcPrefab.sets.size} to " +
                    "'${dstPrefab.source}'/'$dstParentPath'@${System.identityHashCode(dstPrefab)},${dstPrefab.adds.size}+${dstPrefab.sets.size}"
        )
        if (srcPrefab == dstPrefab || (srcPrefab.source == dstPrefab.source && srcPrefab.source != InvalidRef)) {
            LOGGER.debug("src == dst, so trying extraction")
            return add(
                extractPrefab(srcPrefab, srcPath), Path.ROOT_PATH,
                dstPrefab, dstParentPath, dstParentInstance, type, insertIndex
            )
        } else {
            // find all necessary changes
            if (srcPath == Path.ROOT_PATH) {
                LOGGER.debug("Path is empty")
                // find correct type and insert index
                val nameId = Path.generateRandomId()
                val clazz = srcPrefab.clazzName
                val allowLink = srcPrefab.source != InvalidRef
                LOGGER.debug("Allow link? $allowLink")
                if (allowLink) {
                    val srcPrefabSource = srcPrefab.source
                    if (type == ' ') LOGGER.warn("Adding type '$type' (${dstParentInstance.className} += $clazz), might not be supported")
                    val dstPath = dstPrefab.add(dstParentPath, type, clazz, nameId, srcPrefabSource, insertIndex)
                    LOGGER.debug(
                        "Adding element '{}' of class {}, type '{}' to path '{}' [1]",
                        nameId, clazz, type, dstPath
                    )
                    ECSSceneTabs.updatePrefab(dstPrefab)
                    return dstPath
                } else {
                    val srcPrefabSource = srcPrefab.prefab
                    if (type == ' ') LOGGER.warn("Adding type '$type' (${dstParentInstance.className} += $clazz), might not be supported")
                    val dstPath = dstPrefab.add(dstParentPath, type, clazz, nameId, srcPrefabSource, insertIndex)
                    LOGGER.debug(
                        "Adding element '{}' of class {}, type '{}' to path '{}' [2], {}",
                        nameId, clazz, type, dstPath, srcPrefabSource
                    )
                    val adds = srcPrefab.adds
                    assertTrue(adds !== dstPrefab.adds)
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
                    extractPrefab(srcPrefab, srcPath), Path.ROOT_PATH,
                    dstPrefab, dstParentPath, dstParentInstance, type, insertIndex
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
        for (pName in child.getReflections().serializedProperties.keys) {
            val value = child[pName]
            if (value != sample[pName]) {
                // we can do it unsafe, because we just added the path,
                // and know that there is nothing inside it
                dstPrefab.setUnsafe(dstPath, pName, value)
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
    ) {
        saveable.ensurePrefab()
        removePathFromPrefab(prefab, saveable.prefabPath, saveable.className)
    }

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
                LOGGER.info("did not find add @$parentPath[$clazzName], prefab: ${prefab.source}:${prefab.prefab}, ${prefab.adds}, ${prefab.sets}")
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

    fun resetPrefab(prefab: Prefab, path: Path, removeChildren: Boolean) {
        val changes0 = if (removeChildren) {
            val dirtyAdds = prefab.adds
            val cleanedAdds = dirtyAdds.filter {
                !it.path.startsWith0(path)
            }
            prefab.adds = cleanedAdds
            dirtyAdds.size - cleanedAdds.size
        } else 0
        val changes1 = prefab.sets.removeIf { path1, _, _ ->
            path1.startsWith0(path)
        }
        val changes = changes0 + changes1
        if (changes > 0) {
            LOGGER.info("Removed $changes0 instances + $changes1 properties")
            prefab.invalidateInstance()
        } else LOGGER.info("Instance was already reset")
    }

    fun resetPrefabExceptTransform(prefab: Prefab, path: Path, removeChildren: Boolean) {
        val removedPaths = if (removeChildren) {
            val dirtyAdds = prefab.adds
            val cleanedAdds = dirtyAdds.filter {
                !it.path.startsWith0(path)
            }
            prefab.adds = cleanedAdds
            (dirtyAdds - cleanedAdds.toHashSet()).map { it.path }
        } else emptyList()
        val changes0 = removedPaths.size
        val changes1 = prefab.sets.removeIf { path1, key, _ ->
            path1.startsWith0(path) && when (key) {
                "position", "rotation", "scale" -> {
                    // change needs to be removed, if its instance was removed
                    removedPaths.any {
                        path1.startsWith0(it)
                    }
                }
                else -> true
            }
        }
        val changes = changes0 + changes1
        if (changes > 0) {
            LOGGER.info("Removed $changes0 instances + $changes1 properties")
            prefab.invalidateInstance()
        } else LOGGER.info("Instance was already reset")
    }
}