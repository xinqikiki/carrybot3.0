package net.chezxinqi.carrybot3;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

public final class ContrastHelper {

    private ContrastHelper() {
    }

    public static void apply(View root, boolean enabled) {
        if (root == null) {
            return;
        }
        if (enabled) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0f);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));
            root.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } else {
            root.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }
}
