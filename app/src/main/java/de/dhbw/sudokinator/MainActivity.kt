package de.dhbw.sudokinator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.widget.ButtonBarLayout


class MainActivity : ComponentActivity() {

    lateinit var imageView: ImageView
    lateinit var captureButton: Button
    lateinit var editButton: Button
    lateinit var solveButton: Button
    val REQUEST_IMAGE_CAPTURE = 100
    val CAMERA_PERMISSION_REQUEST = 101
    val sudokuBoard = Array(9) { IntArray(9) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Debugging
        val sudokuImage = resources.getIdentifier("sudoku", "drawable", packageName)

        if (sudokuImage != 0) {
            val sudokuImageBitmap: Bitmap? = BitmapFactory.decodeResource(resources, sudokuImage)

            if (sudokuImageBitmap != null) {
                scanBoard(sudokuImageBitmap)
            } else {
                // Handle the case where decoding fails
                Toast.makeText(this, "Failed to decode Sudoku image", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle the case where the resource is not found
            Toast.makeText(this, "Sudoku image not found", Toast.LENGTH_SHORT).show()
        }


        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)
        editButton = findViewById(R.id.editButton)
        solveButton = findViewById(R.id.solveButton)

        val sudokuBoardBitmap = drawSudokuBoard(sudokuBoard)
        imageView.setImageBitmap(sudokuBoardBitmap)

        captureButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Camera permission not granted, request it
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            } else {
                openCamera()
            }
        }

        solveButton.setOnClickListener {
            solveSudoku(sudokuBoard)
            val sudokuBoardBitmap = drawSudokuBoard(sudokuBoard)
            imageView.setImageBitmap(sudokuBoardBitmap)
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Error: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, open the camera
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            scanBoard(imageBitmap)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun drawSudokuBoard(sudokuBoard: Array<IntArray>): Bitmap {
        val size = sudokuBoard.size
        val cellSize = 60
        val blockLineWidth = 4f
        val boardSize = size * cellSize
        val bitmap = Bitmap.createBitmap(boardSize, boardSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, boardSize.toFloat(), boardSize.toFloat(), paint)

        paint.color = Color.BLACK
        paint.strokeWidth = 2f

        for (i in 0..size) {
            val startX = i * cellSize.toFloat()
            val startY = 0f
            val endX = startX
            val endY = boardSize.toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)

            val startX2 = 0f
            val startY2 = i * cellSize.toFloat()
            val endX2 = boardSize.toFloat()
            val endY2 = startY2
            canvas.drawLine(startX2, startY2, endX2, endY2, paint)

            if (i % 3 == 0 && i > 0) {
                paint.strokeWidth = blockLineWidth
                canvas.drawLine(startX, startY, startX, endY, paint)
                canvas.drawLine(startX2, startY2, endX2, startY2, paint)
                paint.strokeWidth = 2f
            }
        }

        val textSize = 40f
        paint.textSize = textSize
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER

        for (row in 0 until size) {
            for (col in 0 until size) {
                val cellValue = sudokuBoard[row][col]
                if (cellValue != 0) {
                    val x = col * cellSize + cellSize / 2
                    val y = row * cellSize + cellSize / 2 - (paint.ascent() + paint.descent()) / 2
                    canvas.drawText(cellValue.toString(), x.toFloat(), y, paint)
                }
            }
        }

        return bitmap
    }


    private fun scanBoard(imageBitmap: Bitmap) {
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener(OnSuccessListener<Text> { visionText ->
                displayNumbers(visionText, image.width / 9, image.height / 9)
            })
            .addOnFailureListener(OnFailureListener { e ->
                Toast.makeText(this, "Text recognition failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            })
    }

    private fun displayNumbers(visionText: Text, width: Int, height: Int) {
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val value = element.text.toInt()
                    Log.d("NumericText", "Numeric text for line: $value")

                    val x = element.boundingBox?.exactCenterX()!!.toInt() / width
                    val y = element.boundingBox?.exactCenterY()!!.toInt() / height
                    if (value != null && x in 0..8 && y in 0..8 && value in 1..9) {
                        sudokuBoard[y][x] = value
                    }

                }
            }
        }
        val sudokuBoardBitmap = drawSudokuBoard(sudokuBoard)
        imageView.setImageBitmap(sudokuBoardBitmap)
    }

    fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Check if the number can be placed in the specified cell
        for (i in 0 until 9) {
            if (board[row][i] == num || board[i][col] == num ||
                board[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3] == num
            ) {
                return false
            }
        }
        return true
    }

    fun solveSudoku(board: Array<IntArray>): Boolean {
        // Find an empty cell
        val emptyCell = findEmptyCell(board)

        // If no empty cell is found, the puzzle is solved
        if (emptyCell == null) {
            return true
        }

        val (row, col) = emptyCell

        // Try placing digits 1 through 9 in the empty cell
        for (num in 1..9) {
            if (isValid(board, row, col, num)) {
                // Place the digit if it's valid
                board[row][col] = num

                // Recursively try to solve the rest of the puzzle
                if (solveSudoku(board)) {
                    return true
                }

                // If placing the current digit doesn't lead to a solution, backtrack
                board[row][col] = 0
            }
        }

        // No valid digit found, backtrack
        return false
    }

    fun findEmptyCell(board: Array<IntArray>): Pair<Int, Int>? {
        // Find the first empty cell (cell with value 0)
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (board[i][j] == 0) {
                    return Pair(i, j)
                }
            }
        }
        return null
    }}

