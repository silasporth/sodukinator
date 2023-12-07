package de.dhbw.sudokinator

import android.content.Intent
import android.os.Bundle
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import de.dhbw.sudokinator.components.EditableSudokuCell
import de.dhbw.sudokinator.databinding.EditActivityBinding

class EditActivity : AppCompatActivity() {

    private lateinit var binding: EditActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gridLayout = binding.gridLayout
        val doneButton = binding.doneButton

        val sudokuBoard = getSudokuFromIntentOrNull(intent)
        if (sudokuBoard == null) {
            setResult(ACTIVITY_RESULT_ERROR)
            finish()
            return
        }

        buildEditableSudokuGrid(gridLayout, sudokuBoard)

        doneButton.setOnClickListener {
            // Update the Sudoku board based on the values entered in EditText views
            // Safer update than before
            gridLayout.children.filterIsInstance<EditableSudokuCell>().forEach {
                val tag = it.getTag(R.id.cell_position)
                if (tag !is Pair<*, *>) return@forEach

                val (x, y) = tag
                if (x !is Int || y !is Int) return@forEach

                val possibleNumber = it.text.toString()
                sudokuBoard[y][x] = if (possibleNumber.isBlank()) 0 else possibleNumber.toInt()
            }

            // Create an intent to pass the modified Sudoku board back to the main activity
            val resultIntent = Intent().putExtra(INTENT_EXTRA_SUDOKU_BOARD, sudokuBoard)
            setResult(RESULT_OK, resultIntent)

            // Finish the EditActivity to return to the main activity
            finish()
        }
    }

    private fun buildEditableSudokuGrid(gridLayout: GridLayout, sudokuBoard: Array<IntArray>) {
        // Add EditText views to each cell in the grid
        for (y in 0 until SUDOKU_ROWS) {
            for (x in 0 until SUDOKU_COLUMNS) {
                gridLayout.addView(createEditableCell(sudokuBoard[y][x].toString(), x to y))
            }
        }
    }

    /**
     * Creates an EditText that represents a cell in the sudoku grid
     *
     * @param originalCellText the text that was already saved in the grid
     * @param position the designated x y position for the cell
     */
    private fun createEditableCell(originalCellText: String, position: Pair<Int, Int>) =
        EditableSudokuCell(this).apply {
            setText(originalCellText)
            setTag(R.id.cell_position, position)

            // Set width and height to 0dp to allow the EditText to stretch
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                // Use weight to fill the available vertical space
                rowSpec = GridLayout.spec(position.second, 1f)
                // Use weight to fill the available horizontal space
                columnSpec = GridLayout.spec(position.first, 1f)
            }
            layoutParams = params
            setBackgroundResource(R.drawable.edit_text_border)
        }
}
