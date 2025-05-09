package com.appsbyayush.noteit.models

import android.os.Parcelable
import com.appsbyayush.noteit.utils.Constants
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class AppSettings(
    var currentSort: SortItem = Constants.SORT_LIST[0],
    var signupPopupLastShownTimestamp: Long = 0,
    var lastSyncTime: Date? = null
): Parcelable