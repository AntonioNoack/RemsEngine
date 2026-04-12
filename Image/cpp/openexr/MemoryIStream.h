#pragma once

#include <OpenEXR/ImfIO.h>
#include <cstring>

class MemoryIStream : public Imf::IStream {
public:
    MemoryIStream(const char* data, size_t size)
        : Imf::IStream("MemoryIStream"), data(data), size(size), pos(0) {}

    bool read(char c[], int n) override {
        if (pos + n > size) return false;
        memcpy(c, data + pos, n);
        pos += n;
        return true;
    }

    uint64_t tellg() override {
        return pos;
    }

    void seekg(uint64_t newPos) override {
        pos = newPos;
    }

    void clear() override {}

private:
    const char* data;
    size_t size;
    size_t pos;
};