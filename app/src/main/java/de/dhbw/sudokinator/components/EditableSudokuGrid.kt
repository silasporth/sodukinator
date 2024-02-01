package de.dhbw.sudokinator.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.dhbw.sudokinator.R
import de.dhbw.sudokinator.SUDOKU_COLUMNS
import de.dhbw.sudokinator.SUDOKU_ROWS
import kotlin.math.ceil
import kotlin.math.min

class EditableSudokuGrid(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var sudokuBoard: Array<IntArray> = Array(SUDOKU_ROWS) { IntArray(SUDOKU_COLUMNS) }
    private val boardPaint = Paint()
    private var selectedColumn: Int? = null
    private var selectedRow: Int? = null
    private var cellSize = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val dimension = min(measuredWidth, measuredHeight)
        cellSize = dimension / 9
        setMeasuredDimension(dimension, dimension)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val standardLineWidth = width / 240f
        val blockLineWidth = standardLineWidth * 2


        // Draw background
        boardPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boardPaint)

        if (selectedColumn != null && selectedRow != null) {
            boardPaint.strokeWidth = standardLineWidth
            boardPaint.color = resources.getColor(R.color.transparent_light_blue, context.theme)

            canvas.drawRect(
                (selectedColumn!! - 1) * cellSize.toFloat(),
                0f,
                selectedColumn!! * cellSize.toFloat(),
                cellSize * 9f,
                boardPaint
            );

            canvas.drawRect(
                0f,
                (selectedRow!! - 1) * cellSize.toFloat(),
                cellSize * 9f,
                selectedRow!! * cellSize.toFloat(),
                boardPaint
            );


            boardPaint.color = resources.getColor(R.color.transparent_blue, context.theme)
            canvas.drawRect(
                (selectedColumn!! - 1) * cellSize.toFloat(),
                (selectedRow!! - 1) * cellSize.toFloat(),
                selectedColumn!! * cellSize.toFloat(),
                selectedRow!! * cellSize.toFloat(),
                boardPaint
            )
        }

        boardPaint.color = Color.BLACK
        for (i in 0..SUDOKU_COLUMNS) {
            // Draw every third line bigger for visible 3x3 boxes
            boardPaint.strokeWidth =
                if (i % 3 == 0 && i > 0 && i < SUDOKU_COLUMNS) blockLineWidth else standardLineWidth

            val lineLength = i * width / 9f
            // Vertical line
            canvas.drawLine(lineLength, 0f, lineLength, width.toFloat(), boardPaint)

            // Horizontal line
            canvas.drawLine(0f, lineLength, height.toFloat(), lineLength, boardPaint)
        }

        // Draw Numbers
        boardPaint.textSize = cellSize * 2f / 3f
        //paint.color = Color.BLACK
        boardPaint.textAlign = Paint.Align.CENTER
        val halfCellSize = cellSize / 2f

        for (row in 0 until SUDOKU_ROWS) {
            for (col in 0 until SUDOKU_COLUMNS) {
                val cellValue = sudokuBoard[row][col]
                if (cellValue == 0) continue

                boardPaint.color = Color.BLACK

                val x = col * cellSize + halfCellSize
                val y =
                    row * cellSize + halfCellSize - (boardPaint.ascent() + boardPaint.descent()) / 2
                canvas.drawText(cellValue.toString(), x, y, boardPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        val isValid = event.action == MotionEvent.ACTION_DOWN

        if (isValid) {
            selectedColumn = ceil(x / cellSize).toInt()
            selectedRow = ceil(y / cellSize).toInt()
        }

        invalidate()

        return isValid
    }

    fun setSudokuNumber(number: Int) {
        if (selectedColumn == null || selectedRow == null) return
        sudokuBoard[selectedRow!! - 1][selectedColumn!! - 1] =
            if (sudokuBoard[selectedRow!! - 1][selectedColumn!! - 1] == number) {
                0
            } else {
                number
            }

        invalidate()
    }

    fun clearSudokuBoard() {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                sudokuBoard[i][j] = 0
            }
        }
        invalidate()
    }
}