#pragma once

#include "MemoryIStream.h"

#include <OpenEXR/ImfInputFile.h>
#include <OpenEXR/ImfHeader.h>
#include <OpenEXR/ImfFrameBuffer.h>
#include <OpenEXR/ImfChannelList.h>

using namespace Imf;

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

        int i = 0;
        for (ChannelList::ConstIterator it = chList.begin();
             it != chList.end() && i < channels;
             ++it, ++i) {

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

        Imath_3_1::Box2i dw = header.dataWindow();
        file.readPixels(dw.min.y, dw.max.y);

        return true;
    } catch (...) {
        return false;
    }
}