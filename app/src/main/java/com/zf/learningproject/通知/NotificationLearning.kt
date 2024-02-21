package com.zf.learningproject.通知

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zf.learningproject.R

/**
 * @description: please add a description here
 * @author: zhang_fang
 * @date: 2024/1/23 15:28
 */
class NotificationLearning {

    fun init(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(getChannel())
        }


        val notification = NotificationCompat.Builder(context)
            .setContentTitle("这是标题")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * 创建通道
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannel(): NotificationChannel {
        val channel = NotificationChannel("", "", NotificationManager.IMPORTANCE_MIN)

        return channel
    }


}