package com.codingblocks.whatsappopener.utils

import android.content.*
import com.codingblocks.whatsappopener.OpenChatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.*

class NumberNotificationManager {

    companion object {
        private var instance: NumberNotificationManager? = null
        fun getInstance(context: Context): NumberNotificationManager {
            synchronized("INSTANCE") {
                instance = instance ?: NumberNotificationManager(context)
                return instance as NumberNotificationManager
            }
        }
        private var LAST_NOTIF_TIME = 0L
    }

    private lateinit var mClipboardManager: ClipboardManager
    private var listenerAssigned: Boolean = false
    private var mNumberNotifications: MutableList<NumberNotification> = Collections.synchronizedList(mutableListOf())
    private val mOnPrimaryClipChangedListener: ClipboardManager.OnPrimaryClipChangedListener

    private constructor(context: Context) {
        runBlocking(Dispatchers.Main) {
            mClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
        mOnPrimaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (!AppPreferences.shouldMontiorClipboard()) {
                return@OnPrimaryClipChangedListener
            }

            val clip = mClipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                if (item != null) {
                    val number = (item.coerceToText(context) ?: "").toString()  //Elvis used to avoid null in item.text
                    synchronized("ABC") {
                        if (LAST_NOTIF_TIME + 2000 < System.currentTimeMillis()) {
                            LAST_NOTIF_TIME = System.currentTimeMillis()
                            if (OpenChatActivity.isNumber(number)) {
                                val urlNotification = NumberNotification(context)
                                mNumberNotifications.add(urlNotification)
                                urlNotification.notifyLongUrl(number)
                            }
                        }
                    }
                }
            }
        }
        context.applicationContext.registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getIntExtra(NumberNotification.EXTRA_NOTIFICATION_ID, -1)
                if (id != -1) {
                    deleteNumberNotification(id)
                }
            }

        }, IntentFilter(NumberNotification.ACTION_REMOVE_URL_NOTIFICATION))
    }

    fun startListener() {
        assignListener()
    }

    private fun assignListener() {
        synchronized("LOCK") {
            if (!listenerAssigned) {
                mClipboardManager.addPrimaryClipChangedListener(mOnPrimaryClipChangedListener)
                listenerAssigned = true
            }
        }
    }

    private fun deleteNumberNotification(notificationId: Int?) {
        synchronized("LOCK_DELETE_LIST_ITEM") {
            val removeList = arrayListOf<NumberNotification>()
            mNumberNotifications.forEach {
                if (notificationId == it.getId()) {
                    it.dismiss()
                    removeList.add(it)
                }
            }
            mNumberNotifications.removeAll(removeList)
        }
    }
}