package me.anno.io.base

class UnknownClassException(className: String) : RuntimeException("Unknown class \"$className\"")