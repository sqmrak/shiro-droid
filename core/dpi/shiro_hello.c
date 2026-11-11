#include "shiro_hello.h"
#include <string.h>

static size_t be16(const uint8_t *p) { return ((size_t)p[0] << 8) | p[1]; }

shiro_status shiro_hello_split(const uint8_t *input, size_t length, uint8_t *output, size_t capacity,
                               size_t *output_length, size_t *record_cut, int midpoint) {
    size_t record, pos, extensions_end, cut = 0;
    if (output_length) *output_length = 0;
    if (record_cut) *record_cut = 0;
    if (!input || !output || !output_length || !record_cut) return SHIRO_E_ARG;
    if (length < 44 || input[0] != 22 || input[5] != 1) return SHIRO_E_MALFORMED;
    record = be16(input + 3);
    if (record + 5 != length || record > 16384 ||
        (((size_t)input[6] << 16) | ((size_t)input[7] << 8) | input[8]) != record - 4) return SHIRO_E_MALFORMED;
    pos = 43;
    if (pos >= length || (size_t)input[pos] > length - pos - 1) return SHIRO_E_MALFORMED;
    pos += 1 + input[pos];
    if (length - pos < 2) return SHIRO_E_MALFORMED;
    { size_t n = be16(input + pos); pos += 2; if (n > length - pos || (n & 1)) return SHIRO_E_MALFORMED; pos += n; }
    if (pos >= length || (size_t)input[pos] > length - pos - 1) return SHIRO_E_MALFORMED;
    pos += 1 + input[pos];
    if (length - pos < 2) return SHIRO_E_MALFORMED;
    { size_t n = be16(input + pos); pos += 2; if (n != length - pos) return SHIRO_E_MALFORMED; extensions_end = pos + n; }
    while (pos < extensions_end) {
        size_t n, end;
        unsigned type;
        if (extensions_end - pos < 4) return SHIRO_E_MALFORMED;
        type = (unsigned)be16(input + pos); n = be16(input + pos + 2); pos += 4;
        if (n > extensions_end - pos) return SHIRO_E_MALFORMED;
        end = pos + n;
        if (type == 0) {
            if (n < 5 || be16(input + pos) != n - 2 || input[pos + 2] != 0 ||
                be16(input + pos + 3) != n - 5 || n <= 6) return SHIRO_E_MALFORMED;
            cut = pos + 5 + (midpoint ? (n - 5) / 2 : 1);
            break;
        }
        pos = end;
    }
    if (!cut || cut <= 5 || cut >= length) return SHIRO_E_MALFORMED;
    if (capacity < length || capacity - length < 5) return SHIRO_E_LIMIT;
    memcpy(output, input, cut);
    output[3] = (uint8_t)((cut - 5) >> 8); output[4] = (uint8_t)(cut - 5);
    memcpy(output + cut, input, 3);
    output[cut + 3] = (uint8_t)((length - cut) >> 8); output[cut + 4] = (uint8_t)(length - cut);
    memcpy(output + cut + 5, input + cut, length - cut);
    *output_length = length + 5;
    *record_cut = cut;
    return SHIRO_OK;
}
