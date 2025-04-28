package browserpicker.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordQueryConfig
import browserpicker.domain.model.query.UriRecordSortField
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.GroupKey
import browserpicker.domain.model.query.UriRecordAdvancedFilter
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriRecordQueryBuilder @Inject constructor() {
    private val TAG = "UriRecordQueryBuilder"

    // --- Column Name Constants (Ensure these EXACTLY match UriRecordEntity @ColumnInfo names) ---
    // It's crucial these are accurate and maintained alongside the Entity definition.
    private object Columns {
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

    private object SafeQuery {
        // --- Selected Columns for standard queries ---
        val SELECT_COLUMNS_SQL = """
        ${Columns.ID}, ${Columns.URI_STRING}, ${Columns.HOST}, ${Columns.TIMESTAMP}, ${Columns.URI_SOURCE},
        ${Columns.INTERACTION_ACTION}, ${Columns.CHOSEN_BROWSER}, ${Columns.ASSOCIATED_RULE_ID}
    """.trimIndent()

        // --- Safe Fallback Queries ---
        val SAFE_EMPTY_QUERY by lazy {
            SimpleSQLiteQuery("SELECT $SELECT_COLUMNS_SQL FROM ${Columns.TABLE_NAME} WHERE 0")
        }
        val SAFE_EMPTY_COUNT_QUERY by lazy {
            SimpleSQLiteQuery("SELECT COUNT(*) FROM ${Columns.TABLE_NAME} WHERE 0")
        }
        val SAFE_EMPTY_DATE_COUNT_QUERY by lazy {
            // Provides the expected columns (dateString, count) with zero results
            SimpleSQLiteQuery("SELECT NULL as dateString, 0 as count WHERE 0")
        }
        val SAFE_EMPTY_GROUP_COUNT_QUERY by lazy {
            // Provides the expected columns (groupValue, count) with zero results
            SimpleSQLiteQuery("SELECT NULL as groupValue, 0 as count WHERE 0")
        }
    }


    // === Public Query Building Methods ===

    /**
     * Builds a query suitable for PagingSource, incorporating filtering, sorting, and grouping display logic.
     */
    fun buildPagedQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val orderByStatement = buildOrderByClause(config)

            val queryString = "SELECT ${SafeQuery.SELECT_COLUMNS_SQL} FROM ${Columns.TABLE_NAME}$whereStatement$orderByStatement"

            Timber.tag(TAG).d("Generated Paged Query: %s | Args: %s", queryString, queryArgs)

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build paged query for config: %s", config)
            SafeQuery.SAFE_EMPTY_QUERY
        }
    }

    /**
     * Builds a query to count the total number of records matching the filter criteria.
     */
    fun buildTotalCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val queryString = "SELECT COUNT(${Columns.ID}) FROM ${Columns.TABLE_NAME}$whereStatement"

            Timber.tag(TAG).d("Generated Total Count Query: %s | Args: %s", queryString, queryArgs)

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build total count query for config: %s", config)
            SafeQuery.SAFE_EMPTY_COUNT_QUERY
        }
    }

    /**
     * Builds a query to count records grouped by date (YYYY-MM-DD).
     */
    fun buildDateCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val dateGroupingExpression = UriRecordGroupField.DATE.dbColumnNameOrExpression
        requireNotNull(dateGroupingExpression) { "Date grouping SQL expression is unexpectedly missing!" }

        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            // Alias the grouping expression to match the DateCount DTO
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
            SafeQuery.SAFE_EMPTY_DATE_COUNT_QUERY
        }
    }

    /**
     * Builds a query to count records grouped by a specific field (excluding Date and None).
     */
    fun buildGroupCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val groupingField = config.groupBy
            val groupingColumn = groupingField.dbColumnNameOrExpression

            // Validate if grouping is feasible and requested
            if (groupingColumn == null || groupingField == UriRecordGroupField.NONE || groupingField == UriRecordGroupField.DATE) {
                Timber.tag(TAG).w("buildGroupCountQuery called with invalid/unsupported group field: %s. Returning empty.", groupingField)
                return SafeQuery.SAFE_EMPTY_GROUP_COUNT_QUERY
            }

            val (whereStatement, queryArgs) = buildWhereClause(config)
            val effectiveGroupingColumn = if (groupingField == UriRecordGroupField.CHOSEN_BROWSER) {
                "COALESCE($groupingColumn, '${GroupKey.NULL_BROWSER_GROUP_VALUE}')"
            } else {
                groupingColumn
            }

            val queryString = """
                SELECT $effectiveGroupingColumn as groupValue, COUNT(${Columns.ID}) as count
                FROM ${Columns.TABLE_NAME}
                $whereStatement
                GROUP BY groupValue
                ORDER BY groupValue ASC
            """.trimIndent()

            Timber.tag(TAG).d("Generated Group Count Query (%s): %s | Args: %s", groupingField, queryString, queryArgs)

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to build group count query for config: %s", config)
            SafeQuery.SAFE_EMPTY_GROUP_COUNT_QUERY // Return safe query on error
        }
    }


    // === Private Helper Methods ===

    /**
     * Constructs the ORDER BY clause based on grouping and sorting configuration.
     * Ensures stable sorting by adding primary key as a secondary sort criterion.
     */
    private fun buildOrderByClause(config: UriRecordQueryConfig): String {
        val orderByClauses = mutableListOf<String>()
        val groupField = config.groupBy
        val groupingExpression = groupField.dbColumnNameOrExpression

        // 1. Grouping Order (if grouping is enabled)
        if (groupField != UriRecordGroupField.NONE && groupingExpression != null) {
            val groupSortExpression = when (groupField) {
                UriRecordGroupField.CHOSEN_BROWSER -> "CASE WHEN $groupingExpression IS NULL THEN 1 ELSE 0 END, COALESCE($groupingExpression, '${GroupKey.NULL_BROWSER_GROUP_VALUE}')" // Sort nulls last
                UriRecordGroupField.DATE -> groupingExpression
                else -> groupingExpression
            }
            // TODO: Make the Group sorting dynamic and should me customisable on sorting instead of hard coded sorting.
            orderByClauses.add("$groupSortExpression ASC")
        }

        // 2. User-defined Sort Order
        val userSortByColumn = config.sortBy.dbColumnName
        val userSortOrder = config.sortOrder.name
        val userSortExpression = when (config.sortBy) {
            UriRecordSortField.CHOSEN_BROWSER -> "CASE WHEN $userSortByColumn IS NULL THEN 1 ELSE 0 END, $userSortByColumn"
            else -> userSortByColumn
        }
        orderByClauses.add("$userSortExpression $userSortOrder")

        // 3. Stable Sort: Add primary key descending as the final tie-breaker
        // Ensures consistent ordering even if all other fields are identical.
        orderByClauses.add("${Columns.ID} DESC")

        return " ORDER BY ${orderByClauses.joinToString(", ")}"
    }

    /**
     * Constructs the WHERE clause and collects bind arguments based on the config.
     * Returns Pair(whereStatement: String, queryArgs: List<Any>).
     */
    private fun buildWhereClause(config: UriRecordQueryConfig): Pair<String, List<Any>> {
        val conditions = mutableListOf<String>()
        val queryArgs = mutableListOf<Any>()

        // Apply filters sequentially, modifying conditions and queryArgs
        appendSearchTermClause(config.searchQuery, conditions, queryArgs)
        appendUriSourceFilter(config.filterByUriSource, conditions, queryArgs)
        appendInteractionActionFilter(config.filterByInteractionAction, conditions, queryArgs)
        appendChosenBrowserFilter(config.filterByChosenBrowser, conditions, queryArgs)
        appendHostFilter(config.filterByHost, conditions, queryArgs)
        appendDateRangeFilter(config.filterByDateRange, conditions, queryArgs)
        appendAdvancedFilters(config.advancedFilters, conditions, queryArgs)

        // Combine all conditions with AND
        val whereStatement = if (conditions.isNotEmpty()) {
            " WHERE ${conditions.joinToString(" AND ")}"
        } else {
            ""
        }

        return Pair(whereStatement, queryArgs)
    }

    // --- Specific Clause Appenders ---

    private fun appendSearchTermClause(
        searchTerm: String?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        searchTerm?.trim()?.takeIf { it.isNotEmpty() }?.let { trimmedSearchTerm ->
            val likePattern = "%$trimmedSearchTerm%"
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
        sources?.takeIf { it.isNotEmpty() }?.let { validSources ->
            val placeholders = validSources.joinToString { "?" }
            conditions.add("${Columns.URI_SOURCE} IN ($placeholders)")
            queryArgs.addAll(validSources.map { it.value })
        }
    }

    private fun appendInteractionActionFilter(
        actions: Set<InteractionAction>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        actions?.takeIf { it.isNotEmpty() }?.let { validActions ->
            val placeholders = validActions.joinToString { "?" }
            conditions.add("${Columns.INTERACTION_ACTION} IN ($placeholders)")
            queryArgs.addAll(validActions.map { it.value })
        }
    }

    private fun appendChosenBrowserFilter(
        browsers: Set<String?>?,
        conditions: MutableList<String>,
        queryArgs: MutableList<Any>
    ) {
        browsers?.takeIf { it.isNotEmpty() }?.let { validBrowsers ->
            val nonNullBrowsers = validBrowsers.filterNotNull()
            val containsNullFilter = validBrowsers.any { it == null }
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
        hosts?.takeIf { it.isNotEmpty() }?.let { validHosts ->
            val placeholders = validHosts.joinToString { "?" }
            conditions.add("${Columns.HOST} IN ($placeholders)")
            queryArgs.addAll(validHosts)
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
                Timber.tag(TAG).w("Invalid date range provided: startTime=%s, endTime=%s. Ignoring filter.", startTime, endTime)
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
        }
    }
}
