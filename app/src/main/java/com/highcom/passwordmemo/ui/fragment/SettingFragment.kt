package com.highcom.passwordmemo.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase
import com.highcom.passwordmemo.databinding.FragmentSettingBinding
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter
import com.highcom.passwordmemo.ui.list.ColorList
import com.highcom.passwordmemo.ui.viewmodel.SettingViewModel
import com.highcom.passwordmemo.ui.viewmodel.BillingViewModel
import com.highcom.passwordmemo.domain.SelectColorUtil
import com.highcom.passwordmemo.domain.TextSizeUtil
import com.highcom.passwordmemo.domain.file.BackupDbFile
import com.highcom.passwordmemo.domain.file.InputExternalFile
import com.highcom.passwordmemo.domain.file.OutputExternalFile
import com.highcom.passwordmemo.domain.file.RestoreDbFile
import com.highcom.passwordmemo.domain.file.SelectInputOutputFileDialog
import com.highcom.passwordmemo.domain.DarkModeUtil
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * 設定画面フラグメント
 *
 */
@AndroidEntryPoint
class SettingFragment : Fragment(), SelectColorUtil.SelectColorListener,
    TextSizeUtil.TextSizeListener, SelectInputOutputFileDialog.InputOutputFileDialogListener,
    RestoreDbFile.RestoreDbFileListener, BackupDbFile.BackupDbFileListener,
    EditMasterPasswordDialogFragment.EditMasterPasswordListener {
    /** パスワードメモRoomデータベース */
    @Inject
    lateinit var db: PasswordMemoRoomDatabase
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
    /** ダークモード設定用スピナー */
    private var darkModeSpinner: Spinner? = null
    /** ダークモード設定名 */
    private var darkModeNames: ArrayList<String?>? = null
    /** 設定ビューモデル */
    private val settingViewModel: SettingViewModel by viewModels()
    /** 課金ビューモデル */
    private val billingViewModel: BillingViewModel by activityViewModels()
    private var selectedCsvInputMode: String? = null
    private var selectedOperation: SelectInputOutputFileDialog.Operation? = null

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

    @RequiresApi(Build.VERSION_CODES.M)
    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType", "UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.setting)
        // ActionBarに戻るボタンを設定
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) {
            activity.drawerMenuDisabled()
            activity.toggle.setToolbarNavigationClickListener {
                findNavController().navigate(SettingFragmentDirections.actionSettingFragmentToPasswordListFragment())
            }
        }

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 背景色を設定する（ダークモード時はテーマの色を優先）
        if (!DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            binding.settingView.setBackgroundColor(loginDataManager.backgroundColor)
        }

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

        // オートフィルスイッチ処理（有料会員のみ操作可能）
        val autofillSwitch = binding.autofillSwitch
        val hasSubscription = billingViewModel.hasActiveSubscription()
        if (!hasSubscription && loginDataManager.autofillSwitchEnable) {
            loginDataManager.setAutofillSwitchEnable(false)
        }
        autofillSwitch.isEnabled = hasSubscription
        autofillSwitch.text = if (hasSubscription) getString(R.string.autofill_setting) else getString(R.string.autofill_setting_paid)
        autofillSwitch.isChecked = loginDataManager.autofillSwitchEnable
        autofillSwitch.setOnCheckedChangeListener { _, isChecked ->
            loginDataManager.setAutofillSwitchEnable(isChecked)
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    autofillSettingDialog()
                }
            }
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

        // ダークモードスピナー処理
        darkModeSpinner = binding.darkModeSpinner
        darkModeNames = ArrayList()
        darkModeNames?.add(getString(R.string.dark_mode_off))
        darkModeNames?.add(getString(R.string.dark_mode_on))
        darkModeNames?.add(getString(R.string.dark_mode_auto))
        val darkModeAdapter =
            SetTextSizeAdapter(requireContext(), darkModeNames, loginDataManager.textSize.toInt())
        darkModeSpinner?.adapter = darkModeAdapter
        darkModeSpinner?.setSelection(loginDataManager.darkMode)
        darkModeSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                loginDataManager.setDarkMode(i)
                // ダークモード設定が変更された場合、アプリを再起動して反映
                DarkModeUtil.applyDarkMode(requireContext(), i)
//                restartPasswordMemoActivity()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        // DBバックアップ復元ボタン処理
        binding.dbBackupButton.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.DB_RESTORE_BACKUP) }

        // CSV出力ボタン処理
        // BillingViewModel 初期化
        billingViewModel.initializeBillingManager()

        // 非会員の場合はCSV入出力ボタンを無効化してリソースからラベルを取得する
        if (!billingViewModel.hasActiveSubscription()) {
            binding.csvOutputButton.text = getString(R.string.select_input_output_paid)
            binding.csvOutputButton.isEnabled = false
            binding.chromeCsvOutputButton.text = getString(R.string.select_input_output_paid)
            binding.chromeCsvOutputButton.isEnabled = false
        } else {
            binding.csvOutputButton.text = getString(R.string.select_input_output)
            binding.csvOutputButton.isEnabled = true
            binding.csvOutputButton.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.CSV_INPUT_OUTPUT) }
            binding.chromeCsvOutputButton.text = getString(R.string.select_input_output)
            binding.chromeCsvOutputButton.isEnabled = true
            binding.chromeCsvOutputButton.setOnClickListener { confirmSelectOperation(SelectInputOutputFileDialog.Operation.CHROME_CSV_INPUT_OUTPUT) }
        }

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

        // 有料会員プラン処理
        binding.membershipPlanButton.setOnClickListener {
            findNavController().navigate(SettingFragmentDirections.actionSettingFragmentToMembershipPlanFragment())
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
        val intent = Intent(requireContext(), PasswordMemoDrawerActivity::class.java)
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
        selectedOperation = operation
        val selectInputOutputFileDialog = SelectInputOutputFileDialog(requireContext(), operation, this)
        selectInputOutputFileDialog.createOpenFileDialog().show()
    }

    /**
     * 操作選択処理
     *
     * @param path 選択操作名称
     */
    override fun onSelectOperationClicked(path: String?, isChromeCsv: Boolean) {
        when (path) {
            getString(R.string.restore_db) -> {
                confirmRestoreSelectFile()
            }
            getString(R.string.backup_db) -> {
                confirmBackupSelectFile()
            }
            getString(R.string.input_csv_override), getString(R.string.input_csv_add) -> {
                selectedCsvInputMode = path
                confirmInputSelectFile()
            }
            getString(R.string.output_csv) -> {
                confirmOutputSelectFile(isChromeCsv)
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
    private fun confirmOutputSelectFile(isChromeCsv: Boolean) {
        val fileName = if (isChromeCsv) "PasswordListChromeFile_$nowDateString.csv" else "PasswordListFile_$nowDateString.csv"
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
                    val backupDbFile = BackupDbFile(requireContext(), db, this)
                    backupDbFile.backupSelectFolder(uri)
                }
                // CSV入力
                INPUT_CSV -> {
                    val inputExternalFile = InputExternalFile(requireActivity(), settingViewModel)
                    val isOverride = selectedCsvInputMode == getString(R.string.input_csv_override)
                    inputExternalFile.confirmInputDialog(uri, isOverride, selectedOperation == SelectInputOutputFileDialog.Operation.CHROME_CSV_INPUT_OUTPUT)
                }
                // CSV出力
                OUTPUT_CSV -> {
                    val outputExternalFile = OutputExternalFile(requireContext(), settingViewModel)
                    lifecycleScope.launch {
                        outputExternalFile.outputSelectFolder(uri, selectedOperation == SelectInputOutputFileDialog.Operation.CHROME_CSV_INPUT_OUTPUT)
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
        // 背景色を設定
        val selectColorUtil = SelectColorUtil(ColorList.backgroundColors, this)
        selectColorUtil.createSelectColorDialog(requireContext())
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
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("InflateParams")
    private fun operationInstructionDialog() {
        val themeResId = if (DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            android.R.style.Theme_DeviceDefault_Dialog
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog
        }
        val alertDialog = AlertDialog.Builder(requireContext(), themeResId)
            .setTitle(R.string.operation_instruction)
            .setView(layoutInflater.inflate(R.layout.alert_operating_instructions, null))
            .setPositiveButton(R.string.close, null)
            .create()

        // タイトル部分の色をダークモードに対応させる
        alertDialog.setOnShowListener {
            try {
                // AlertDialogのタイトル部分の背景色を設定
                val titleView = alertDialog.findViewById<TextView>(android.R.id.title)
                if (titleView != null) {
                    titleView.setTextColor(if (DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
                        resources.getColor(android.R.color.white, null)
                    } else {
                        resources.getColor(android.R.color.black, null)
                    })
                    // タイトル部分の背景色も設定
                    titleView.setBackgroundColor(if (DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
                        resources.getColor(android.R.color.black, null)
                    } else {
                        resources.getColor(android.R.color.white, null)
                    })
                }
            } catch (e: Exception) {
                // タイトルが見つからない場合は無視
            }
        }

        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener { alertDialog.dismiss() }
    }

    /**
     * オートフィル設定ダイアログ表示処理
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    private fun autofillSettingDialog() {
        val themeResId = if (DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            android.R.style.Theme_DeviceDefault_Dialog
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog
        }

        val alertDialog = AlertDialog.Builder(requireContext(), themeResId)
            .setTitle(R.string.autofill_setting_title)
            .setView(layoutInflater.inflate(R.layout.alert_autofill_setting_dialog, null))
            .setPositiveButton(R.string.autofill_setting_service_button, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        alertDialog.setOnShowListener {
            // Dark mode handling for title
        }

        alertDialog.show()

        // Positive button action
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                data = "package:${requireContext().packageName}".toUri()
            }
            startActivity(intent)
            alertDialog.dismiss()
        }

        // Negative button action
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            loginDataManager.setAutofillSwitchEnable(false)
            binding.autofillSwitch.isChecked = false
            alertDialog.dismiss()
        }
    }

    /**
     * 背景色選択処理
     *
     * @param color 選択背景色
     */
    @SuppressLint("ResourceType")
    override fun onSelectColorClicked(color: Int) {
        // ダークモード時はテーマの背景色を優先するため、設定画面の背景色は変更しない
        if (!DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            // 設定画面の背景色を設定
            binding.settingView.setBackgroundColor(color)
        }
        // ドロワー画面の背景色を設定（ダークモード時も適用）
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) {
            activity.setBackgroundColor(color)
        }
        loginDataManager.setBackgroundColor(color)
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
        binding.autofillSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
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
        binding.textChromeCsvOutputView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.chromeCsvOutputButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textColorSelectView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.colorSelectButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textMasterPasswordView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.masterPasswordSetButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textDarkModeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        val darkModeAdapter = SetTextSizeAdapter(requireContext(), darkModeNames, size.toInt())
        darkModeSpinner?.adapter = darkModeAdapter
        darkModeSpinner?.setSelection(loginDataManager.darkMode)
        binding.textOperationInstructionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.operationInstructionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textRateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.rateButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textLicenseView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.licenseButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textPrivacyPolicyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.privacyPolicyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.textMembershipPlanView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.membershipPlanButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
    }

    /**
     * DB復元完了時処理
     *
     */
    override fun restoreComplete() {
        restartActivity()
    }

    /**
     * DBバックアップ完了時処理
     *
     */
    override fun backupComplete() {
        restartActivity()
    }

    /**
     * アプリ再起動処理
     *
     */
    private fun restartActivity() {
        // 起動しているActivityをすべて削除し、新しいタスクでActivityを起動する
        // Hilt用モジュールがActivityRetainedComponentなので、データベースとリポジトリも再読み込みされる
        val intent = Intent(requireContext(), PasswordMemoDrawerActivity::class.java)
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