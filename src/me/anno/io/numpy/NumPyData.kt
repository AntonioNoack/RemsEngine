package me.anno.io.numpy

import java.io.Serializable

class NumPyData(val descriptor: String, val shape: IntArray, val columnMajor: Boolean, val data: Any)