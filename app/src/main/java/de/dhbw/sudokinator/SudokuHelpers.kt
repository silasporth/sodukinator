package de.dhbw.sudokinator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import kotlin.math.roundToInt


fun Array<IntArray>.flatten(): IntArray = flatMap { it.asIterable() }.toIntArray()

fun IntArray.toSudokuBoard(): Array<IntArray> =
    (this + IntArray(SUDOKU_ROWS * SUDOKU_COLUMNS - size)).asIterable().chunked(9)
        .map { it.toIntArray() }.toTypedArray()

fun createCleanSudokuImage(): Bitmap {
    val standardLineWidth = 2f
    val blockLineWidth = 4f
    val boardLength = SUDOKU_COLUMNS * SUDOKU_CELL_PIXEL_SIZE

    val bitmap = Bitmap.createBitmap(
        boardLength.roundToInt(), boardLength.roundToInt(), Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Draw background
    paint.color = Color.WHITE
    canvas.drawRect(0f, 0f, boardLength, boardLength, paint)

    paint.color = Color.BLACK
    for (i in 0..SUDOKU_COLUMNS) {
        // Draw every third line bigger for visible 3x3 boxes
        paint.strokeWidth =
            if (i % 3 == 0 && i > 0 && i < SUDOKU_COLUMNS) blockLineWidth else standardLineWidth

        val width = i * SUDOKU_CELL_PIXEL_SIZE
        // Vertical line
        canvas.drawLine(width, 0f, width, boardLength, paint)

        // Horizontal line
        canvas.drawLine(0f, width, boardLength, width, paint)
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

// Check if the sudoku board state is solvable (faster than before)
fun isSolvable(sudokuBoard: Array<IntArray>): Boolean =
    checkBlocksSolvable(sudokuBoard) && checkRowsSolvable(sudokuBoard) && checkColumnsSolvable(
        sudokuBoard
    )

fun checkBlocksSolvable(sudokuBoard: Array<IntArray>): Boolean {
    for (i in 0..2) for (j in 0..2) {
        val numberUnique = BooleanArray(9)
        for (y in 0..2) for (x in 0..2) {
            val number = sudokuBoard[i * 3 + y][j * 3 + x]
            if (number == 0) continue
            if (numberUnique[number - 1]) return false
            numberUnique[number - 1] = true
        }
    }
    return true
}

fun checkRowsSolvable(sudokuBoard: Array<IntArray>): Boolean {
    for (y in 0 until SUDOKU_ROWS) {
        val numberUnique = BooleanArray(9)
        for (x in 0 until SUDOKU_COLUMNS) {
            val number = sudokuBoard[y][x]
            if (number == 0) continue
            if (numberUnique[number - 1]) return false
            numberUnique[number - 1] = true
        }
    }
    return true
}

fun checkColumnsSolvable(sudokuBoard: Array<IntArray>): Boolean {
    for (x in 0 until SUDOKU_COLUMNS) {
        val numberUnique = BooleanArray(9)
        for (y in 0 until SUDOKU_ROWS) {
            val number = sudokuBoard[y][x]
            if (number == 0) continue
            if (numberUnique[number - 1]) return false
            numberUnique[number - 1] = true
        }
    }
    return true
}

class SolvableSudoku(val sudokuBoard: Array<IntArray>, var solvable: Boolean)

fun logBoard(board: Array<IntArray>) {
    for (row in board) {
        Log.d("solveSudoku", row.joinToString(" "))
    }
    Log.d("solveSudoku", "----")
}