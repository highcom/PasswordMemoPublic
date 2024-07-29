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
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.Objects
import java.util.concurrent.Executors

class InputExternalFile(private val activity: Activity,
                        private val passwordListViewModel: PasswordListViewModel,
                        private val groupListViewModel: GroupListViewModel,
                        private val listener: InputExternalFileListener) {
    private var passwordList: MutableList<PasswordEntity>? = null
    private var groupList: MutableList<GroupEntity>? = null
    private var id = 0L
    private var uri: Uri? = null
    private var progressAlertDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null

    interface InputExternalFileListener {
        fun importComplete()
    }

    private inner class BackgroundTask(private val _handler: Handler) : Runnable {
        @WorkerThread
        override fun run() {
            // 既存のデータは全て削除してCSVから読み込んだデータを登録する
            passwordListViewModel.reInsert(passwordList!!)
            progressBar!!.progress = 50
            groupListViewModel.reInsert(groupList!!)
            progressBar!!.progress = 100
            val postExecutor = PostExecutor()
            _handler.post(postExecutor)
        }
    }

    private inner class PostExecutor : Runnable {
        @UiThread
        override fun run() {
            progressAlertDialog!!.dismiss()
            listener.importComplete()
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.input_csv))
                .setMessage(
                    activity.getString(R.string.csv_input_complete_message) + System.getProperty(
                        "line.separator"
                    ) + getFileNameByUri(activity, uri)
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    fun inputSelectFolder(uri: Uri?) {
        this.uri = uri
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.input_csv))
            .setMessage(
                activity.getString(R.string.input_message_front) + getFileNameByUri(
                    activity,
                    uri
                ) + System.getProperty("line.separator") + activity.getString(R.string.input_message_rear)
            )
            .setPositiveButton(R.string.input_button) { dialog, which ->
                if (importDatabase(uri)) {
                    execImportDatabase()
                } else {
                    failedImportDatabase()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importDatabase(uri: Uri?): Boolean {
        var file: File
        val printWriter: PrintWriter? = null
        var inputStream: InputStream? = null
        try {
            // 文字コードを判定し、判定できなければデフォルトをutf8とする
            val fd = FileCharDetector(activity, uri)
            var encType = fd.detect()
            if (encType == null) encType = "UTF-8"

            // 判定された文字コードを指定してファイル読み込みを行う
            inputStream = activity.contentResolver.openInputStream(uri!!)
            val reader =
                BufferedReader(InputStreamReader(Objects.requireNonNull(inputStream), encType))
            var line: String
            var isHeaderCorrect = false
            var columnCount = 0
            passwordList = ArrayList()
            groupList = ArrayList()
            groupList?.add(GroupEntity(
                groupId = 1L,
                groupOrder = 1,
                name = activity.getString(R.string.list_title)
            ))
            id = HEADER_RECORD
            while (reader.readLine().also { line = it ?: "" } != null) {
                val result = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // カラム数が合っていなかった場合終了
                if (result.size != 6 && result.size != 7) return false
                // 入力最大レコード数を30000件とする
                if (id > MAX_RECORD) return false
                // ヘッダが正しく設定されているか
                if (!isHeaderCorrect && result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "MEMO" && result[5] == "INPUTDATE") {
                    // バージョン2系の場合
                    isHeaderCorrect = true
                    columnCount = 6
                    continue
                } else if (!isHeaderCorrect && result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "GROUP" && result[5] == "MEMO" && result[6] == "INPUTDATE") {
                    // バージョン3系の場合
                    isHeaderCorrect = true
                    columnCount = 7
                    continue
                }
                if (isHeaderCorrect && columnCount == 6) {
                    val passwordEntity = PasswordEntity(
                        id = id,
                        title = result[0],
                        account = result[1],
                        password = result[2],
                        url = result[3],
                        groupId = 1,
                        memo = result[4],
                        inputDate = result[5]
                    )
                    passwordList?.add(passwordEntity)
                } else if (isHeaderCorrect && columnCount == 7) {
                    var groupId = 0L
                    for (entity in groupList!!) {
                        if (entity.name == result[4]) {
                            groupId = entity.groupId
                        }
                    }
                    // 登録されているグループ名と一致するものが無かったら追加登録
                    if (groupId == 0L) {
                        groupList?.size?.let {
                            groupId = it.plus(1).toLong()
                            val groupEntity = GroupEntity(
                                groupId = it.plus(1).toLong(),
                                groupOrder = it.plus(1),
                                name = result[4]
                            )
                            groupList?.add(groupEntity)
                        }
                    }
                    val passwordEntity = PasswordEntity(
                        id = id,
                        title = result[0],
                        account = result[1],
                        password = result[2],
                        url = result[3],
                        groupId = groupId,
                        memo = result[5].replace("  ", "\n"),
                        inputDate = result[6]
                    )
                    passwordList?.add(passwordEntity)
                }
                id++
            }

            // ヘッダが正しく設定されていなければ取り込みを行わない
            if (!isHeaderCorrect) return false
        } catch (exc: Exception) {
            val ts = Toast.makeText(
                activity,
                activity.getString(R.string.csv_input_failed_message),
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

    private fun execImportDatabase() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.input_csv))
            .setMessage(activity.getString(R.string.csv_input_confirm_message))
            .setPositiveButton(R.string.execute) { dialog, which -> // 取込み中のプログレスバーを表示する
                progressAlertDialog = AlertDialog.Builder(activity)
                    .setTitle(R.string.csv_input_processing)
                    .setView(activity.layoutInflater.inflate(R.layout.alert_progressbar, null))
                    .create()
                progressAlertDialog?.show()
                progressAlertDialog?.setCancelable(false)
                progressAlertDialog?.setCanceledOnTouchOutside(false)
                progressBar = progressAlertDialog?.findViewById(R.id.ProgressBarHorizontal)

                // ワーカースレッドで取込みを開始する
                val mainLooper = Looper.getMainLooper()
                val handler = HandlerCompat.createAsync(mainLooper)
                val backgroundTask: BackgroundTask = BackgroundTask(handler)
                val executorService = Executors.newSingleThreadExecutor()
                executorService.submit(backgroundTask)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun failedImportDatabase() {
        if (id == HEADER_RECORD) {
            // ヘッダが正しくないエラーを表示する
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.input_csv))
                .setMessage(activity.getString(R.string.csv_input_failed_header_message))
                .setPositiveButton(R.string.ok, null)
                .show()
        } else if (id > MAX_RECORD) {
            // 入力上限を超えたエラーを表示する
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.input_csv))
                .setMessage(activity.getString(R.string.csv_input_failed_counts_message))
                .setPositiveButton(R.string.ok, null)
                .show()
        } else {
            // 指定行がエラーであるエラーを表示する
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.input_csv))
                .setMessage(activity.getString(R.string.csv_input_failed_body_message) + id)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun getFileNameByUri(activity: Context, uri: Uri?): String {
        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = activity.contentResolver
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

    companion object {
        private const val HEADER_RECORD = 1L
        private const val MAX_RECORD = 10000
    }
}