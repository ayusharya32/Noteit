package com.appsbyayush.noteit.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.repo.NoteRepository
import com.appsbyayush.noteit.utils.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val repository: NoteRepository
): CoroutineWorker(context, workerParams) {

    companion object {
        const val PERIODIC_REQUEST_NAME = "SYNC_WORKER_PERIODIC_REQUEST"
        const val ONE_TIME_REQUEST_NAME = "SYNC_WORKER_ONE_TIME_REQUEST"

        private const val TAG = "SyncWorkeryy"
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("doWork: Called")
        initForegroundService()

        return withContext(Dispatchers.IO) {
            try {
                repository.syncNotes()
                repository.syncNoteMediaItems()

                val appSettings = repository.getAppSettings()
                repository.saveAppSettings(appSettings.copy(lastSyncTime = Calendar.getInstance().time))

                repository.clearTrashedNotesOlderThan30days()
                repository.clearTrashedNoteMediaItemsOlderThan30days()
                repository.deleteAllLocalMediaFiles()

                Result.success(workDataOf(Constants.WORK_RESULT to Constants.WORK_RESULT_SUCCESS))

            } catch(e: Exception) {
                Timber.tag(TAG).d("doWork: ${e.message}")
                
                Result.failure(workDataOf(Constants.WORK_RESULT to Constants.WORK_RESULT_FAILURE))
            }
        }
    }

    private suspend fun initForegroundService() {
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(1, getSyncNotesNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1, getSyncNotesNotification())
        }
        setForeground(foregroundInfo)
    }

    private fun getSyncNotesNotification(): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_LOW)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_text_note)
            .setContentTitle("Syncing Notes...")
            .build()
    }
}