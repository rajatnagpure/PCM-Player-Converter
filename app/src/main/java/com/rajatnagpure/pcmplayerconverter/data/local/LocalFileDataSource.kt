package com.rajatnagpure.pcmplayerconverter.data.local

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Ported logic from FileUtils.java to get usable File path or copy to cache
    
    fun getFileFromUri(uri: Uri): File? {
        // If this is a content URI, prefer copying to cache to avoid storage permission issues
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val cached = getFileFromCache(uri)
            if (cached != null) return cached
            // fall through to try path resolution as a last resort
        }

        val path = getPath(context, uri)
        if (path != null) {
            val f = File(path)
            try {
                if (f.exists() && f.canRead()) {
                    return f
                }
            } catch (e: Exception) {
                // ignore and fallback to cache
            }
        }
        // Fallback: copy to cache if path resolution fails or file is not readable
        return getFileFromCache(uri)
    }

    // Public helper to force copying a URI to app cache (useful when external path access is denied)
    fun copyUriToCache(uri: Uri): File? {
        return getFileFromCache(uri)
    }

    private fun getFileFromCache(uri: Uri): File? {
        var name = "temp_audio_file"
        try {
            val returnCursor = context.contentResolver.query(uri, null, null, null, null)
            returnCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val fileName = cursor.getString(nameIndex)
                        if (!fileName.isNullOrEmpty()) {
                            name = fileName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val file = File(context.cacheDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Simplified version of FileUtils.getPath
    private fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "")
                    }
                    val contentUriPrefixesToTry = arrayOf(
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                    )
                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        try {
                            val contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), java.lang.Long.valueOf(id))
                            val path = getDataColumn(context, contentUri, null, null)
                            if (path != null) return path
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (isGooglePhotosUri(uri)) return uri.lastPathSegment
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean = "com.android.externalstorage.documents" == uri.authority
    private fun isDownloadsDocument(uri: Uri): Boolean = "com.android.providers.downloads.documents" == uri.authority
    private fun isMediaDocument(uri: Uri): Boolean = "com.android.providers.media.documents" == uri.authority
    private fun isGooglePhotosUri(uri: Uri): Boolean = "com.google.android.apps.photos.content" == uri.authority
}
