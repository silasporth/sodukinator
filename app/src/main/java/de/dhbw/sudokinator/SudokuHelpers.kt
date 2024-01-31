package de.dhbw.sudokinator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log


fun Array<IntArray>.flatten(): IntArray = flatMap { it.asIterable() }.toIntArray()

fun IntArray.toSudokuBoard(): Array<IntArray> =
    (this + IntArray(SUDOKU_ROWS * SUDOKU_COLUMNS - size)).asIterable().chunked(9)
        .map { it.toIntArray() }.toTypedArray()

fun createCleanSudokuImage(boardLength: Int): Bitmap {
    val standardLineWidth = boardLength / 240f
    val blockLineWidth = standardLineWidth * 2

    val bitmap = Bitmap.createBitmap(
        boardLength, boardLength, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Draw background
    paint.color = Color.WHITE
    canvas.drawRect(0f, 0f, boardLength.toFloat(), boardLength.toFloat(), paint)

    paint.color = Color.BLACK
    for (i in 0..SUDOKU_COLUMNS) {
        // Draw every third line bigger for visible 3x3 boxes
        paint.strokeWidth =
            if (i % 3 == 0 && i > 0 && i < SUDOKU_COLUMNS) blockLineWidth else standardLineWidth

        val lineLength = i * boardLength / 9f
        // Vertical line
        canvas.drawLine(lineLength, 0f, lineLength, boardLength.toFloat(), paint)

        // Horizontal line
        canvas.drawLine(0f, lineLength, boardLength.toFloat(), lineLength, paint)
    }
    return bitmap
}

@Suppress("UNCHECKED_CAST")
fun getSudokuFromIntentOrNull(intent: Intent?): Array<IntArray>? =
    intent?.getSerializableExtra(INTENT_EXTRA_SUDOKU_BOARD).let { sudoku ->
        if (sudoku is Array<*> && sudoku.size == SUDOKU_ROWS && sudoku.all { arr -> arr is IntArray && arr.size == SUDOKU_COLUMNS }) {
            sudoku as Array<IntArray>
        } else null
    }

// saves the coordinates of the numbers in a sudokuboard into a list
fun saveStartNumbersCoordinates(sudokuBoard: Array<IntArray>): MutableList<Pair<Int, Int>> {
    val coordinates = mutableListOf<Pair<Int, Int>>()
    for (row in 0 until SUDOKU_ROWS) {
        for (column in 0 until SUDOKU_COLUMNS) {
            if (sudokuBoard[row][column] != 0) {
                coordinates.add(column to row)
            }
        }
    }
    return coordinates
}

fun logBoard(board: Array<IntArray>) {
    for (row in board) {
        Log.d("solveSudoku", row.joinToString(" "))
    }
    Log.d("solveSudoku", "----")
}