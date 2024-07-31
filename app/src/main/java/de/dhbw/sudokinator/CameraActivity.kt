package de.dhbw.sudokinator

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import de.dhbw.sudokinator.databinding.ActivityCameraBinding
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class CameraActivity : AppCompatActivity(), CvCameraViewListener2 {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraBridgeViewBase: CameraBridgeViewBase
    private lateinit var scanButton: Button
    private var timeFound = 0L
    private lateinit var extractedImg: Mat
    private lateinit var corners: MatOfPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (OpenCVLoader.initLocal()) {
            Log.d(MainActivity::class.simpleName, "OpenCV loaded successfully")
        } else {
            Log.e(MainActivity::class.simpleName, "OpenCV initialization failed!")
            setResult(ACTIVITY_RESULT_ERROR)
            finish()
            return
        }

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraBridgeViewBase = binding.cameraView
        cameraBridgeViewBase.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase.setCvCameraViewListener(this)
        cameraBridgeViewBase.setCameraPermissionGranted()

        scanButton = binding.scanButton
        scanButton.isEnabled = false
        scanButton.setOnClickListener {

            val goal = MatOfPoint2f(
                Point(450.0, 0.0), Point(0.0, 0.0), Point(0.0, 450.0), Point(450.0, 450.0)
            )

            val corners2f = MatOfPoint2f(*corners.toArray())
            val matrix = Imgproc.getPerspectiveTransform(corners2f, goal)
            val rotatedImg = Mat()
            Imgproc.warpPerspective(extractedImg, rotatedImg, matrix, Size(450.0, 450.0))

            val bitmap = createBitmap(
                rotatedImg.width(), rotatedImg.height(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(rotatedImg, bitmap)
            rotatedImg.release()

            openFileOutput(BITMAP_FILE_NAME, MODE_PRIVATE).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            bitmap.recycle()

            setResult(RESULT_OK, Intent())
            finish()
        }
    }

    private fun setButtonEnabled(bool: Boolean) {
        try {
            runOnUiThread {
                scanButton.isEnabled = bool
            }
        } catch (_: IllegalStateException) {
            Log.e(CameraActivity::class.simpleName, "Could not update scanButton!")
        }
    }

    override fun onPause() {
        super.onPause()
        if (::cameraBridgeViewBase.isInitialized) cameraBridgeViewBase.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (::cameraBridgeViewBase.isInitialized) cameraBridgeViewBase.enableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraBridgeViewBase.isInitialized) cameraBridgeViewBase.disableView()
        if (::extractedImg.isInitialized) extractedImg.release()
        if (::corners.isInitialized) corners.release()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}

    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        val img = inputFrame.rgba()
        val (frame, contour) = findContour(inputFrame.gray())
        if (frame != null) {
            Imgproc.drawContours(img, listOf(contour), -1, Scalar(0.0, 255.0, 0.0, 255.0), 4)
            if (timeFound == 0L) {
                timeFound = System.currentTimeMillis()
            }
            if (System.currentTimeMillis() - timeFound > 400) {
                setButtonEnabled(true)
                extractedImg = frame
                corners = contour
            }
        } else {
            timeFound = 0
            setButtonEnabled(false)
        }
        return img
    }

    private fun findContour(img: Mat): Pair<Mat?, MatOfPoint> {
        val blurred = Mat()
        Imgproc.GaussianBlur(img, blurred, Size(3.0, 3.0), 0.0)

        val thresh = Mat()
        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, 1, 9, 5.0)
        blurred.release()

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            thresh, contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE
        )
        thresh.release()

        val biggestContour = MatOfPoint()
        var maxArea = 0.0
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            val areaDouble = MatOfPoint2f(*contour.toArray())
            val perimeter = Imgproc.arcLength(areaDouble, true)
            val approxCurve = MatOfPoint2f()
            Imgproc.approxPolyDP(areaDouble, approxCurve, 0.01 * perimeter, true)
            areaDouble.release()

            val approxCurveArray = approxCurve.toArray()
            approxCurve.release()
            if (approxCurveArray.size == 4 && area > maxArea && area > 100000) {
                biggestContour.fromArray(*approxCurveArray)
                maxArea = area
            }
            contour.release()
        }
        var result: Mat? = null
        if (biggestContour.toArray().isNotEmpty()) {
            val frame = Mat.zeros(img.size(), img.type())
            Imgproc.drawContours(frame, listOf(biggestContour), 0, Scalar(255.0, 0.0, 0.0), -1)
            Imgproc.drawContours(frame, listOf(biggestContour), 0, Scalar(0.0, 0.0, 0.0), 2)
            result = Mat()
            Core.bitwise_and(img, frame, result)
        }

        return result to biggestContour
    }
}