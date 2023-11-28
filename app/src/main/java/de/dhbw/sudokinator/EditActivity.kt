package de.dhbw.sudokinator

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity

class EditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_activity)

        val gridLayout: GridLayout = findViewById(R.id.gridLayout)

        // Add buttons to each cell in the grid
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                val button = Button(this)
                button.text = "Cell $i-$j"
                val params = GridLayout.LayoutParams()
                params.rowSpec = GridLayout.spec(i)
                params.columnSpec = GridLayout.spec(j)
                button.layoutParams = params
                gridLayout.addView(button)
            }
        }
    }
}