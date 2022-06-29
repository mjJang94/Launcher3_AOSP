package com.android.launcher3.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import android.Manifest.permission as P

sealed class Perm(private val checkDefault: suspend Context.() -> Boolean?) {

    constructor() : this({ false })

    constructor(permission: String) : this({ checkSelf(permission) })

    suspend fun check(context: Context): Boolean =
        runCatching { context.checkActual() }.getOrNull() ?: false

    protected open suspend fun Context.checkActual(): Boolean? = checkDefault()

    // 위치 접근 권한
    object InstallShortCut : Perm(P.INSTALL_SHORTCUT)
    object CallPhone : Perm(P.CALL_PHONE)
    object WallPaper : Perm(P.SET_WALLPAPER)
    object WallPaperHints : Perm(P.SET_WALLPAPER_HINTS)
    object ReadExternalStorage : Perm(P.READ_EXTERNAL_STORAGE)
    object ReceiveBootCompleted : Perm(P.RECEIVE_BOOT_COMPLETED)
    object RequestDeletePackages : Perm(P.REQUEST_DELETE_PACKAGES)
    @RequiresApi(Build.VERSION_CODES.R)
    object QueryAllPackages : Perm(P.QUERY_ALL_PACKAGES)
    object WriteSecureSettings : Perm(P.WRITE_SECURE_SETTINGS)
    object WriteSettings : Perm(P.WRITE_SETTINGS)

    companion object {

        private fun Context.checkSelf(permission: String): Boolean =
            ActivityCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED

        private fun Context.checkCallingOrSelf(permission: String): Boolean =
            checkCallingOrSelfPermission(permission) ==
                    PackageManager.PERMISSION_GRANTED

        // Device Admin은 해제 후 바로 반영이 안되어 보강코드
        suspend fun pack(context: Context): Long = listOf(
            // LSB (right-most bit)
            InstallShortCut,
            CallPhone,
            WallPaper,
            WallPaperHints,
            ReadExternalStorage,
            ReceiveBootCompleted,
            RequestDeletePackages,
            QueryAllPackages,
            WriteSecureSettings,
            WriteSettings,
        ).foldIndexed(0L) { index, acc, permission ->
            when (permission.check(context)) {
                true -> acc or (1L shl index)
                else -> acc
            }
        }
    }
}