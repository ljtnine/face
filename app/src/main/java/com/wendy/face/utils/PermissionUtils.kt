package com.wendy.face.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 * 负责检查和管理应用所需的各种权限
 */
object PermissionUtils {
    
    /**
     * 检查相机权限
     * @param context 上下文
     * @return 是否已授权
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查存储权限（仅适用于Android P及以下版本）
     * @param context 上下文
     * @return 是否已授权
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android Q及以上版本不需要存储权限
        }
    }
    
    /**
     * 获取需要请求的权限列表
     * @param context 上下文
     * @return 需要请求的权限数组
     */
    fun getPermissionsToRequest(context: Context): Array<String> {
        val permissionsToRequest = mutableListOf<String>()
        
        if (!hasCameraPermission(context)) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        
        if (!hasStoragePermission(context)) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        return permissionsToRequest.toTypedArray()
    }
    
    /**
     * 检查是否所有必要权限都已授权
     * @param context 上下文
     * @return 是否所有权限都已授权
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasCameraPermission(context) && hasStoragePermission(context)
    }
}
