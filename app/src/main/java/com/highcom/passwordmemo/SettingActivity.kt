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
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import com.highcom.passwordmemo.util.BackgroundColorUtil
import com.highcom.passwordmemo.util.BackgroundColorUtil.BackgroundColorListener
import com.highcom.passwordmemo.util.TextSizeUtil
import com.highcom.passwordmemo.util.TextSizeUtil.TextSizeListener
import com.highcom.passwordmemo.util.file.BackupDbFile
import com.highcom.passwordmemo.util.file.InputExternalFile
import com.highcom.passwordmemo.util.file.InputExternalFile.InputExternalFileListener
import com.highcom.passwordmemo.util.file.OutputExternalFile
import com.highcom.passwordmemo.util.file.RestoreDbFile
import com.highcom.passwordmemo.util.file.RestoreDbFile.RestoreDbFileListener
import com.highcom.passwordmemo.util.file.SelectInputOutputFileDialog
import com.highcom.passwordmemo.util.file.SelectInputOutputFileDialog.InputOutputFileDialogListener
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class SettingActivity : AppCompatActivity(), BackgroundColorListener, TextSizeListener,
    InputOutputFileDialogListener, RestoreDbFileListener, InputExternalFileListener {
    private var loginDataManager: LoginDataManager? = null
    private val handler = Handler()
    var copyClipboardSpinner: Spinner? = null
    var copyClipboardNames: ArrayList<String?>? = null

    private val passwordListViewModel: PasswordListViewModel by viewModels {
        PasswordListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((application as PasswordMemoApplication).repository)
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        title = getString(R.string.setting)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        loginDataManager = LoginDataManager.Companion.getInstance(this)

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
        deleteSwitch.setOnCheckedChangeListener { compoundButton, b ->
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
        biometricLoginSwitch.setOnCheckedChangeListener { compoundButton, b ->
            loginDataManager!!.setBiometricLoginSwitchEnable(b)
        }

        // バックグラウンド時の非表示設定
        val displayBackgroundSwitch = findViewById<View>(R.id.displayBackgroundSwitch) as Switch
        displayBackgroundSwitch.isChecked = loginDataManager!!.displayBackgroundSwitchEnable
        displayBackgroundSwitch.setOnCheckedChangeListener { compoundButton, b ->
            loginDataManager!!.setDisplayBackgroundSwitchEnable(b)
            restartPasswordMemoActivity()
        }

        // メモ表示スイッチ処理
        val memoVisibleSwitch = findViewById<View>(R.id.memoVisibleSwitch) as Switch
        memoVisibleSwitch.isChecked = loginDataManager!!.memoVisibleSwitchEnable
        memoVisibleSwitch.setOnCheckedChangeListener { compoundButton, b ->
            loginDataManager!!.setMemoVisibleSwitchEnable(b)
        }

        // パスワード表示スイッチ処理
        val passwordVisibleSwitch = findViewById<View>(R.id.passwordVisibleSwitch) as Switch
        passwordVisibleSwitch.isChecked = loginDataManager!!.passwordVisibleSwitchEnable
        passwordVisibleSwitch.setOnCheckedChangeListener { compoundButton, b ->
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
        copyClipboardSpinner?.setAdapter(copyClipboardAdapter)
        copyClipboardSpinner?.setSelection(loginDataManager!!.copyClipboard)
        copyClipboardSpinner?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                loginDataManager!!.setCopyClipboard(i)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        })

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
        val PrivacyPolicyBtn = findViewById<View>(R.id.PrivacyPolicyButton) as Button
        PrivacyPolicyBtn.setOnClickListener {
            val uri = Uri.parse("https://high-commu.amebaownd.com/pages/2891722/page_201905200001")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    private fun restartPasswordMemoActivity() {
        val ts = Toast.makeText(this, getString(R.string.restart_message), Toast.LENGTH_SHORT)
        ts.show()
        val restartRunnable = Runnable { executeRestart() }
        handler.postDelayed(restartRunnable, 500)
    }

    private fun executeRestart() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK // 起動しているActivityをすべて削除し、新しいタスクでMainActivityを起動する
        startActivity(intent)
    }

    // パスワード変更完了時の表示
    private fun passwordChangeComplete() {
        val ts =
            Toast.makeText(this, getString(R.string.password_change_message), Toast.LENGTH_SHORT)
        ts.show()
    }

    private fun confirmSelectOperation(operation: SelectInputOutputFileDialog.Operation) {
        val selectInputOutputFileDialog = SelectInputOutputFileDialog(this, operation, this)
        selectInputOutputFileDialog.createOpenFileDialog().show()
    }

    override fun onSelectOperationClicked(path: String?) {
        if (path == getString(R.string.restore_db)) {
            confirmRestoreSelectFile()
        } else if (path == getString(R.string.backup_db)) {
            confirmBackupSelectFile()
        } else if (path == getString(R.string.input_csv)) {
            confirmInputSelectFile()
        } else if (path == getString(R.string.output_csv)) {
            confirmOutputSelectFile()
        }
    }

    private fun confirmRestoreSelectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, RESTORE_DB)
    }

    private fun confirmBackupSelectFile() {
        val fileName = "PasswordMemoDB_" + nowDateString + ".db"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/db"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, BACKUP_DB)
    }

    private fun confirmInputSelectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/*"
        startActivityForResult(intent, INPUT_CSV)
    }

    private fun confirmOutputSelectFile() {
        val fileName = "PasswordListFile_" + nowDateString + ".csv"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, OUTPUT_CSV)
    }

    private val nowDateString: String
        private get() {
            val date = Date()
            val sdf = SimpleDateFormat("yyyyMMdd")
            return sdf.format(date)
        }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK && resultData!!.data != null) {
            val uri = resultData.data
            when (requestCode) {
                RESTORE_DB -> {
                    val restoreDbFile = RestoreDbFile(this, this)
                    restoreDbFile.restoreSelectFolder(uri)
                }

                BACKUP_DB -> {
                    val backupDbFile = BackupDbFile(this)
                    backupDbFile.backupSelectFolder(uri)
                }

                INPUT_CSV -> {
                    val inputExternalFile = InputExternalFile(this, passwordListViewModel, groupListViewModel, this)
                    inputExternalFile.inputSelectFolder(uri)
                }

                OUTPUT_CSV -> {
                    val outputExternalFile = OutputExternalFile(this, passwordListViewModel, groupListViewModel)
                    lifecycleScope.launch {
                        outputExternalFile.outputSelectFolder(uri)
                    }
                }
            }
        }
    }

    private fun colorSelectDialog() {
        val backgroundColorUtil = BackgroundColorUtil(applicationContext, this)
        backgroundColorUtil.createBackgroundColorDialog(this)
    }

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

    override fun onTextSizeSelected(size: Float) {
        setTextSize(size)
        loginDataManager!!.setTextSize(size)
    }

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

    override fun restoreComplete() {
        // データベースとリポジトリを初期化する
        (application as PasswordMemoApplication).initializeDatabaseAndRepository()
        // 起動しているActivityをすべて削除し、新しいタスクでLoginActivityを起動する
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun importComplete() {
        setResult(NEED_UPDATE)
    }

    public override fun onDestroy() {
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

    companion object {
        const val NEED_UPDATE = 1
        private const val RESTORE_DB = 1001
        private const val BACKUP_DB = 1002
        private const val INPUT_CSV = 1003
        private const val OUTPUT_CSV = 1004
    }
}