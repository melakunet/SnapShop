package com.melakunet.snapshop

import android.app.Application
import com.melakunet.snapshop.data.SnapShopDatabase

class SnapShopApplication : Application() {
    val database: SnapShopDatabase by lazy { SnapShopDatabase.getDatabase(this) }
}
