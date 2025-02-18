package com.atomicfi.transactreactnative

import android.content.Context
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import financial.atomic.transact.Config
import financial.atomic.transact.ActionConfig
import financial.atomic.transact.Transact
import financial.atomic.transact.receiver.TransactBroadcastReceiver
import org.json.JSONObject
import java.lang.Exception

class TransactReactNativeModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val _reactContext = reactContext

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): MutableMap<String, Any>? {
    val constants = mutableMapOf<String, Any>()

    constants["VERSION"] = BuildConfig.TRANSACT_VERSION

    return constants
  }

  @ReactMethod
  fun presentTransact(token: String, environment: String, promise: Promise) {
    val context = currentActivity as Context
    val emitter = _reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
    val config = Config(token = token, environment = "CUSTOM", environmentURL = environment)

    try {
      Transact.present(context, config, object : TransactBroadcastReceiver() {
        override fun onClose(data: JSONObject) {
          val reason = data.optString("reason")
          val result = Arguments.createMap().apply {
            putString("reason", reason)
          }

          Transact.unregisterReceiver(context, this)
          emitter.emit("onClose", data.toString())
          promise.resolve(result)
        }

        override fun onFinish(data: JSONObject) {
          val taskId = data.optString("taskId")
          val result = Arguments.createMap().apply {
            putString("taskId", taskId)
          }

          Transact.unregisterReceiver(context, this)
          emitter.emit("onFinish", data.toString())
          promise.resolve(result)
        }

        override fun onInteraction(data: JSONObject) {
          emitter.emit("onInteraction", data.toString())
        }

        override fun onDataRequest(data: JSONObject) {
          emitter.emit("onDataRequest", data.toString())
        }
      })
    } catch (e: Exception) {
      promise.reject(e)
    }
  }

  @ReactMethod
  fun presentAction(id: String, environment: String, promise: Promise) {
    val context = currentActivity as Context
    val emitter = _reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)

    try {
      val config = ActionConfig(
        id = id,
        environment = Config.Environment.CUSTOM,
        environmentURL = environment
      )

      Transact.presentAction(context, config)

      // Register broadcast receiver after presenting action
      val receiver = object : TransactBroadcastReceiver() {
        override fun onClose(data: JSONObject) {
          val result = Arguments.createMap()
          val closedData = Arguments.createMap()
          val iterator = data.keys()
          while (iterator.hasNext()) {
            val key = iterator.next()
            closedData.putString(key, data.getString(key))
          }
          result.putMap("closed", closedData)

          Transact.unregisterReceiver(context, this)
          emitter.emit("onClose", data.toString())
          promise.resolve(result)
        }

        override fun onFinish(data: JSONObject) {
          val result = Arguments.createMap()
          val finishedData = Arguments.createMap()
          val iterator = data.keys()
          while (iterator.hasNext()) {
            val key = iterator.next()
            finishedData.putString(key, data.getString(key))
          }
          result.putMap("finished", finishedData)

          Transact.unregisterReceiver(context, this)
          emitter.emit("onFinish", data.toString())
          promise.resolve(result)
        }

        override fun onLaunch() {
          emitter.emit("onLaunch", null)
        }
      }

      Transact.registerReceiver(context, receiver)
    } catch (e: Exception) {
      promise.reject(e)
    }
  }

  companion object {
    const val NAME = "TransactReactNative"
  }
}
