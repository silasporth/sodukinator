package de.dhbw.sudokinator

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity

class EditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_activity)

        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
        val doneButton: Button = findViewById(R.id.doneButton)

        // Retrieve Sudoku board data from the intent
        val sudokuBoard: Array<IntArray> = intent.getSerializableExtra("sudokuBoard") as Array<IntArray>

        // Add EditText views to each cell in the grid
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                val editText = EditText(this)
                editText.setText(sudokuBoard[i][j].toString())
                editText.gravity = Gravity.CENTER
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                editText.isFocusable = true // Enable focus
                editText.isClickable = true // Enable click
                editText.isFocusableInTouchMode = true // Enable touch mode

                // Add a TextWatcher to limit input to single-digit numbers
                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        if (!s.isNullOrBlank() && s.toString().toInt() > 9) {
                            // If the entered number is greater than 9, set it to 9
                            s.replace(0, s.length, "9")
                        }
                    }
                })

                // Set width and height to 0dp to make the EditText stretch
                val params = GridLayout.LayoutParams()
                params.width = 0
                params.height = 0
                params.rowSpec = GridLayout.spec(i, 1f) // Use weight to fill the available vertical space
                params.columnSpec = GridLayout.spec(j, 1f) // Use weight to fill the available horizontal space
                editText.layoutParams = params
                gridLayout.addView(editText)
                editText.setBackgroundResource(R.drawable.edit_text_border)
            }
        }

        doneButton.setOnClickListener {
            // Update the Sudoku board based on the values entered in EditText views
            val modifiedBoard = Array(9) { IntArray(9) }
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    val editText = gridLayout.getChildAt(i * 9 + j) as EditText
                    modifiedBoard[i][j] = editText.text.toString().toInt()
                }
            }

            // Create an intent to pass the modified Sudoku board back to the main activity
            val resultIntent = Intent()
            resultIntent.putExtra("modifiedBoard", modifiedBoard)
            setResult(RESULT_OK, resultIntent)

            // Finish the EditActivity to return to the main activity
            finish()
        }
    }
}
