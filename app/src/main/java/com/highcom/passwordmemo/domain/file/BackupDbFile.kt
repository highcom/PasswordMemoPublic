package com.highcom.passwordmemo.domain.file

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException

/**
 * DBファイルのバックアップ処理クラス
 *
 * @property context コンテキスト
 */
class BackupDbFile (private val context: Context, private val db: PasswordMemoRoomDatabase, private val listener: BackupDbFileListener) {
    /**
     * DBファイルバックアップ処理完了通知リスナークラス
     *
     */
    interface BackupDbFileListener {
        /**
         * DBファイルバックアップ処理完了通知処理
         *
         */
        fun backupComplete()
    }

    /**
     * DBファイルバックアップ先フォルダ確認ダイアログ表示処理
     *
     * @param uri バックアップ先URI
     */
    fun backupSelectFolder(uri: Uri?) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.backup_db))
            .setMessage(
                context.getString(R.string.backup_message_front) + uri!!.path!!
                    .replace(
                        ":",
                        "/"
                    ) + System.getProperty("line.separator") + context.getString(R.string.backup_message_rear)
            )
            .setPositiveButton(R.string.backup_button) { _, _ -> backupDatabase(uri) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * DBファイルバックアップ処理
     *
     * @param uri バックアップ先URI
     * @return バックアップ完了可否
     */
    private fun backupDatabase(uri: Uri?): Boolean {
        var outputStream: OutputStream? = null
        try {
            // バックアップをする前にDBを閉じる
            db.close()
            // DBバックアップの実施
            val path = context.getDatabasePath("PasswordMemoDB").path
            val file = File(path)
            val inputStream: InputStream = FileInputStream(file)
            outputStream = context.contentResolver.openOutputStream(uri!!)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream!!.write(buf, 0, len)
            }
        } catch (exc: FileNotFoundException) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.backup_db))
                .setMessage(context.getString(R.string.no_access_message))
                .setPositiveButton(R.string.move) { _, _ ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                }
                .setNegativeButton(R.string.end, null)
                .setOnDismissListener { listener.backupComplete() }
                .show()
            return false
        } catch (exc: Exception) {
            val ts = Toast.makeText(
                context,
                context.getString(R.string.db_backup_failed_message),
                Toast.LENGTH_SHORT
            )
            ts.show()
            listener.backupComplete()
            return false
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.backup_db))
            .setMessage(
                uri?.path?.replace(":", "/")
                        + System.getProperty("line.separator")
                        + context.getString(R.string.db_backup_complete_message)
            )
            .setPositiveButton(R.string.ok, null)
            .setOnDismissListener { listener.backupComplete() }
            .show()
        return true
    }
}