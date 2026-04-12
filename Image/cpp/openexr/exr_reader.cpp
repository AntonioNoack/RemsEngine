#include "MemoryIStream.h"

#include <jni.h>
#include <OpenEXR/ImfInputFile.h>
#include <OpenEXR/ImfFrameBuffer.h>
#include <OpenEXR/ImfChannelList.h>
#include <OpenEXR/ImfArray.h>
#include <OpenEXR/ImfHeader.h>

using namespace Imf;
using namespace Imath;

template<typename T>
bool readImageImpl(
    int width, int height, int channels,
    T* dst,
    const char* data, int dataSize,
    PixelType targetType
) {
    try {
        MemoryIStream stream(data, dataSize);
        InputFile file(stream);

        const Header& header = file.header();
        const ChannelList& chList = header.channels();

        FrameBuffer fb;

        int pixelSize = sizeof(T);
        int strideX = channels * pixelSize;
        int strideY = width * strideX;

        // Map channels (R,G,B,A or first N channels)
        const char* names[4] = {"R", "G", "B", "A"};

        int i = 0;
        for (ChannelList::ConstIterator it = chList.begin(); it != chList.end() && i < channels; ++it, ++i) {
            fb.insert(
                it.name(),
                Slice(
                    targetType,
                    (char*)dst + i * pixelSize,
                    strideX,
                    strideY,
                    1, 1, 0.0f
                )
            );
        }

        file.setFrameBuffer(fb);
        file.readPixels(file.header().dataWindow().min.y,
                        file.header().dataWindow().max.y);

        return true;
    } catch (...) {
        return false;
    }
}