package uy.com.rutacamion.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uy.com.rutacamion.R

class AlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("route_alerts", "Alertas de ruta", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(applicationContext, "route_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("RutaCamión activo")
            .setContentText("Las alertas de restricciones están funcionando en segundo plano.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(1001, notification)
        return Result.success()
    }
}
