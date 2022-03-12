package me.anno.io.base

class UnknownClassException(className: String) : InvalidFormatException("Class \"$className\" is unknown / hasn't been registered")