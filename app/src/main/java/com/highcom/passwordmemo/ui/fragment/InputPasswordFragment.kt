package com.highcom.passwordmemo.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
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
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.databinding.FragmentInputPasswordBinding
import com.highcom.passwordmemo.ui.PasswordEditData
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import com.highcom.passwordmemo.util.AdBanner
import com.highcom.passwordmemo.util.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

/**
 * パスワード入力画面フラグメント
 *
 */
@AndroidEntryPoint
class InputPasswordFragment : Fragment(), GeneratePasswordDialogFragment.GeneratePasswordDialogListener {
    /** パスワード入力画面のbinding */
    private lateinit var binding: FragmentInputPasswordBinding
    /** Navigationで渡された引数 */
    private val args: InputPasswordFragmentArgs by navArgs()
    /** パスワード編集データ */
    lateinit var passwordEditData: PasswordEditData
    /** バナー広告処理 */
    private var adBanner: AdBanner? = null
    /** 広告コンテナ */
    private var adContainerView: FrameLayout? = null
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** グループ選択スピナー */
    private var selectGroupSpinner: Spinner? = null
    /** グループ名称一覧 */
    private var selectGroupNames: ArrayList<String?>? = null
    /** 選択グループID */
    private var selectGroupId: Long? = null
    /** パスワード一覧ビューモデル */
    private val passwordListViewModel: PasswordListViewModel by viewModels()
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels()

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
        passwordEditData = args.editData
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_input_password, container, false)
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adContainerView = binding.adViewFrameInput
        adBanner = AdBanner(this, adContainerView)
        adContainerView?.post { adBanner?.loadBanner(getString(R.string.admob_unit_id_3)) }

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // グループ選択スピナーの設定
        selectGroupSpinner = binding.selectGroup
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
                    SetTextSizeAdapter(requireContext(), selectGroupNames, loginDataManager.textSize.toInt())
                selectGroupSpinner?.adapter = selectGroupAdapter
                for (i in list.indices) {
                    if (passwordEditData.groupId == list[i].groupId) {
                        selectGroupSpinner?.setSelection(i)
                        break
                    }
                }
            }
        }

        // タイトルを編集にする
        requireActivity().title = if (passwordEditData.edit) {
            getString(R.string.edit)
        } else {
            getString(R.string.create_new)
        }
        // ActionBarに戻るボタンを設定
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
                    id = passwordEditData.id,
                    title = passwordEditData.title,
                    account = passwordEditData.account,
                    password = passwordEditData.password,
                    url = passwordEditData.url,
                    groupId = selectGroupId ?: 1,
                    memo = passwordEditData.memo,
                    inputDate = nowDate
                )
                if (passwordEditData.edit) {
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
        binding.inputPasswordView.setBackgroundColor(loginDataManager.backgroundColor)
        // テキストサイズを設定する
        setTextSize(loginDataManager.textSize)
    }

    /**
     * パスワード自動生成用ダイアログ表示処理
     *
     */
    fun showGeneratePasswordDialog() {
        val dialog = GeneratePasswordDialogFragment()
        dialog.show(childFragmentManager, "GeneratePasswordDialog")
    }

    /**
     * パスワード自動生成結果通知
     *
     * @param result 自動生成パスワード
     */
    override fun onDialogResult(result: String) {
        binding.editPassword.setText(result)
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
        adBanner?.destroy()
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        binding.titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.accountView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editAccount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.passwordView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editPassword.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.generateButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.urlView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editUrl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.groupView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editMemo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }
}