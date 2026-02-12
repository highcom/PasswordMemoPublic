package com.highcom.passwordmemo.service

import android.app.assist.AssistStructure
import android.content.Context
import android.os.Build
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.MIGRATION_2_3
import com.highcom.passwordmemo.data.MIGRATION_3_4
import com.highcom.passwordmemo.data.MIGRATION_4_5
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase
import com.highcom.passwordmemo.domain.billing.BillingManager
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory

/**
 * 登録データを利用したオートフィルサービス。
 * PasswordEntityのurlとリクエストのドメインが一致するものを特定し、accountとpasswordを提供する。
 */
@RequiresApi(Build.VERSION_CODES.O)
class PasswordMemoAutofillService : AutofillService() {

    override fun onFillRequest(request: android.service.autofill.FillRequest, cancellationSignal: android.os.CancellationSignal, callback: FillCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val prefs = applicationContext.getSharedPreferences("com.highcom.LoginActivity.MasterPass", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("autofillSwitchEnable", false)) {
            callback.onSuccess(null)
            return
        }
        val purchasePrefs = applicationContext.getSharedPreferences("purchase_prefs", Context.MODE_PRIVATE)
        val hasActiveSubscription = BillingManager.SUBSCRIPTION_PRODUCT_IDS.any { id ->
            purchasePrefs.getBoolean("purchase_$id", false)
        }
        if (!hasActiveSubscription) {
            callback.onSuccess(null)
            return
        }

        val lastContext = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }
        val structure = lastContext.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val (requestDomainFromStructure, usernameIds, passwordIds) = parseStructure(structure)
        var requestDomain = requestDomainFromStructure
        if (requestDomain.isNullOrBlank()) {
            for (ctx in request.fillContexts) {
                if (ctx.structure === structure) continue
                val s = ctx.structure ?: continue
                val (domain, _, _) = parseStructure(s)
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
                getDatabase().passwordDao().getAllPasswords()
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

        if (matched.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val responseBuilder = FillResponse.Builder()
        for (entity in matched) {
            val presentation = RemoteViews(applicationContext.packageName, R.layout.autofill_dataset_item).apply {
                setTextViewText(R.id.autofill_item_title, entity.title.ifBlank { entity.account }.ifBlank { applicationContext.getString(R.string.app_name) })
            }
            val datasetBuilder = Dataset.Builder(presentation)
            usernameIds.forEach { id -> datasetBuilder.setValue(id, AutofillValue.forText(entity.account)) }
            passwordIds.forEach { id -> datasetBuilder.setValue(id, AutofillValue.forText(entity.password)) }
            responseBuilder.addDataset(datasetBuilder.build())
        }
        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: android.service.autofill.SaveRequest, callback: android.service.autofill.SaveCallback) {
        // 保存リクエストは扱わない（登録はアプリ内のみ）
        callback.onSuccess()
    }

    private fun parseStructure(structure: AssistStructure): Triple<String?, List<AutofillId>, List<AutofillId>> {
        var domain: String? = null
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            node.webDomain?.let { d -> if (domain.isNullOrBlank()) domain = normalizeDomain(d) }
            val hints = node.autofillHints?.map { it.lowercase() } ?: emptyList()
            when {
                hints.any { it in listOf("username", "email", "emailaddress") } -> node.autofillId?.let { if (it !in usernameIds) usernameIds.add(it) }
                hints.any { it in listOf("password", "current-password", "new-password") } -> node.autofillId?.let { if (it !in passwordIds) passwordIds.add(it) }
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            structure.getWindowNodeAt(i).rootViewNode?.let { traverse(it) }
        }
        return Triple(domain, usernameIds, passwordIds)
    }

    private fun normalizeDomain(domain: String?): String? {
        if (domain.isNullOrBlank()) return null
        var d = domain.lowercase().trim()
        if (d.startsWith("www.")) d = d.removePrefix("www.")
        return d.ifBlank { null }
    }

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

    private fun getDatabase(): PasswordMemoRoomDatabase {
        if (db == null) {
            val key = applicationContext.getString(R.string.db_secret_key).toCharArray()
            db = Room.databaseBuilder(
                applicationContext,
                PasswordMemoRoomDatabase::class.java,
                "PasswordMemoDB"
            ).allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(key), object : SQLiteDatabaseHook {
                    override fun preKey(database: SQLiteDatabase?) {}
                    override fun postKey(database: SQLiteDatabase?) {}
                }))
                .build()
        }
        return db!!
    }

    companion object {
        private var db: PasswordMemoRoomDatabase? = null
    }
}
