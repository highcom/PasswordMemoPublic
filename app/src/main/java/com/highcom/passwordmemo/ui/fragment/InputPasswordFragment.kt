package com.highcom.passwordmemo.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.highcom.passwordmemo.PasswordMemoApplication
import com.highcom.passwordmemo.PasswordMemoLifecycle
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * パスワード入力画面フラグメント
 *
 */
class InputPasswordFragment : Fragment() {
    /** パスワード入力画面のビュー */
    private var rootView: View? = null
    /** パスワードデータID */
    private var id: Long = 0
    /** 広告コンテナ */
    private var adContainerView: FrameLayout? = null
    /** 広告ビュー */
    private var mAdView: AdView? = null
    /** 編集モードかどうか */
    private var editState = false
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    /** パスワード自動生成種別 */
    private var passwordKind = 0
    /** パスワード自動生成文字数 */
    private var passwordCount = 0
    /** パスワード自動生が成小文字のみかどうか */
    private var isLowerCaseOnly = false
    /** 自動生成パスワード */
    private var generatePassword: String? = null
    /** 自動生成パスワードテキストビュー */
    private var generatePasswordText: EditText? = null
    /** 編集モードでの既存の選択グループID */
    private var groupId: Long = 0
    /** グループ選択スピナー */
    private var selectGroupSpinner: Spinner? = null
    /** グループ名称一覧 */
    private var selectGroupNames: ArrayList<String?>? = null
    /** 選択グループID */
    private var selectGroupId: Long? = null
    /** パスワード一覧ビューモデル */
    private val passwordListViewModel: PasswordListViewModel by viewModels {
        PasswordListViewModel.Factory((requireActivity().application as PasswordMemoApplication).repository)
    }
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((requireActivity().application as PasswordMemoApplication).repository)
    }

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
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_input_password, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adContainerView = rootView?.findViewById(R.id.adView_frame_input)
        adContainerView?.post { loadBanner() }
        loginDataManager = (requireActivity().application as PasswordMemoApplication).loginDataManager

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // グループ選択スピナーの設定
        selectGroupSpinner = rootView?.findViewById(R.id.select_group)
        selectGroupSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                lifecycleScope.launch {
                    groupListViewModel.groupList.collect {
                        selectGroupId = it[i].groupId
                    }
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        selectGroupNames = ArrayList()
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                for (group in list) {
                    selectGroupNames?.add(group.name)
                }
                val selectGroupAdapter =
                    SetTextSizeAdapter(requireContext(), selectGroupNames, loginDataManager!!.textSize.toInt())
                selectGroupSpinner?.adapter = selectGroupAdapter
                for (i in list.indices) {
                    if (groupId == list[i].groupId) {
                        selectGroupSpinner?.setSelection(i)
                        break
                    }
                }
            }
        }

        // 渡されたデータを取得する
        val args: InputPasswordFragmentArgs by navArgs()
        id = args.editData.id
        groupId = args.editData.groupId
        editState = args.editData.edit
        rootView?.findViewById<EditText>(R.id.edit_title)?.setText(args.editData.title)
        rootView?.findViewById<EditText>(R.id.edit_account)?.setText(args.editData.account)
        rootView?.findViewById<EditText>(R.id.edit_password)?.setText(args.editData.password)
        rootView?.findViewById<EditText>(R.id.edit_url)?.setText(args.editData.url)
        rootView?.findViewById<EditText>(R.id.edit_memo)?.setText(args.editData.memo)
        val generateBtn = rootView?.findViewById<Button>(R.id.generate_button)
        generateBtn?.setOnClickListener { generatePasswordDialog() }

        // タイトルを編集にする
        requireActivity().title = if (editState) {
            getString(R.string.edit)
        } else {
            getString(R.string.create_new)
        }
        // ActionBarに戻るボタンを設定
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * バナー広告ロード処理
     *
     */
    private fun loadBanner() {
        // Create an ad request.
        mAdView = AdView(requireContext())
        mAdView!!.adUnitId = getString(R.string.admob_unit_id_3)
        adContainerView!!.removeAllViews()
        adContainerView!!.addView(mAdView)
        val adSize = adSize
        mAdView!!.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        mAdView!!.loadAd(adRequest)
    }

    /** 広告サイズ設定 */
    @Suppress("DEPRECATION")
    private val adSize: AdSize
        get() {
            // Determine the screen width (less decorations) to use for the ad width.
            val display = requireActivity().windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainerView!!.width.toFloat()

            // If the ad hasn't been laid out, default to the full screen width.
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireContext(), adWidth)
        }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "inflater.inflate(R.menu.menu_done, menu)",
        "com.highcom.passwordmemo.R"
    )
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_done, menu)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> findNavController().navigate(R.id.action_inputPasswordFragment_to_passwordListFragment)
            R.id.action_done -> {
                // 入力データを登録する
                val passwordEntity = PasswordEntity(
                    id = id,
                    title = rootView?.findViewById<EditText>(R.id.edit_title)?.text.toString(),
                    account = rootView?.findViewById<EditText>(R.id.edit_account)?.text.toString(),
                    password = rootView?.findViewById<EditText>(R.id.edit_password)?.text.toString(),
                    url = rootView?.findViewById<EditText>(R.id.edit_url)?.text.toString(),
                    groupId = selectGroupId ?: 1,
                    memo = rootView?.findViewById<EditText>(R.id.edit_memo)?.text.toString(),
                    inputDate = nowDate
                )
                if (editState) {
                    passwordListViewModel.update(passwordEntity)
                } else {
                    passwordListViewModel.insert(passwordEntity)
                }
                // 詳細画面を終了
                findNavController().navigate(R.id.action_inputPasswordFragment_to_passwordListFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()

        // 背景色を設定する
        rootView?.findViewById<View>(R.id.input_password_view)?.setBackgroundColor(loginDataManager!!.backgroundColor)
        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    /**
     * パスワード自動生成用ダイアログ表示処理
     *
     */
    private fun generatePasswordDialog() {
        val generatePasswordView = layoutInflater.inflate(R.layout.alert_generate_password, null)
        // 文字種別ラジオボタンの初期値を設定
        val passwordRadio = generatePasswordView.findViewById<RadioGroup>(R.id.password_kind_menu)
        passwordRadio.check(R.id.radio_letters_numbers)
        passwordKind = passwordRadio.checkedRadioButtonId
        passwordRadio.setOnCheckedChangeListener { _, checkedId ->
            passwordKind = checkedId
            generatePasswordString()
        }
        // 小文字のみチェクボックスを設定
        val lowerCaseOnlyCheckBox = generatePasswordView.findViewById<CheckBox>(R.id.lower_case_only)
        lowerCaseOnlyCheckBox.isChecked = false
        isLowerCaseOnly = false
        lowerCaseOnlyCheckBox.setOnClickListener {
            isLowerCaseOnly = lowerCaseOnlyCheckBox.isChecked
            if (isLowerCaseOnly) {
                generatePasswordText!!.setText(generatePassword!!.lowercase(Locale.getDefault()))
            } else {
                generatePasswordText!!.setText(generatePassword)
            }
        }

        // 文字数ピッカーの初期値を設定
        val passwordPicker =
            generatePasswordView.findViewById<NumberPicker>(R.id.password_number_picker)
        passwordPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        passwordPicker.maxValue = 32
        passwordPicker.minValue = 4
        passwordPicker.value = 8
        passwordCount = passwordPicker.value
        passwordPicker.setOnValueChangedListener { _, _, newVal ->
            passwordCount = newVal
            generatePasswordString()
        }
        // ボタンのカラーフィルターとイベントを設定
        val generateButton = generatePasswordView.findViewById<ImageButton>(R.id.generate_button)
        generateButton.setColorFilter(Color.parseColor("#007AFF"))
        generateButton.setOnClickListener { generatePasswordString() }

        // ダイアログの生成
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.generate_password_title)
            .setView(generatePasswordView)
            .setPositiveButton(R.string.apply, null)
            .setNegativeButton(R.string.discard, null)
            .create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (isLowerCaseOnly) {
                rootView?.findViewById<EditText>(R.id.edit_password)?.setText(
                    generatePassword!!.lowercase(Locale.getDefault())
                )
            } else {
                rootView?.findViewById<EditText>(R.id.edit_password)?.setText(generatePassword)
            }
            alertDialog.dismiss()
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener { alertDialog.dismiss() }
        generatePasswordText = generatePasswordView.findViewById(R.id.generate_password_text)
        generatePasswordString()
    }

    /**
     * パスワード文字列自動生成処理
     *
     */
    private fun generatePasswordString() {
        generatePassword = when (passwordKind) {
            R.id.radio_numbers -> {
                // 数字のみ
                RandomStringUtils.randomNumeric(passwordCount)
            }
            R.id.radio_letters_numbers -> {
                // 数字＋文字
                RandomStringUtils.randomAlphanumeric(passwordCount)
            }
            else -> {
                // 数字＋文字＋記号
                RandomStringUtils.randomGraph(passwordCount)
            }
        }
        // 小文字のみでの生成かどうか
        if (isLowerCaseOnly) {
            generatePasswordText!!.setText(generatePassword?.lowercase(Locale.getDefault()))
        } else {
            generatePasswordText!!.setText(generatePassword)
        }
    }

    /** 現在日付 */
    private val nowDate: String
        // 現在の日付取得処理
        @SuppressLint("SimpleDateFormat")
        get() {
            val date = Date()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            return sdf.format(date)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.isBackground) {
            // TODO:これでログイン画面に戻るのか？
            requireActivity().finishAffinity()
        }
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        rootView?.findViewById<TextView>(R.id.title_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_title)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<TextView>(R.id.account_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_account)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<TextView>(R.id.password_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_password)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<Button>(R.id.generate_button)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<TextView>(R.id.url_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_url)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<TextView>(R.id.group_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                val selectGroupAdapter =
                    SetTextSizeAdapter(requireContext(), selectGroupNames, loginDataManager!!.textSize.toInt())
                selectGroupSpinner!!.adapter = selectGroupAdapter
                for (i in list.indices) {
                    if (groupId == list[i].groupId) {
                        selectGroupSpinner?.setSelection(i)
                        break
                    }
                }
            }
        }
        rootView?.findViewById<EditText>(R.id.edit_memo)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
    }
}