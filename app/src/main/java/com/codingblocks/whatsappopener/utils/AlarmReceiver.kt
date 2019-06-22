package com.codingblocks.whatsappopener.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        ClipBoardWorker.scheduleOneTimeWork()
        ClipBoardWorker.schedulePeriodicWork()
    }

}