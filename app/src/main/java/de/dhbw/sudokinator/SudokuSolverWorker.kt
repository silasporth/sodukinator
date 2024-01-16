package de.dhbw.sudokinator

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class SudokuSolverWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val sudokuBoard = inputData.getIntArray(WORKER_DATA_SUDOKU_BOARD) ?: return Result.failure()

        val possibleSolution = solveSudoku(sudokuBoard)

        val outputDataBuilder = Data.Builder()
        outputDataBuilder.apply {
            val isSolvable = possibleSolution.isNotEmpty()
            putBoolean(WORKER_DATA_SUDOKU_SOLVABLE, isSolvable)
            if (isSolvable) {
                putIntArray(WORKER_DATA_SUDOKU_BOARD, possibleSolution)
            }
        }

        return Result.success(outputDataBuilder.build())
    }

    // Translated version of Peter Norvig's sudoku solving algorithm
    // http://norvig.com/sudoku.html
    private fun solveSudoku(board: IntArray): IntArray = try {
        search(parseSudokuBoard(board))
    } catch (e: IllegalArgumentException) {
        IntArray(0)
    }

    private fun search(values: Map<String, String>): IntArray {
        if (values.isEmpty()) return IntArray(0)

        if (cells.all { values[it]!!.length == 1 }) {
            return values.map { it.value.toInt() }.toIntArray()
        }

        val cell = cells.filter { values[it] != null && values[it]!!.length > 1 }
            .minBy { values[it]!!.length }

        for (value in values[cell]!!) {
            val copy = values.toMutableMap()
            if (!isAssignable(copy, cell, value)) continue
            val result = search(copy)
            if (result.isNotEmpty()) return result
        }

        return IntArray(0)
    }


    private fun isAssignable(
        possibleValuesPerCell: MutableMap<String, String>, cell: String, value: Char
    ): Boolean {
        val otherDigits = possibleValuesPerCell[cell]?.replace(value.toString(), "")
            ?: digits.replace(value.toString(), "")
        return otherDigits.all { isKillable(possibleValuesPerCell, cell, it) }
    }

    private fun isKillable(
        possibleValuesPerCell: MutableMap<String, String>, cell: String, value: Char
    ): Boolean {
        if (possibleValuesPerCell[cell]?.contains(value) != true) {
            return true
        }

        val newValues = possibleValuesPerCell[cell]?.replace(value.toString(), "")
            ?: digits.replace(value.toString(), "")
        possibleValuesPerCell[cell] = newValues

        when (newValues.length) {
            // Contradiction: removed last value
            0 -> return false
            // If there is only one possible value left, then eliminate it from the peers
            1 -> {
                if (peers[cell]?.all {
                        isKillable(
                            possibleValuesPerCell, it, newValues.first()
                        )
                    } != true) {
                    return false
                }
            }
        }

        units[cell]?.forEach { unit ->
            val digitPlaces = unit.filter { possibleValuesPerCell[it]?.contains(value) == true }
            when (digitPlaces.size) {
                // Contradiction: no place for this value
                0 -> return false
                // the value can only be in one place in the unit; assign it there
                1 -> {
                    if (!isAssignable(possibleValuesPerCell, digitPlaces.first(), value)) {
                        return false
                    }
                }
            }
        } ?: return false

        return true
    }

    private fun parseSudokuBoard(board: IntArray): Map<String, String> {
        if (board.size != 81) throw IllegalArgumentException()

        val possibleValuesPerCell = cells.associateWith { digits }.toMutableMap()
        val valuePerCell = cells.zip(board.map { it.digitToChar() }).toMap()

        for ((cell, value) in valuePerCell) {
            if (value in digits && !isAssignable(
                    possibleValuesPerCell, cell, value
                )
            ) {
                return emptyMap()
            }
        }
        return possibleValuesPerCell
    }


    companion object {
        private const val digits = "123456789"
        private const val rows = "ABCDEFGHI"
        private const val cols = digits
        private val cells = crossStrings(rows, cols)
        private val unitList = cols.map { crossStrings(rows, it.toString()) } + rows.map {
            crossStrings(
                it.toString(), cols
            )
        } + listOf("ABC", "DEF", "GHI").flatMap { s1 ->
            listOf(
                "123", "456", "789"
            ).map { s2 -> crossStrings(s1, s2) }
        }
        private val units = cells.associateWith { s -> unitList.filter { u -> s in u } }
        private val peers = cells.associateWith { s ->
            units[s]?.flatten()?.toMutableSet()?.minus(s)?.toList() ?: emptyList()
        }

        private fun crossStrings(a: String, b: String): List<String> =
            a.flatMap { c1 -> b.map { c2 -> "$c1$c2" } }
    }
}