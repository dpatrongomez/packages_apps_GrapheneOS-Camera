package app.grapheneos.camera.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.EXTRA_OUTPUT
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import app.grapheneos.camera.R
import java.nio.ByteBuffer
import android.graphics.Bitmap.CompressFormat
import android.widget.ImageButton
import android.widget.ImageView
import java.io.ByteArrayOutputStream

class CaptureActivity: MainActivity() {

    lateinit var outputUri: Uri
    lateinit var bitmap: Bitmap

    lateinit var retakeIcon: ImageView

    lateinit var flipCameraContent: ImageView
    lateinit var confirmButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retakeIcon = findViewById(R.id.retake_icon)
        flipCameraContent = findViewById(R.id.flip_camera_icon_content)

        confirmButton = findViewById(R.id.confirm_button)

        if (intent.extras?.containsKey(EXTRA_OUTPUT)==true) {
            outputUri = intent.extras?.get(EXTRA_OUTPUT) as Uri
        }

        // Disable capture button for a while (to avoid picture capture)
        captureButton.isEnabled = false
        captureButton.alpha = 0f

        // Enable the capture button after a while
        Handler(Looper.getMainLooper()).postDelayed({

            captureButton.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction {
                    captureButton.isEnabled = true
                }

        }, 2000)

        // Remove the modes tab layout as we do not want the user to be able to switch to
        // another custom mode in this state
        tabLayout.visibility = View.INVISIBLE

        // Remove the margin so that that the previewView can take some more space
        (previewView.layoutParams as MarginLayoutParams).let {
            it.setMargins(it.leftMargin, it.topMargin, it.rightMargin, 0)
        }

        // Bring the three buttons a bit down in the UI
        (threeButtons.layoutParams as MarginLayoutParams).let {
            it.setMargins(it.leftMargin, it.topMargin, it.rightMargin, 0)
        }

        // Change the drawable to cancel mode
        captureModeView.setImageResource(R.drawable.cancel)

        // Overwrite the existing listener to just close the existing activity
        // (in this case)
        captureModeView.setOnClickListener {
            finish()
        }

        // Remove the third option/circle from the UI
        thirdOption.visibility = View.INVISIBLE

        captureButton.setOnClickListener {
            takePicture()
        }

        retakeIcon.setOnClickListener {
            hidePreview()
        }

        confirmButton.setOnClickListener {
            confirmImage()
        }

        // Display the activity
    }

    private fun takePicture(){

        previewLoader.visibility = View.VISIBLE
        config.imageCapture?.takePicture(
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    bitmap = imageProxyToBitmap(image)
                    showPreview()
                    previewLoader.visibility = View.GONE

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    exception.printStackTrace()
                    Toast.makeText(this@CaptureActivity,
                        "Unable to capture image", Toast.LENGTH_LONG)
                        .show()

                    finishActivity(RESULT_CANCELED)
                }
            }

        )
    }

    private fun showPreview(){

        config.cameraProvider?.unbindAll()

        mainOverlay.setImageBitmap(bitmap)
        mainOverlay.visibility = View.VISIBLE

        settingsIcon.visibility = View.INVISIBLE
        flashPager.visibility = View.INVISIBLE

        flipCameraContent.visibility = View.INVISIBLE
        retakeIcon.visibility = View.VISIBLE

        captureButton.visibility = View.INVISIBLE
        confirmButton.visibility = View.VISIBLE

        previewView.visibility = View.INVISIBLE
    }

    private fun hidePreview(){
        config.startCamera(true)

        settingsIcon.visibility = View.VISIBLE
        flashPager.visibility = View.VISIBLE

        flipCameraContent.visibility = View.VISIBLE
        retakeIcon.visibility = View.INVISIBLE

        captureButton.visibility = View.VISIBLE
        confirmButton.visibility = View.INVISIBLE

        previewView.visibility = View.VISIBLE
    }

    private fun confirmImage() {

        val resultIntent = Intent("inline-data")

        if(::outputUri.isInitialized){

            val bos = ByteArrayOutputStream()

            val cf: CompressFormat =
                if(outputUri.path?.endsWith(".png")==true) {
                    CompressFormat.PNG
                } else {
                    CompressFormat.JPEG
                }

            bitmap.compress(cf, 100, bos)
            val bitmapData: ByteArray = bos.toByteArray()

            val oStream =
                contentResolver.openOutputStream(outputUri)

            if(oStream!=null){
                oStream.write(bitmapData)
                oStream.close()

                setResult(RESULT_OK)
            } else {
                setResult(RESULT_CANCELED)
            }

        } else {
            bitmap = resizeImage(bitmap)
            resultIntent.putExtra("data", bitmap)
            setResult(RESULT_OK, resultIntent)
        }

        finish()
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun resizeImage(image: Bitmap): Bitmap {

        val width = image.width
        val height = image.height

        val scaleWidth = width / 10
        val scaleHeight = height / 10

        if (image.byteCount <= 1000000)
            return image

        return Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false)
    }
}