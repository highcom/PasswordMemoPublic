package com.highcom.passwordmemo.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.FragmentSettingBinding
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter
import com.highcom.passwordmemo.ui.viewmodel.SettingViewModel
import com.highcom.passwordmemo.util.BackgroundColorUtil
import com.highcom.passwordmemo.util.TextSizeUtil
import com.highcom.passwordmemo.util.file.BackupDbFile
import com.highcom.passwordmemo.util.file.InputExternalFile
import com.highcom.passwordmemo.util.file.OutputExternalFile
import com.highcom.passwordmemo.util.file.RestoreDbFile
import com.highcom.passwordmemo.util.file.SelectInputOutputFileDialog
import com.highcom.passwordmemo.util.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

/**
 * 設定画面フラグメント
 *
 */
@AndroidEntryPoint
class SettingFragment : Fragment(), BackgroundColorUtil.BackgroundColorListener,
    TextSizeUtil.TextSizeListener, SelectInputOutputFileDialog.InputOutputFileDialogListener,
    RestoreDbFile.RestoreDbFileListener, EditMasterPasswordDialogFragment.EditMasterPasswordListener {
    /** 設定画面のbinding */
    private lateinit var binding: FragmentSettingBinding
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** 処理ハンドラ */
    @Suppress("DEPRECATION")
    private val handler = Handler()
    /** クリップボードコピー設定用リスナー */
    private var copyClipboardSpinner: Spinner? = null
    /** クリップボードコピー設定名 */
    private var copyClipboardNames: ArrayList<String?>? = null
    /** 設定ビューモデル */
    private val settingViewModel: SettingViewModel by viewModels()

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragmentのメニューを有効にする
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater)
        return binding.root
    }

    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType", "UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.setting)
        // ActionBarに戻るボタンを設定
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 背景色を設定する
        binding.settingView.setBackgroundColor(
            loginDataManager.backgroundColor
        )

        // データ削除スイッチ処理
        val deleteSwitch = binding.deleteSwitch
        deleteSwitch.isChecked = loginDataManager.deleteSwitchEnable
        deleteSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager.setDeleteSwitchEnable(b)
        }

        // 生体認証ログインスイッチ処理
        val biometricLoginSwitch = binding.biometricLoginSwitch
        val biometricManager = BiometricManager.from(requireContext())
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            biometricLoginSwitch.isChecked = false
            biometricLoginSwitch.isEnabled = false
        } else {
            biometricLoginSwitch.isChecked = loginDataManager.biometricLoginSwitchEnable
            biometricLoginSwitch.isEnabled = true
        }
        biometricLoginSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager.setBiometricLoginSwitchEnable(b)
        }

        // バックグラウンド時の非表示設定
        val displayBackgroundSwitch = binding.displayBackgroundSwitch
        displayBackgroundSwitch.isChecked = loginDataManager.displayBackgroundSwitchEnable
        displayBackgroundSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager.setDisplayBackgroundSwitchEnable(b)
            restartPasswordMemoActivity()
        }

        // メモ表示スイッチ処理
        val memoVisibleSwitch = binding.memoVisibleSwitch
        memoVisibleSwitch.isChecked = loginDataManager.memoVisibleSwitchEnable
        memoVisibleSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager.setMemoVisibleSwitchEnable(b)
        }

        // パスワード表示スイッチ処理
        val passwordVisibleSwitch = binding.passwordVisibleSwitch
        passwordVisibleSwitch.isChecked = loginDataManager.passwordVisibleSwitchEnable
        passwordVisibleSwitch.setOnCheckedChangeListener { _, b ->
            loginDataManager.setPasswordVisibleSwitchEnable(b)
        }

        // テキストサイズスピナー処理
        val textSizeSpinner = binding.textSizeSpinner
        val textSizeUtil = TextSizeUtil(requireContext(), this)
        textSizeUtil.createTextSizeSpinner(textSizeSpinner)
        textSizeSpinner.setSelection(textSizeUtil.getSpecifiedValuePosition(loginDataManager.textSize))

        // パスワードコピー方法スピナー処理
        copyClipboardSpinner = binding.copyClipboardSpinner
        copyClipboardNames = ArrayList()
        copyClipboardNames?.add(getString(R.string.copy_with_longpress))
        copyClipboardNames?.add(getString(R.string.copy_with_tap))
        val copyClipboardAdapter =
            SetTextSizeAdapter(requireContext(), copyClipboardNames, loginDataManager.textSize.toInt())
        copyClipboardSpinner?.adapter = copyClipboardAdapter
        copyClipboardSpinner?.setSelection(loginDataManager.copyClipboard)
        copyClipboardSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                loginDataManager.setCopyClipboard(i)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        // DBバックアップ復元ボタン処理
        binding.dbBackupButton.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.DB_RESTORE_BACKUP) }

        // CSV出力ボタン処理
        binding.csvOutputButton.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.CSV_INPUT_OUTPUT) }

        // 背景色ボタン処理
        binding.colorSelectButton.setOnClickListener { colorSelectDialog() }

        // パスワード設定ボタン処理
        binding.masterPasswordSetButton.setOnClickListener { editMasterPassword() }

        // 操作説明ボタン処理
        binding.operationInstructionButton.setOnClickListener { operationInstructionDialog() }

        // このアプリを評価ボタン押下処理
        binding.rateButton.setOnClickListener {
            val uri =
                Uri.parse("https://play.google.com/store/apps/details?id=com.highcom.passwordmemo")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // ライセンスボタン処理
        binding.licenseButton.setOnClickListener {
            findNavController().navigate(SettingFragmentDirections.actionSettingFragmentToLicenseFragment())
        }

        // プライバシーポリシーボタン処理
        binding.privacyPolicyButton.setOnClickListener {
            val uri = Uri.parse("https://high-commu.amebaownd.com/pages/2891722/page_201905200001")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // テキストサイズを設定する
        setTextSize(loginDataManager.textSize)
    }

    /**
     * パスワードメモアプリの再起動処理
     *
     */
    private fun restartPasswordMemoActivity() {
        val ts = Toast.makeText(requireContext(), getString(R.string.restart_message), Toast.LENGTH_SHORT)
        ts.show()
        val restartRunnable = Runnable { executeRestart() }
        handler.postDelayed(restartRunnable, 500)
    }

    /**
     * 全アクティビティを破棄して新規アクティビティを生成する処理
     *
     */
    private fun executeRestart() {
        val intent = Intent(requireContext(), PasswordMemoActivity::class.java)
        // 起動しているActivityをすべて削除し、新しいタスクでMainActivityを起動する
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * パスワード変更完了時の通知処理
     *
     */
    override fun passwordChangeComplete() {
        val ts = Toast.makeText(requireContext(), getString(R.string.password_change_message), Toast.LENGTH_SHORT)
        ts.show()
    }

    /**
     * ファイル入出力選択ダイアログ表示処理
     *
     * @param operation 選択操作
     */
    private fun confirmSelectOperation(operation: SelectInputOutputFileDialog.Operation) {
        val selectInputOutputFileDialog = SelectInputOutputFileDialog(requireContext(), operation, this)
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == AppCompatActivity.RESULT_OK && resultData!!.data != null) {
            val uri = resultData.data
            when (requestCode) {
                // DB復元
                RESTORE_DB -> {
                    val restoreDbFile = RestoreDbFile(requireActivity(), this)
                    restoreDbFile.restoreSelectFolder(uri)
                }
                // DBバックアップ
                BACKUP_DB -> {
                    val backupDbFile = BackupDbFile(requireContext())
                    backupDbFile.backupSelectFolder(uri)
                }
                // CSV入力
                INPUT_CSV -> {
                    val inputExternalFile = InputExternalFile(requireActivity(), settingViewModel)
                    inputExternalFile.inputSelectFolder(uri)
                }
                // CSV出力
                OUTPUT_CSV -> {
                    val outputExternalFile = OutputExternalFile(requireContext(), settingViewModel)
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
        val backgroundColorUtil = BackgroundColorUtil(requireContext(), this)
        backgroundColorUtil.createBackgroundColorDialog(requireActivity())
    }

    /**
     * マスターパスワード編集処理
     *
     */
    private fun editMasterPassword() {
        val dialog = EditMasterPasswordDialogFragment(loginDataManager)
        dialog.show(childFragmentManager, "EditMasterPasswordDialog")
    }

    /**
     * 操作説明ダイアログ表示処理
     *
     */
    @SuppressLint("InflateParams")
    private fun operationInstructionDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
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
        binding.settingView.setBackgroundColor(color)
        loginDataManager.setBackgroundColor(color)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> findNavController().navigate(SettingFragmentDirections.actionSettingFragmentToPasswordListFragment())
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
        loginDataManager.setTextSize(size)
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        // レイアウトが崩れるので詳細説明のテキストはサイズ変更しない
        binding.deleteSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.biometricLoginSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.displayBackgroundSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.memoVisibleSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.passwordVisibleSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.textSizeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        // テキストサイズ設定のSpinnerは設定不要
        binding.copyClipboardView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        val copyClipboardAdapter = SetTextSizeAdapter(requireContext(), copyClipboardNames, size.toInt())
        copyClipboardSpinner?.adapter = copyClipboardAdapter
        copyClipboardSpinner?.setSelection(loginDataManager.copyClipboard)
        binding.textDbBackupView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.dbBackupButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textCsvOutputView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.csvOutputButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textColorSelectView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.colorSelectButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textMasterPasswordView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.masterPasswordSetButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textOperationInstructionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.operationInstructionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textRateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.rateButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textLicenseView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.licenseButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textPrivacyPolicyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.privacyPolicyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
    }

    /**
     * DB復元完了時処理
     *
     */
    override fun restoreComplete() {
        // 起動しているActivityをすべて削除し、新しいタスクでActivityを起動する
        // Hilt用モジュールがActivityRetainedComponentなので、データベースとリポジトリも再読み込みされる
        val intent = Intent(requireContext(), PasswordMemoActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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