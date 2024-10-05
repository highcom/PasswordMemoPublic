package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter
import com.highcom.passwordmemo.ui.viewmodel.SettingViewModel
import com.highcom.passwordmemo.util.BackgroundColorUtil
import com.highcom.passwordmemo.util.BackgroundColorUtil.BackgroundColorListener
import com.highcom.passwordmemo.util.TextSizeUtil
import com.highcom.passwordmemo.util.TextSizeUtil.TextSizeListener
import com.highcom.passwordmemo.util.file.BackupDbFile
import com.highcom.passwordmemo.util.file.InputExternalFile
import com.highcom.passwordmemo.util.file.OutputExternalFile
import com.highcom.passwordmemo.util.file.RestoreDbFile
import com.highcom.passwordmemo.util.file.RestoreDbFile.RestoreDbFileListener
import com.highcom.passwordmemo.util.file.SelectInputOutputFileDialog
import com.highcom.passwordmemo.util.file.SelectInputOutputFileDialog.InputOutputFileDialogListener
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 設定画面アクティビティ
 *
 */
class SettingActivity : AppCompatActivity(), BackgroundColorListener, TextSizeListener,
    InputOutputFileDialogListener, RestoreDbFileListener {
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    /** 処理ハンドラ */
    @Suppress("DEPRECATION")
    private val handler = Handler()
    /** クリップボードコピー設定用リスナー */
    private var copyClipboardSpinner: Spinner? = null
    /** クリップボードコピー設定名 */
    private var copyClipboardNames: ArrayList<String?>? = null
    /** 設定ビューモデル */
    private val settingViewModel: SettingViewModel by viewModels {
        SettingViewModel.Factory((application as PasswordMemoApplication).repository)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        title = getString(R.string.setting)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        loginDataManager = LoginDataManager.getInstance(application)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 背景色を設定する
        (findViewById<View>(R.id.settingView) as ScrollView).setBackgroundColor(
            loginDataManager!!.backgroundColor
        )

        // データ削除スイッチ処理
        val deleteSwitch = findViewById<View>(R.id.deleteSwitch) as Switch
        deleteSwitch.isChecked = loginDataManager!!.deleteSwitchEnable
        deleteSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager!!.setDeleteSwitchEnable(b)
        }

        // 生体認証ログインスイッチ処理
        val biometricLoginSwitch = findViewById<View>(R.id.biometricLoginSwitch) as Switch
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            biometricLoginSwitch.isChecked = false
            biometricLoginSwitch.isEnabled = false
        } else {
            biometricLoginSwitch.isChecked = loginDataManager!!.biometricLoginSwitchEnable
            biometricLoginSwitch.isEnabled = true
        }
        biometricLoginSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager!!.setBiometricLoginSwitchEnable(b)
        }

        // バックグラウンド時の非表示設定
        val displayBackgroundSwitch = findViewById<View>(R.id.displayBackgroundSwitch) as Switch
        displayBackgroundSwitch.isChecked = loginDataManager!!.displayBackgroundSwitchEnable
        displayBackgroundSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager!!.setDisplayBackgroundSwitchEnable(b)
            restartPasswordMemoActivity()
        }

        // メモ表示スイッチ処理
        val memoVisibleSwitch = findViewById<View>(R.id.memoVisibleSwitch) as Switch
        memoVisibleSwitch.isChecked = loginDataManager!!.memoVisibleSwitchEnable
        memoVisibleSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager!!.setMemoVisibleSwitchEnable(b)
        }

        // パスワード表示スイッチ処理
        val passwordVisibleSwitch = findViewById<View>(R.id.passwordVisibleSwitch) as Switch
        passwordVisibleSwitch.isChecked = loginDataManager!!.passwordVisibleSwitchEnable
        passwordVisibleSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager!!.setPasswordVisibleSwitchEnable(b)
        }

        // テキストサイズスピナー処理
        val textSizeSpinner = findViewById<View>(R.id.textSizeSpinner) as Spinner
        val textSizeUtil = TextSizeUtil(applicationContext, this)
        textSizeUtil.createTextSizeSpinner(textSizeSpinner)
        textSizeSpinner.setSelection(textSizeUtil.getSpecifiedValuePosition(loginDataManager!!.textSize))

        // パスワードコピー方法スピナー処理
        copyClipboardSpinner = findViewById(R.id.copyClipboardSpinner)
        copyClipboardNames = ArrayList()
        copyClipboardNames!!.add(getString(R.string.copy_with_longpress))
        copyClipboardNames!!.add(getString(R.string.copy_with_tap))
        val copyClipboardAdapter =
            SetTextSizeAdapter(this, copyClipboardNames, loginDataManager!!.textSize.toInt())
        copyClipboardSpinner?.adapter = copyClipboardAdapter
        copyClipboardSpinner?.setSelection(loginDataManager!!.copyClipboard)
        copyClipboardSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                loginDataManager!!.setCopyClipboard(i)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        // DBバックアップ復元ボタン処理
        val dbBackupBtn = findViewById<View>(R.id.dbBackupButton) as Button
        dbBackupBtn.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.DB_RESTORE_BACKUP) }

        // CSV出力ボタン処理
        val csvOutputBtn = findViewById<View>(R.id.csvOutputButton) as Button
        csvOutputBtn.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.CSV_INPUT_OUTPUT) }

        // 背景色ボタン処理
        val colorSelectBtn = findViewById<View>(R.id.colorSelectButton) as Button
        colorSelectBtn.setOnClickListener { colorSelectDialog() }

        // パスワード設定ボタン処理
        val masterPasswordSetBtn = findViewById<View>(R.id.masterPasswordSetButton) as Button
        masterPasswordSetBtn.setOnClickListener { editMasterPassword() }

        // 操作説明ボタン処理
        val operationInstructionBtn = findViewById<View>(R.id.operationInstructionButton) as Button
        operationInstructionBtn.setOnClickListener { operationInstructionDialog() }

        // このアプリを評価ボタン押下処理
        val rateBtn = findViewById<View>(R.id.RateButton) as Button
        rateBtn.setOnClickListener {
            val uri =
                Uri.parse("https://play.google.com/store/apps/details?id=com.highcom.passwordmemo")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // ライセンスボタン処理
        val licenseBtn = findViewById<View>(R.id.licenseButton) as Button
        licenseBtn.setOnClickListener {
            val intent = Intent(this@SettingActivity, LicenseActivity::class.java)
            startActivity(intent)
        }

        // プライバシーポリシーボタン処理
        val privacyPolicyBtn = findViewById<View>(R.id.PrivacyPolicyButton) as Button
        privacyPolicyBtn.setOnClickListener {
            val uri = Uri.parse("https://high-commu.amebaownd.com/pages/2891722/page_201905200001")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    /**
     * パスワードメモアプリの再起動処理
     *
     */
    private fun restartPasswordMemoActivity() {
        val ts = Toast.makeText(this, getString(R.string.restart_message), Toast.LENGTH_SHORT)
        ts.show()
        val restartRunnable = Runnable { executeRestart() }
        handler.postDelayed(restartRunnable, 500)
    }

    /**
     * 全アクティビティを破棄して新規アクティビティを生成する処理
     *
     */
    private fun executeRestart() {
        val intent = Intent(this, LoginActivity::class.java)
        // 起動しているActivityをすべて削除し、新しいタスクでMainActivityを起動する
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * パスワード変更完了時の通知処理
     *
     */
    private fun passwordChangeComplete() {
        val ts =
            Toast.makeText(this, getString(R.string.password_change_message), Toast.LENGTH_SHORT)
        ts.show()
    }

    /**
     * ファイル入出力選択ダイアログ表示処理
     *
     * @param operation 選択操作
     */
    private fun confirmSelectOperation(operation: SelectInputOutputFileDialog.Operation) {
        val selectInputOutputFileDialog = SelectInputOutputFileDialog(this, operation, this)
        selectInputOutputFileDialog.createOpenFileDialog().show()
    }

    /**
     * 操作選択処理
     *
     * @param path 選択操作名称
     */
    override fun onSelectOperationClicked(path: String?) {
        when (path) {
            getString(R.string.restore_db) -> {
                confirmRestoreSelectFile()
            }
            getString(R.string.backup_db) -> {
                confirmBackupSelectFile()
            }
            getString(R.string.input_csv) -> {
                confirmInputSelectFile()
            }
            getString(R.string.output_csv) -> {
                confirmOutputSelectFile()
            }
        }
    }

    /**
     * DB復元ファイル選択アクティビティ遷移処理
     *
     */
    @Suppress("DEPRECATION")
    private fun confirmRestoreSelectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, RESTORE_DB)
    }

    /**
     * DBバックアップファイル選択アクティビティ遷移処理
     *
     */
    @Suppress("DEPRECATION")
    private fun confirmBackupSelectFile() {
        val fileName = "PasswordMemoDB_$nowDateString.db"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/db"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, BACKUP_DB)
    }

    /**
     * CSV入力ファイル選択アクティビティ遷移処理
     *
     */
    @Suppress("DEPRECATION")
    private fun confirmInputSelectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/*"
        startActivityForResult(intent, INPUT_CSV)
    }

    /**
     * CSV出力ファイル選択アクティビティ遷移処理
     *
     */
    @Suppress("DEPRECATION")
    private fun confirmOutputSelectFile() {
        val fileName = "PasswordListFile_$nowDateString.csv"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, OUTPUT_CSV)
    }

    /** 現在日付 */
    private val nowDateString: String
        @SuppressLint("SimpleDateFormat")
        get() {
            val date = Date()
            val sdf = SimpleDateFormat("yyyyMMdd")
            return sdf.format(date)
        }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK && resultData!!.data != null) {
            val uri = resultData.data
            when (requestCode) {
                // DB復元
                RESTORE_DB -> {
                    val restoreDbFile = RestoreDbFile(this, this)
                    restoreDbFile.restoreSelectFolder(uri)
                }
                // DBバックアップ
                BACKUP_DB -> {
                    val backupDbFile = BackupDbFile(this)
                    backupDbFile.backupSelectFolder(uri)
                }
                // CSV入力
                INPUT_CSV -> {
                    val inputExternalFile = InputExternalFile(this, settingViewModel)
                    inputExternalFile.inputSelectFolder(uri)
                }
                // CSV出力
                OUTPUT_CSV -> {
                    val outputExternalFile = OutputExternalFile(this, settingViewModel)
                    lifecycleScope.launch {
                        outputExternalFile.outputSelectFolder(uri)
                    }
                }
            }
        }
    }

    /**
     * 背景色選択ダイアログ生成処理
     *
     */
    private fun colorSelectDialog() {
        val backgroundColorUtil = BackgroundColorUtil(applicationContext, this)
        backgroundColorUtil.createBackgroundColorDialog(this)
    }

    /**
     * マスターパスワード編集処理
     *
     */
    @SuppressLint("InflateParams")
    private fun editMasterPassword() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.change_master_password)
            .setView(layoutInflater.inflate(R.layout.alert_edit_master_password, null))
            .setPositiveButton(R.string.execute, null)
            .setNegativeButton(R.string.discard, null)
            .create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
            val orgPassword = loginDataManager!!.masterPassword
            val newPassword =
                (alertDialog.findViewById<View>(R.id.editNewMasterPassword) as TextInputEditText).text.toString()
            val newPassword2 =
                (alertDialog.findViewById<View>(R.id.editNewMasterPassword2) as TextInputEditText).text.toString()
            if (newPassword != newPassword2) {
                // 入力内容が異なっていたらエラー
                (alertDialog.findViewById<View>(R.id.inputNavigateText) as TextView).setText(R.string.input_different_message)
                return@OnClickListener
            } else if (newPassword == "") {
                // 入力内容が空ならエラー
                (alertDialog.findViewById<View>(R.id.inputNavigateText) as TextView).setText(R.string.nothing_entered_message)
                return@OnClickListener
            } else if (newPassword == orgPassword) {
                // マスターパスワードと同じならエラー
                (alertDialog.findViewById<View>(R.id.inputNavigateText) as TextView).setText(R.string.password_same_message)
                return@OnClickListener
            } else {
                (alertDialog.findViewById<View>(R.id.inputNavigateText) as TextView).text = " "
                loginDataManager!!.setMasterPassword(newPassword)
                passwordChangeComplete()
                alertDialog.dismiss()
            }
        })
    }

    /**
     * 操作説明ダイアログ表示処理
     *
     */
    @SuppressLint("InflateParams")
    private fun operationInstructionDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.operation_instruction)
            .setView(layoutInflater.inflate(R.layout.alert_operating_instructions, null))
            .setPositiveButton(R.string.close, null)
            .create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener { alertDialog.dismiss() }
    }

    /**
     * 背景色選択処理
     *
     * @param color 選択背景色
     */
    @SuppressLint("ResourceType")
    override fun onSelectColorClicked(color: Int) {
        (findViewById<View>(R.id.settingView) as ScrollView).setBackgroundColor(color)
        loginDataManager!!.setBackgroundColor(color)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * テキストサイズ選択処理
     *
     * @param size 選択テキストサイズ
     */
    override fun onTextSizeSelected(size: Float) {
        setTextSize(size)
        loginDataManager!!.setTextSize(size)
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        // レイアウトが崩れるので詳細説明のテキストはサイズ変更しない
        (findViewById<View>(R.id.deleteSwitch) as Switch).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.biometricLoginSwitch) as Switch).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.displayBackgroundSwitch) as Switch).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.memoVisibleSwitch) as Switch).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.passwordVisibleSwitch) as Switch).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.textSizeView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        // テキストサイズ設定のSpinnerは設定不要
        (findViewById<View>(R.id.copyClipboardView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        val copyClipboardAdapter = SetTextSizeAdapter(this, copyClipboardNames, size.toInt())
        copyClipboardSpinner!!.adapter = copyClipboardAdapter
        copyClipboardSpinner!!.setSelection(loginDataManager!!.copyClipboard)
        (findViewById<View>(R.id.textDbBackupView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.dbBackupButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textCsvOutputView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.csvOutputButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textColorSelectView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.colorSelectButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textMasterPasswordView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.masterPasswordSetButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textOperationInstructionView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.operationInstructionButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textRateView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.RateButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textLicenseView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.licenseButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.textPrivacyPolicyView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.PrivacyPolicyButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
    }

    /**
     * DB復元完了時処理
     *
     */
    override fun restoreComplete() {
        // データベースとリポジトリを初期化する
        (application as PasswordMemoApplication).initializeDatabaseAndRepository()
        // 起動しているActivityをすべて削除し、新しいタスクでLoginActivityを起動する
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    public override fun onDestroy() {
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

    companion object {
        /** DB復元操作 */
        private const val RESTORE_DB = 1001
        /** DBバックアップ操作 */
        private const val BACKUP_DB = 1002
        /** CSV入力操作 */
        private const val INPUT_CSV = 1003
        /** CSV復元操作 */
        private const val OUTPUT_CSV = 1004
    }
}