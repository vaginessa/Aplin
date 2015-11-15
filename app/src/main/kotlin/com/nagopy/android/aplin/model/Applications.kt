package com.nagopy.android.aplin.model

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import com.nagopy.android.aplin.entity.AppEntity
import com.nagopy.android.aplin.entity.names.AppEntityNames
import com.nagopy.android.aplin.model.converter.AppConverter
import com.nagopy.android.aplin.model.preference.SortSetting
import com.nagopy.android.kotlinames.equalTo
import io.realm.Realm
import io.realm.RealmResults
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class Applications
@Inject constructor(
        var application: Application
        , val packageManager: PackageManager
        , val appConverter: AppConverter
        , val sortSetting: SortSetting
) {

    val handler: Handler = Handler(Looper.getMainLooper())
    val enabledSettingField: FieldReflection<Int> = FieldReflection(ApplicationInfo::class.java, "enabledSetting")

    open fun initialize(func: () -> Unit) {
        Thread({
            if (!isLoaded()) {
                refresh()
            }
            handler.post {
                func()
            }
        }).start()
    }

    open fun isLoaded(): Boolean {
        Realm.getInstance(application).use {
            return it.where(AppEntity::class.java).count() > 0
        }
    }

    open fun refresh() {
        val realm = Realm.getInstance(application)
        realm.use {
            realm.executeTransaction {
                realm.where(AppEntity::class.java).findAll().clear()
                val allApps = getInstalledApplications()
                allApps.forEach {
                    if (shouldSkip(it)) {
                        Timber.d("skip:" + it.packageName)
                        return@forEach
                    }

                    val entity = realm.createObject(AppEntity::class.java)
                    appConverter.setValues(entity, it)
                }
            }
        }
    }

    open fun getApplicationList(category: Category): RealmResults<AppEntity> {
        Realm.getInstance(application).use {
            Timber.d("getApplicationList " + category)
            val query = it.where(AppEntity::class.java)
            val result = sortSetting.value.findAllSortedAsync(category.where(query))
            return result
        }
    }

    /**
     * アプリケーション一覧を取得する.<br>
     * [android.content.pm.PackageManager.getInstalledApplications]の引数については、以下のクラスを参照
     * /packages/apps/Settings/src/com/android/settings/applications/ApplicationsState.java
     */
    fun getInstalledApplications(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(getFlags())
    }

    open fun getFlags(): Int {
        val ownerRetrieveFlags = PackageManager.GET_UNINSTALLED_PACKAGES or
                PackageManager.GET_DISABLED_COMPONENTS or
                PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS

        val retrieveFlags = PackageManager.GET_DISABLED_COMPONENTS or
                PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS

        val flags: Int
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val myUserIdMethod = UserHandle::class.java.getDeclaredMethod("myUserId")
            flags = if (myUserIdMethod.invoke(null) == 0) {
                ownerRetrieveFlags
            } else {
                retrieveFlags
            }
        } else {
            val myUserHandle = android.os.Process.myUserHandle()
            val isOwnerMethod = UserHandle::class.java.getDeclaredMethod("isOwner")
            flags = if (isOwnerMethod.invoke(myUserHandle) as Boolean) {
                ownerRetrieveFlags
            } else {
                retrieveFlags
            }
        }
        return flags or PackageManager.GET_SIGNATURES
    }

    open fun shouldSkip(applicationInfo: ApplicationInfo): Boolean {
        if (!applicationInfo.enabled) {
            // 無効になっていて、かつenabledSettingが3でないアプリは除外する
            val enabledSetting = enabledSettingField.get(applicationInfo)
            if (enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                return true
            }
        }
        return false
    }

    open fun insert(pkg: String) {
        Timber.d("insert $pkg")
        upsert(pkg)
        listeners.forEach { it.onPackageChanged() }
    }

    open fun update(pkg: String) {
        Timber.d("update $pkg")
        upsert(pkg)
        listeners.forEach { it.onPackageChanged() }
    }

    private fun upsert(pkg: String) {
        val applicationInfo = packageManager.getApplicationInfo(pkg, getFlags())
        if (!shouldSkip(applicationInfo)) {
            val realm = Realm.getInstance(application)
            realm.use {
                realm.executeTransaction {
                    var entity = realm.where(AppEntity::class.java).equalTo(AppEntityNames.packageName(), pkg).findFirst()
                    if(entity == null) {
                        entity = realm.createObject(AppEntity::class.java)
                    }
                    appConverter.setValues(entity, applicationInfo)
                }
            }
        }
    }

    open fun delete(pkg: String) {
        Timber.d("delete $pkg")
        val realm = Realm.getInstance(application)
        realm.use {
            realm.executeTransaction {
                val entity = realm.where(AppEntity::class.java).equalTo(AppEntityNames.packageName(), pkg).findAll()
                entity.clear()
            }
        }
        listeners.forEach { it.onPackageChanged() }
    }

    var listeners: List<PackageChangedListener> = ArrayList()
    open fun addPackageChangedListener(listener: PackageChangedListener) {
        listeners = listeners.plus(listener)
    }

    open fun removePackageChangedListener(listener: PackageChangedListener) {
        listeners = listeners.minus(listener)
    }

    interface PackageChangedListener {
        fun onPackageChanged()
    }
}