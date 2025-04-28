package browserpicker.data.local.query

import androidx.sqlite.db.*
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.*
import browserpicker.data.local.query.model.UriRecordQueryConfig
import browserpicker.data.local.query.model.UriRecordAdvancedFilter
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordSortField

@Singleton
class UriRecordQueryBuilder @Inject constructor() {

    companion object {
        private const val TAG = "UriRecordQueryBuilder"

        internal object Columns {
            const val TABLE_NAME = "uri_records"
            const val ID = "uri_record_id"
            const val URI_STRING = "uri_string"
            const val HOST = "host"
            const val TIMESTAMP = "timestamp"
            const val URI_SOURCE = "uri_source"
            const val INTERACTION_ACTION = "interaction_action"
            const val CHOSEN_BROWSER = "chosen_browser_package"
            const val ASSOCIATED_RULE_ID = "associated_host_rule_id"
        }

        internal object Expressions {
            val DATE_GROUP = "STRFTIME('%Y-%m-%d', ${Columns.TIMESTAMP} / 1000, 'unixepoch', 'localtime')"
        }

        internal object GroupingConstants {
            const val NULL_BROWSER_GROUP_VALUE = "browser_picker_null_browser"
        }

        private val SELECT_COLUMNS_SQL = """
            ${Columns.ID}, ${Columns.URI_STRING}, ${Columns.HOST}, ${Columns.TIMESTAMP}, ${Columns.URI_SOURCE},
            ${Columns.INTERACTION_ACTION}, ${Columns.CHOSEN_BROWSER}, ${Columns.ASSOCIATED_RULE_ID}
        """.trimIndent()

        val SAFE_EMPTY_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_QUERY")
            SimpleSQLiteQuery("SELECT $SELECT_COLUMNS_SQL FROM ${Columns.TABLE_NAME} WHERE 0")
        }
        val SAFE_EMPTY_COUNT_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_COUNT_QUERY")
            SimpleSQLiteQuery("SELECT COUNT(${Columns.ID}) FROM ${Columns.TABLE_NAME} WHERE 0")
        }
        val SAFE_EMPTY_DATE_COUNT_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_DATE_COUNT_QUERY")
            SimpleSQLiteQuery("SELECT NULL as dateString, 0 as count WHERE 0")
        }
        val SAFE_EMPTY_GROUP_COUNT_QUERY: SupportSQLiteQuery by lazy {
            Timber.tag(TAG).w("Returning SAFE_EMPTY_GROUP_COUNT_QUERY")
            SimpleSQLiteQuery("SELECT NULL as groupValue, 0 as count WHERE 0")
        }
    }

    fun buildPagedQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val orderByStatement = buildOrderByClause(config)

            val queryString = "SELECT $SELECT_COLUMNS_SQL FROM ${Columns.TABLE_NAME}$whereStatement$orderByStatement"

            Timber.tag(TAG).d("Generated Paged Query: %s | Args: %s", queryString, queryArgs)
            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build paged query for config: %s", config)
            SAFE_EMPTY_QUERY
        }
    }

    fun buildTotalCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val queryString = "SELECT COUNT(${Columns.ID}) FROM ${Columns.TABLE_NAME}$whereStatement"

            Timber.tag(TAG).d("Generated Total Count Query: %s | Args: %s", queryString, queryArgs)
            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build total count query for config: %s", config)
            SAFE_EMPTY_COUNT_QUERY
        }
    }

    fun buildDateCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val dateGroupingExpression = mapGroupFieldToSql(UriRecordGroupField.DATE)
        requireNotNull(dateGroupingExpression)

        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val queryString = """
                SELECT $dateGroupingExpression as dateString, COUNT(${Columns.ID}) as count
                FROM ${Columns.TABLE_NAME}
                $whereStatement
                GROUP BY dateString
                ORDER BY dateString DESC
            """.trimIndent()

            Timber.tag(TAG).d("Generated Date Count Query: %s | Args: %s", queryString, queryArgs)
            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

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

            val (whereStatement, queryArgs) = buildWhereClause(config)

            val effectiveGroupingColumn = if (groupingField == UriRecordGroupField.CHOSEN_BROWSER) {
                "COALESCE($groupingColumnOrExpr, '${GroupingConstants.NULL_BROWSER_GROUP_VALUE}')"
            } else {
                groupingColumnOrExpr
            }

            val groupOrderBy = "groupValue ${config.groupSortOrder.name}"

            val queryString = """
                SELECT $effectiveGroupingColumn as groupValue, COUNT(${Columns.ID}) as count
                FROM ${Columns.TABLE_NAME}
                $whereStatement
                GROUP BY groupValue
                ORDER BY $groupOrderBy
            """.trimIndent()

            Timber.tag(TAG).d("Generated Group Count Query (%s): %s | Args: %s", groupingField, queryString, queryArgs)
            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build group count query for config: %s", config)
            SAFE_EMPTY_GROUP_COUNT_QUERY
        }
    }

    private fun mapSortFieldToSql(field: UriRecordSortField): String {
        return when (field) {
            UriRecordSortField.TIMESTAMP -> Columns.TIMESTAMP
            UriRecordSortField.URI_STRING -> Columns.URI_STRING
            UriRecordSortField.HOST -> Columns.HOST
            UriRecordSortField.CHOSEN_BROWSER -> Columns.CHOSEN_BROWSER
            UriRecordSortField.INTERACTION_ACTION -> Columns.INTERACTION_ACTION
            UriRecordSortField.URI_SOURCE -> Columns.URI_SOURCE
        }
    }

    private fun mapGroupFieldToSql(field: UriRecordGroupField): String? {
        return when (field) {
            UriRecordGroupField.NONE -> null
            UriRecordGroupField.INTERACTION_ACTION -> Columns.INTERACTION_ACTION
            UriRecordGroupField.CHOSEN_BROWSER -> Columns.CHOSEN_BROWSER
            UriRecordGroupField.URI_SOURCE -> Columns.URI_SOURCE
            UriRecordGroupField.HOST -> Columns.HOST
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
                        "$userSortByColumn ${userSortOrder}"
            }
            else -> "$userSortByColumn $userSortOrder"
        }
        orderByClauses.add(userSortExpression)

        orderByClauses.add("${Columns.ID} DESC")

        return " ORDER BY ${orderByClauses.joinToString(", ")}"
    }

    private fun buildWhereClause(config: UriRecordQueryConfig): Pair<String, List<Any>> {
        val conditions = mutableListOf<String>()
        val queryArgs = mutableListOf<Any>()

        appendSearchTermClause(config.searchQuery, conditions, queryArgs)
        appendUriSourceFilter(config.filterByUriSource, conditions, queryArgs)
        appendInteractionActionFilter(config.filterByInteractionAction, conditions, queryArgs)
        appendChosenBrowserFilter(config.filterByChosenBrowser, conditions, queryArgs)
        appendHostFilter(config.filterByHost, conditions, queryArgs)
        appendDateRangeFilter(config.filterByDateRange, conditions, queryArgs)
        appendAdvancedFilters(config.advancedFilters, conditions, queryArgs)

        val whereStatement = if (conditions.isNotEmpty()) {
            " WHERE ${conditions.joinToString(" AND ")}"
        } else {
            ""
        }

        return Pair(whereStatement, queryArgs)
    }

    private fun appendSearchTermClause(
        searchTerm: String?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        val trimmedSearch = searchTerm?.trim()
        if (!trimmedSearch.isNullOrEmpty()) {
            val likePattern = "%$trimmedSearch%"
            conditions.add("(${Columns.URI_STRING} LIKE ? OR ${Columns.HOST} LIKE ? OR COALESCE(${Columns.CHOSEN_BROWSER}, '') LIKE ?)")
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
            conditions.add("${Columns.URI_SOURCE} IN ($placeholders)")
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
            conditions.add("${Columns.INTERACTION_ACTION} IN ($placeholders)")
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
                browserConditions.add("${Columns.CHOSEN_BROWSER} IN ($placeholders)")
                queryArgs.addAll(nonNullBrowsers)
            }
            if (containsNullFilter) {
                browserConditions.add("${Columns.CHOSEN_BROWSER} IS NULL")
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
            conditions.add("${Columns.HOST} IN ($placeholders)")
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
                conditions.add("${Columns.TIMESTAMP} BETWEEN ? AND ?")
                queryArgs.add(startMillis)
                queryArgs.add(endMillis)
            } else {
                Timber.tag(TAG).w("Invalid date range provided: startTime=%s (%dms), endTime=%s (%dms). Ignoring filter.",
                    startTime, startMillis, endTime, endMillis)
            }
        }
    }

    private fun appendAdvancedFilters(
        advancedFilters: List<UriRecordAdvancedFilter>,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        advancedFilters.forEach { advFilter ->
            conditions.add("(${advFilter.customSqlCondition})")
            queryArgs.addAll(advFilter.args)
            Timber.tag(TAG).d("Appending advanced filter: SQL='%s', Args=%s", advFilter.customSqlCondition, advFilter.args)
        }
    }
}
