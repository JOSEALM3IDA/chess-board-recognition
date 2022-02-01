package pt.josealm3ida.android.chesser

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.glide.GlideImage
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import org.opencv.core.Scalar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.opencv.core.Mat
import kotlin.random.Random


class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private lateinit var context: Context
    private val imageUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initDebug()

        setContent {
            context = LocalContext.current;

            Column (
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
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
                                        .size(40.dp)
                                )
                            }
                        }
                    }
                }

                ButtonComposable()
                UserImage()
            }
        }
    }

    @Composable
    fun ButtonComposable() {
        MaterialTheme {
            Button(
                onClick = {
                    selectImageLauncher.launch("image/*")
                },
                // Uses ButtonDefaults.ContentPadding by default
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 12.dp
                )
            ) {
                // Inner content including an icon and a text label
                Icon(
                    Icons.Filled.Create,
                    contentDescription = "Create",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Open Gallery")
            }
        }
    }
    
    @Composable
    fun UserImage() {
        if (imageUriState.value == null) return
        GlideImage(imageModel = imageUriState.value)
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        //imageUriState.value = uri
        val imgDir = context.filesDir.absolutePath + "/" + Constants.IMAGE_DIR
        val imgFile = File(imgDir, "original.png")
        imgFile.parentFile?.mkdirs()
        imgFile.createNewFile()

        val imgInputStream = contentResolver.openInputStream(uri)
        imgInputStream.use { input ->
            imgFile.outputStream().use { output ->
                input?.copyTo(output)
            }
        }

        val originalImgMat = Imgcodecs.imread(imgFile.absolutePath)
        val imgMat = Mat()
        originalImgMat.copyTo(imgMat)

        //val bilateralFilteredImg = Mat()
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


        /*
        val hLines = mutableListOf<DoubleArray?>()
        val vLines = mutableListOf<DoubleArray?>()
        for (x in 0 until houghMat.rows()) {
            val row = houghMat.get(x, 0)
            val theta = row[1]

            if (theta < Math.PI / 4 || theta > Math.PI - Math.PI / 4)
                vLines.add(row)
            else
                hLines.add(row)
        }*/

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

            Log.d(TAG, "$avgX $avgY")

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
                0 -> -60
                1 -> -50
                2 -> -45
                3 -> -35
                4 -> -35
                5 -> -35
                6 -> -35
                else -> -35
            }
        }

        originalImgMat.copyTo(imgMat)
        val roiDir = context.filesDir.absolutePath + "/" + Constants.ROI_DIR
        File(roiDir).mkdirs()
        for (x in 0 until 8) {
            for (y in 0 until 8) {
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

                Log.d(TAG, "x: $xTopLeft y: $yTopLeft width: $width height: $height")

                val roiNum = y * 8 + x
                val randomColor = Scalar(Random.nextDouble(0.0, 255.0), Random.nextDouble(0.0, 255.0), Random.nextDouble(0.0, 255.0))
                Imgproc.rectangle(imgMat, roiRect, randomColor, 3)
                Imgcodecs.imwrite("$roiDir/ROI_$roiNum.png", originalImgMat.submat(roiRect))
            }
        }

        Imgcodecs.imwrite("$imgDir/final.png", imgMat)

        /*
       val dst = Mat.zeros(imgMat.size(), CvType.CV_32F);
       Imgproc.cornerHarris(imgMat, dst, 2, 3, 0.04)

       val dstNorm = Mat()
       val dstNormScaled = Mat()

       Core.normalize(dst, dstNorm, 0.0, 255.0, Core.NORM_MINMAX);
       Core.convertScaleAbs(dstNorm, dstNormScaled);

       val dstNormData = FloatArray((dstNorm.total() * dstNorm.channels()).toInt())
       dstNorm[0, 0, dstNormData]

       Imgcodecs.imwrite("$imgDir/dstNormScaled.png", dstNormScaled)


       for (i in 0 until dstNorm.rows()) {
           for (j in 0 until dstNorm.cols()) {
               if (dstNormData[i * dstNorm.cols() + j].toInt() > 200) {
                   Imgproc.circle(
                       dstNormScaled,
                       Point(j.toDouble(), i.toDouble()),
                       5,
                       Scalar(0.0),
                       2,
                       8,
                       0
                   )
               }
           }
       }



        //Imgproc.threshold(dstNormScaled, dstNormScaled, 70.0, 255.0, THRESH_BINARY)
        //dstNormScaled.convertTo(dstNormScaled, -1, 150.0, 0.0)
        //Imgproc.morphologyEx(dstNormScaled, dstNormScaled, MORPH_DILATE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0 * 2 + 1,2.0 * 2 + 1)))
        val sharpenKernelMat = Mat(3, 3, CvType.CV_32F)

        for (i in 0..3) sharpenKernelMat.put(0, i, -1.0)
        sharpenKernelMat.put(1, 0, -1.0);
        sharpenKernelMat.put(1, 1, 9.0);
        sharpenKernelMat.put(1, 2, -1.0);
        for (i in 0..3) sharpenKernelMat.put(2, i, -1.0);

        val sharpened = Mat()
        Imgproc.filter2D(dstNormScaled, sharpened, -1, sharpenKernelMat)

        Imgproc.threshold(sharpened, sharpened, 60.0, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C)

        Imgcodecs.imwrite("$imgDir/sharp.png", sharpened)

        val corners = Mat()
        Imgproc.cornerSubPix(dstNormScaled, corners, Size(5.0,5.0), Size(-1.0, -1.0), TermCriteria(EPS + COUNT, 100, 0.001))


        val result = Mat()
        originalImgMat.copyTo(result)

        val lines = Mat()
        Imgproc.HoughLinesP(sharpened, lines, 5.0, 4.0, 7)
        for (i in 0 until lines.cols()) {
            val `val` = lines[0, i]
            Imgproc.line(
                result,
                Point(`val`[0], `val`[1]),
                Point(`val`[2], `val`[3]),
                Scalar(0.0, 0.0, 255.0),
                2
            )
        }

        //Imgcodecs.imwrite("$imgDir/lines.png", result)
        //Imgproc.bilateralFilter(imgMat, bilateralFilteredImg, 5, 200.0, 200.0)

        //Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGB2GRAY)
        //Imgproc.medianBlur(imgMat, imgMat, 5)
        //Imgcodecs.imwrite("$imgDir/blur.png", imgMat)
        //Imgcodecs.imwrite("$imgDir/bilateral.png", bilateralFilteredImg)

        /*
        val patternSize = Size(7.0, 7.0)

        val corners = MatOfPoint2f();
        val foundChessboard = Calib3d.findChessboardCorners(bilateralFilteredImg, patternSize, corners, CALIB_CB_ADAPTIVE_THRESH)
        Log.d(TAG, "Detected chessboard: $foundChessboard")
        if (!foundChessboard) return@registerForActivityResult

        Log.d(TAG, "Corners: $corners");

        Calib3d.drawChessboardCorners(bilateralFilteredImg, patternSize, corners, foundChessboard)
        */
        //Imgcodecs.imwrite("$imgDir/corners.png", harrisMat)
        */
    }
}