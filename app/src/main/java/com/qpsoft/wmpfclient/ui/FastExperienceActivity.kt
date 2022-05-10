package com.qpsoft.wmpfclient.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.qpsoft.wmpfclient.Api
import com.qpsoft.wmpfclient.BuildConfig
import com.qpsoft.wmpfclient.R
import java.io.File


class FastExperienceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fast_experience)

        if (!checkPermission(this)) {
            requestPermission(this)
        }

        ToastUtils.showShort("正在加载运行环境，请稍后...")
    }


    override fun onResume() {
        super.onResume()
        if (AppUtils.isAppInstalled("com.tencent.wmpf")) {
            readyData()
            return
        }
        installWMPFService()
    }

    private fun installWMPFService() {
        val file: File? = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val destFilePath = file?.absolutePath + File.separator + "wmpf_service.apk"
        if (!FileUtils.isFileExists(destFilePath)) {
            val success = ResourceUtils.copyFileFromAssets("wmpfservice/wmpf-arm-production-release-v1.1.0-652-signed.apk", destFilePath)
            LogUtils.e("-------$success")
        }
        AppUtils.installApp(destFilePath)
    }

    private fun readyData() {
        OkGo.get<String>("https://wmpfapi.qingpai365.com/getDeviceInfo")
            .tag(this)
            .params("sn", Build.SERIAL)
            .execute(object: StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    val jsonBody = JSON.parseObject(response.body())
                    if (jsonBody.getIntValue("code") == 0) {
                        val jsonData = jsonBody.getJSONObject("data");
                        val hostAppId = jsonData.getString("host_appid");
                        val productId = jsonData.getIntValue("productId");
                        val keyVersion = jsonData.getIntValue("keyVersion");
                        val deviceId = jsonData.getString("deviceId");
                        val signature = jsonData.getString("signature");
                        launchWxa(productId, keyVersion, deviceId, signature, hostAppId)
                        return
                    }
                    ToastUtils.showShort("启动参数获取失败，请联系管理员")
                }

            })
    }

    private fun launchWxa(productId: Int, keyVersion: Int,
                          deviceId: String, signature: String, hostAppId: String) {
        Api.activateDevice(productId, keyVersion, deviceId, signature, hostAppId)
            .subscribe({
                Log.e(TAG, "success: $it")
                if (it.invokeToken == null) {
                    LogUtils.e("activate device fail for a null token, may ticket is expired")
                } else {
                    val invokeToken = it.invokeToken
                    LogUtils.e("-------$invokeToken")
                    Api.launchWxaApp(optLaunchAppId(), "").subscribe({
                        Log.i(TAG, "success: ${it.baseResponse.errCode} ${it.baseResponse.errMsg}")
                        finish()
                    }, {
                        ToastUtils.showShort("error: $it")
                        Log.e(TAG, "error: $it")
                    })
                }
            }, {
                Log.e(TAG, "error: $it")
            })
    }

    private fun optLaunchAppId(): String {
        var launchAppId: String? = null
        if (launchAppId == null || launchAppId.isEmpty()) {
            launchAppId = "wx2b1766bca6959dfa"
        }
        return BuildConfig.APP_ID
    }


    private fun checkPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val ret0 = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            val ret1 = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val ret2 = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            val ret3 = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            val ret4 = context.checkSelfPermission(Manifest.permission.CAMERA)
            val ret5 = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
            return ret0 == PackageManager.PERMISSION_GRANTED &&
                    ret1 == PackageManager.PERMISSION_GRANTED &&
                    ret2 == PackageManager.PERMISSION_GRANTED &&
                    ret3 == PackageManager.PERMISSION_GRANTED &&
                    ret4 == PackageManager.PERMISSION_GRANTED &&
                    ret5 == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    private fun requestPermission(context: Activity) {
        try {
            ActivityCompat.requestPermissions(context, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE),
                0)
        } catch (e: Exception) {

        }

    }

    companion object {
        private const val TAG = "FastExperienceActivity"
    }
}
