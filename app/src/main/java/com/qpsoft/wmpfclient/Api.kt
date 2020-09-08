package com.qpsoft.wmpfclient

import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.tencent.mm.ipcinvoker.IPCInvokeCallbackEx
import com.tencent.wmpf.cli.task.*
import com.tencent.wmpf.cli.task.pb.WMPFBaseRequestHelper
import com.tencent.wmpf.cli.task.pb.WMPFIPCInvoker
import com.tencent.wmpf.proto.*
import io.reactivex.Single
import java.lang.Exception

object Api {

    private const val TAG = "Demo.Api"

    fun activateDevice(productId: Int, keyVerion: Int,
                       deviceId: String, signature: String, hostAppId: String): Single<WMPFActivateDeviceResponse> {
        return Single.create {
            Log.i(TAG, "activateDevice: isInProductionEnv = " + true)
            val request = WMPFActivateDeviceRequest().apply {
                this.baseRequest = WMPFBaseRequestHelper.checked()
                this.productId = productId
                this.keyVersion = keyVerion
                this.deviceId = deviceId
                this.signature = signature.replace(Regex("[\t\r\n]"), "")
                this.hostAppId = hostAppId
            }

            val result = WMPFIPCInvoker.invokeAsync<IPCInvokerTask_ActivateDevice, WMPFActivateDeviceRequest, WMPFActivateDeviceResponse>(
                    request,
                    IPCInvokerTask_ActivateDevice::class.java,
                    object : IPCInvokeCallbackEx<WMPFActivateDeviceResponse> {
                        override fun onBridgeNotFound() {
                            it.onError(Exception("bridge not found"))
                        }

                        override fun onCallback(response: WMPFActivateDeviceResponse) {
                            it.onSuccess(response)
                        }
                    })

            if (!result) {
                it.onError(Exception("invoke activateDevice fail"))
            }
        }
    }


    fun launchWxaApp(launchAppId: String, path: String, appType: Int = 0, landsapeMode: Int = 0): Single<WMPFLaunchWxaAppResponse> {
        return Single.create {
            val request = WMPFLaunchWxaAppRequest()
            request.baseRequest = WMPFBaseRequestHelper.checked()
            // Launch target(wxa appId)
            // WARNING: hostAppIds and wxaAppIds are binded sets.
            request.appId = launchAppId // Binded with HOST_APPID: wx64b7714cf1f64585
            request.path = path
            request.appType = appType // 0-正式版 1-开发版 2-体验版
            // mayRunInLandscapeCompatMode Deprecated
//            request.mayRunInLandscapeCompatMode = true
            request.forceRequestFullscreen = false
            request.landscapeMode = landsapeMode // 0:和微信行为保持一致;1:允许横屏铺满显示，忽略小程序的pageOrientation配置;2:强制横屏并居中以16:9显示，忽略pageOrientation配置
            //Log.i(TAG, "launchWxaApp: appId = " + launchAppId + ", hostAppID = " + BuildConfig.HOST_APPID + ", deviceId = " + DeviceInfo.deviceId)
            val result = WMPFIPCInvoker.invokeAsync<IPCInvokerTask_LaunchWxaApp, WMPFLaunchWxaAppRequest,
                    WMPFLaunchWxaAppResponse>(
                    request,
                    IPCInvokerTask_LaunchWxaApp::class.java
            ) { response -> it.onSuccess(response) }

            if (!result) {
                it.onError(Exception("invoke launchWxaApp fail"))
            }
        }
    }
}