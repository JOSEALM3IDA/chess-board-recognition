package pt.josealm3ida.android.chesser

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import pt.josealm3ida.android.chesser.ml.Model
import pt.josealm3ida.android.chesser.pieces.*
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random


class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private lateinit var context: Context
    private lateinit var pieceMatrix: Array<Array<Piece>>


    @ExperimentalMaterialApi
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OpenCVLoader.initDebug()

        pieceMatrix = Array(8) { Array(8) { EmptyPiece() } }

        setContent {
            context = LocalContext.current;
            MainScreen()
        }
    }

    var isCameraSelected = false
    var imageUri: Uri? = null
    var bitmap: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.P)
    @ExperimentalMaterialApi
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
        val coroutineScope = rememberCoroutineScope()

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            this.imageUri = uri
            this.bitmap = null
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { btm: Bitmap? ->
            this.bitmap = btm
            this.imageUri = null
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            if (isCameraSelected) {
                cameraLauncher.launch()
            } else {
                galleryLauncher.launch("image/*")
            }

            coroutineScope.launch {
                bottomSheetModalState.hide()
            }
        }
        
        ModalBottomSheetLayout(
            sheetContent = {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(MaterialTheme.colors.primary.copy(0.08f))
                ) {
                    Column (
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text (
                            text = "Open Camera",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(
                                            context, CAMERA
                                        ) -> {
                                            cameraLauncher.launch()
                                            coroutineScope.launch {
                                                bottomSheetModalState.hide()
                                            }
                                        }
                                        else -> {
                                            isCameraSelected = true
                                            permissionLauncher.launch(CAMERA)
                                        }
                                    }
                                }
                                .padding(15.dp),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )

                        Text (
                            text = "Choose from Gallery",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(
                                            context, READ_EXTERNAL_STORAGE
                                        ) -> {
                                            cameraLauncher.launch()
                                            coroutineScope.launch {
                                                bottomSheetModalState.hide()
                                            }
                                        }
                                        else -> {
                                            isCameraSelected = false
                                            permissionLauncher.launch(READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                }
                                .padding(15.dp),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )

                        Text (
                            text = "Cancel",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch { bottomSheetModalState.hide() }
                                }
                                .padding(15.dp),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
           },

            sheetState = bottomSheetModalState,
            sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            Box (
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
            ) {
                Spacer(Modifier.fillMaxHeight())

                Button (
                    onClick = {
                        coroutineScope.launch {
                            if (!bottomSheetModalState.isVisible) {
                                bottomSheetModalState.show()
                            } else {
                                bottomSheetModalState.hide()
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = "Create",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )

                    Text (
                        text = "Capture Board",
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            }

            imageUri?.let {
                if (!isCameraSelected) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    this.bitmap = ImageDecoder.decodeBitmap(source)
                }
            }

            this.bitmap?.let {
                parseBitmap()
                ElementLayout()
            }
        }

        bitmap?.let {
            parseBitmap()
            ElementLayout()
        }
    }

    @Composable
    fun ElementLayout() {
        Column (
            Modifier
                .fillMaxSize()
                .padding(top = 30.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.border(BorderStroke(2.dp, Color.Black)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                for (c in 0..7) {
                    Column {
                        for (r in 0..7) {
                            Box(
                                Modifier
                                    .background(if (r % 2 == 0) if (c % 2 == 0) Color.Black else Color.White else if (c % 2 == 0) Color.White else Color.Black)
                                    .align(Alignment.CenterHorizontally)
                                    .size(30.dp)
                            ) {
                                val piece = pieceMatrix[c][r]
                                if (piece.pieceColor != PieceColor.NONE) {
                                        Image(
                                            modifier = Modifier
                                                .padding(3.dp),
                                            painter = painterResource(id = piece.drawable),
                                            contentDescription = piece.figurine.toString()
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            Image (
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Image",
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .padding(top = 30.dp, bottom = 100.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        }
    }

    @ExperimentalMaterialApi
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        setContent {
            MainScreen()
        }
    }

    // Future me, please reorganize the code and split this monster function
    private fun parseBitmap() {
        val originalImgMat = Mat()
        val bmp32 = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp32, originalImgMat)

        Imgproc.cvtColor(originalImgMat, originalImgMat, Imgproc.COLOR_BGR2RGB)

        val imgDir = context.filesDir.absolutePath + "/" + Constants.IMAGE_DIR
        val fileDir = File(imgDir)
        fileDir.mkdirs()
        Imgcodecs.imwrite("$imgDir/original.png", originalImgMat)
        val imgMat = Mat()
        originalImgMat.copyTo(imgMat)

        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGB2GRAY)

        val blurMat = Mat(5, 5, CvType.CV_32F)
        for (i in 0..5) {
            for (j in 0..5) {
                blurMat.put(i, j, 1/25.0)
            }
        }

        val blurredMat = Mat()
        Imgproc.filter2D(imgMat, blurredMat, -1, blurMat)
        Imgcodecs.imwrite("$imgDir/blurred.png", blurredMat)

        val edgeMap = Mat()
        Imgproc.Canny(blurredMat, edgeMap, 10.0, 200.0)
        Imgcodecs.imwrite("$imgDir/edge.png", edgeMap)

        val houghMat = Mat()
        Imgproc.HoughLines(edgeMap, houghMat, 1.0, Math.PI / 180, 150)

        originalImgMat.copyTo(imgMat)

        val labels = Mat()
        Core.kmeans(houghMat,
            2,
            labels,
            TermCriteria(TermCriteria.COUNT, 100, 1.0),
            10,
            Core.KMEANS_RANDOM_CENTERS
        )

        val hLinePoints = mutableListOf<Array<Point>>()
        val vLinePoints = mutableListOf<Array<Point>>()
        for (x in 0 until houghMat.rows()) {
            val rho = houghMat.get(x, 0)[0]
            val theta = houghMat.get(x, 0)[1]
            val a = cos(theta)
            val b = sin(theta)
            val x0 = a * rho
            val y0 = b * rho

            val pt1 = Point(
                (x0 + 1000 * -b).roundToInt().toDouble(),
                (y0 + 1000 * a).roundToInt().toDouble()
            )

            val pt2 = Point(
                (x0 - 1000 * -b).roundToInt().toDouble(),
                (y0 - 1000 * a).roundToInt().toDouble()
            )

            val color: Scalar
            if (theta < Math.PI / 4 || theta > Math.PI - Math.PI / 4) {
                color = Scalar(0.0, 0.0, 255.0)
                vLinePoints.add(arrayOf(pt1, pt2))
            } else {
                color = Scalar(0.0, 255.0, 0.0)
                hLinePoints.add(arrayOf(pt1, pt2))
            }

            Imgproc.line(imgMat, pt1, pt2, color, 2, Imgproc.LINE_AA, 0)
        }

        val intersections = mutableListOf<Point>()
        for (hLinePoint in hLinePoints) {
            val p1 = hLinePoint[0]
            val p2 = hLinePoint[1]
            for (vLinePoint in vLinePoints) {
                val p3 = vLinePoint[0]
                val p4 = vLinePoint[1]

                val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
                val px = ((p1.x * p2.y - p1.y * p2.x) * (p3.x - p4.x) - (p1.x - p2.x) * (p3.x * p4.y - p3.y * p4.x)) / d
                val py = ((p1.x * p2.y - p1.y * p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x * p4.y - p3.y * p4.x)) / d

                if (px >= 0 && px < originalImgMat.cols() && py >= 0 && py < originalImgMat.rows()) intersections.add(Point(px, py))
            }
        }

        Imgcodecs.imwrite("$imgDir/hough.png", imgMat)
        originalImgMat.copyTo(imgMat)

        val pointsToCluster = mutableListOf<DoublePoint>()
        for (point in intersections) pointsToCluster.add(DoublePoint(doubleArrayOf(point.x, point.y)))

        val clusters = DBSCANClusterer<DoublePoint>(50.0, 1).cluster(pointsToCluster)
        val finalPoints = mutableListOf<Point>()

        var smallestX = -1.0
        var smallestY = -1.0
        var biggestY = -1.0
        var biggestX = -1.0

        var smallestSum = originalImgMat.rows().toDouble() + originalImgMat.cols()
        var biggestSum = 0.0

        for (cluster in clusters) {
            var avgX = 0.0
            var avgY = 0.0
            for (point in cluster.points) {
                avgX += point.point[0]
                avgY += point.point[1]
            }

            avgX /= cluster.points.size
            avgY /= cluster.points.size

            val currSum = avgX + avgY
            if (currSum < smallestSum) {
                smallestSum = currSum
                smallestX = avgX
                smallestY = avgY
            }

            if (currSum > biggestSum) {
                biggestSum = currSum
                biggestX = avgX
                biggestY = avgY
            }
            val point = Point(avgX, avgY)
            finalPoints.add(point)
            Imgproc.circle(imgMat, point, 2, Scalar(0.0, 0.0, 255.0), 2)
        }

        Imgcodecs.imwrite("$imgDir/points.png", imgMat)

        val deltaX = (biggestX - smallestX) / 8
        val deltaY = (biggestY - smallestY) / 8

        val extraWidth = deltaX / 3
        val extraHeight = deltaY / 5

        val widthShift = { i: Int ->
            when (i) {
                0 -> 50
                1 -> 35
                2 -> 20
                3 -> 10
                4 -> 0
                5 -> -15
                6 -> -20
                else -> -20
            }
        }

        val heightShift = { i: Int ->
            when (i) {
                0 -> -20
                1 -> -20
                2 -> -20
                3 -> -15
                4 -> -15
                5 -> -15
                6 -> -15
                else -> -15
            }
        }

        originalImgMat.copyTo(imgMat)
        val roiDir = context.filesDir.absolutePath + "/" + Constants.ROI_DIR
        val roiMats = mutableListOf<List<Mat>>()
        File(roiDir).mkdirs()
        for (y in 0 until 8) {
            val currRow = mutableListOf<Mat>()
            for (x in 0 until 8) {
                var xTopLeft = smallestX + x * deltaX - extraWidth + widthShift(x)
                xTopLeft = if (xTopLeft >= 0) xTopLeft else 0.0
                xTopLeft = if (xTopLeft <= originalImgMat.cols()) xTopLeft else originalImgMat.cols().toDouble()


                var yTopLeft = smallestY + y * deltaY - extraWidth + heightShift(y)
                yTopLeft = if (yTopLeft >= 0) yTopLeft else 0.0
                yTopLeft = if (yTopLeft <= originalImgMat.rows()) yTopLeft else originalImgMat.rows().toDouble()

                val width = if (xTopLeft + deltaX + extraWidth <= originalImgMat.cols()) deltaX + extraWidth
                            else originalImgMat.cols() - xTopLeft

                val height = if (yTopLeft + deltaY + extraHeight <= originalImgMat.rows()) deltaY + extraHeight
                             else originalImgMat.rows() - yTopLeft

                val roiRect = Rect(Point(xTopLeft, yTopLeft), Size(width, height))

                val roiNum = y * 8 + x
                val randomColor = Scalar(Random.nextDouble(0.0, 255.0), Random.nextDouble(0.0, 255.0), Random.nextDouble(0.0, 255.0))
                Imgproc.rectangle(imgMat, roiRect, randomColor, 3)

                val resizedRoi = Mat()
                Imgproc.resize(originalImgMat.submat(roiRect), resizedRoi, Size(256.0, 256.0))
                currRow.add(resizedRoi)
                Imgcodecs.imwrite("$roiDir/ROI_$roiNum.png", resizedRoi)
            }
            roiMats.add(currRow)
        }

        val getChosenPiece = { idx: Int ->
            when (idx) {
                0 -> Bishop(PieceColor.BLACK)
                1 -> King(PieceColor.BLACK)
                2 -> Knight(PieceColor.BLACK)
                3 -> Pawn(PieceColor.BLACK)
                4 -> Queen(PieceColor.BLACK)
                5 -> Rook(PieceColor.BLACK)

                7 -> Bishop(PieceColor.WHITE)
                8 -> King(PieceColor.WHITE)
                9 -> Knight(PieceColor.WHITE)
                10 -> Pawn(PieceColor.WHITE)
                11 -> Queen(PieceColor.WHITE)
                12 -> Rook(PieceColor.WHITE)

                else -> EmptyPiece()
            }
        }

        val model = Model.newInstance(context)
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val roiMat = roiMats[y][x]
                val bmp = Bitmap.createBitmap(roiMat.cols(), roiMat.rows(), Bitmap.Config.ARGB_8888)
                Imgproc.cvtColor(roiMat, roiMat, Imgproc.COLOR_RGB2BGR)
                Utils.matToBitmap(roiMat, bmp, true)
                val tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(bmp)

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
                inputFeature0.loadBuffer(tensorImage.buffer)
                val outputs = model.process(inputFeature0).outputFeature0AsTensorBuffer.floatArray

                var chosenIdx = 0
                val logBuffer = StringBuffer(16)
                for (j in 1 until 13) {
                    logBuffer.append(outputs[j]).append(" ")
                    if (outputs[j] > outputs[chosenIdx]) chosenIdx = j
                }

                Log.d(TAG, "Output values ($y,$x): $logBuffer")
                pieceMatrix[y][x] = getChosenPiece(chosenIdx)
            }
        }

        model.close()

        Imgcodecs.imwrite("$imgDir/final.png", imgMat)

        val bmp = Bitmap.createBitmap(imgMat.cols(), imgMat.rows(), Bitmap.Config.ARGB_8888)
        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGB2BGR)
        Utils.matToBitmap(imgMat, bmp)
        bitmap = bmp
    }
}