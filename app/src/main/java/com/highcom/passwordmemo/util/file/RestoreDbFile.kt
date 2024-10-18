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
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase
import com.highcom.passwordmemo.databinding.AlertProgressbarBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

/**
 * DBファイルの復元処理クラス
 *
 * @property activity ダイアログ表示用アクティビティ
 * @constructor
 * DBファイルの復元処理コンストラクタ
 *
 * @param listener 復元処理完了通知リスナー
 */
class RestoreDbFile(private val activity: Activity, listener: RestoreDbFileListener) {
    /** ダイアログ表示用コンテキスト */
    private val context: Context
    /** 復元処理完了通知リスナー */
    private val listener: RestoreDbFileListener
    /** DBファイル復元元ファイルURI */
    private var uri: Uri? = null
    /** プログレスダイアログ */
    private var progressAlertDialog: AlertDialog? = null
    /** プログレスバー */
    private var progressBar: ProgressBar? = null

    /**
     * DBファイル復元処理完了通知リスナークラス
     *
     */
    interface RestoreDbFileListener {
        /**
         * DBファイル復元処理完了通知処理
         *
         */
        fun restoreComplete()
    }

    /**
     * DBファイル復元用バックグラウンドタスク
     *
     * @property _handler 処理ハンドラ
     */
    private inner class BackgroundTask(private val _handler: Handler) : Runnable {
        /**
         * DBファイル復元実行処理
         *
         */
        @WorkerThread
        override fun run() {
            val destPath = context.getDatabasePath("PasswordMemoDB").path
            val destFile = File(destPath)
            destFile.delete()
            val srcPath = context.getDatabasePath("PasswordMemoDB_tmp").path
            val srcFile = File(srcPath)
            srcFile.renameTo(destFile)
            progressBar!!.progress = 100
            val postExecutor = PostExecutor()
            _handler.post(postExecutor)
        }
    }

    /**
     * バックグラウンド実行後のランナークラス
     *
     */
    private inner class PostExecutor : Runnable {
        /**
         * バックグラウンド実行後処理
         *
         */
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
                .setPositiveButton(R.string.ok) { _, _ -> }
                .setOnDismissListener { listener.restoreComplete() }
                .show()
        }
    }

    init {
        context = activity
        this.listener = listener
    }

    /**
     * DBファイル復元元フォルダ選択確認ダイアログ表示処理
     *
     * @param uri 復元元DBファイルURI
     */
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
                .setPositiveButton(R.string.restore_button) { _, _ ->
                    if (restoreDatabase(uri)) {
                        execRestoreDatabase()
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    val path = context.getDatabasePath("PasswordMemoDB_tmp").path
                    val file = File(path)
                    file.delete()
                }
                .show()
        } else {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.restore_db))
                .setMessage(context.getString(R.string.restore_message_error_file))
                .setPositiveButton(R.string.ok) { _, _ ->
                    val path = context.getDatabasePath("PasswordMemoDB_tmp").path
                    val file = File(path)
                    file.delete()
                }
                .show()
        }
    }

    /**
     * DBファイル復元処理
     * * 指定されたパスのDBファイルを復元する
     *
     * @param uri 復元元DBファイルURI
     * @return 復元完了可否
     */
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

    /**
     * DBファイル復元実行処理
     * * DBファイル復元処理をバックグラウンドで実行する
     *
     */
    private fun execRestoreDatabase() {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.restore_db))
            .setMessage(context.getString(R.string.db_restore_confirm_message))
            .setPositiveButton(R.string.execute) { _, _ ->
                // リストアする前にDBを閉じる
                PasswordMemoRoomDatabase.closeDatabase()
                // 取込み中のプログレスバーを表示する
                val binding = AlertProgressbarBinding.inflate(activity.layoutInflater)
                progressAlertDialog = AlertDialog.Builder(context)
                    .setTitle(R.string.db_restore_processing)
                    .setView(binding.root)
                    .create()
                progressAlertDialog?.show()
                progressAlertDialog?.setCancelable(false)
                progressAlertDialog?.setCanceledOnTouchOutside(false)
                progressBar = binding.progressBarHorizontal

                // ワーカースレッドで取込みを開始する
                val mainLooper = Looper.getMainLooper()
                val handler = HandlerCompat.createAsync(mainLooper)
                val backgroundTask = BackgroundTask(handler)
                val executorService = Executors.newSingleThreadExecutor()
                executorService.submit(backgroundTask)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                val path = context.getDatabasePath("PasswordMemoDB_tmp").path
                val file = File(path)
                file.delete()
            }
            .show()
    }

    /**
     * URIからファイルパス名を取得する処理
     *
     * @param context コンテキスト
     * @param uri ファイルURI
     * @return ファイルパス名
     */
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