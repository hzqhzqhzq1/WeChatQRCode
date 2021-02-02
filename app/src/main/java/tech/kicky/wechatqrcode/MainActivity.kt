package tech.kicky.wechatqrcode

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import org.opencv.android.*
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.wechat_qrcode.WeChatQRCode
import tech.kicky.wechatqrcode.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {


    private var mDetectorPrototxtFile: File? = null
    private var mDetectorCaffeModelFile: File? = null
    private var mSuperResolutionPrototxtFile: File? = null
    private var mSuperResolutionCaffeModelFile: File? = null

    private lateinit var mWeChatQRCode: WeChatQRCode

    private lateinit var mRgba: Mat
    private lateinit var mGray: Mat

    private val mBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var mOpenCvCameraView: CameraBridgeViewBase

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(App.TAG, "OpenCV loaded successfully")
                    initModelFile()
                    mOpenCvCameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    fun initModelFile() {
        try {
            // detect.prototxt
            val detectorIs = resources.openRawResource(R.raw.detect_prototxt)
            val qrcodeDir = getDir("qrcode", Context.MODE_PRIVATE)
            mDetectorPrototxtFile = File(qrcodeDir, "detect.prototxt")
            val os = FileOutputStream(mDetectorPrototxtFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (detectorIs.read(buffer).also { bytesRead = it } != -1) {
                os.write(buffer, 0, bytesRead)
            }
            detectorIs.close()
            os.close()

            // detect.caffemodel
            val detectorCaffeIs = resources.openRawResource(R.raw.detect_caffemodel)
            mDetectorCaffeModelFile = File(qrcodeDir, "detect.caffemodel")
            val ios = FileOutputStream(mDetectorCaffeModelFile)
            val ibuffer = ByteArray(4096)
            var ibytesRead: Int
            while (detectorCaffeIs.read(ibuffer).also { ibytesRead = it } != -1) {
                ios.write(ibuffer, 0, ibytesRead)
            }
            detectorCaffeIs.close()
            ios.close()

            // sr.prototxt
            val srPrototxtIs = resources.openRawResource(R.raw.sr_prototxt)
            mSuperResolutionPrototxtFile = File(qrcodeDir, "sr.prototxt")
            val jos = FileOutputStream(mSuperResolutionPrototxtFile)
            val jbuffer = ByteArray(4096)
            var jbytesRead: Int
            while (srPrototxtIs.read(jbuffer).also { jbytesRead = it } != -1) {
                jos.write(jbuffer, 0, jbytesRead)
            }
            srPrototxtIs.close()
            jos.close()

            // sr.caffemodel
            val srCaffeIs = resources.openRawResource(R.raw.sr_caffemodel)
            mSuperResolutionCaffeModelFile = File(qrcodeDir, "sr.caffemodel")
            val kos = FileOutputStream(mSuperResolutionCaffeModelFile)
            val kbuffer = ByteArray(4096)
            var kbytesRead: Int
            while (srCaffeIs.read(kbuffer).also { kbytesRead = it } != -1) {
                kos.write(kbuffer, 0, kbytesRead)
            }
            srCaffeIs.close()
            kos.close()

            mWeChatQRCode = WeChatQRCode(
                mDetectorPrototxtFile?.absolutePath,
                mDetectorCaffeModelFile?.absolutePath,
                mSuperResolutionPrototxtFile?.absolutePath,
                mSuperResolutionCaffeModelFile?.absolutePath
            )

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(
                App.TAG,
                "Failed to load cascade. Exception thrown: $e"
            )
        }
    }

    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(App.TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(mBinding.root)
        mOpenCvCameraView = mBinding.preview
        mOpenCvCameraView.setCvCameraViewListener(this)
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                App.TAG,
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            )
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(App.TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase?> {
        return listOf(mOpenCvCameraView)
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat()
        mGray = Mat()
    }

    override fun onCameraViewStopped() {
        mGray.release()
        mRgba.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()

        val rectangles = ArrayList<Mat>()
        val results = mWeChatQRCode.detectAndDecode(mGray, rectangles)
        println(results)

        for ((index, result) in results.withIndex()) {
            val points = rectangles[index]
            val pointArr = FloatArray(8)
            points.get(0, 0, pointArr)
            var pt1 = Point(pointArr[0].toDouble(), pointArr[1].toDouble()  - 100)
            for (i in pointArr.indices step 2) {
                val start =
                    Point(pointArr[i % 8].toDouble(), pointArr[(i + 1) % 8].toDouble())
                val end = Point(pointArr[(i + 2) % 8].toDouble(), pointArr[(i + 3) % 8].toDouble())
                Imgproc.line(mRgba, start, end, Scalar(255.0, 0.0, 0.0), 8, Imgproc.LINE_8)
            }
            Imgproc.putText(mRgba, result, pt1, 0, 1.0, Scalar(255.0, 0.0, 0.0), 2);
        }
        return mRgba
    }
}