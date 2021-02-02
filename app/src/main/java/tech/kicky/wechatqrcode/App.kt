package tech.kicky.wechatqrcode

import android.app.Application

/**
 * Main App
 * author: yidong
 * 2021/2/2
 */
class App : Application() {
    companion object {
        const val TAG = "WeChat_QRCode"

        init {
            System.loadLibrary("opencv_java4")
        }
    }
}