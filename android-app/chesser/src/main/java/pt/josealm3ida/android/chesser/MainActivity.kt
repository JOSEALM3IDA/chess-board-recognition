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
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File


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
        val imgFile = File(imgDir, "original.png");
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

        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGB2GRAY)
        Imgproc.medianBlur(imgMat, imgMat, 5)
        Imgcodecs.imwrite("$imgDir/blur.png", imgMat)

        val sharpenKernelMat = Mat(3, 3, CvType.CV_32F)

        for (i in 0..3) sharpenKernelMat.put(0, i, -1.0)
        sharpenKernelMat.put(1, 0, -1.0);
        sharpenKernelMat.put(1, 1, 9.0);
        sharpenKernelMat.put(1, 2, -1.0);
        for (i in 0..3) sharpenKernelMat.put(2, i, -1.0);

        Imgproc.filter2D(imgMat, imgMat, -1, sharpenKernelMat)
        Imgcodecs.imwrite("$imgDir/sharp.png", imgMat)

        Imgproc.threshold(imgMat, imgMat, 160.0, 255.0, Imgproc.THRESH_BINARY_INV)
        Imgcodecs.imwrite("$imgDir/threshold.png", imgMat)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.morphologyEx(imgMat, imgMat, Imgproc.MORPH_CLOSE, kernel)
        Imgcodecs.imwrite("$imgDir/close.png", imgMat)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(imgMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val minArea = 100
        val maxArea = 1500
        var imgNum = 0
        val roiDir = context.filesDir.absolutePath + "/" + Constants.ROI_DIR
        File(roiDir).mkdirs()
        for (i in 0 until contours.size) {
            val curr = contours[i]
            val area = Imgproc.contourArea(curr)
            if (area <= minArea || area >= maxArea) continue
            val rect = Imgproc.boundingRect(curr)
            Imgcodecs.imwrite("$roiDir/ROI_$imgNum.png", originalImgMat.submat(rect))
            imgNum++
        }
    }
}