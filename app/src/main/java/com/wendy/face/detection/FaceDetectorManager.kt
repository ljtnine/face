package com.wendy.face.detection

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions

/**
 * 人脸网格检测管理器
 * 负责配置和执行人脸网格检测功能
 */
class FaceDetectorManager {

    companion object {
        private const val TAG = "FaceDetectorManager"
    }

    private val faceMeshDetectorOptions = FaceMeshDetectorOptions.Builder()
        .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
        .build()

    private val faceMeshDetector = FaceMeshDetection.getClient(faceMeshDetectorOptions)

    /**
     * 检测图片中的人脸网格
     * @param inputImage 要检测的图片
     * @param onSuccess 检测成功回调
     * @param onFailure 检测失败回调
     */
    fun detectFaces(
        inputImage: InputImage,
        onSuccess: (List<FaceMesh>, Int, Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // 执行人脸网格检测
            faceMeshDetector.process(inputImage)
                .addOnSuccessListener { detectedFaceMeshes ->
                    Log.d(TAG, "Face mesh detection completed, faces found: ${detectedFaceMeshes.size}")
                    Log.d(TAG, "Image dimensions: ${inputImage.width} x ${inputImage.height}")
                    detectedFaceMeshes.forEachIndexed { index, faceMesh ->
                        Log.d(TAG, "FaceMesh $index: points=${faceMesh.allPoints.size}")
                    }
                    onSuccess(detectedFaceMeshes, inputImage.width, inputImage.height)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face mesh detection failed", e)
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing InputImage", e)
            onFailure(e)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            faceMeshDetector.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing face mesh detector", e)
        }
    }
}
