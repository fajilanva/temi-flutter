package tech.blockmanic.flutter_temi

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class FlutterTemiPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {

    private lateinit var applicationContext: Context
    private lateinit var activity: Activity

    private val robot: Robot = Robot.getInstance()
    private val goToLocationStatusChangedImpl = GoToLocationStatusChangedImpl()
    private val onBeWithMeStatusChangedImpl = OnBeWithMeStatusChangedImpl()
    private val onLocationsUpdatedImpl = OnLocationsUpdatedImpl()
    private val nlpImpl = NlpImpl()
    private val ttsListenerImpl = TtsListenerImpl()
    private val asrListenerImpl = ASRListenerImpl()
    private val wakeupWordListenerImpl = WakeupWordListenerImpl()
    private val onConstraintBeWithStatusListenerImpl = OnConstraintBeWithStatusListenerImpl()
    private val onPrivacyModeChangedListenerImpl = OnPrivacyModeChangedListenerImpl()
    private val onBatteryStatusChangedListenerImpl = OnBatteryStatusChangedListenerImpl()
    private val onDetectionStateChangedListenerImpl = OnDetectionStateChangedListenerImpl()
    private val onRobotReadyListenerImpl = OnRobotReadyListenerImpl()

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, "flutter_temi")
        channel.setMethodCallHandler(this)

        applicationContext = binding.applicationContext

        setupEventChannels(binding)
    }

    private fun setupEventChannels(binding: FlutterPlugin.FlutterPluginBinding) {
        val eventChannels = mapOf(
            OnBeWithMeStatusChangedImpl.STREAM_CHANNEL_NAME to onBeWithMeStatusChangedImpl,
            GoToLocationStatusChangedImpl.STREAM_CHANNEL_NAME to goToLocationStatusChangedImpl,
            OnLocationsUpdatedImpl.STREAM_CHANNEL_NAME to onLocationsUpdatedImpl,
            NlpImpl.STREAM_CHANNEL_NAME to nlpImpl,
            TtsListenerImpl.STREAM_CHANNEL_NAME to ttsListenerImpl,
            ASRListenerImpl.STREAM_CHANNEL_NAME to asrListenerImpl,
            WakeupWordListenerImpl.STREAM_CHANNEL_NAME to wakeupWordListenerImpl,
            OnConstraintBeWithStatusListenerImpl.STREAM_CHANNEL_NAME to onConstraintBeWithStatusListenerImpl,
            OnPrivacyModeChangedListenerImpl.STREAM_CHANNEL_NAME to onPrivacyModeChangedListenerImpl,
            OnBatteryStatusChangedListenerImpl.STREAM_CHANNEL_NAME to onBatteryStatusChangedListenerImpl,
            OnDetectionStateChangedListenerImpl.STREAM_CHANNEL_NAME to onDetectionStateChangedListenerImpl,
            OnRobotReadyListenerImpl.STREAM_CHANNEL_NAME to onRobotReadyListenerImpl
        )

        eventChannels.forEach { (name, handler) ->
            val eventChannel = EventChannel(binding.binaryMessenger, name)
            eventChannel.setStreamHandler(handler)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "temi_serial_number" -> result.success(robot.serialNumber)
            "temi_privacy_mode" -> result.success(robot.privacyMode)
            "temi_set_privacy_mode" -> {
                val privacyMode = call.arguments<Boolean>()
                if (privacyMode != null) {
                    robot.privacyMode = privacyMode
                }
                result.success(privacyMode)
            }
            "temi_battery_data" -> result.success(OnBatteryStatusChangedListenerImpl.batteryToMap(robot.batteryData!!))
            "temi_show_top_bar" -> {
                robot.showTopBar()
                result.success(true)
            }
            "temi_hide_top_bar" -> {
                robot.hideTopBar()
                result.success(true)
            }
            "temi_speak" -> {
                val speech = call.arguments<String>()
                val request = speech?.let { TtsRequest.create(it, true) }
                if (request != null) {
                    robot.speak(request)
                }
                result.success(true)
            }
            "temi_speak_force" -> {
                val speech = call.arguments<String>()
                val request = speech?.let { TtsRequest.create(it, false) }
                if (request != null) {
                    robot.speak(request)
                }
                result.success(true)
            }
            "temi_finishe_conversation" -> {
                robot.finishConversation()
                result.success(true)
            }
            "temi_goto" -> {
                val location = call.arguments<String>()
                if (location != null) {
                    robot.goTo(location)
                }
                result.success(true)
            }
            "temi_save_location" -> {
                val location = call.arguments<String>()
                result.success(location?.let { robot.saveLocation(it) })
            }
            "temi_get_locations" -> {
                result.success(robot.locations)
            }
            "temi_delete_location" -> {
                val location = call.arguments<String>()
                result.success(location?.let { robot.deleteLocation(it) })
            }
            "temi_be_with_me" -> {
                robot.beWithMe()
                result.success(true)
            }
            "temi_constraint_be_with" -> {
                robot.constraintBeWith()
                result.success(true)
            }
            "temi_follow_me" -> {
                robot.beWithMe()
                result.success(true)
            }
            "temi_stop_movement" -> {
                robot.stopMovement()
                result.success(true)
            }
            "temi_skid_joy" -> {
                val values = call.arguments<List<Double>>()
                values?.get(0)?.let { values[1].let { it1 -> robot.skidJoy(it.toFloat(), it1.toFloat()) } }
                result.success(true)
            }
            "temi_tilt_angle" -> {
                val tiltAngle = call.arguments<Int>()
                if (tiltAngle != null) {
                    robot.tiltAngle(tiltAngle)
                }
                result.success(true)
            }
            "temi_turn_by" -> {
                val turnByAngle = call.arguments<Int>()
                if (turnByAngle != null) {
                    robot.turnBy(turnByAngle)
                }
                result.success(true)
            }
            "temi_tilt_by" -> {
                val degrees = call.arguments<Int>()
                if (degrees != null) {
                    robot.tiltBy(degrees)
                }
                result.success(true)
            }
            "temi_start_telepresence" -> {
                val arguments = call.arguments<List<String>>()
                result.success(robot.startTelepresence(arguments!![0], arguments[1]))
            }
            "temi_user_info" -> {
                val userInfo = robot.adminInfo!!
                result.success(OnUsersUpdatedListenerImpl.contactToMap(userInfo))
            }
            "temi_get_contacts" -> {
                val contacts = robot.allContact
                val maps = contacts.map { contact -> OnUsersUpdatedListenerImpl.contactToMap(contact) }
                result.success(maps)
            }
            "temi_get_recent_calls" -> {
                val recentCalls = robot.recentCalls
                val recentCallMaps = recentCalls.map { call ->
                    val callMap = HashMap<String, Any?>(4)
                    callMap["callType"] = call.callType
                    callMap["sessionId"] = call.sessionId
                    callMap["timestamp"] = call.timestamp
                    callMap["userId"] = call.userId
                    callMap
                }
                result.success(recentCallMaps)
            }
            "temi_wakeup" -> {
                robot.wakeup()
                result.success(true)
            }
            "temi_showAppList" -> {
                robot.showAppList()
                result.success(true)
            }
            "temi_toggle_wakeup" -> {
                val disable = call.arguments<Boolean>()
                if (disable != null) {
                    robot.toggleWakeup(disable)
                }
                result.success(true)
            }
            "temi_toggle_navigation_billboard" -> {
                val hide = call.arguments<Boolean>()
                if (hide != null) {
                    robot.toggleNavigationBillboard(hide)
                }
                result.success(true)
            }
            "temi_turnKoiskMode" -> {
                val activityInfo = applicationContext.packageManager
                    .getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
                robot.onStart(activityInfo)
                result.success(true)
            }
            "temi_repose" -> {
                robot.repose()
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {}

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}
}