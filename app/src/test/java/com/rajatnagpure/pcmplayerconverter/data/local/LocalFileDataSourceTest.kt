package com.rajatnagpure.pcmplayerconverter.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalFileDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mockk()
    private val contentResolver: ContentResolver = mockk()
    private val dataSource = LocalFileDataSource(context)

    @Test
    fun `getFileFromUri returns file if uri has file scheme`() = runTest {
        val file = tempFolder.newFile("test.pcm")
        val uri = Uri.fromFile(file)
        
        val result = dataSource.getFileFromUri(uri)
        
        assertNotNull(result)
        assertEquals(file.absolutePath, result?.absolutePath)
    }

    @Test
    fun `getFileFromUri copies content if scheme is content`() = runTest {
        // Arrange
        val content = "test data".toByteArray()
        val uri = Uri.parse("content://test/audio")
        val cacheDir = tempFolder.newFolder("cache")
        val cursor: android.database.Cursor = mockk()
        
        every { context.contentResolver } returns contentResolver
        every { context.cacheDir } returns cacheDir
        
        // Mock cursor for display name
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor
        every { cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.moveToFirst() } returns true
        every { cursor.getString(0) } returns "test.pcm"
        every { cursor.close() } returns Unit
        
        every { contentResolver.openInputStream(uri) } returns content.inputStream()

        // Act
        val result = dataSource.getFileFromUri(uri)

        // Assert
        assertNotNull(result)
        assertEquals(content.size.toLong(), result?.length())
    }
}
