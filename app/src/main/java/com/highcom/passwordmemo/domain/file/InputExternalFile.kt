package com.highcom.passwordmemo.domain.file

import android.annotation.SuppressLint
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
import com.highcom.passwordmemo.databinding.AlertProgressbarBinding
import com.highcom.passwordmemo.ui.viewmodel.SettingViewModel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.concurrent.Executors

/**
 * 外部ファイル取込処理クラス
 * * CSV形式のファイルを取り込む
 *
 * @property activity ダイアログ表示用アクティビティ
 * @property settingViewModel ビューモデル
 */
class InputExternalFile(private val activity: Activity, private val settingViewModel: SettingViewModel) {
    /** パスワード一覧データ */
    private var passwordList: MutableList<PasswordEntity>? = null
    /** グループ一覧データ */
    private var groupList: MutableList<GroupEntity>? = null
    /** パスワードデータID */
    private var id = 0L
    /** 取込ファイルURI */
    private var uri: Uri? = null
    /** 取込処理中のプログレスダイアログ */
    private var progressAlertDialog: AlertDialog? = null
    /** 取込処理中のプログレスバー */
    private var progressBar: ProgressBar? = null
    /** 上書きモードか */
    private var isOverride = false

    /**
     * ファイル取込処理用バックグラウンドタスク
     *
     * @property _handler 処理ハンドラ
     */
    private inner class BackgroundTask(private val _handler: Handler) : Runnable {
        /**
         * ファイル取込実行処理
         * 既存のデータは全て削除してCSVから読み込んだデータを登録する
         *
         */
        @WorkerThread
        override fun run() {
            if (isOverride) {
                settingViewModel.reInsertPassword(passwordList!!)
                progressBar!!.progress = 50
                settingViewModel.reInsertGroup(groupList!!)
            } else {
                settingViewModel.insertPassword(passwordList!!)
                progressBar!!.progress = 50
                settingViewModel.insertGroup(groupList!!)
            }
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
            val title = if (isOverride) activity.getString(R.string.input_csv_override) else activity.getString(R.string.input_csv_add)
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(
                    activity.getString(R.string.csv_input_complete_message) + System.getProperty(
                        "line.separator"
                    ) + getFileNameByUri(activity, uri)
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    /**
     * CSVファイル取込元フォルダ選択確認ダイアログ表示処理
     *
     * @param uri 取込元ファイルURI
     * @param isOverride 上書きモードか
     * @param isChromeCsv ChromeCSVか
     */
    fun confirmInputDialog(uri: Uri?, isOverride: Boolean, isChromeCsv: Boolean) {
        this.uri = uri
        this.isOverride = isOverride
        val title: String
        val messageFront: String
        val messageRear: String
        if (isOverride) {
            title = activity.getString(R.string.input_csv_override)
            messageFront = activity.getString(R.string.input_override_message_front)
            messageRear = activity.getString(R.string.input_override_message_rear)
        } else {
            title = activity.getString(R.string.input_csv_add)
            messageFront = activity.getString(R.string.input_add_message_front)
            messageRear = activity.getString(R.string.input_add_message_rear)
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(
                messageFront + getFileNameByUri(
                    activity,
                    uri
                ) + System.getProperty("line.separator") + messageRear

            )
            .setPositiveButton(R.string.input_button) { _, _ ->
                if (importDatabase(uri, isChromeCsv)) {
                    execImportDatabase()
                } else {
                    failedImportDatabase(isChromeCsv)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * CSVファイル取込処理
     * * CSVファイルを1行ずつ読み取りエンティティデータへ変換してリストデータを作成する
     *
     * @param uri 取込元ファイルURI
     * @param isChromeCsv ChromeCSVか
     * @return 取込完了可否
     */
    @SuppressLint("SimpleDateFormat")
    @Suppress("KotlinConstantConditions")
    private fun importDatabase(uri: Uri?, isChromeCsv: Boolean): Boolean {
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
                name = activity.getString(R.string.list_title),
                color = 0
            ))
            id = HEADER_RECORD
            while (reader.readLine().also { line = it ?: "" } != null) {
                val result = if (isChromeCsv) line.split(",".toRegex()).toTypedArray() else line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // ヘッダが正しく設定されているか
                if (isChromeCsv) {
                    // カラム数が合っていなかった場合終了
                    if (result.size != COLUMN_COUNT_5) return false
                    if (!isHeaderCorrect && result.size == COLUMN_COUNT_5 &&
                        result[0] == "name" && result[1] == "url" && result[2] == "username" && result[3] == "password" && result[4] == "note") {
                        isHeaderCorrect = true
                        columnCount = COLUMN_COUNT_5
                        continue
                    }
                } else {
                    // カラム数が合っていなかった場合終了
                    if (result.size != COLUMN_COUNT_6 && result.size != COLUMN_COUNT_7 && result.size != COLUMN_COUNT_9) return false
                    if (!isHeaderCorrect && result.size == COLUMN_COUNT_6 &&
                        result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "MEMO" && result[5] == "INPUTDATE") {
                        // バージョン2系の場合
                        isHeaderCorrect = true
                        columnCount = COLUMN_COUNT_6
                        continue
                    } else if (!isHeaderCorrect && result.size == COLUMN_COUNT_7 &&
                        result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "GROUP" && result[5] == "MEMO" && result[6] == "INPUTDATE") {
                        // バージョン3系4系の場合
                        isHeaderCorrect = true
                        columnCount = COLUMN_COUNT_7
                        continue
                    } else if (!isHeaderCorrect && result.size == COLUMN_COUNT_9 &&
                        result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "GROUP" && result[5] == "MEMO" && result[6] == "INPUTDATE" && result[7] == "GCOLOR" && result[8] == "PCOLOR") {
                        // バージョン5系の場合
                        isHeaderCorrect = true
                        columnCount = COLUMN_COUNT_9
                        continue
                    }
                }
                // 入力最大レコード数を10000件とする
                if (id > MAX_RECORD) return false

                if (isHeaderCorrect && isChromeCsv) {
                    val date = Date()
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    val passwordEntity = PasswordEntity(
                        id = if (isOverride) id else 0,
                        title = result[0],
                        account = result[2],
                        password = result[3],
                        url = result[1],
                        groupId = 1,
                        memo = result[4],
                        inputDate = sdf.format(date),
                        color = 0
                    )
                    passwordList?.add(passwordEntity)
                } else if (isHeaderCorrect && columnCount == COLUMN_COUNT_6) {
                    val passwordEntity = PasswordEntity(
                        id = if (isOverride) id else 0,
                        title = result[0],
                        account = result[1],
                        password = result[2],
                        url = result[3],
                        groupId = 1,
                        memo = result[4],
                        inputDate = result[5],
                        color = 0
                    )
                    passwordList?.add(passwordEntity)
                } else if (isHeaderCorrect && columnCount == COLUMN_COUNT_7) {
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
                                name = result[4],
                                color = 0
                            )
                            groupList?.add(groupEntity)
                        }
                    }
                    val passwordEntity = PasswordEntity(
                        id = if (isOverride) id else 0,
                        title = result[0],
                        account = result[1],
                        password = result[2],
                        url = result[3],
                        groupId = groupId,
                        memo = result[5].replace("  ", "\n"),
                        inputDate = result[6],
                        color = 0
                    )
                    passwordList?.add(passwordEntity)
                } else if (isHeaderCorrect && columnCount == COLUMN_COUNT_9) {
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
                                name = result[4],
                                color = result[7].toInt(),
                            )
                            groupList?.add(groupEntity)
                        }
                    }
                    val passwordEntity = PasswordEntity(
                        id = if (isOverride) id else 0,
                        title = result[0],
                        account = result[1],
                        password = result[2],
                        url = result[3],
                        groupId = groupId,
                        memo = result[5].replace("  ", "\n"),
                        inputDate = result[6],
                        color = result[8].toInt()
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

    /**
     * CSVファイル取込実行処理
     * * エンティティへ変換されたデータリストをDBへ取込を実行する
     *
     */
    @SuppressLint("InflateParams")
    private fun execImportDatabase() {
        val title: String
        val message: String
        if (isOverride) {
            title = activity.getString(R.string.input_csv_override)
            message = activity.getString(R.string.csv_input_override_confirm_message)
        } else {
            title = activity.getString(R.string.input_csv_add)
            message = activity.getString(R.string.csv_input_add_confirm_message)
        }

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.execute) { _, _ -> // 取込み中のプログレスバーを表示する
                val binding = AlertProgressbarBinding.inflate(activity.layoutInflater)
                progressAlertDialog = AlertDialog.Builder(activity)
                    .setTitle(R.string.csv_input_processing)
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
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * CSVファイル取込失敗ダイアログ表示処理
     *
     */
    private fun failedImportDatabase(isChromeCsv: Boolean) {
        val title = if (isOverride) activity.getString(R.string.input_csv_override) else activity.getString(R.string.input_csv_add)
        if (id == HEADER_RECORD) {
            val failedHeaderMessage = if (isChromeCsv) activity.getString(R.string.csv_input_failed_chrome_header_message) else activity.getString(R.string.csv_input_failed_header_message)
            // ヘッダが正しくないエラーを表示する
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(failedHeaderMessage)
                .setPositiveButton(R.string.ok, null)
                .show()
        } else if (id > MAX_RECORD) {
            // 入力上限を超えたエラーを表示する
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(activity.getString(R.string.csv_input_failed_counts_message))
                .setPositiveButton(R.string.ok, null)
                .show()
        } else {
            // 指定行がエラーであるエラーを表示する
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(activity.getString(R.string.csv_input_failed_body_message) + id)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    /**
     * URIからファイルパス名を取得する処理
     *
     * @param activity コンテキスト
     * @param uri ファイルURI
     * @return ファイルパス名
     */
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
        /** ヘッダーレコード行 */
        private const val HEADER_RECORD = 1L
        /** 取込最大レコード数 */
        private const val MAX_RECORD = 10000
        /** カラム数が5 */
        private const val COLUMN_COUNT_5 = 5
        /** カラム数が6 */
        private const val COLUMN_COUNT_6 = 6
        /** カラム数が7 */
        private const val COLUMN_COUNT_7 = 7
        /** カラム数が8 */
        private const val COLUMN_COUNT_9 = 9
    }
}