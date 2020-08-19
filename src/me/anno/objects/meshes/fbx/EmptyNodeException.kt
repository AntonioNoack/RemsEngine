package me.anno.objects.meshes.fbx

import java.lang.RuntimeException

class EmptyNodeException : RuntimeException("Shouldn't read further in this fbx node!"){}