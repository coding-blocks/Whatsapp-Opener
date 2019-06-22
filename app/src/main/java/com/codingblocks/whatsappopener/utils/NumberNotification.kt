package com.codingblocks.whatsappopener.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.SyncStateContract
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.codingblocks.whatsappopener.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class UrlNotification {

    companion object {
        const val CHANNEL_ID = "OPENER_CHANNEL_ID_MAIN"
        const val ACTION_OPEN_WHATSAPP = "ACTION_OPEN_WHATSAPP"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val ACTION_REMOVE_NUMBER_NOTIFICATION = "ACTION_REMOVE_NUMBER_NOTIFICATION"
        const val ACTION_COPY_NUMBER = "ACTION_COPY_NUMBER"
        const val ACTION_TURN_OFF_NOTIF = "ACTION_TURN_OFF_NOTIF"
        const val EXTRA_COPY_TEXT = "EXTRA_COPY_TEXT"
    }

    private var parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main
    private val scope = CoroutineScope(coroutineContext)
    private var isBroadcastShortenUrlRegistered: Boolean = false
    private var isBroadcastCopyToClipboardRegistered: Boolean = false
    private var isBroadcastTurnOffNotifRegistered: Boolean = false

    private var mContext: Context
    val NOTIFICATION_ID = (System.currentTimeMillis() and 0xfffffff).toInt()
    private val broadcastReceiverShortenUrl: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getIntExtra(EXTRA_NOTIFICATION_ID,-1)
            if (id != NOTIFICATION_ID) return
            val number = intent.getStringExtra(Constants.EXTRA_LONG_URL) ?: return
            handleWhatsappOpener(number)
        }
    }
    private val broadcastReceiverCopyToClipboard: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getIntExtra(EXTRA_NOTIFICATION_ID,-1)
            if (id != NOTIFICATION_ID) return
            val text = intent.getStringExtra(EXTRA_COPY_TEXT) ?: return
            val cbManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("Open Whatsapp", text)
            cbManager.primaryClip = clipData
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            Toast.makeText(context, context.getString(R.string.url_copied_clipboard), Toast.LENGTH_LONG).show()
        }
    }
    private val broadcastReceiverTurnOffNotif: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getIntExtra(EXTRA_NOTIFICATION_ID,-1)
            if (id != NOTIFICATION_ID) return
            AppPreferences.setMonitorClipboard(false)
            getNotificationManager().cancel(id)
            context?.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            Toast.makeText(context, context!!.getString(R.string.can_enable_notifications), Toast.LENGTH_LONG).show()
        }
    }


    constructor(context: Context) {
        mContext = context
    }

    fun getId() = NOTIFICATION_ID
    private fun getNotificationManager() = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun getNotificationBuilderInternal(): NotificationCompat.Builder {
        val pendingDeleteIntent = PendingIntent.getBroadcast(mContext, 0,
            Intent(ACTION_REMOVE_NUMBER_NOTIFICATION).putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID), 0)
        val builder = NotificationCompat.Builder(mContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clipicon)
            .setColor(ContextCompat.getColor(mContext, R.color.notificationColor))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDeleteIntent(pendingDeleteIntent)
        if (AppPreferences.shouldVibrateNotification()) {
            builder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        }
        return builder
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Create Short URL Notification Channel"
            val descriptionText = "Create Short URL Notification Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                importance
            ).apply {
                description = descriptionText
            }
            // Register the channel with the system
            getNotificationManager().createNotificationChannel(channel)
        }
    }

    fun notify(notification: Notification) {
        with(NotificationManagerCompat.from(mContext)) {
            notify(NOTIFICATION_ID/*notificationId*/, notification)   // notificationId is a unique int for each notification that you must define
        }
    }

    fun notifyLongUrl(longUrl: String) {
        registerBroadcast()
        with(NotificationManagerCompat.from(mContext)) {
            notify(NOTIFICATION_ID/*notificationId*/, getLongUrlNotification(longUrl))   // notificationId is a unique int for each notification that you must define
        }
    }

    private fun getLongUrlNotification(longUrl: String): Notification {
        createNotificationChannel()

        val startAppIntent = Intent(mContext, LaunchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.EXTRA_LONG_URL, longUrl)
        }
        val startAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            mContext,
            0,
            startAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shortenUrlIntent = Intent(ACTION_SHORTEN_URL).apply {
            putExtra(SyncStateContract.Constants.EXTRA_LONG_URL, longUrl)
            putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val shortenUrlPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            mContext,
            NOTIFICATION_ID,
            shortenUrlIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val turnOffNotifIntent = Intent(ACTION_TURN_OFF_NOTIF).apply {
            putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val turnOffNotifPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            mContext,
            NOTIFICATION_ID,
            turnOffNotifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return getNotificationBuilderInternal()
            .setContentTitle(mContext.getString(R.string.notif_shortenAsk))
            .setContentText(longUrl)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longUrl))
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(startAppPendingIntent)
            .addAction(0, "Shorten", shortenUrlPendingIntent)
            .addAction(0, "Turn Off Notifications", turnOffNotifPendingIntent)
            .build()
    }

    fun getShortUrlNotfication(url: Url): Notification {
        createNotificationChannel()
        registerBroadcastCopyToClipboard()
//        val shareUrlIntent = Intent(Intent.ACTION_SEND).apply {
//            type = "text/plain"
//            putExtra(Intent.EXTRA_SUBJECT, "Short URL")
//            putExtra(Intent.EXTRA_TEXT, shortUrl)
//        }
//        //TODO: share not working, send broadcast through pendingintent, catch it and fire an ACION_SEND intent
//        val shareUrlPendingIntent: PendingIntent = PendingIntent.getBroadcast(
//            mContext,
//            0,
//            shareUrlIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT
//        )

        val startAppIntent = Intent(mContext, LaunchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.EXTRA_SHORT_URL_ID, url.id)
        }
        val startAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            mContext,
            0,
            startAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val copyUrlIntent = Intent(ACTION_COPY_URL).apply {
            putExtra(EXTRA_COPY_TEXT, url.shortUrl)
            putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val copyUrlPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            mContext,
            NOTIFICATION_ID,
            copyUrlIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return getNotificationBuilderInternal()
            .setContentTitle(mContext.getString(R.string.notif_url_success))
            .setContentText(url.shortUrl)
            .setProgress( 0, 0, false)
            .setContentIntent(startAppPendingIntent)
//            .addAction(0, "Share", shareUrlPendingIntent)
            .addAction(0, "Copy", copyUrlPendingIntent)
            .build()
    }


    private fun registerBroadcast() {
        registerBroadcastShortenUrl()
        registerBroadcastCopyToClipboard()
        registerBroadcastTurnOffNotif()
    }

    private fun registerBroadcastShortenUrl() {
        if (!isBroadcastShortenUrlRegistered) {
            val filter = IntentFilter()
            filter.addAction(ACTION_OPEN_WHATSAPP)
            mContext.registerReceiver(broadcastReceiverShortenUrl, filter)
            isBroadcastShortenUrlRegistered = true
        }
    }

    private fun registerBroadcastCopyToClipboard() {
        if (!isBroadcastCopyToClipboardRegistered) {
            val filterClipbaord = IntentFilter()
            filterClipbaord.addAction(ACTION_COPY_NUMBER)
            mContext.registerReceiver(broadcastReceiverCopyToClipboard, filterClipbaord)
            isBroadcastCopyToClipboardRegistered = true
        }
    }

    private fun registerBroadcastTurnOffNotif() {
        if (!isBroadcastTurnOffNotifRegistered) {
            val filter = IntentFilter()
            filter.addAction(ACTION_TURN_OFF_NOTIF)
            mContext.registerReceiver(broadcastReceiverTurnOffNotif, filter)
            isBroadcastTurnOffNotifRegistered = true
        }
    }

    fun dismiss() {
        parentJob.cancel()
        unregisterBroadcast()
    }
    private fun unregisterBroadcast() {
        if (isBroadcastShortenUrlRegistered) {
            mContext.unregisterReceiver(broadcastReceiverShortenUrl)
            isBroadcastShortenUrlRegistered = false
        }
        if (isBroadcastCopyToClipboardRegistered) {
            mContext.unregisterReceiver(broadcastReceiverCopyToClipboard)
            isBroadcastCopyToClipboardRegistered = false
        }
        if (isBroadcastTurnOffNotifRegistered) {
            mContext.unregisterReceiver(broadcastReceiverTurnOffNotif)
            isBroadcastTurnOffNotifRegistered = false
        }
    }
}
