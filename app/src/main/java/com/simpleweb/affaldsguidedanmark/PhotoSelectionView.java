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

/** Keeps the object frame in bitmap coordinates as the surrounding layout changes size. */
public class PhotoSelectionView extends View {
    interface OnSelectionChangedListener {
        void onSelectionChanged(boolean hasSelection);
    }

    private Bitmap bitmap;
    private final RectF imageBounds = new RectF();
    private final RectF selection = new RectF();
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF displayedSelection = new RectF();
    private float downX, downY;
    private OnSelectionChangedListener selectionChangedListener;

    public PhotoSelectionView(Context context, AttributeSet attrs) { super(context, attrs); }

    void setBitmap(Bitmap value) {
        bitmap = value;
        selection.setEmpty();
        notifySelectionChanged();
        invalidate();
    }

    void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        selectionChangedListener = listener;
        notifySelectionChanged();
    }

    void clearSelection() { selection.setEmpty(); notifySelectionChanged(); invalidate(); }

    private boolean hasSelection() {
        return bitmap != null && !imageBounds.isEmpty()
                && selection.width() * imageBounds.width() / bitmap.getWidth() >= 24
                && selection.height() * imageBounds.height() / bitmap.getHeight() >= 24;
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged(hasSelection());
    }

    Bitmap selectedBitmap() {
        if (!hasSelection()) return null;
        int left = Math.max(0, Math.min(bitmap.getWidth() - 1, Math.round(selection.left)));
        int top = Math.max(0, Math.min(bitmap.getHeight() - 1, Math.round(selection.top)));
        int width = Math.min(bitmap.getWidth() - left, Math.round(selection.width()));
        int height = Math.min(bitmap.getHeight() - top, Math.round(selection.height()));
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
            displayedSelection.set(
                    imageBounds.left + selection.left * scale,
                    imageBounds.top + selection.top * scale,
                    imageBounds.left + selection.right * scale,
                    imageBounds.top + selection.bottom * scale);
            framePaint.setStyle(Paint.Style.FILL);
            framePaint.setColor(Color.argb(46, 104, 165, 91)); // 18%, matching iOS.
            canvas.drawRect(displayedSelection, framePaint);
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setColor(Color.rgb(104, 165, 91));
            framePaint.setStrokeWidth(5);
            canvas.drawRect(displayedSelection, framePaint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || bitmap == null || imageBounds.isEmpty()) return false;
        float x = Math.max(0, Math.min(bitmap.getWidth(),
                (event.getX() - imageBounds.left) * bitmap.getWidth() / imageBounds.width()));
        float y = Math.max(0, Math.min(bitmap.getHeight(),
                (event.getY() - imageBounds.top) * bitmap.getHeight() / imageBounds.height()));
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = x; downY = y;
            selection.set(x, y, x, y);
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
            selection.set(Math.min(downX, x), Math.min(downY, y), Math.max(downX, x), Math.max(downY, y));
            invalidate();
            if (event.getAction() == MotionEvent.ACTION_UP) notifySelectionChanged();
            return true;
        }
        return true;
    }
}
