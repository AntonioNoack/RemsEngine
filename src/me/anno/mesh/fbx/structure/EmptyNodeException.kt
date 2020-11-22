package me.anno.mesh.fbx.structure

import java.lang.RuntimeException

object EmptyNodeException : RuntimeException("Shouldn't read further in this fbx node!"){}