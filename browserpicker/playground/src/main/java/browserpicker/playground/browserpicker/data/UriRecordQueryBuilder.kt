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

    // Helper structure to pass WHERE clause details
    private data class WhereClauseResult(
        val statement: String, // e.g., " WHERE ur.host = ? AND hr.status = ?"
        val args: List<Any>,
        val needsHostRuleJoin: Boolean
    )

    fun buildPagedQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val whereClauseResult = buildWhereClause(config)
            val orderByStatement = buildOrderByClause(config)

            val fromClause = if (whereClauseResult.needsHostRuleJoin) {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS LEFT JOIN $HOST_RULES_TABLE $HR_ALIAS ON ${Columns.UR_ASSOCIATED_RULE_ID} = ${Columns.HR_ID}"
            } else {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS"
            }

            val queryString = "SELECT $SELECT_UR_COLUMNS_SQL $fromClause${whereClauseResult.statement}$orderByStatement"

            Timber.tag(TAG).d("Generated Paged Query: %s | Args: %s", queryString, whereClauseResult.args)
            SimpleSQLiteQuery(queryString, whereClauseResult.args.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build paged query for config: %s", config)
            SAFE_EMPTY_QUERY
        }
    }

    fun buildTotalCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val whereClauseResult = buildWhereClause(config)

            val fromClause = if (whereClauseResult.needsHostRuleJoin) {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS LEFT JOIN $HOST_RULES_TABLE $HR_ALIAS ON ${Columns.UR_ASSOCIATED_RULE_ID} = ${Columns.HR_ID}"
            } else {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS"
            }

            val queryString = "SELECT COUNT(${Columns.UR_ID}) $fromClause${whereClauseResult.statement}"

            Timber.tag(TAG).d("Generated Total Count Query: %s | Args: %s", queryString, whereClauseResult.args)
            SimpleSQLiteQuery(queryString, whereClauseResult.args.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build total count query for config: %s", config)
            SAFE_EMPTY_COUNT_QUERY
        }
    }

    fun buildDateCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val dateGroupingExpression = mapGroupFieldToSql(UriRecordGroupField.DATE)
        requireNotNull(dateGroupingExpression) { "Date grouping expression should not be null" }

        return runCatching {
            val whereClauseResult = buildWhereClause(config)

            val fromClause = if (whereClauseResult.needsHostRuleJoin) {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS LEFT JOIN $HOST_RULES_TABLE $HR_ALIAS ON ${Columns.UR_ASSOCIATED_RULE_ID} = ${Columns.HR_ID}"
            } else {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS"
            }

            // Note: DateCount maps to 'date' and 'count' columns
            val queryString = """
                SELECT $dateGroupingExpression as date, COUNT(${Columns.UR_ID}) as count
                $fromClause
                ${whereClauseResult.statement}
                GROUP BY date
                ORDER BY date DESC
            """.trimIndent()

            Timber.tag(TAG).d("Generated Date Count Query: %s | Args: %s", queryString, whereClauseResult.args)
            SimpleSQLiteQuery(queryString, whereClauseResult.args.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build date count query for config: %s", config)
            SAFE_EMPTY_DATE_COUNT_QUERY
        }
    }

    fun buildGroupCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val groupingField = config.groupBy
            val groupingColumnOrExpr = mapGroupFieldToSql(groupingField)

            if (groupingColumnOrExpr == null || groupingField == UriRecordGroupField.NONE || groupingField == UriRecordGroupField.DATE) {
                Timber.tag(TAG).w("buildGroupCountQuery called with invalid/unsupported group field: %s. Returning empty.", groupingField)
                return SAFE_EMPTY_GROUP_COUNT_QUERY
            }

            val whereClauseResult = buildWhereClause(config)

            val fromClause = if (whereClauseResult.needsHostRuleJoin) {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS LEFT JOIN $HOST_RULES_TABLE $HR_ALIAS ON ${Columns.UR_ASSOCIATED_RULE_ID} = ${Columns.HR_ID}"
            } else {
                "FROM $URI_RECORDS_TABLE $UR_ALIAS"
            }

            val effectiveGroupingColumn = if (groupingField == UriRecordGroupField.CHOSEN_BROWSER) {
                "COALESCE($groupingColumnOrExpr, '${GroupingConstants.NULL_BROWSER_GROUP_VALUE}')"
            } else {
                groupingColumnOrExpr // Already uses alias
            }

            val groupOrderBy = "groupValue ${config.groupSortOrder.name}"

            // Note: GroupCount maps to 'groupValue' and 'count' columns
            val queryString = """
                SELECT $effectiveGroupingColumn as groupValue, COUNT(${Columns.UR_ID}) as count
                $fromClause
                ${whereClauseResult.statement}
                GROUP BY groupValue
                ORDER BY $groupOrderBy
            """.trimIndent()

            Timber.tag(TAG).d("Generated Group Count Query (%s): %s | Args: %s", groupingField, queryString, whereClauseResult.args)
            SimpleSQLiteQuery(queryString, whereClauseResult.args.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build group count query for config: %s", config)
            SAFE_EMPTY_GROUP_COUNT_QUERY
        }
    }

    private fun mapSortFieldToSql(field: UriRecordSortField): String {
        return when (field) {
            UriRecordSortField.TIMESTAMP -> Columns.UR_TIMESTAMP
            UriRecordSortField.URI_STRING -> Columns.UR_URI_STRING
            UriRecordSortField.HOST -> Columns.UR_HOST
            UriRecordSortField.CHOSEN_BROWSER -> Columns.UR_CHOSEN_BROWSER
            UriRecordSortField.INTERACTION_ACTION -> Columns.UR_INTERACTION_ACTION
            UriRecordSortField.URI_SOURCE -> Columns.UR_URI_SOURCE
        }
    }

    private fun mapGroupFieldToSql(field: UriRecordGroupField): String? {
        return when (field) {
            UriRecordGroupField.NONE -> null
            UriRecordGroupField.INTERACTION_ACTION -> Columns.UR_INTERACTION_ACTION
            UriRecordGroupField.CHOSEN_BROWSER -> Columns.UR_CHOSEN_BROWSER
            UriRecordGroupField.URI_SOURCE -> Columns.UR_URI_SOURCE
            UriRecordGroupField.HOST -> Columns.UR_HOST
            UriRecordGroupField.DATE -> Expressions.DATE_GROUP
        }
    }

    private fun buildOrderByClause(config: UriRecordQueryConfig): String {
        val orderByClauses = mutableListOf<String>()
        val groupField = config.groupBy
        val groupingExpression = mapGroupFieldToSql(groupField)
        val groupSortOrder = config.groupSortOrder

        if (groupField != UriRecordGroupField.NONE && groupingExpression != null) {
            val groupSortExpression = when (groupField) {
                UriRecordGroupField.CHOSEN_BROWSER -> {
                    "CASE WHEN $groupingExpression IS NULL THEN 1 ELSE 0 END ${groupSortOrder.name}, " +
                            "COALESCE($groupingExpression, '${GroupingConstants.NULL_BROWSER_GROUP_VALUE}') ${groupSortOrder.name}"
                }
                UriRecordGroupField.DATE -> "$groupingExpression ${groupSortOrder.name}"
                else -> "$groupingExpression ${groupSortOrder.name}"
            }
            orderByClauses.add(groupSortExpression)
        }

        val userSortByColumn = mapSortFieldToSql(config.sortBy)
        val userSortOrder = config.sortOrder.name
        val userSortExpression = when (config.sortBy) {
            UriRecordSortField.CHOSEN_BROWSER -> {
                "CASE WHEN $userSortByColumn IS NULL THEN 1 ELSE 0 END ${userSortOrder}, " +
                        "$userSortByColumn $userSortOrder"
            }
            else -> "$userSortByColumn $userSortOrder"
        }
        orderByClauses.add(userSortExpression)

        orderByClauses.add("${Columns.UR_ID} DESC")

        return " ORDER BY ${orderByClauses.joinToString(", ")}"
    }

    private fun buildWhereClause(config: UriRecordQueryConfig): WhereClauseResult {
        val conditions = mutableListOf<String>()
        val queryArgs = mutableListOf<Any>()

        appendSearchTermClause(config.searchQuery, conditions, queryArgs)
        appendUriSourceFilter(config.filterByUriSource, conditions, queryArgs)
        appendInteractionActionFilter(config.filterByInteractionAction, conditions, queryArgs)
        appendChosenBrowserFilter(config.filterByChosenBrowser, conditions, queryArgs)
        appendHostFilter(config.filterByHost, conditions, queryArgs)
        appendDateRangeFilter(config.filterByDateRange, conditions, queryArgs)

        // Process advanced filters and determine if JOIN is needed
        val needsHostRuleJoin = appendAdvancedFilters(config.advancedFilters, conditions, queryArgs)

        val whereStatement = if (conditions.isNotEmpty()) {
            " WHERE ${conditions.joinToString(" AND ")}"
        } else {
            ""
        }

        return WhereClauseResult(whereStatement, queryArgs, needsHostRuleJoin)
    }

    private fun appendSearchTermClause(
        searchTerm: String?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        val trimmedSearch = searchTerm?.trim()
        if (!trimmedSearch.isNullOrEmpty()) {
            val likePattern = "%$trimmedSearch%"
            conditions.add("(${Columns.UR_URI_STRING} LIKE ? OR ${Columns.UR_HOST} LIKE ? OR COALESCE(${Columns.UR_CHOSEN_BROWSER}, '') LIKE ?)")
            queryArgs.add(likePattern)
            queryArgs.add(likePattern)
            queryArgs.add(likePattern)
        }
    }

    private fun appendUriSourceFilter(
        sources: Set<UriSource>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        if (!sources.isNullOrEmpty()) {
            val placeholders = sources.joinToString { "?" }
            conditions.add("${Columns.UR_URI_SOURCE} IN ($placeholders)")
            queryArgs.addAll(sources.map { it.value })
        }
    }

    private fun appendInteractionActionFilter(
        actions: Set<InteractionAction>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        if (!actions.isNullOrEmpty()) {
            val placeholders = actions.joinToString { "?" }
            conditions.add("${Columns.UR_INTERACTION_ACTION} IN ($placeholders)")
            queryArgs.addAll(actions.map { it.value })
        }
    }

    private fun appendChosenBrowserFilter(
        browsers: Set<String?>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        if (!browsers.isNullOrEmpty()) {
            val nonNullBrowsers = browsers.filterNotNull()
            val containsNullFilter = browsers.any { it == null }
            val browserConditions = mutableListOf<String>()

            if (nonNullBrowsers.isNotEmpty()) {
                val placeholders = nonNullBrowsers.joinToString { "?" }
                browserConditions.add("${Columns.UR_CHOSEN_BROWSER} IN ($placeholders)")
                queryArgs.addAll(nonNullBrowsers)
            }
            if (containsNullFilter) {
                browserConditions.add("${Columns.UR_CHOSEN_BROWSER} IS NULL")
            }

            if (browserConditions.isNotEmpty()) {
                conditions.add("(${browserConditions.joinToString(" OR ")})")
            }
        }
    }

    private fun appendHostFilter(
        hosts: Set<String>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        if (!hosts.isNullOrEmpty()) {
            val placeholders = hosts.joinToString { "?" }
            conditions.add("${Columns.UR_HOST} IN ($placeholders)")
            queryArgs.addAll(hosts)
        }
    }

    private fun appendDateRangeFilter(
        dateRange: Pair<Instant, Instant>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        dateRange?.let { (startTime, endTime) ->
            val startMillis = startTime.toEpochMilliseconds()
            val endMillis = endTime.toEpochMilliseconds()

            if (startMillis <= endMillis) {
                conditions.add("${Columns.UR_TIMESTAMP} BETWEEN ? AND ?")
                queryArgs.add(startMillis)
                queryArgs.add(endMillis)
            } else {
                Timber.tag(TAG).w("Invalid date range provided: startTime=%s (%dms), endTime=%s (%dms). Ignoring filter.",
                    startTime, startMillis, endTime, endMillis)
            }
        }
    }

    private fun appendAdvancedFilters(
        advancedFilters: List<UriRecordAdvancedFilterDomain>,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ): Boolean {
        var needsJoin = false
        advancedFilters.forEach { advFilter ->
            when (advFilter) {
                is UriRecordAdvancedFilterDomain.HasAssociatedRule -> {
                    val condition = if (advFilter.hasRule) {
                        "${Columns.UR_ASSOCIATED_RULE_ID} IS NOT NULL"
                    } else {
                        "${Columns.UR_ASSOCIATED_RULE_ID} IS NULL"
                    }
                    conditions.add("($condition)")
                    Timber.tag(TAG).d("Appending advanced filter: HasAssociatedRule(%s) -> SQL='%s'", advFilter.hasRule, condition)
                }
                is UriRecordAdvancedFilterDomain.HasUriStatus -> {
                    needsJoin = true // Mark that JOIN is required
                    val condition = "${Columns.HR_URI_STATUS} = ?"
                    conditions.add("($condition)")
                    queryArgs.add(advFilter.status.value)
                    Timber.tag(TAG).d("Appending advanced filter: HasUriStatus(%s) -> SQL='%s', Args=%s", advFilter.status, condition, advFilter.status.value)
                }
                // Add cases for other advanced filters here...
                // else -> Timber.tag(TAG).w("Unsupported advanced filter type: %s", advFilter::class.simpleName)
            }
        }
        return needsJoin
    }
}
