package de.dhbw.sudokinator.components

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText

class EditableSudokuCell(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    AppCompatEditText(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.editTextStyle)

    constructor(context: Context) : this(context, null)

    init {
        gravity = Gravity.CENTER
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
        isFocusable = true // Enable focus
        isClickable = true // Enable click
        isFocusableInTouchMode = true // Enable touch mode
        filters = arrayOf(InputFilter.LengthFilter(2),
            InputFilter { source, _, _, _, _, _ -> return@InputFilter if (source == "0") "" else source })
        isCursorVisible = false

        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) setBackgroundResource(de.dhbw.sudokinator.R.drawable.edit_text_border_focus)
            else setBackgroundResource(de.dhbw.sudokinator.R.drawable.edit_text_border)
        }

        // Add a TextWatcher to limit input to single-digit numbers
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {
                if (s.length > 1) {
                    s.delete(0, 1)
                }
            }
        })

        setBackgroundResource(de.dhbw.sudokinator.R.drawable.edit_text_border)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        setSelection(length())
    }
}