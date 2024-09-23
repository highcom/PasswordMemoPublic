package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Menu
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
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
 * パスワード入力画面アクティビティ
 *
 */
class InputPasswordActivity : AppCompatActivity() {
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
        PasswordListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((application as PasswordMemoApplication).repository)
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_password)
        adContainerView = findViewById(R.id.adView_frame_input)
        adContainerView?.post { loadBanner() }
        loginDataManager = LoginDataManager.getInstance(application)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // グループ選択スピナーの設定
        selectGroupSpinner = findViewById(R.id.selectGroup)
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
                    SetTextSizeAdapter(this@InputPasswordActivity, selectGroupNames, loginDataManager!!.textSize.toInt())
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
        val intent = intent
        id = intent.getLongExtra("ID", 0)
        groupId = intent.getLongExtra("GROUP", 1)
        editState = intent.getBooleanExtra("EDIT", false)
        (findViewById<View>(R.id.editTitle) as EditText).setText(intent.getStringExtra("TITLE"))
        (findViewById<View>(R.id.editAccount) as EditText).setText(intent.getStringExtra("ACCOUNT"))
        (findViewById<View>(R.id.editPassword) as EditText).setText(intent.getStringExtra("PASSWORD"))
        (findViewById<View>(R.id.editUrl) as EditText).setText(intent.getStringExtra("URL"))
        (findViewById<View>(R.id.editMemo) as EditText).setText(intent.getStringExtra("MEMO"))
        val generateBtn = findViewById<Button>(R.id.generateButton)
        generateBtn.setOnClickListener { generatePasswordDialog() }

        // タイトルを編集にする
        title = if (editState) {
            getString(R.string.edit)
        } else {
            getString(R.string.create_new)
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * バナー広告ロード処理
     *
     */
    private fun loadBanner() {
        // Create an ad request.
        mAdView = AdView(this)
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
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainerView!!.width.toFloat()

            // If the ad hasn't been laid out, default to the full screen width.
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_done, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> {
                // 入力データを登録する
                val passwordEntity = PasswordEntity(
                    id = id,
                    title = findViewById<EditText>(R.id.editTitle).text.toString(),
                    account = findViewById<EditText>(R.id.editAccount).text.toString(),
                    password = findViewById<EditText>(R.id.editPassword).text.toString(),
                    url = findViewById<EditText>(R.id.editUrl).text.toString(),
                    groupId = selectGroupId ?: 1,
                    memo = findViewById<EditText>(R.id.editMemo).text.toString(),
                    inputDate = nowDate
                )
                if (editState) {
                    passwordListViewModel.update(passwordEntity)
                } else {
                    passwordListViewModel.insert(passwordEntity)
                }
                // 詳細画面を終了
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()

        // 背景色を設定する
        findViewById<View>(R.id.inputPasswordView).setBackgroundColor(loginDataManager!!.backgroundColor)
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
        val passwordRadio = generatePasswordView.findViewById<RadioGroup>(R.id.passwordKindMenu)
        passwordRadio.check(R.id.radioLettersNumbers)
        passwordKind = passwordRadio.checkedRadioButtonId
        passwordRadio.setOnCheckedChangeListener { _, checkedId ->
            passwordKind = checkedId
            generatePasswordString()
        }
        // 小文字のみチェクボックスを設定
        val lowerCaseOnlyCheckBox = generatePasswordView.findViewById<CheckBox>(R.id.lowerCaseOnly)
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
            generatePasswordView.findViewById<NumberPicker>(R.id.passwordNumberPicker)
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
        val generateButton = generatePasswordView.findViewById<ImageButton>(R.id.generateButton)
        generateButton.setColorFilter(Color.parseColor("#007AFF"))
        generateButton.setOnClickListener { generatePasswordString() }

        // ダイアログの生成
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.generate_password_title)
            .setView(generatePasswordView)
            .setPositiveButton(R.string.apply, null)
            .setNegativeButton(R.string.discard, null)
            .create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (isLowerCaseOnly) {
                (findViewById<View>(R.id.editPassword) as EditText).setText(
                    generatePassword!!.lowercase(Locale.getDefault())
                )
            } else {
                (findViewById<View>(R.id.editPassword) as EditText).setText(generatePassword)
            }
            alertDialog.dismiss()
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener { alertDialog.dismiss() }
        generatePasswordText = generatePasswordView.findViewById(R.id.generatePasswordText)
        generatePasswordString()
    }

    /**
     * パスワード文字列自動生成処理
     *
     */
    private fun generatePasswordString() {
        generatePassword = when (passwordKind) {
            R.id.radioNumbers -> {
                // 数字のみ
                RandomStringUtils.randomNumeric(passwordCount)
            }
            R.id.radioLettersNumbers -> {
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

    public override fun onDestroy() {
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        (findViewById<View>(R.id.titleView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editTitle) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.accountView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editAccount) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.passwordView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editPassword) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.generateButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.urlView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editUrl) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.groupView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                val selectGroupAdapter =
                    SetTextSizeAdapter(this@InputPasswordActivity, selectGroupNames, loginDataManager!!.textSize.toInt())
                selectGroupSpinner!!.adapter = selectGroupAdapter
                for (i in list.indices) {
                    if (groupId == list[i].groupId) {
                        selectGroupSpinner?.setSelection(i)
                        break
                    }
                }
            }
        }
        (findViewById<View>(R.id.editMemo) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
    }
}