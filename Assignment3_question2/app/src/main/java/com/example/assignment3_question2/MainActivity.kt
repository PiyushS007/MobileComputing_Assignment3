package com.example.assignment3_question2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier



import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap


import androidx.compose.ui.unit.dp
import com.example.assignment3_question2.ml.ImageModel

import org.tensorflow.lite.DataType

import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

import android.net.Uri
import java.io.IOException

private val PICK_IMAGE_REQUEST = 1
private var imageBitmap by mutableStateOf<Bitmap?>(null)

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(applicationContext, this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val uri: Uri? = data.data
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val selectedImage = BitmapFactory.decodeStream(inputStream)
                    imageBitmap = selectedImage
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}

@Composable
fun MyApp(context: Context, activity: Activity) {
    Surface(color = MaterialTheme.colorScheme.background) {
        ImageClassifier(context, activity)
    }
}

@Composable
fun ImageClassifier(context: Context, activity: Activity) {
    var classificationResult by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show the selected image if available
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier.size(200.dp)
            )
        } else {
            Text(text = "No image selected")
        }

        // Button to load/select image
        Row {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                activity.startActivityForResult(intent, PICK_IMAGE_REQUEST)
            }) {
                Text(text = "Select Image")
            }
        }

        // Button to start classification process
        Button(
            onClick = {
                if (imageBitmap != null) {
                    output = runInference(imageBitmap!!, context)
                    Log.d("Processed Image", "Done")
                }
            },
            enabled = imageBitmap != null // Enable button only if an image is selected
        ) {
            Text(text = "Classify Image")
        }

        // Display the classification result if available
        if (output.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = output)
        }
    }
}

fun runInference(image: Bitmap, context: Context): String {
    val label = context.applicationContext.assets.open("labels.txt").bufferedReader().readLines()

    val inputSize = 224
    val tens = TensorImage(DataType.UINT8)
    tens.load(image)
    val td = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    val processedImage = td.process(tens)

    val model = ImageModel.newInstance(context)

    // Creates inputs for reference.
    val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
    inputFeature0.loadBuffer(processedImage.buffer)

    // Runs model inference and gets result.
    val outputs = model.process(inputFeature0)
    val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

    var idx = 0
    outputFeature0.forEachIndexed { index, fl ->
        if (outputFeature0[idx] < fl) {
            idx = index
        }
    }
    val result: String = label[idx]

    // Releases model resources if no longer used.
    model.close()

    return result
}






