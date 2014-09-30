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

package fi.harism.app.opengl3x;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.util.AttributeSet;
import android.widget.TextView;

import fi.harism.app.opengl3x.ScriptC_BlurEffect;
import fi.harism.app.opengl3x.ScriptField_FadeStruct;
import fi.harism.app.opengl3x.ScriptField_SizeStruct;

public class FadeTextView extends TextView {

    private final RenderScript renderScript;
    private final ScriptC_BlurEffect scriptBlurEffect;
    private final ScriptField_SizeStruct.Item sizeStruct;
    private final ScriptField_FadeStruct.Item fadeStruct;

    private Canvas offscreenCanvas;
    private Bitmap offscreenBitmap;
    private Allocation allocationBitmap;
    private Allocation allocationInOut;
    private Allocation allocationTemp;

    public FadeTextView(Context context) {
        this(context, null, 0);
    }

    public FadeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FadeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        MainApplication app = (MainApplication) context.getApplicationContext();
        renderScript = app.getRenderScript();
        scriptBlurEffect = app.getBlurEffect();
        sizeStruct = new ScriptField_SizeStruct.Item();
        fadeStruct = new ScriptField_FadeStruct.Item();
        fadeStruct.alphaLeft = 1.0f;
        fadeStruct.alphaRight = 1.0f;
        fadeStruct.blurLeft = 0.0f;
        fadeStruct.blurRight = 0.0f;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        w = (w + 3) & ~3;

        sizeStruct.width = w;
        sizeStruct.height = h;
        sizeStruct.widthInv = 1f / w;
        sizeStruct.heightInv = 1f / h;

        offscreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        offscreenCanvas = new Canvas(offscreenBitmap);

        allocationBitmap = Allocation.createFromBitmap(renderScript, offscreenBitmap);
        allocationInOut = Allocation.createSized(renderScript, Element.RGBA_8888(renderScript), w * h, Allocation.USAGE_SCRIPT);
        allocationTemp = Allocation.createSized(renderScript, Element.RGBA_8888(renderScript), w * h, Allocation.USAGE_SCRIPT);
    }

    @Override
    public void onDraw(Canvas canvas) {
        offscreenBitmap.eraseColor(Color.TRANSPARENT);
        super.onDraw(offscreenCanvas);

        // do filter..
        scriptBlurEffect.set_sizeStruct(sizeStruct);
        scriptBlurEffect.set_fadeStruct(fadeStruct);
        scriptBlurEffect.bind_allocationInOut(allocationInOut);
        scriptBlurEffect.bind_allocationTemp(allocationTemp);

        allocationBitmap.copyFrom(offscreenBitmap);
        scriptBlurEffect.forEach_copyFromBitmap(allocationBitmap);
        scriptBlurEffect.forEach_blurBoxH(allocationBitmap);
        scriptBlurEffect.forEach_blurBoxV(allocationBitmap);
        scriptBlurEffect.forEach_copyToBitmap(allocationBitmap);
        allocationBitmap.copyTo(offscreenBitmap);

        canvas.drawBitmap(offscreenBitmap, 0, 0, null);
    }

    public void setAlphaLeft(float alphaLeft) {
        fadeStruct.alphaLeft = alphaLeft;
        postInvalidate();
    }

    public void setAlphaRight(float alphaRight) {
        fadeStruct.alphaRight = alphaRight;
        postInvalidate();
    }

    public void setBlurLeft(float blurLeft) {
        fadeStruct.blurLeft = blurLeft;
        postInvalidate();
    }

    public void setBlurRight(float blurRight) {
        fadeStruct.blurRight = blurRight;
        postInvalidate();
    }

}
