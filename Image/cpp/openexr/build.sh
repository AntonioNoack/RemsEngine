g++ -shared -fPIC \
  -I"$JAVA_HOME/include" \
  -I"$JAVA_HOME/include/linux" \
  exr_reader.cpp exr_jni.cpp \
  -o ../../assets/libopenexr_java.so \
  $(pkg-config --cflags --libs OpenEXR)