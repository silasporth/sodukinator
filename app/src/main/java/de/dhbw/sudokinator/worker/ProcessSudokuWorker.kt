package de.dhbw.sudokinator.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.dhbw.sudokinator.BITMAP_FILE_NAME
import de.dhbw.sudokinator.WORKER_DATA_SUDOKU_BOARD
import java.io.File

class ProcessSudokuWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val bitmap =
            BitmapFactory.decodeFile(File(applicationContext.filesDir, BITMAP_FILE_NAME).path)
                ?: return Result.failure()

        val sudokuArray = scanAndParseBitmap(bitmap)

        return if (sudokuArray.isNotEmpty()) {
            val data = Data.Builder().putIntArray(WORKER_DATA_SUDOKU_BOARD, sudokuArray).build()
            Result.success(data)
        } else Result.failure()
    }

    private fun scanAndParseBitmap(imageBitmap: Bitmap): IntArray {
        val cellBitmaps = mutableListOf<Bitmap>()
        for (y in 0..8) {
            for (x in 0..8) {
                cellBitmaps.add(
                    Bitmap.createBitmap(
                        imageBitmap,
                        x * bitmapCellSize,
                        y * bitmapCellSize,
                        bitmapCellSize,
                        bitmapCellSize
                    )
                )
            }
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val totalTask = Tasks.whenAllComplete(cellBitmaps.map {
            val image = InputImage.fromBitmap(it, 0)
            recognizer.process(image)
        })

        Tasks.await(totalTask)
        if (!totalTask.isSuccessful) return IntArray(0)

        val sudokuArray = IntArray(9 * 9)
        val allTasks = totalTask.result.takeIf { it.size <= 9 * 9 } ?: return IntArray(0)
        for ((i, task) in allTasks.withIndex()) {
            val taskResult = task.result
            if (taskResult !is Text) {
                sudokuArray[i] = 0
                continue
            }
            sudokuArray[i] = parseText(taskResult)
        }
        return sudokuArray
    }

    private fun parseText(visionText: Text): Int {
        val elements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }
        for (element in elements) {
            val value = element.text.toIntOrNull() ?: continue

            if (value in 1..9) {
                return value
            }
        }
        return 0
    }

    companion object {
        private const val bitmapCellSize = 50
    }
}