package browserpicker.data.local.query

import android.annotation.SuppressLint
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import android.util.Log
import androidx.room.*
import browserpicker.domain.model.query.LogGroupField
import browserpicker.domain.model.query.LogQueryConfig

@SuppressLint("LogNotTimber")
object LogQueryBuilder {

    private const val TAG = "LogQueryBuilder"

    // --- Table and Column Constants ---
    private const val TABLE_NAME = "interaction_log"
    private const val COL_ID = "interaction_id"
    private const val COL_INTERCEPTED_URI = "intercepted_uri"
    private const val COL_URI_SOURCE = "uri_source"
    private const val COL_TIMESTAMP = "intercepted_timestamp"
    private const val COL_BROWSER_PACKAGE = "chosen_browser_package_name"
    private const val COL_ACTION_TAKEN = "action_taken"

    // --- Safe Query Constants ---
    private const val SELECT_COLUMNS = "$COL_ID, $COL_INTERCEPTED_URI, $COL_URI_SOURCE, $COL_TIMESTAMP, $COL_BROWSER_PACKAGE, $COL_ACTION_TAKEN"
    private val SAFE_EMPTY_QUERY = SimpleSQLiteQuery("SELECT $SELECT_COLUMNS FROM $TABLE_NAME WHERE 0")
    private val SAFE_EMPTY_COUNT_QUERY = SimpleSQLiteQuery("SELECT COUNT(*) FROM $TABLE_NAME WHERE 0")
    private val SAFE_EMPTY_DATE_COUNT_QUERY = SimpleSQLiteQuery("SELECT NULL as dateString, 0 as count WHERE 0") // Return no rows
    private val SAFE_EMPTY_GROUP_COUNT_QUERY = SimpleSQLiteQuery("SELECT NULL as groupValue, 0 as count WHERE 0") // Return no rows

    fun buildQuery(config: LogQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)
            val userSortByColumn = config.sortBy.dbColumnName
            val userSortOrder = config.sortOrder.name
            val groupingColumn = config.groupBy.dbColumnName
            val groupField = config.groupBy

            // --- Build ORDER BY Clause ---
            val orderByClauses = mutableListOf<String>()

            // 1. Primary Sort: Grouping Column (if grouping is active)
            //    - We typically sort groups Ascending (e.g., A-Z, oldest-newest date) for logical structure,
            //      regardless of the user's preferred sort order for items *within* the group.
            //    - Use COALESCE for nullable columns like browser to ensure NULLs are handled consistently (e.g., grouped last).
            if (groupField != LogGroupField.NONE && groupingColumn != null) {
                // Handle potential NULLs in grouping column for consistent sorting.
                // Sorting NULLs last (using high value like 'ZZZ') is common.
                val effectiveGroupingColumn = when (groupField) {
                    LogGroupField.BROWSER -> "COALESCE($groupingColumn, 'ZZZ_Unknown')" // Sort nulls/unknown last
                    // Add other cases if null handling is needed for other enum types
                    else -> groupingColumn
                }
                // Groups should typically be ordered ASC for logical blocks, regardless of user sort preference.
                orderByClauses.add("$effectiveGroupingColumn ASC")
            }

            // 2. Secondary Sort: User's selected field and order
            //    - This determines the order of items *within* each group (or the overall order if not grouping).
            orderByClauses.add("$userSortByColumn $userSortOrder")

            // 3. Tertiary Sort (Tie Breaker): Primary Key (ensures stable order)
            //    - If the primary and secondary sort keys are identical for multiple rows,
            //      sorting by a unique ID ensures the order is deterministic and stable across refreshes.
            //    - Use DESC to often align with the default timestamp DESC preference when timestamps are equal.
            orderByClauses.add("$COL_ID DESC")

            val orderByStatement = if (orderByClauses.isNotEmpty()) {
                " ORDER BY ${orderByClauses.joinToString(", ")}"
            } else {
                ""
            }

            val queryString = "SELECT $SELECT_COLUMNS FROM $TABLE_NAME$whereStatement$orderByStatement"

            Log.d(TAG, "Generated Entity Query: $queryString")
            Log.d(TAG, "Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Log.e(TAG, "Failed to build entity query for config: $config", e)
            SAFE_EMPTY_QUERY
        }
    }

    fun buildTotalCountQuery(config: LogQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)

            val queryString = "SELECT COUNT(*) FROM $TABLE_NAME$whereStatement"

            Log.d(TAG, "Generated Total Count Query: $queryString")
            Log.d(TAG, "Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Log.e(TAG, "Failed to build total count query for config: $config", e)
            SAFE_EMPTY_COUNT_QUERY
        }
    }

    fun buildDateCountQuery(config: LogQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val (whereStatement, queryArgs) = buildWhereClause(config)

            val queryString = """
                SELECT STRFTIME('%Y-%m-%d', $COL_TIMESTAMP / 1000, 'unixepoch', 'localtime') as dateString, COUNT(*) as count
                FROM $TABLE_NAME
                $whereStatement
                GROUP BY dateString
                ORDER BY dateString DESC -- Order by date desc is useful for processing
            """.trimIndent()

            Log.d(TAG, "Generated Date Count Query: $queryString")
            Log.d(TAG, "Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Log.e(TAG, "Failed to build date count query for config: $config", e)
            SAFE_EMPTY_DATE_COUNT_QUERY
        }
    }

    fun buildGroupCountQuery(config: LogQueryConfig): SupportSQLiteQuery {
        return runCatching {
            val groupingField = config.groupBy
            val groupingColumn = groupingField.dbColumnName

            // Should not happen if called from DAO correctly, but defensive check.
            if (groupingColumn == null || groupingField == LogGroupField.NONE || groupingField == LogGroupField.DATE) {
                Log.e(TAG, "buildGroupCountQuery called with invalid or handled group field: $groupingField. Returning empty.")
                return SAFE_EMPTY_GROUP_COUNT_QUERY
            }

            val (whereStatement, queryArgs) = buildWhereClause(config)

            // Handle potential NULLs in browser package name
            // Group null browser entries under a specific display name for clarity.
            val effectiveGroupingColumn = if (groupingField == LogGroupField.BROWSER) {
                "COALESCE($groupingColumn, 'Unknown Browser')" // Group nulls into 'Unknown Browser'
            } else {
                groupingColumn
            }

            val queryString = """
                SELECT $effectiveGroupingColumn as groupValue, COUNT(*) as count
                FROM $TABLE_NAME
                $whereStatement
                GROUP BY groupValue
                ORDER BY groupValue DESC -- Consistent ordering, adjust if needed
            """.trimIndent()

            Log.d(TAG, "Generated Group Count Query ($groupingField): $queryString")
            Log.d(TAG, "Query Args: $queryArgs")

            SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())

        }.getOrElse { e ->
            Log.e(TAG, "Failed to build group count query for config: $config", e)
            SAFE_EMPTY_GROUP_COUNT_QUERY
        }
    }

    private fun buildWhereClause(config: LogQueryConfig): Pair<String, List<Any>> {
        // ... (This function remains the same as in the previous version) ...
        val queryArgs = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        // Search Query
        config.searchQuery?.takeIf { it.isNotBlank() }?.let { searchTerm ->
            val likePattern = "%${searchTerm.trim()}%"
            whereClauses.add("($COL_INTERCEPTED_URI LIKE ? OR $COL_BROWSER_PACKAGE LIKE ?)")
            queryArgs.add(likePattern)
            queryArgs.add(likePattern)
        }
        // Filter by UriSource(s)
        config.filterByUriSource?.takeIf { it.isNotEmpty() }?.let { sources ->
            val placeholders = sources.joinToString { "?" }
            whereClauses.add("$COL_URI_SOURCE IN ($placeholders)")
            queryArgs.addAll(sources.map { it.name })
        }
        // Filter by InteractionAction(s)
        config.filterByAction?.takeIf { it.isNotEmpty() }?.let { actions ->
            val placeholders = actions.joinToString { "?" }
            whereClauses.add("$COL_ACTION_TAKEN IN ($placeholders)")
            queryArgs.addAll(actions.map { it.name })
        }
        // Filter by specific Browser Package Name
        config.filterByBrowser?.takeIf { it.isNotEmpty() }?.let { browser ->
            val placeholders = browser.joinToString { "?" }
            whereClauses.add("$COL_BROWSER_PACKAGE IN ($placeholders)")
            queryArgs.addAll(browser.map { it })
        }
        // Filter by Date Range
        config.filterByDateRange?.let { (startTime, endTime) ->
            if (startTime <= endTime) {
                whereClauses.add("$COL_TIMESTAMP BETWEEN ? AND ?")
                queryArgs.add(startTime)
                queryArgs.add(endTime)
            } else {
                Log.w(TAG, "Invalid date range provided: startTime=$startTime, endTime=$endTime. Ignoring.")
            }
        }
        // Advanced Filters
        config.advancedFilters.forEach { advFilter ->
            whereClauses.add("(${advFilter.customSqlCondition})")
            queryArgs.addAll(advFilter.args)
        }


        val whereStatement = if (whereClauses.isNotEmpty()) {
            " WHERE ${whereClauses.joinToString(" AND ")}"
        } else {
            ""
        }
        return Pair(whereStatement, queryArgs)
    }
}
