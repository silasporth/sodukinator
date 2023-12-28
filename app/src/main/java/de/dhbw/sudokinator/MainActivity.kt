package de.dhbw.sudokinator

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.dhbw.sudokinator.databinding.ActivityMainBinding
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var editButton: Button
    private lateinit var solveButton: Button
    private lateinit var clearButton: Button
    private val CAMERA_PERMISSION_REQUEST = 101
    private val sudokuBoard = Array(9) { IntArray(9) }
    private val cleanSudokuImage: Bitmap = createCleanSudokuImage()

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                RESULT_OK -> {
                    val imageBitmap = it.data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        scanBoard(imageBitmap)
                    } else {
                        toastErrorSomething()
                    }
                }
            }

        }
    private val editActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                RESULT_OK -> {
                    val modifiedBoard = getSudokuFromIntentOrNull(it.data)
                    if (modifiedBoard == null) {
                        toastErrorSomething()
                    } else {
                        updateSudokuBoard(modifiedBoard)
                    }
                }

                ACTIVITY_RESULT_ERROR -> {
                    toastErrorSomething()
                }
            }
        }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val bitmap = result.getBitmap(this)
            if (bitmap != null) {
                scanBoard(bitmap)
            } else {
                toastErrorSomething()
            }
        } else {
            // An error occurred.
            toastErrorSomething()
            val exception = result.error
            Log.e(MainActivity::class.simpleName, "CROPPING ERROR: $exception")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageView = binding.imageView
        captureButton = binding.captureButton
        editButton = binding.editButton
        solveButton = binding.solveButton
        clearButton = binding.clearButton

        drawSudokuBoard(sudokuBoard)

        captureButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Camera permission not granted, request it
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST
                )
            } else {
                //openCamera()
                startCrop()
            }
        }

        editButton.setOnClickListener {
            editActivityResultLauncher.launch(
                Intent(this@MainActivity, EditActivity::class.java).putExtra(
                    INTENT_EXTRA_SUDOKU_BOARD, sudokuBoard
                )
            )
            Log.d("State", "Sudoku board: ${sudokuBoard.contentDeepToString()}")
        }

        solveButton.setOnClickListener {
            if (isSolvable(sudokuBoard)) {
                if (solveSudoku(sudokuBoard)) {
                    updateSudokuBoard(sudokuBoard)
                } else {
                    Toast.makeText(this, "The Sudoku board is unsolvable!", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "The Sudoku board is unsolvable!", Toast.LENGTH_SHORT).show()
            }
        }


        clearButton.setOnClickListener {
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    sudokuBoard[i][j] = 0
                }
            }
            updateSudokuBoard(sudokuBoard)
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraActivityResultLauncher.launch(takePictureIntent)
    }

    private fun startCrop() {
        // Start picker to get image for cropping from only gallery and then use the image in cropping activity.
        cropImage.launch(
            CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeCamera = true,
                    imageSourceIncludeGallery = false,
                    fixAspectRatio = true
                ),
            ),
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, open the camera

            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSudokuBoard(modifiedBoard: Array<IntArray>) {
        // Update the sudokuBoard with the modified board
        for (i in 0 until 9) {
            sudokuBoard[i] = modifiedBoard[i].clone()
        }

        // Redraw the Sudoku board
        drawSudokuBoard(modifiedBoard)
    }

    private fun drawSudokuBoard(sudokuBoard: Array<IntArray>) {
        val sudokuBoardBitmap = generateSudokuImage(sudokuBoard)
        imageView.setImageBitmap(sudokuBoardBitmap)
    }

    private fun generateSudokuImage(sudokuBoard: Array<IntArray>): Bitmap {
        val bitmap = cleanSudokuImage.copy(cleanSudokuImage.config, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw Numbers
        val textSize = 40f
        paint.textSize = textSize
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        val halfCellSize = SUDOKU_CELL_PIXEL_SIZE / 2

        for (row in 0 until SUDOKU_ROWS) {
            for (col in 0 until SUDOKU_COLUMNS) {
                val cellValue = sudokuBoard[row][col]
                if (cellValue == 0) continue

                val x = col * SUDOKU_CELL_PIXEL_SIZE + halfCellSize
                val y =
                    row * SUDOKU_CELL_PIXEL_SIZE + halfCellSize - (paint.ascent() + paint.descent()) / 2
                canvas.drawText(cellValue.toString(), x, y, paint)
            }
        }
        return bitmap
    }

    private fun scanBoard(imageBitmap: Bitmap) {
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image).addOnSuccessListener { visionText ->
            displayNumbers(visionText, image.width / 9, image.height / 9)
        }.addOnFailureListener { e ->
            Log.e(MainActivity::class.simpleName, "Text recognition failed: $e")
            Toast.makeText(
                this, "Couldn't recognize your image. Please try again!", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun displayNumbers(visionText: Text, width: Int, height: Int) {
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    try {
                        val value = element.text.toInt()
                        Log.d("NumericText", "Numeric text for line: $value")

                        val x = element.boundingBox?.exactCenterX()?.toInt()?.div(width) ?: 0
                        val y = element.boundingBox?.exactCenterY()?.toInt()?.div(height) ?: 0
                        if (x in 0..8 && y in 0..8 && value in 1..9) {
                            sudokuBoard[y][x] = value
                        }
                    } catch (e: NumberFormatException) {
                        Log.e(
                            MainActivity::class.simpleName,
                            "Error converting text to number: ${element.text}"
                        )
                        // Continue to the next element even if conversion fails
                        continue
                    }
                }
            }
        }
        updateSudokuBoard(sudokuBoard)
    }

    private fun toastErrorSomething() = Toast.makeText(
        this, "Something went wrong, please try again", Toast.LENGTH_SHORT
    ).show()
}

