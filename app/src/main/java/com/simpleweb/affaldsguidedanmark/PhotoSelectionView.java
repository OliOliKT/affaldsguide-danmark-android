package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Draws a selectable object frame in the displayed image's own coordinates. */
public class PhotoSelectionView extends View {
    private Bitmap bitmap;
    private final RectF imageBounds = new RectF();
    private final RectF selection = new RectF();
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float downX, downY;

    public PhotoSelectionView(Context context, AttributeSet attrs) { super(context, attrs); }

    void setBitmap(Bitmap value) {
        bitmap = value;
        selection.setEmpty();
        invalidate();
    }

    void clearSelection() { selection.setEmpty(); invalidate(); }

    Bitmap selectedBitmap() {
        if (bitmap == null || selection.width() < 24 || selection.height() < 24) return null;
        float scaleX = bitmap.getWidth() / imageBounds.width();
        float scaleY = bitmap.getHeight() / imageBounds.height();
        int left = Math.max(0, Math.round((selection.left - imageBounds.left) * scaleX));
        int top = Math.max(0, Math.round((selection.top - imageBounds.top) * scaleY));
        int width = Math.min(bitmap.getWidth() - left, Math.round(selection.width() * scaleX));
        int height = Math.min(bitmap.getHeight() - top, Math.round(selection.height() * scaleY));
        if (width < 1 || height < 1) return null;
        return Bitmap.createBitmap(bitmap, left, top, width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;
        float scale = Math.min(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
        float width = bitmap.getWidth() * scale, height = bitmap.getHeight() * scale;
        imageBounds.set((getWidth() - width) / 2, (getHeight() - height) / 2,
                (getWidth() + width) / 2, (getHeight() + height) / 2);
        canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), imageBounds, imagePaint);
        if (!selection.isEmpty()) {
            framePaint.setColor(Color.rgb(104, 165, 91));
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setStrokeWidth(5);
            canvas.drawRect(selection, framePaint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null || imageBounds.isEmpty()) return false;
        float x = Math.max(imageBounds.left, Math.min(imageBounds.right, event.getX()));
        float y = Math.max(imageBounds.top, Math.min(imageBounds.bottom, event.getY()));
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = x; downY = y;
            selection.set(x, y, x, y);
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
            selection.set(Math.min(downX, x), Math.min(downY, y), Math.max(downX, x), Math.max(downY, y));
            invalidate();
            return true;
        }
        return true;
    }
}
