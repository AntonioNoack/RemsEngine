package me.anno.ecs.prefab

import me.anno.io.files.FileReference

/**
 * you cannot modify prefabs that were marked immutable, e.g., because they are derived from a file that we cannot edit (like FBX),
 * because we have no writers for that format; or when the file is inside a .zip file;
 *
 * the solution is to inherit from that prefab, or create a deep copy
 * */
class ImmutablePrefabException(src: FileReference): Exception("Prefab from '$src' cannot be modified, inherit from it!")