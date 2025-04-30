package browserpicker.playground.browserpicker.data

import androidx.sqlite.db.*
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.*
import browserpicker.data.local.query.model.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import kotlin.collections.forEach

@Singleton
class UriRecordQueryBuilder @Inject constructor() {

    companion object {
        private const val TAG = "UriRecordQueryBuilder"

        // Table Aliases
        private const val UR_ALIAS = "ur"
        private const val HR_ALIAS = "hr"

        // Table Names (for joins)
        private const val URI_RECORDS_TABLE = "uri_records"
        private const val HOST_RULES_TABLE = "host_rules"

        // --- Column Names with Alias (ur) ---
        internal object Columns {
            // From uri_records (aliased as ur)
            const val UR_ID = "$UR_ALIAS.uri_record_id"
            const val UR_URI_STRING = "$UR_ALIAS.uri_string"
            const val UR_HOST = "$UR_ALIAS.host"
            const val UR_TIMESTAMP = "$UR_ALIAS.timestamp"
            const val UR_URI_SOURCE = "$UR_ALIAS.uri_source"
            const val UR_INTERACTION_ACTION = "$UR_ALIAS.interaction_action"
            const val UR_CHOSEN_BROWSER = "$UR_ALIAS.chosen_browser_package"
            const val UR_ASSOCIATED_RULE_ID = "$UR_ALIAS.associated_host_rule_id"

            // From host_rules (aliased as hr) - only those needed for filtering
            const val HR_ID = "$HR_ALIAS.host_rule_id"
            const val HR_URI_STATUS = "$HR_ALIAS.uri_status"
        }

        internal object Expressions {
            // Use alias in expression
            const val DATE_GROUP = "STRFTIME('%Y-%m-%d', ${Columns.UR_TIMESTAMP} / 1000, 'unixepoch', 'localtime')"
        }

        internal object GroupingConstants {
            const val NULL_BROWSER_GROUP_VALUE = "browser_picker_null_browser"
        }

        // Explicit SELECT clause using alias 'ur' for all UriRecordEntity columns
        private val SELECT_UR_COLUMNS_SQL = """
            ${Columns.UR_ID}, ${Columns.UR_URI_STRING}, ${Columns.UR_HOST}, ${Columns.UR_TIMESTAMP}, 
            ${Columns.UR_URI_SOURCE}, ${Columns.UR_INTERACTION_ACTION}, ${Columns.UR_CHOSEN_BROWSER}, 
            ${Columns.UR_ASSOCIATED_RULE_ID}
        """.trimIndent()

        // Updated Safe Fallbacks
        val SAFE_EMPTY_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_QUERY")
            // Selects specific columns, FROM clause is irrelevant as WHERE 0
            SimpleSQLiteQuery("SELECT $SELECT_UR_COLUMNS_SQL FROM $URI_RECORDS_TABLE $UR_ALIAS WHERE 0")
        }
        val SAFE_EMPTY_COUNT_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_COUNT_QUERY")
            SimpleSQLiteQuery("SELECT COUNT(${Columns.UR_ID}) FROM $URI_RECORDS_TABLE $UR_ALIAS WHERE 0")
        }
        val SAFE_EMPTY_DATE_COUNT_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_DATE_COUNT_QUERY")
            // Needs specific columns for DateCount mapping
            SimpleSQLiteQuery("SELECT NULL as date, 0 as count WHERE 0")
        }
        val SAFE_EMPTY_GROUP_COUNT_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_GROUP_COUNT_QUERY")
            // Needs specific columns for GroupCount mapping
            SimpleSQLiteQuery("SELECT NULL as groupValue, 0 as count WHERE 0")
        }
    }

    fun buildPagedQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        // Code already implemented
    }

    fun buildTotalCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        // Code already implemented
    }

    fun buildDateCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        // Code already implemented
    }

    fun buildGroupCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        // Code already implemented
    }
}
