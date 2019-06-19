package com.codingblocks.whatsappclick.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        ClipBoardWorker.scheduleOneTimeWork()
        ClipBoardWorker.schedulePeriodicWork()
    }

    fun setAlarm(context: Context?) {
        val service = PendingIntent.getBroadcast(
            context?.applicationContext,
            1001,
            Intent(context, AlarmReceiver::class.java),
            0
        )
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, 5000, service)
    }

    fun cancelAlarm(context: Context?) {
        val service = PendingIntent.getBroadcast(
            context?.applicationContext,
            1001,
            Intent(context?.applicationContext, AlarmReceiver::class.java),
            0
        )
        val alarmManager =
            context?.applicationContext?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(service)
    }
}