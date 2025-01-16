package com.example.tasknumbertree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        color = android.graphics.Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val faceRectangles = mutableListOf<RectF>()

    fun drawFaceRectangles(rectangles: List<RectF>) {
        faceRectangles.clear()
        faceRectangles.addAll(rectangles)
        invalidate() // Перерисовка View
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in faceRectangles) {
            canvas.drawRect(rect, paint)
        }
    }
}