package com.qpsoft.wmpfclient

import android.app.Application
import android.util.Log
import com.tencent.mm.ipcinvoker.IPCInvokeCallbackEx
import com.tencent.wmpf.app.WMPFBoot
import com.tencent.wmpf.cli.task.IPCInvokerTask_ActivateDevice
import com.tencent.wmpf.cli.task.IPCInvokerTask_LaunchWxaApp
import com.tencent.wmpf.cli.task.TaskError
import com.tencent.wmpf.cli.task.pb.WMPFBaseRequestHelper
import com.tencent.wmpf.cli.task.pb.WMPFIPCInvoker
import com.tencent.wmpf.cli.task.pb.proto.WMPFResponse
import com.tencent.wmpf.proto.WMPFActivateDeviceRequest
import com.tencent.wmpf.proto.WMPFActivateDeviceResponse
import com.tencent.wmpf.proto.WMPFLaunchWxaAppRequest
import com.tencent.wmpf.proto.WMPFLaunchWxaAppResponse
import io.reactivex.Single
import java.lang.Exception

object Api {

    private const val TAG = "Demo.Api"

    private fun isSuccess(response: WMPFResponse): Boolean {
        return response != null && response.baseResponse.errCode == TaskError.ErrType_OK
    }

    fun init(context: Application) {
        WMPFBoot.init(context)
        val invokeToken = getInvokeToken()
        WMPFIPCInvoker.initInvokeToken(invokeToken)
    }

    private fun getInvokeToken(): String {
        if (WMPFBoot.getAppContext() == null) {
            throw java.lang.Exception("need invoke Api.Init")
        }
        val pref = WMPFBoot.getAppContext()!!.getSharedPreferences("InvokeTokenHelper", 0)
        return pref?.getString(TAG, "")!!
    }

    private fun initInvokeToken(invokeToken: String) {
        if (WMPFBoot.getAppContext() == null) {
            throw java.lang.Exception("need invoke Api.Init")
        }
        val pref = WMPFBoot.getAppContext()!!.getSharedPreferences("InvokeTokenHelper", 0)
        val editor = pref?.edit()
        editor?.putString(TAG, invokeToken)?.apply()
        WMPFIPCInvoker.initInvokeToken(invokeToken)
    }

    private fun createTaskError(response: WMPFResponse?): TaskError {
        if (response == null) {
            return TaskError(TaskError.ErrType_NORMAL, -1, "response is null")
        }
        return TaskError(response.baseResponse.errType, response.baseResponse.errCode, response.baseResponse.errMsg)
    }

    class TaskErrorException(val taskError: TaskError): java.lang.Exception() {
        override fun toString(): String {
            return "TaskErrorException(taskError=$taskError)"
        }
    }

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
                            if (isSuccess(response)) {
                                if (response != null && !response.invokeToken.isNullOrEmpty()) {
                                    initInvokeToken(response.invokeToken)
                                }

                                it.onSuccess(response)
                            } else {
                                it.onError(TaskErrorException(createTaskError(response)))
                            }
                        }

                        override fun onCaughtInvokeException(exception: java.lang.Exception?) {
                            if (exception != null) {
                                it.onError(exception)
                            } else {
                                it.onError(java.lang.Exception("null"))
                            }
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