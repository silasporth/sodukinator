package de.dhbw.sudokinator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import de.dhbw.sudokinator.databinding.ActivityEditBinding

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val editableSudokuGrid = binding.editableSudokuGrid
        val clearButton = binding.clearButton
        val doneButton = binding.doneButton

        val sudokuBoard = intent.getIntArrayExtra(INTENT_EXTRA_SUDOKU_BOARD)?.toSudokuBoard()
        if (sudokuBoard == null) {
            setResult(ACTIVITY_RESULT_ERROR)
            finish()
            return
        }

        editableSudokuGrid.sudokuBoard = sudokuBoard
        buildButtonRow()

        clearButton.setOnClickListener {
            editableSudokuGrid.clearSudokuBoard()
        }

        doneButton.setOnClickListener {
            // Create an intent to pass the modified Sudoku board back to the main activity
            val newSudokuBoard = editableSudokuGrid.sudokuBoard
            val resultIntent = Intent().putExtra(INTENT_EXTRA_SUDOKU_BOARD, newSudokuBoard.flatten())
            setResult(RESULT_OK, resultIntent)

            // Finish the EditActivity to return to the main activity
            finish()
        }
    }

    private fun buildButtonRow() {
        binding.buttonRow.apply {
            for (i in 1..9) {
                val button = TextView(this@EditActivity).apply {
                    text = "$i"
                    textSize = 32f
                    setTextColor(getColor(R.color.turquoise))
                    setBasicSettings(i)
                }
                addView(button)
            }
            addView(ImageView(this@EditActivity).apply {
                setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.clear))
                setBasicSettings(0)
            })
        }
    }

    private fun View.setBasicSettings(number: Int) {
        setPadding(24, 0, 24, 0)
        setOnClickListener {
            binding.editableSudokuGrid.setSudokuNumber(number)
        }
    }
}
