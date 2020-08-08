package me.anno.video

import java.io.File
import java.lang.RuntimeException

class MissingFrameException(src: File?): RuntimeException(src.toString()){
}