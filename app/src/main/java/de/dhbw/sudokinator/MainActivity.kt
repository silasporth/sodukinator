package de.dhbw.sudokinator

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.dhbw.sudokinator.databinding.ActivityMainBinding
import de.dhbw.sudokinator.worker.ProcessSudokuWorker
import de.dhbw.sudokinator.worker.SudokuSolverWorker
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var workManager: WorkManager
    private lateinit var imageView: ImageView
    private lateinit var cleanSudokuImage: Bitmap
    private val sudokuBoard = Array(SUDOKU_ROWS) { IntArray(SUDOKU_COLUMNS) }
    private var startNumbersCoordinates = mutableListOf<Pair<Int, Int>>()
    private lateinit var loadingIndicator: ProgressDialog

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) return@registerForActivityResult

            val uuid = startProcessSudoku()
            trackProcessSudoku(uuid)
        }

    private val editActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                RESULT_OK -> {
                    val modifiedBoard = it.data?.getIntArrayExtra(INTENT_EXTRA_SUDOKU_BOARD)?.toSudokuBoard()
                    if (modifiedBoard == null) {
                        toastErrorSomething()
                    } else {
                        startNumbersCoordinates = saveStartNumbersCoordinates(modifiedBoard)
                        updateSudokuBoard(modifiedBoard)
                    }
                }

                ACTIVITY_RESULT_ERROR -> {
                    toastErrorSomething()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workManager = WorkManager.getInstance(this)
        loadingIndicator = ProgressDialog(this)

        imageView = binding.imageView

        cleanSudokuImage = createCleanSudokuImage(imageView.layoutParams.width)

        drawSudokuBoard(sudokuBoard)

        binding.captureButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Camera permission not granted, request it
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST
                )
            } else {
                openCamera()
            }
        }

        binding.editButton.setOnClickListener {
            editActivityResultLauncher.launch(
                Intent(this@MainActivity, EditActivity::class.java).putExtra(
                    INTENT_EXTRA_SUDOKU_BOARD, sudokuBoard.flatten()
                )
            )
        }

        binding.solveButton.setOnClickListener {
            startNumbersCoordinates = saveStartNumbersCoordinates(sudokuBoard)
            val workerUUID = startSudokuSolver()
            trackSudokuSolver(workerUUID)
        }
    }

    private fun openCamera() {
        cameraActivityResultLauncher.launch(Intent(this@MainActivity, CameraActivity::class.java))
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

        val sudokuCellSize = imageView.layoutParams.width / SUDOKU_COLUMNS

        // Draw Numbers
        paint.textSize = sudokuCellSize * 2f / 3f
        paint.textAlign = Paint.Align.CENTER
        val halfCellSize = sudokuCellSize / 2f

        for (row in 0 until SUDOKU_ROWS) {
            for (col in 0 until SUDOKU_COLUMNS) {
                val cellValue = sudokuBoard[row][col]
                if (cellValue == 0) continue

                paint.color = if (startNumbersCoordinates.contains(col to row)) {
                    Color.BLACK
                } else {
                    getColor(R.color.royal_blue)
                }

                val x = col * sudokuCellSize + halfCellSize
                val y = row * sudokuCellSize + halfCellSize - (paint.ascent() + paint.descent()) / 2
                canvas.drawText(cellValue.toString(), x, y, paint)
            }
        }
        return bitmap
    }

    private fun startSudokuSolver(): UUID {
        val data =
            Data.Builder().putIntArray(WORKER_DATA_SUDOKU_BOARD, sudokuBoard.flatten()).build()

        val solverWorker =
            OneTimeWorkRequestBuilder<SudokuSolverWorker>().setInputData(data).build()

        workManager.beginUniqueWork(
            UNIQUE_SOLVER_WORKER_ID, ExistingWorkPolicy.KEEP, solverWorker
        ).enqueue()

        return solverWorker.id
    }

    private fun trackSudokuSolver(workerUUID: UUID) {
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_SOLVER_WORKER_ID).observe(this) {
            val solverWorkerInfo =
                it.find { workInfo -> workInfo.id == workerUUID } ?: return@observe

            when (solverWorkerInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    val data = solverWorkerInfo.outputData
                    if (data.getBoolean(WORKER_DATA_SUDOKU_SOLVABLE, false)) {
                        val sudokuBoard =
                            data.getIntArray(WORKER_DATA_SUDOKU_BOARD)?.toSudokuBoard()
                                ?: return@observe
                        updateSudokuBoard(sudokuBoard)
                    } else {
                        Toast.makeText(
                            this, "The Sudoku board is unsolvable!", Toast.LENGTH_SHORT
                        ).show()
                    }
                    loadingIndicator.dismiss()
                }

                WorkInfo.State.RUNNING -> {
                    loadingIndicator.apply {
                        setTitle("Calculating...")
                        setMessage("")
                        setCancelable(false)
                        show()
                    }
                }

                WorkInfo.State.FAILED -> {
                    toastErrorSomething()
                    loadingIndicator.dismiss()
                }

                else -> {}
            }
        }
    }

    private fun startProcessSudoku(): UUID {
        val processSudokuWorker = OneTimeWorkRequestBuilder<ProcessSudokuWorker>().build()

        workManager.beginUniqueWork(
            UNIQUE_PROCESS_SUDOKU_WORKER_ID, ExistingWorkPolicy.KEEP, processSudokuWorker
        ).enqueue()

        return processSudokuWorker.id
    }

    private fun trackProcessSudoku(workerUUID: UUID) {
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_PROCESS_SUDOKU_WORKER_ID)
            .observe(this) {
                val processSudokuWorkerInfo =
                    it.find { workInfo -> workInfo.id == workerUUID } ?: return@observe

                when (processSudokuWorkerInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val sudokuBoard =
                            processSudokuWorkerInfo.outputData.getIntArray(WORKER_DATA_SUDOKU_BOARD)
                                ?.toSudokuBoard()
                        if (sudokuBoard == null) {
                            toastErrorSomething()
                            loadingIndicator.dismiss()
                            return@observe
                        }
                        startNumbersCoordinates = saveStartNumbersCoordinates(sudokuBoard)
                        updateSudokuBoard(sudokuBoard)
                        loadingIndicator.dismiss()
                    }

                    WorkInfo.State.RUNNING -> {
                        loadingIndicator.apply {
                            setTitle("Extracting numbers...")
                            setMessage("")
                            setCancelable(false)
                            show()
                        }
                    }

                    WorkInfo.State.FAILED -> {
                        Log.e(MainActivity::class.simpleName, "Text recognition failed!")
                        Toast.makeText(
                            this,
                            "Could not process your sudoku. Please try again",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingIndicator.dismiss()
                    }

                    else -> {}
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
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

    private fun toastErrorSomething() = Toast.makeText(
        this, "Something went wrong, please try again", Toast.LENGTH_SHORT
    ).show()

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 101
    }
}

