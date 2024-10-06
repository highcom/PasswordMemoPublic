package com.highcom.passwordmemo.ui.fragment

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import com.highcom.passwordmemo.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoApplication
import com.highcom.passwordmemo.util.login.LoginDataManager

/**
 * ライセンス画面フラグメント
 *
 */
class LicenseFragment : Fragment() {
    /** ライセンス画面のビュー */
    private var rootView: View? = null
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null

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
        rootView = inflater.inflate(R.layout.fragment_license, container, false)
        return rootView
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.license)
        // ActionBarに戻るボタンを設定
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loginDataManager = (requireActivity().application as PasswordMemoApplication).loginDataManager

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        rootView?.findViewById<LinearLayout>(R.id.licenseView)?.setBackgroundColor(
            loginDataManager!!.backgroundColor
        )
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> findNavController().navigate(LicenseFragmentDirections.actionLicenseFragmentToSettingFragment())
        }
        return super.onOptionsItemSelected(item)
    }
}