package com.bookkeeper.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机自启：让通知栏"记一笔"快捷入口在重启后依然常驻。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            QuickRecordService.start(context)
        }
    }
}
