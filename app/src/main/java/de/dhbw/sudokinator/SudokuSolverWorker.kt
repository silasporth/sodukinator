package de.dhbw.sudokinator

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class SudokuSolverWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val sudokuBoard = inputData.getIntArray(WORKER_DATA_SUDOKU_BOARD)?.toSudokuBoard()
            ?: return Result.failure()

        val possibleSolution = solveSudoku(sudokuBoard)

        val outputDataBuilder = Data.Builder()
        outputDataBuilder.apply {
            putBoolean(WORKER_DATA_SUDOKU_SOLVABLE, possibleSolution.solvable)
            if (possibleSolution.solvable) {
                putIntArray(WORKER_DATA_SUDOKU_BOARD, possibleSolution.sudokuBoard.flatten())
            }
        }

        return Result.success(outputDataBuilder.build())
    }

    private fun solveSudoku(board: Array<IntArray>): SolvableSudoku {
        val freeCells = calculateCandidates(board)
        return solveSudokuWithCandidates(SolvableSudoku(board, true), freeCells)
    }

    private fun calculateCandidates(board: Array<IntArray>): MutableList<Cell> {
        val freeCells = mutableListOf<Cell>()

        for (row in 0 until SUDOKU_ROWS) {
            for (col in 0 until SUDOKU_COLUMNS) {
                if (board[row][col] != 0) continue

                val cell = Cell(row, col)
                for (num in 1..9) {
                    if (isValid(board, row, col, num)) {
                        cell.candidates.add(num)
                    }
                }
                freeCells.add(cell)
            }
        }
        return freeCells
    }

    private fun solveSudokuWithCandidates(
        solvableSudoku: SolvableSudoku, freeCells: MutableList<Cell>
    ): SolvableSudoku {

        val board = solvableSudoku.sudokuBoard
        if (freeCells.isEmpty()) {
            solvableSudoku.solvable = true
            return solvableSudoku
        }

        // sort candidates by size and pop cell with least candidates
        freeCells.sortByDescending { it.candidates.size }
        val cell = freeCells.removeLast()

        // test all possibilities for the cell
        for (num in cell.candidates) {
            if (!isValid(board, cell.row, cell.col, num)) {
                continue
            }

            Log.d("SudokuHelpers#solveSudoku", "Placed $num at (${cell.row}, ${cell.col})")
            logBoard(board)
            board[cell.row][cell.col] = num

            // remove cell to avoid conflicts
            val solution = solveSudokuWithCandidates(solvableSudoku, freeCells)
            if (solution.solvable) {
                return solution
            }

            board[cell.row][cell.col] = 0
        }

        freeCells.add(cell)
        Log.d("SudokuHelpers#solveSudoku", "Backtracked at (${cell.row}, ${cell.col})")
        logBoard(board)
        solvableSudoku.solvable = false
        return solvableSudoku
    }

    // Checks if the number can be placed in the specified cell
    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (board[row][i] == num || board[i][col] == num || board[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3] == num) {
                return false
            }
        }
        return true
    }

    private class Cell(
        val row: Int, val col: Int, val candidates: MutableList<Int> = mutableListOf()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Cell

            if (row != other.row) return false
            return col == other.col
        }

        override fun hashCode(): Int {
            var result = row
            result = 31 * result + col
            return result
        }
    }

}