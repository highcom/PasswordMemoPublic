package com.highcom.passwordmemo.service

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordDao
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import com.highcom.passwordmemo.domain.login.LoginDataManager
import com.highcom.passwordmemo.ui.AutofillInputActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * 登録データを利用したオートフィルサービス。
 * PasswordEntityのurlとリクエストのドメインが一致するものを特定し、accountとpasswordを提供する。
 */
@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class PasswordMemoAutofillService : AutofillService() {

    @Inject
    lateinit var passwordDao: PasswordDao

    @Inject
    lateinit var loginDataManager: LoginDataManager

    @Inject
    lateinit var purchaseManager: PurchaseManager

    data class ParsedStructure(
        val domain: String?,
        val usernameIds: List<AutofillId>,
        val passwordIds: List<AutofillId>,
        val values: Map<AutofillId, String>
    )

    /**
     * オートフィルのリクエストがあった際に呼び出される
     *
     * @param request オートフィルリクエスト
     * @param cancellationSignal キャンセルシグナル
     * @param callback コールバック
     */
    override fun onFillRequest(request: FillRequest, cancellationSignal: android.os.CancellationSignal, callback: FillCallback) {

        if (!loginDataManager.autofillSwitchEnable) {
            callback.onSuccess(null)
            return
        }
        if (!purchaseManager.hasActiveSubscription()) {
            callback.onSuccess(null)
            return
        }

        val lastContext = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }
        val structure = lastContext.structure

        val parsed = parseStructure(structure)
        val requestDomainFromStructure = parsed.domain
        val usernameIds = parsed.usernameIds
        val passwordIds = parsed.passwordIds
        var requestDomain = requestDomainFromStructure
        if (requestDomain.isNullOrBlank()) {
            for (ctx in request.fillContexts) {
                if (ctx.structure === structure) continue
                val s = ctx.structure ?: continue
                val domain = parseStructure(s).domain
                if (!domain.isNullOrBlank()) {
                    requestDomain = domain
                    break
                }
            }
        }
        if (usernameIds.isEmpty() && passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val passwords = runBlocking {
            try {
                passwordDao.getAllPasswords()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val matched = if (requestDomain.isNullOrBlank()) {
            // Chromeなどでドメインが取得できない場合は全件を候補表示（ユーザーが選択）
            passwords
        } else {
            passwords.filter { entity ->
                requestDomain == extractDomain(entity.url)
            }
        }

        val responseBuilder = FillResponse.Builder()

        if (matched.isEmpty()) {
            val saveInfo = SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                (usernameIds + passwordIds).toTypedArray()
            ).build()
            responseBuilder.setSaveInfo(saveInfo)
            callback.onSuccess(responseBuilder.build())
            return
        }

        for (entity in matched) {
            val presentation = RemoteViews(applicationContext.packageName, R.layout.autofill_dataset_item).apply {
                setTextViewText(R.id.autofill_item_title, entity.title.ifBlank { entity.account }.ifBlank { applicationContext.getString(R.string.app_name) })
                if (usernameIds.isNotEmpty()) {
                    setTextViewText(R.id.autofill_item_subtitle, entity.account)
                } else {
                    setTextViewText(R.id.autofill_item_subtitle, "••••••••")
                }
            }
            val datasetBuilder = Dataset.Builder(presentation)
            usernameIds.forEach { id -> datasetBuilder.setValue(id, AutofillValue.forText(entity.account)) }
            passwordIds.forEach { id -> datasetBuilder.setValue(id, AutofillValue.forText(entity.password)) }
            responseBuilder.addDataset(datasetBuilder.build())
        }
        val saveInfo = SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
            (usernameIds + passwordIds).toTypedArray()
        ).build()
        responseBuilder.setSaveInfo(saveInfo)
        callback.onSuccess(responseBuilder.build())
    }

    /**
     * オートフィルの保存リクエストがあった際に呼び出される
     *
     * @param request 保存リクエスト
     * @param callback コールバック
     */
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onSaveRequest(request: android.service.autofill.SaveRequest, callback: android.service.autofill.SaveCallback) {
        if (!loginDataManager.autofillSwitchEnable) {
            callback.onSuccess()
            return
        }
        val context = request.fillContexts
        val structure = context.last().structure

        val parsed = parseStructure(structure)
        val domain = parsed.domain
        val usernameIds = parsed.usernameIds
        val passwordIds = parsed.passwordIds
        if (usernameIds.isEmpty() || passwordIds.isEmpty()) {
            callback.onFailure(getString(R.string.autofill_save_message))
            return
        }

        val username = usernameIds.firstOrNull()?.let { parsed.values[it] }
        val password = passwordIds.firstOrNull()?.let { parsed.values[it] }

        val intent = Intent(this, AutofillInputActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("url", domain)
            putExtra("account", username)
            putExtra("password", password)
        }
        val intentSender = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_MUTABLE).intentSender

        callback.onSuccess(intentSender)
    }

    /**
     * AssistStructureを解析して、ドメインとユーザー名・パスワードのIDを取得する
     *
     * @param structure 解析対象のAssistStructure
     * @return ドメイン、ユーザー名IDリスト、パスワードIDリストのTriple
     */
    private fun parseStructure(structure: AssistStructure): ParsedStructure {
        var domain: String? = null
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()
        val values = mutableMapOf<AutofillId, String>()

        val usernameHints = listOf(
            "username", "email", "emailaddress", "name",
            "loginid", "userid", "accountname", "membername", "tel", "phone"
        )
        val passwordHints = listOf(
            "password", "current-password", "new-password", "passwd"
        )

        fun traverse(node: AssistStructure.ViewNode) {
            node.webDomain?.let { d -> if (domain.isNullOrBlank()) domain = normalizeDomain(d) }
            val hints = node.autofillHints?.map { it.lowercase() } ?: emptyList()
            when {
                hints.any { it in usernameHints } -> node.autofillId?.let {
                    if (it !in usernameIds) {
                        usernameIds.add(it)
                        node.text?.toString()?.let { text -> values[it] = text }
                    }
                }
                hints.any { it in passwordHints } -> node.autofillId?.let {
                    if (it !in passwordIds) {
                        passwordIds.add(it)
                        node.text?.toString()?.let { text -> values[it] = text }
                    }
                }
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            structure.getWindowNodeAt(i).rootViewNode?.let { traverse(it) }
        }

        if (domain.isNullOrBlank()) {
            domain = structure.activityComponent?.packageName
        }

        return ParsedStructure(domain, usernameIds, passwordIds, values)
    }

    /**
     * ドメイン名を正規化する
     *
     * @param domain 正規化対象のドメイン名
     * @return 正規化されたドメイン名
     */
    private fun normalizeDomain(domain: String?): String? {
        if (domain.isNullOrBlank()) return null
        var d = domain.lowercase().trim()
        if (d.startsWith("www.")) d = d.removePrefix("www.")
        return d.ifBlank { null }
    }

    /**
     * URLからドメインを抽出する
     *
     * @param url 抽出対象のURL
     * @return 抽出されたドメイン名
     */
    private fun extractDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return try {
            val host = if (trimmed.contains("://")) {
                java.net.URI(trimmed).host
            } else {
                trimmed.trimStart('/').split('/').firstOrNull()?.takeIf { it.contains('.') || it.isNotEmpty() }
            }
            host?.let { normalizeDomain(it) }
        } catch (e: Exception) {
            null
        }
    }
}
