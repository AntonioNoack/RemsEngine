package me.anno.io.base

import java.lang.RuntimeException

class UnknownClassException (className: String):
        RuntimeException("Unknown class \"$className\"")