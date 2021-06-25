
About GLFW with touch:

these changes need to be added to GLFW to enable touch
the key event is used as transmitter/bridge (so I don't have to modify lwjgl, too)
the window currently is send as 0 (null), and then pressureId, x, y, (-1 for down, -2 for up, other = 255 for move; shall be pressure one day)


How to compile GLFW:

GLFW can be downloaded,
then opened with Visual Studio as CMake project
then the CMake config must be opened as json
enable BUILD_SHARED_LIBRARIES
build for x64 (64 bit windows) (or x86 for 32 bit windows)
Windows 7 is the lowest supported windows, which supports touch
the resulting dll can be found as glfw3.dll inside (~ = home = C:/Users/yourName/) ~/CMakeBuilds/large-number-in-hex/build/BuildName/src
it needs to be packed into lwjgl-glfw-natives-windows(-x86)(-touch).jar; and %appdata%/../Local/Temp/lwjglUsername needs to be deleted, so lwjgl updates the file
(alternatively for direct consumers, the file in Temp/lwjglUsername could be updated; it's not recommended though, because it's a temporary content and it could be deleted)
