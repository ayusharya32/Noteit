package com.appsbyayush.noteit.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CommonMethods {
    private const val TAG = "CommonMethods"

    fun getFormattedDateTime(date: Date, format: String): String {
        return try {
            val sdf = SimpleDateFormat(format, Locale.UK)
            sdf.format(date)

        } catch(e: Exception) {
            Timber.tag(TAG).d("getFormattedDateTime: $e")
            "Error in Formatting Date"
        }
    }

    fun getTimeAgoString(date: Date): String {
        val currentTimestamp = System.currentTimeMillis()
        val dateInMillis = date.time

        val differenceInSeconds = (currentTimestamp - dateInMillis) / 1000
        if(differenceInSeconds < 60) {
            return "$differenceInSeconds second${if(differenceInSeconds > 1) "s" else ""} ago"
        }

        val differenceInMinutes = differenceInSeconds / 60
        if(differenceInMinutes < 60) {
            return "$differenceInMinutes minute${if(differenceInMinutes > 1) "s" else ""} ago"
        }

        val differenceInHours = differenceInMinutes / 60
        if(differenceInHours < 24) {
            return "$differenceInHours hour${if(differenceInHours > 1) "s" else ""} ago"
        }

        val differenceInDays = differenceInHours / 24
        if(differenceInDays < 30) {
            return "$differenceInDays day${if(differenceInDays > 1) "s" else ""} ago"
        }

        return ""
    }

    fun isDateOfToday(date: Date): Boolean {
        val todayCalendar = Calendar.getInstance()
        val givenDateCalender = Calendar.getInstance()
        givenDateCalender.time = date

        return (todayCalendar[Calendar.DAY_OF_YEAR] == givenDateCalender[Calendar.DAY_OF_YEAR]
                && todayCalendar[Calendar.YEAR] == givenDateCalender[Calendar.YEAR])
    }

    fun isDateOfYesterday(date: Date): Boolean {
        val yesterdayCalendar = Calendar.getInstance()
        yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1)
        val givenDateCalender = Calendar.getInstance()
        givenDateCalender.time = date

        return (yesterdayCalendar[Calendar.DAY_OF_YEAR] == givenDateCalender[Calendar.DAY_OF_YEAR]
                && yesterdayCalendar[Calendar.YEAR] == givenDateCalender[Calendar.YEAR])
    }

    @Throws(java.lang.Exception::class)
    fun getImageBitmapFromUri(context: Context, imageUri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        } else {
            val source = ImageDecoder.createSource(
                context.contentResolver,
                imageUri
            )
            ImageDecoder.decodeBitmap(source)
        }
    }

    suspend fun getCompressedImageUri(context: Context, uri: Uri): Uri? {
        return try {
            val bitmap = getImageBitmapFromUri(context, uri)

            val fileName = "IMG" + getFormattedDateTime(
                Calendar.getInstance().time,
                Constants.DATE_TIME_FORMAT_1
            )

            val filePath = "${context.getExternalFilesDir("images")}/$fileName.jpg"
            val file = File(filePath)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            outputStream.flush()
            outputStream.close()

            Uri.fromFile(file)

        } catch(e: Exception) {
            return null
        }
    }

    fun getExtensionFromFileName(fileName: String): String {
        val dotSplitList = fileName.split(".")
        return dotSplitList.last()
    }

    fun showSoftKeyboard(context: Context, view: View) {
        if (view.requestFocus()) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun hideSoftKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}