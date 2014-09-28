/*
   Copyright 2014 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

#pragma version(1)
#pragma rs java_package_name(fi.harism.app.opengl3x)
#pragma rs_fp_imprecise

#include "rs_types.rsh"

//
// For holding current bitmap size
//
typedef struct SizeStruct {
    int width;
    int height;
    float widthInv;
    float heightInv;
} SizeStruct_t;
SizeStruct_t sizeStruct;

//
// For holding current alpha and blur values
//
typedef struct FadeStruct {
    float alphaLeft;
    float alphaRight;
    float blurLeft;
    float blurRight;
} FadeStruct_t;
FadeStruct_t fadeStruct;

//
// Variables for dynamic allocations
//
uchar4* allocationInOut;
uchar4* allocationTemp;

static uchar4 getColor(const uchar4* buffer, float x, float y) {
    x = round(x * sizeStruct.width);
    y = round(y * sizeStruct.height);
    x = clamp(x, 0.0f, sizeStruct.width - 1.0f);
    y = clamp(y, 0.0f, sizeStruct.height - 1.0f);
    return buffer[(int)y * sizeStruct.width + (int)x];
}

void blurBoxH(const uchar4* value, const void* userData, uint32_t x, uint32_t y) {
    float posx = x * sizeStruct.widthInv;
    float posy = y * sizeStruct.heightInv;
    float sx = max(0.005f, sizeStruct.widthInv);
    sx *= mix(fadeStruct.blurLeft, fadeStruct.blurRight, posx);
    float sy = (sx * sizeStruct.width) * sizeStruct.heightInv;

    float4 colorSum = 0.0f;
    for (int xx = -2; xx <= 2; ++xx) {
        uchar4 color = getColor(allocationInOut, posx + xx * sx, posy);
        colorSum += rsUnpackColor8888(color);
    }
    colorSum /= 5.0f;

    allocationTemp[y * sizeStruct.width + x] = rsPackColorTo8888(colorSum);
}

void blurBoxV(const uchar4* value, const void* userData, uint32_t x, uint32_t y) {
    float posx = x * sizeStruct.widthInv;
    float posy = y * sizeStruct.heightInv;
    float sx = max(0.005f, sizeStruct.widthInv);
    sx *= mix(fadeStruct.blurLeft, fadeStruct.blurRight, posx);
    float sy = (sx * sizeStruct.width) * sizeStruct.heightInv;

    float4 colorSum = 0.0f;
    for (int yy = -2; yy <= 2; ++yy) {
        uchar4 color = getColor(allocationTemp, posx, posy + yy * sy);
        colorSum += rsUnpackColor8888(color);
    }
    colorSum /= 5.0f;

    colorSum.rgba *= mix(fadeStruct.alphaLeft, fadeStruct.alphaRight, posx);
    allocationInOut[y * sizeStruct.width + x] = rsPackColorTo8888(colorSum);
}

void copyFromBitmap(const uchar4* value, const void* userData, uint32_t x, uint32_t y) {
    allocationInOut[y * sizeStruct.width + x] = *value;
}

void copyToBitmap(uchar4* value, const void* userData, uint32_t x, uint32_t y) {
    *value = allocationInOut[y * sizeStruct.width + x];
}
