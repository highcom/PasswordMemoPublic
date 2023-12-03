package com.highcom.passwordmemo.util.file

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import com.highcom.passwordmemo.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

class RestoreDbFile(private val activity: Activity, listener: RestoreDbFileListener) {
    private val context: Context
    private val listener: RestoreDbFileListener
    private var uri: Uri? = null
    private var progressAlertDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null

    interface RestoreDbFileListener {
        fun restoreComplete()
    }

    private inner class BackgroundTask(private val _handler: Handler) : Runnable {
        @WorkerThread
        override fun run() {
            val destPath = context.getDatabasePath("PasswordMemoDB").path
            val destFile = File(destPath)
            destFile.delete()
            val srcPath = context.getDatabasePath("PasswordMemoDB_tmp").path
            val srcFile = File(srcPath)
            srcFile.renameTo(destFile)
            progressBar!!.progress = 100
            val postExecutor: PostExecutor = PostExecutor()
            _handler.post(postExecutor)
        }
    }

    private inner class PostExecutor : Runnable {
        @UiThread
        override fun run() {
            progressAlertDialog!!.dismiss()
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.restore_db))
                .setMessage(
                    context.getString(R.string.db_restore_complete_message) + System.getProperty(
                        "line.separator"
                    ) + getFileNameByUri(context, uri)
                )
                .setPositiveButton(R.string.ok) { dialog, which -> listener.restoreComplete() }
                .show()
        }
    }

    init {
        context = activity
        this.listener = listener
    }

    fun restoreSelectFolder(uri: Uri?) {
        this.uri = uri
        val fileName = getFileNameByUri(context, uri)
        if (fileName.contains(".db")) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.restore_db))
                .setMessage(
                    context.getString(R.string.restore_message_front) + getFileNameByUri(
                        context,
                        uri
                    ) + System.getProperty("line.separator") + context.getString(R.string.restore_message_rear)
                )
                .setPositiveButton(R.string.restore_button) { dialog, which ->
                    if (restoreDatabase(uri)) {
                        execRestoreDatabase()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, which ->
                    val path = context.getDatabasePath("PasswordMemoDB_tmp").path
                    val file = File(path)
                    file.delete()
                }
                .show()
        } else {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.restore_db))
                .setMessage(context.getString(R.string.restore_message_error_file))
                .setPositiveButton(R.string.ok) { dialog, which ->
                    val path = context.getDatabasePath("PasswordMemoDB_tmp").path
                    val file = File(path)
                    file.delete()
                }
                .show()
        }
    }

    private fun restoreDatabase(uri: Uri?): Boolean {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri!!)
            val path = context.getDatabasePath("PasswordMemoDB_tmp").path
            val file = File(path)
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream!!.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
            }
            inputStream.close()
        } catch (exc: Exception) {
            val ts = Toast.makeText(
                context,
                context.getString(R.string.db_restore_failed_message),
                Toast.LENGTH_SHORT
            )
            ts.show()
            return false
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    private fun execRestoreDatabase() {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.restore_db))
            .setMessage(context.getString(R.string.db_restore_confirm_message))
            .setPositiveButton(R.string.execute) { dialog, which -> // 取込み中のプログレスバーを表示する
                progressAlertDialog = AlertDialog.Builder(context)
                    .setTitle(R.string.db_restore_processing)
                    .setView(activity.layoutInflater.inflate(R.layout.alert_progressbar, null))
                    .create()
                progressAlertDialog.show()
                progressAlertDialog.setCancelable(false)
                progressAlertDialog.setCanceledOnTouchOutside(false)
                progressBar = progressAlertDialog.findViewById(R.id.ProgressBarHorizontal)

                // ワーカースレッドで取込みを開始する
                val mainLooper = Looper.getMainLooper()
                val handler = HandlerCompat.createAsync(mainLooper)
                val backgroundTask: BackgroundTask = BackgroundTask(handler)
                val executorService = Executors.newSingleThreadExecutor()
                executorService.submit(backgroundTask)
            }
            .setNegativeButton(R.string.cancel) { dialog, which ->
                val path = context.getDatabasePath("PasswordMemoDB_tmp").path
                val file = File(path)
                file.delete()
            }
            .show()
    }

    private fun getFileNameByUri(context: Context, uri: Uri?): String {
        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = context.contentResolver
            .query(uri!!, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                )
            }
            cursor.close()
        }
        return fileName
    }
}