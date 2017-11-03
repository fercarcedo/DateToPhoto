package fergaral.datetophoto.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.SparseArray
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

import fergaral.datetophoto.R
import fergaral.datetophoto.utils.Point
import fergaral.datetophoto.utils.RectImageView
import fergaral.datetophoto.utils.Utils

class DetectDateActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_date)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (supportActionBar != null)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val selectPhotoBtn = findViewById<Button>(R.id.select_photo_btn)

        selectPhotoBtn?.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT

            startActivityForResult(Intent.createChooser(intent, "Select photo"), SELECT_IMAGE_REQUEST)
        }

        val visionAPIBtn = findViewById<Button>(R.id.vision_btn)

        visionAPIBtn?.setOnClickListener { runVisionAPI() }

        val colorBtn = findViewById<Button>(R.id.color_btn)

        colorBtn?.setOnClickListener { runColor() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null
                && data.data != null) {
            val uri = data.data
            var printWriter: PrintWriter? = null

            try {
                val startTimeDecodeBitmap = System.currentTimeMillis()

                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                val endTimeDecodeBitmap = System.currentTimeMillis()

                val startTimeRecognition = System.currentTimeMillis()

                val textRecognizer = TextRecognizer.Builder(applicationContext).build()

                if (textRecognizer.isOperational) {
                    Toast.makeText(this, "Text recognizer operational", Toast.LENGTH_SHORT).show()

                    val frame = Frame.Builder()
                            .setBitmap(bitmap)
                            .build()

                    val textBlocks = textRecognizer.detect(frame)

                    val endTimeRecognition = System.currentTimeMillis()

                    val downloadFile = File(Environment.getExternalStorageDirectory().path
                            + File.separator + "Download")

                    if (!downloadFile.exists())
                        downloadFile.mkdirs()

                    printWriter = PrintWriter(FileWriter(
                            downloadFile.absolutePath + File.separator + "dtpdetections.txt"
                    ))

                    val selectedPhotoIv = findViewById<View>(R.id.selected_photo_iv) as RectImageView

                    selectedPhotoIv.setImageBitmap(bitmap)

                    Toast.makeText(this, textBlocks.size().toString(), Toast.LENGTH_SHORT).show()

                    for (i in 0 until textBlocks.size()) {
                        val boundingBox = textBlocks.valueAt(i).boundingBox

                        selectedPhotoIv.addBoundingBox(boundingBox)

                        printWriter.println("Top left: (" + boundingBox.left + "," + boundingBox.top + ")")
                        printWriter.println("Top right: " + boundingBox.right + "," + boundingBox.top + ")")
                        printWriter.println("Bottom left: " + boundingBox.left + "," + boundingBox.bottom + ")")
                        printWriter.println("Bottom right: " + boundingBox.right + "," + boundingBox.bottom + ")")
                        printWriter.print("Image dimensions: ")
                        printWriter.print(bitmap.width)
                        printWriter.print(" x ")
                        printWriter.println(bitmap.height)

                        val bottomRightPoint = Point(boundingBox.right, boundingBox.bottom)

                        if (bottomRightPoint.isInBottomRightCorner(bitmap.width, bitmap.height))
                            printWriter.println("Datestamped")
                        else
                            printWriter.println("Not datestamped")

                        printWriter.print("Decode bitmap time: ")
                        printWriter.println(endTimeDecodeBitmap - startTimeDecodeBitmap)
                        printWriter.print("Recognition time: ")
                        printWriter.println(endTimeRecognition - startTimeRecognition)
                        printWriter.println()
                    }
                } else {
                    Toast.makeText(this, "Text recognizer not operational", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (printWriter != null)
                    printWriter.close()
            }
        }
    }

    private fun runVisionAPI() {
        Utils.testVisionDatestampDetectionAlgorithm(this)
    }

    private fun runColor() {
        Utils.testColorDatestampDetectionAlgorithm(this)
    }

    companion object {

        private val SELECT_IMAGE_REQUEST = 1
    }
}
