package com.appsbyayush.noteit.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import com.appsbyayush.noteit.R
import timber.log.Timber

class LinedTextView(
    context: Context,
    attrs: AttributeSet
): AppCompatTextView(context, attrs) {

    companion object {
        private const val TAG = "LinedTextView"
        private const val DEFAULT_LINES_TO_DRAW = 10
        private const val LINE_EXTRA_SPACE = 4
        private const val DEFAULT_LINE_WIDTH = 3.0f
        private const val DEFAULT_LINE_HEIGHT = 108
    }

    private var rect: Rect = Rect()
    private var paint: Paint = Paint()

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = DEFAULT_LINE_WIDTH

        context.theme.obtainStyledAttributes(attrs, R.styleable.LinedEditText,
            0, 0).apply {

            try {
                Timber.tag(TAG)
                    .d("Style Line Color: ${getString(R.styleable.LinedEditText_lineColor)}")
                Timber.tag(TAG).d("onDraw: rect on init: $rect")
                paint.color = Color.parseColor(getString(R.styleable.LinedEditText_lineColor))

            } catch(e: Exception) {
                Timber.tag(TAG).d("Error of styles: ")
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val baseline = getLineBounds(0, rect)
//        val calculatedLinesFromHeight = height / (lineHeight - 5)
        val calculatedLinesFromHeight = height / lineHeight
        val linesToDraw = if(lineCount > calculatedLinesFromHeight) lineCount
        else if(calculatedLinesFromHeight > DEFAULT_LINES_TO_DRAW) calculatedLinesFromHeight
        else DEFAULT_LINES_TO_DRAW

//        val linesToDraw = if(lineCount > DEFAULT_LINES_TO_DRAW) lineCount else DEFAULT_LINES_TO_DRAW

        var lastBaseline: Float = 0f

        for(i in 0 until linesToDraw) {
//            val calculatedBaseline = (baseline + i * (lineHeight - 5)).toFloat()
            val calculatedBaseline = (baseline + i * lineHeight).toFloat()

            Timber.tag(TAG).d("onDraw: last baseline: $lastBaseline")
            Timber.tag(TAG).d("onDraw: calculated baseline: ${(baseline + i * lineHeight)}")
//            Log.d(TAG, "onDraw: function defined baseline: ${getLineBounds(i, rect)}")

            Timber.tag(TAG).d("onDraw: totalheight: $height")
            Timber.tag(TAG).d("onDraw: lineheight: $lineHeight")
            Timber.tag(TAG).d("onDraw: lineSpacingMultiplier: $lineSpacingMultiplier")
            Timber.tag(TAG).d("onDraw: lineCount(returned): $lineCount")
            Timber.tag(TAG).d("onDraw: lineCount(calculated): $lineCount")
            Timber.tag(TAG).d("onDraw: rect: $rect")
            Timber.tag(TAG).d("onDraw: totalLines: ${height / lineHeight} -- $lineHeight")

            canvas.drawLine(rect.left.toFloat(), calculatedBaseline,
                rect.right.toFloat(), calculatedBaseline, paint)

            lastBaseline = calculatedBaseline
        }

        super.onDraw(canvas)
    }

    fun setLineColor(color: Int) {
        paint.color = color
        invalidate()
    }
}