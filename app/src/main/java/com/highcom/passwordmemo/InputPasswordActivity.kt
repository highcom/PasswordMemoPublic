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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.highcom.passwordmemo.database.ListDataManager
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter
import com.highcom.passwordmemo.util.login.LoginDataManager
import org.apache.commons.lang3.RandomStringUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class InputPasswordActivity : AppCompatActivity() {
    private var id: Long = 0
    private var adContainerView: FrameLayout? = null
    private var mAdView: AdView? = null
    private var editState = false
    private var loginDataManager: LoginDataManager? = null
    private var listDataManager: ListDataManager? = null
    private var passwordKind = 0
    private var passwordCount = 0
    private var isLowerCaseOnly = false
    private var generatePassword: String? = null
    var generatePasswordText: EditText? = null
    private var groupId: Long = 0
    var selectGroupSpinner: Spinner? = null
    var selectGroupNames: ArrayList<String?>? = null
    private var selectGroupId: Long? = null
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_password)
        adContainerView = findViewById(R.id.adView_frame_input)
        adContainerView?.post(Runnable { loadBanner() })
        loginDataManager = LoginDataManager.Companion.getInstance(this)
        listDataManager = ListDataManager.Companion.getInstance(this)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // グループ選択スピナーの設定
        selectGroupSpinner = findViewById(R.id.selectGroup)
        selectGroupNames = ArrayList()
        for (group in listDataManager!!.groupList) {
            selectGroupNames!!.add(group!!["name"])
        }
        val selectGroupAdapter =
            SetTextSizeAdapter(this, selectGroupNames, loginDataManager!!.textSize.toInt())
        selectGroupSpinner?.setAdapter(selectGroupAdapter)
        selectGroupSpinner?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                val groupList = listDataManager!!.groupList
                val group = groupList!![i]
                selectGroupId = Objects.requireNonNull(group!!["group_id"])?.toLong()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        })

        // 渡されたデータを取得する
        val intent = intent
        id = intent.getLongExtra("ID", -1)
        groupId = intent.getLongExtra("GROUP", 1)
        editState = intent.getBooleanExtra("EDIT", false)
        (findViewById<View>(R.id.editTitle) as EditText).setText(intent.getStringExtra("TITLE"))
        (findViewById<View>(R.id.editAccount) as EditText).setText(intent.getStringExtra("ACCOUNT"))
        (findViewById<View>(R.id.editPassword) as EditText).setText(intent.getStringExtra("PASSWORD"))
        (findViewById<View>(R.id.editUrl) as EditText).setText(intent.getStringExtra("URL"))
        (findViewById<View>(R.id.editMemo) as EditText).setText(intent.getStringExtra("MEMO"))
        val groupList = listDataManager!!.groupList
        for (i in groupList!!.indices) {
            val id = Objects.requireNonNull(
                groupList[i]!!["group_id"]
            )?.toLong()
            if (groupId == id) {
                selectGroupSpinner?.setSelection(i)
                break
            }
        }
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

    private val adSize: AdSize
        private get() {
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
                val editTitle = findViewById<View>(R.id.editTitle) as EditText
                val editAccount = findViewById<View>(R.id.editAccount) as EditText
                val editPassword = findViewById<View>(R.id.editPassword) as EditText
                val editUrl = findViewById<View>(R.id.editUrl) as EditText
                val editMemo = findViewById<View>(R.id.editMemo) as EditText
                val data: MutableMap<String?, String?> = HashMap()
                data["id"] = java.lang.Long.valueOf(id).toString()
                data["title"] = editTitle.text.toString()
                data["account"] = editAccount.text.toString()
                data["password"] = editPassword.text.toString()
                data["url"] = editUrl.text.toString()
                data["group_id"] = selectGroupId.toString()
                data["memo"] = editMemo.text.toString()
                data["inputdate"] = nowDate
                ListDataManager.Companion.getInstance(this)!!.setData(editState, data)
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

    // パスワード生成ダイアログ
    private fun generatePasswordDialog() {
        val generatePasswordView = layoutInflater.inflate(R.layout.alert_generate_password, null)
        // 文字種別ラジオボタンの初期値を設定
        val passwordRadio = generatePasswordView.findViewById<RadioGroup>(R.id.passwordKindMenu)
        passwordRadio.check(R.id.radioLettersNumbers)
        passwordKind = passwordRadio.checkedRadioButtonId
        passwordRadio.setOnCheckedChangeListener { group, checkedId ->
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
        passwordPicker.setOnValueChangedListener { picker, oldVal, newVal ->
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

    // パスワード文字列生成
    private fun generatePasswordString() {
        generatePassword = if (passwordKind == R.id.radioNumbers) {
            RandomStringUtils.randomNumeric(passwordCount)
        } else if (passwordKind == R.id.radioLettersNumbers) {
            RandomStringUtils.randomAlphanumeric(passwordCount)
        } else {
            RandomStringUtils.randomGraph(passwordCount)
        }
        if (isLowerCaseOnly) {
            generatePasswordText!!.setText(generatePassword?.lowercase(Locale.getDefault()))
        } else {
            generatePasswordText!!.setText(generatePassword)
        }
    }

    val nowDate: String
        // 現在の日付取得処理
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
        val selectGroupAdapter =
            SetTextSizeAdapter(this, selectGroupNames, loginDataManager!!.textSize.toInt())
        selectGroupSpinner!!.adapter = selectGroupAdapter
        val groupList = listDataManager!!.groupList
        for (i in groupList!!.indices) {
            val id = Objects.requireNonNull(
                groupList[i]!!["group_id"]
            )?.toLong()
            if (groupId == id) {
                selectGroupSpinner!!.setSelection(i)
                break
            }
        }
        (findViewById<View>(R.id.editMemo) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
    }
}