package de.dhbw.sudokinator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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


class MainActivity : ComponentActivity() {

    lateinit var imageView: ImageView
    lateinit var captureButton: Button
    lateinit var textView: TextView
    val REQUEST_IMAGE_CAPTURE = 100
    val CAMERA_PERMISSION_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)
        textView = findViewById(R.id.textView)

        captureButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Camera permission not granted, request it
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            } else {
                openCamera()
            }
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
            imageView.setImageBitmap(imageBitmap)
            scanText(imageBitmap)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun scanText(imageBitmap: Bitmap){
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener(OnSuccessListener<Text> { visionText ->
                // Task completed successfully
                displayText(visionText)
            })
            .addOnFailureListener(OnFailureListener { e ->
                // Task failed with an exception
                Toast.makeText(this, "Text recognition failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            })
    }

    private fun  displayText(visionText: Text) {
        val result = StringBuilder()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    result.append(element.text).append(" ")
                }
                result.append("\n")
            }
        }

        // Update the textView with the recognized text
        textView.text = result.toString()
    }
}
