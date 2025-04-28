package browserpicker.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.domain.model.query.GroupKey.ChosenBrowserKey
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordQueryConfig
import browserpicker.domain.model.query.UriRecordSortField

object UriRecordQueryBuilder {
    private const val TAG = "UriRecordQueryBuilder"

    private const val TABLE_NAME = "uri_records"
    private const val COL_ID = "uri_record_id"
    private const val COL_URI_STRING = "uri_string"
    private const val COL_HOST = "host"
    private const val COL_TIMESTAMP = "timestamp"
    private const val COL_URI_SOURCE = "uri_source"
    private const val COL_INTERACTION_ACTION = "interaction_action"
    private const val COL_CHOSEN_BROWSER = "chosen_browser_package"
    private const val COL_ASSOCIATED_RULE_ID = "associated_host_rule_id"

    private const val SELECT_COLUMNS = """
        $COL_ID, $COL_URI_STRING, $COL_HOST, $COL_TIMESTAMP, $COL_URI_SOURCE,
        $COL_INTERACTION_ACTION, $COL_CHOSEN_BROWSER, $COL_ASSOCIATED_RULE_ID
    """
    private val SAFE_EMPTY_QUERY = SimpleSQLiteQuery("SELECT $SELECT_COLUMNS FROM $TABLE_NAME WHERE 0")
    private val SAFE_EMPTY_COUNT_QUERY = SimpleSQLiteQuery("SELECT COUNT(*) FROM $TABLE_NAME WHERE 0")
    private val SAFE_EMPTY_DATE_COUNT_QUERY = SimpleSQLiteQuery("SELECT NULL as dateString, 0 as count WHERE 0")
    private val SAFE_EMPTY_GROUP_COUNT_QUERY = SimpleSQLiteQuery("SELECT NULL as groupValue, 0 as count WHERE 0")

    fun buildPagedQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val userSortByColumn = config.sortBy.dbColumnName
            val userSortOrder = config.sortOrder.name
            val groupingExpression = config.groupBy.dbColumnNameOrExpression
            val groupField = config.groupBy

            val orderByClauses = mutableListOf<String>()

            if (groupField != UriRecordGroupField.NONE && groupingExpression != null) {
                val effectiveGroupingSort = when (groupField) {
                    UriRecordGroupField.CHOSEN_BROWSER -> "COALESCE($groupingExpression, 'ZZZ_Unknown')"
                    UriRecordGroupField.DATE -> groupingExpression
                    else -> groupingExpression
                }
                orderByClauses.add("$effectiveGroupingSort ASC")
            }

            val effectiveUserSortColumn = when (config.sortBy) {
                UriRecordSortField.CHOSEN_BROWSER -> "COALESCE($userSortByColumn, 'ZZZ_Unknown')"
                else -> userSortByColumn
            }
            orderByClauses.add("$effectiveUserSortColumn $userSortOrder")
            orderByClauses.add("$COL_ID DESC")

            val orderByStatement = " ORDER BY ${orderByClauses.joinToString(", ")}"

            val queryString = "SELECT $SELECT_COLUMNS FROM $TABLE_NAME$whereStatement$orderByStatement"

            println("$TAG: Generated Paged Query: $queryString")
            println("$TAG: Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            System.err.println("$TAG: Failed to build paged query for config: $config. Error: ${e.message}")
            e.printStackTrace()
            SAFE_EMPTY_QUERY
        }
    }

    fun buildTotalCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val queryString = "SELECT COUNT(*) FROM $TABLE_NAME$whereStatement"

            println("$TAG: Generated Total Count Query: $queryString")
            println("$TAG: Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            System.err.println("$TAG: Failed to build total count query for config: $config. Error: ${e.message}")
            e.printStackTrace()
            SAFE_EMPTY_COUNT_QUERY
        }
    }

    fun buildDateCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val dateGroupingExpression = UriRecordGroupField.DATE.dbColumnNameOrExpression
        requireNotNull(dateGroupingExpression) { "Date grouping expression is missing!" }

        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val queryString = """
                SELECT $dateGroupingExpression as dateString, COUNT(*) as count
                FROM $TABLE_NAME
                $whereStatement
                GROUP BY dateString
                ORDER BY dateString DESC -- Often useful to process newest dates first
            """.trimIndent()

            println("$TAG: Generated Date Count Query: $queryString")
            println("$TAG: Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            System.err.println("$TAG: Failed to build date count query for config: $config. Error: ${e.message}")
            e.printStackTrace()
            SAFE_EMPTY_DATE_COUNT_QUERY
        }
    }

    fun buildGroupCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val groupingField = config.groupBy
            val groupingColumn = groupingField.dbColumnNameOrExpression
            if (groupingColumn == null || groupingField == UriRecordGroupField.NONE || groupingField == UriRecordGroupField.DATE) {
                System.err.println("$TAG: buildGroupCountQuery called with invalid group field: $groupingField. Use specific methods or NONE.")
                return SAFE_EMPTY_GROUP_COUNT_QUERY
            }

            val (whereStatement, queryArgs) = buildWhereClause(config)

            val effectiveGroupingColumn = if (groupingField == UriRecordGroupField.CHOSEN_BROWSER) {
                "COALESCE($groupingColumn, '${ChosenBrowserKey.NULL_BROWSER_MARKER}')"
            } else {
                groupingColumn
            }

            val queryString = """
                SELECT $effectiveGroupingColumn as groupValue, COUNT(*) as count
                FROM $TABLE_NAME
                $whereStatement
                GROUP BY groupValue
                ORDER BY groupValue ASC -- Consistent ordering, adjust if needed (e.g., count DESC)
            """.trimIndent()

            println("$TAG: Generated Group Count Query ($groupingField): $queryString")
            println("$TAG: Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            System.err.println("$TAG: Failed to build group count query for config: $config. Error: ${e.message}")
            e.printStackTrace()
            SAFE_EMPTY_GROUP_COUNT_QUERY
        }
    }

    private fun buildWhereClause(config: UriRecordQueryConfig): Pair<String, List<Any>> {
        val queryArgs = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        // --- Search Query ---
        config.searchQuery?.takeIf { it.isNotBlank() }?.let { searchTerm ->
            val likePattern = "%${searchTerm.trim()}%"
            whereClauses.add("($COL_URI_STRING LIKE ? OR $COL_HOST LIKE ? OR COALESCE($COL_CHOSEN_BROWSER, '') LIKE ?)")
            queryArgs.add(likePattern)
            queryArgs.add(likePattern)
            queryArgs.add(likePattern)
        }

        // --- Filter by UriSource(s) ---
        config.filterByUriSource?.takeIf { it.isNotEmpty() }?.let { sources ->
            val values = sources.map { it.value }
            val placeholders = values.joinToString { "?" }
            whereClauses.add("$COL_URI_SOURCE IN ($placeholders)")
            queryArgs.addAll(values)
        }

        // --- Filter by InteractionAction(s) ---
        config.filterByInteractionAction?.takeIf { it.isNotEmpty() }?.let { actions ->
            val values = actions.map { it.value }
            val placeholders = values.joinToString { "?" }
            whereClauses.add("$COL_INTERACTION_ACTION IN ($placeholders)")
            queryArgs.addAll(values)
        }

        // --- Filter by specific Browser Package Name(s) ---
        config.filterByChosenBrowser?.takeIf { it.isNotEmpty() }?.let { browsers ->
            val nonNullBrowsers = browsers.filterNotNull()
            val containsNullFilter = browsers.any { it == null }
            val conditions = mutableListOf<String>()

            if (nonNullBrowsers.isNotEmpty()) {
                val placeholders = nonNullBrowsers.joinToString { "?" }
                conditions.add("$COL_CHOSEN_BROWSER IN ($placeholders)")
                queryArgs.addAll(nonNullBrowsers)
            }
            if (containsNullFilter) {
                conditions.add("$COL_CHOSEN_BROWSER IS NULL")
            }

            if (conditions.isNotEmpty()) {
                whereClauses.add("(${conditions.joinToString(" OR ")})")
            }
        }

        // --- Filter by specific Host(s) ---
        config.filterByHost?.takeIf { it.isNotEmpty() }?.let { hosts ->
            val placeholders = hosts.joinToString { "?" }
            whereClauses.add("$COL_HOST IN ($placeholders)")
            queryArgs.addAll(hosts)
        }

        // --- Filter by Date Range ---
        config.filterByDateRange?.let { (startTime, endTime) ->
            val startMillis = startTime.toEpochMilliseconds()
            val endMillis = endTime.toEpochMilliseconds()
            if (startMillis <= endMillis) {
                whereClauses.add("$COL_TIMESTAMP BETWEEN ? AND ?")
                queryArgs.add(startMillis)
                queryArgs.add(endMillis)
            } else {
                System.err.println("$TAG: Invalid date range provided: startTime=$startTime, endTime=$endTime. Ignoring.")
            }
        }

        // --- Advanced Filters ---
        config.advancedFilters.forEach { advFilter ->
            whereClauses.add("(${advFilter.customSqlCondition})")
            queryArgs.addAll(advFilter.args)
        }

        // Combine all clauses with AND
        val whereStatement = if (whereClauses.isNotEmpty()) {
            " WHERE ${whereClauses.joinToString(" AND ")}"
        } else {
            ""
        }

        return Pair(whereStatement, queryArgs)
    }
}
