package com.example.myapplication3_testing
import androidx.compose.ui.graphics.Paint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.example.myapplication3_testing.AccelerometerData
import com.example.myapplication3_testing.AppDatabase


import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException


class MainActivity : ComponentActivity() {
    private lateinit var accelerometerManager: AccelerometerManager
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app-db").build()
        accelerometerManager = AccelerometerManager(getSystemService(SENSOR_SERVICE) as SensorManager, db.accelerometerDataDao())
        setContent {
            MyApp(accelerometerManager,db,applicationContext)
        }
    }
}

@Composable
fun MyApp(accelerometerManager: AccelerometerManager,db: AppDatabase,context: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "screen1") {
        composable("screen1") {
            Screen1(navController, accelerometerManager,db, context)
        }
        composable("screen2") {
            Screen2(navController, db )
        }
    }
}

@Composable
fun Screen1(navController: NavController, accelerometerManager: AccelerometerManager, db: AppDatabase, context: Context) {
    var accelerationText by remember { mutableStateOf(TextFieldValue()) }
    var accelerometerDataList by remember { mutableStateOf(emptyList<AccelerometerData>()) }

    // Collect accelerometer data from the database when the composable is first composed
    LaunchedEffect(key1 = db) {
        accelerometerDataList = db.accelerometerDataDao().getAll()
    }

    val exportButtonClicked = remember { mutableStateOf(false) }

    // Launch a coroutine when the export button is clicked
    LaunchedEffect(exportButtonClicked.value) {
        if (exportButtonClicked.value) {
            exportDataToCSV(context, accelerometerDataList)
            // Reset the state after export
            exportButtonClicked.value = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Accelerometer App",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 20.sp,// Adjust the style as needed
            modifier = Modifier.padding(bottom = 16.dp) // Add some bottom padding
        )
        BasicTextField(
            value = accelerationText,
            onValueChange = { accelerationText = it },
            modifier = Modifier.width(200.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp)
        )
        Button(
            onClick = {
                accelerometerManager.startListening(object : AccelerometerManager.Listener {
                    override fun onAccelerationChanged(x: Float, y: Float, z: Float) {
                        accelerationText = TextFieldValue(text = "X: $x, Y: $y, Z: $z")
                    }
                })
            }
        ) {
            Text("Start Accelerometer")
        }
        Button(
            onClick = {
                exportButtonClicked.value = true // Set the state to indicate button click
            }
        ) {
            Text("Export Data to CSV")
        }
        Button(
            onClick = { navController.navigate("screen2") }
        ) {
            Text("Go to Screen 2")
        }
    }
}



suspend fun exportDataToCSV(context: Context, accelerometerDataList: List<AccelerometerData>) {
    withContext(Dispatchers.IO) {
        val csvFileName = "accelerometer_data.csv"
        val csvFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), csvFileName)

        try {
            FileWriter(csvFile).use { writer ->
                // Write header
                writer.append("ID,X,Y,Z\n")

                // Write data
                for (data in accelerometerDataList) {
                    writer.append("${data.id},${data.x},${data.y},${data.z}\n")
                }
                Log.d("AccelerometerData", "Saved CSV")
                writer.flush()
            }
        } catch (e: IOException) {
            Log.d("AccelerometerData", "not Saved CSV")
            e.printStackTrace()
        }
    }
}

@Composable
fun Screen2(navController: NavController, db: AppDatabase) {
    var accelerometerData by remember { mutableStateOf<List<AccelerometerData>?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Log.d("AccelerometerData", "Start fetching data")
            val allData = db.accelerometerDataDao().getAll()
            accelerometerData = allData.takeLast(10)
        }
    }

    if (accelerometerData != null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = { navController.navigate("screen1") },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Go to Screen 1")
                }
            }
            item {
                Text("X vs Time", modifier = Modifier.padding(start = 16.dp))
                Graph(accelerometerData!!) { it.x }
            }
            item {
                Text("Y vs Time", modifier = Modifier.padding(start = 16.dp))
                Graph(accelerometerData!!) { it.y }
            }
            item {
                Text("Z vs Time", modifier = Modifier.padding(start = 16.dp))
                Graph(accelerometerData!!) { it.z }
            }
        }
        Log.d("AccelerometerData", accelerometerData.toString())
    } else {
        // Show a loading indicator or placeholder until data is fetched
        Text("Loading data...")
    }
}



@Composable
fun Graph(
    data: List<AccelerometerData>,
    getValue: (AccelerometerData) -> Float,

) {
    Canvas(
        modifier = Modifier
            .width(300.dp)
            .height(150.dp)
    ) {
        val maxValue = data.maxOf { getValue(it) }
        val minValue = data.minOf { getValue(it) }
        val graphContentWidth = size.width * 0.8f // Adjust the width of the graph content
        val offsetX = (size.width - graphContentWidth) / 2 // Calculate the offset to center the graph content horizontally
        val deltaX =calculateDeltaX(graphContentWidth, data.size) //graphContentWidth / (data.size - 1)
        val deltaY = size.height / (maxValue - minValue)

        // Draw y-axis
        drawLine(
            start = Offset(offsetX, 0f),
            end = Offset(offsetX, size.height),
            color = Color.Black,
            strokeWidth = 2f
        )

        // Draw x-axis
        drawLine(
            start = Offset(offsetX, size.height),
            end = Offset(offsetX + graphContentWidth, size.height),
            color = Color.Black,
            strokeWidth = 2f
        )

        // Label x-axis with time values
        val timeLabels = List(data.size) { index -> (index * 500).toString() + "ms" }
        timeLabels.forEachIndexed { index, label ->
            drawContext.canvas.nativeCanvas.drawText(
                label,
                offsetX + index * deltaX,
                size.height + 20f, // Offset to position labels below x-axis
                android.graphics.Paint().apply {
                    textSize = 16f
                    color = Color.Black.toArgb()
                }
            )
        }

        // Draw data points
        val path = Path()
        path.moveTo(offsetX, size.height - (getValue(data[0]) - minValue) * deltaY)
        data.forEachIndexed { index, item ->
            val x = offsetX + index * deltaX
            val y = size.height - (getValue(item) - minValue) * deltaY
            path.lineTo(x, y)
            drawCircle(
                color = Color.Red,
                center = Offset(x, y),
                radius = 4f
            )
        }

        drawPath(
            path = path,
            color = Color.Blue,
            style = Stroke(width = 2f)
        )
    }
}

fun calculateDeltaX(graphContentWidth: Float, dataSize: Int): Float {
    val maxLabels = 5 // Maximum number of labels to display
    val optimalDeltaX = graphContentWidth / maxLabels // Optimal spacing between labels
    val minDeltaX = graphContentWidth / (dataSize - 1) // Minimum spacing between labels to fit all labels
    return if (optimalDeltaX > minDeltaX) minDeltaX else optimalDeltaX
}
class AccelerometerManager(private val sensorManager: SensorManager, private val accelerometerDataDao: AccelerometerDataDao) : SensorEventListener {

    interface Listener {
        fun onAccelerationChanged(x: Float, y: Float, z: Float)
    }

    private var listener: Listener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastUpdateTime: Long = 0
    private val delayMillis = 200L // 2 seconds

    fun startListening(listener: Listener) {
        this.listener = listener
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        listener = null
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Ignored for this example
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= delayMillis) {
                    listener?.onAccelerationChanged(it.values[0], it.values[1], it.values[2])
                    lastUpdateTime = currentTime

                    // Insert data into database
                    GlobalScope.launch(Dispatchers.IO) {
                        accelerometerDataDao.insert(AccelerometerData(x = it.values[0], y = it.values[1], z = it.values[2]))
                    }
                }
            }
        }
    }
}
