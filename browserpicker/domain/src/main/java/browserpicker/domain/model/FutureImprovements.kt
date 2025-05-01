package browserpicker.domain.model
/*
import androidx.compose.runtime.Immutable
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordSortField
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriRecordQueryBuilder @Inject constructor() {
    /**
     * Builds a dynamic query for paged results of URI records with full filtering, sorting and grouping.
     */
    fun buildPagedQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val queryBuilder = StringBuilder("SELECT * FROM uri_records")
        val whereClause = buildWhereClause(config)
        val args = mutableListOf<Any>()

        if (whereClause.isNotEmpty()) {
            queryBuilder.append(" WHERE ").append(whereClause.first)
            args.addAll(whereClause.second)
        }

        // Add sorting
        queryBuilder.append(" ORDER BY ")
        queryBuilder.append(getSortFieldColumn(config.sortBy))
        queryBuilder.append(if (config.sortOrder == SortOrder.ASC) " ASC" else " DESC")

        Timber.d("Built paged query: $queryBuilder with ${args.size} args")
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toArray())
    }

    /**
     * Builds a query to count the total number of URI records matching the filters.
     */
    fun buildTotalCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val queryBuilder = StringBuilder("SELECT COUNT(*) FROM uri_records")
        val whereClause = buildWhereClause(config)
        val args = mutableListOf<Any>()

        if (whereClause.isNotEmpty()) {
            queryBuilder.append(" WHERE ").append(whereClause.first)
            args.addAll(whereClause.second)
        }

        Timber.d("Built count query: $queryBuilder with ${args.size} args")
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toArray())
    }

    /**
     * Builds a query to get date-based counts for timeline visualization.
     */
    fun buildDateCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        // Extract date part for grouping by day
        val queryBuilder = StringBuilder(
            "SELECT date(timestamp/1000, 'unixepoch') as date_value, COUNT(*) as count "
                    + "FROM uri_records"
        )

        val whereClause = buildWhereClause(config)
        val args = mutableListOf<Any>()

        if (whereClause.isNotEmpty()) {
            queryBuilder.append(" WHERE ").append(whereClause.first)
            args.addAll(whereClause.second)
        }

        queryBuilder.append(" GROUP BY date_value")
        queryBuilder.append(" ORDER BY date_value")
        queryBuilder.append(if (config.groupSortOrder == SortOrder.ASC) " ASC" else " DESC")

        Timber.d("Built date count query: $queryBuilder with ${args.size} args")
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    /**
     * Builds a query to get counts grouped by a specified field for visualization and statistics.
     */
    fun buildGroupCountQuery(config: UriRecordQueryConfig): SupportSQLiteQuery {
        val groupField = when (config.groupBy) {
            UriRecordGroupField.NONE -> {
                Timber.w("Attempted to build group count query with NONE groupBy field")
                return SimpleSQLiteQuery("SELECT NULL as group_value, COUNT(*) as count FROM uri_records LIMIT 0")
            }
            UriRecordGroupField.DATE -> return buildDateCountQuery(config)
            UriRecordGroupField.INTERACTION_ACTION -> "interaction_action"
            UriRecordGroupField.CHOSEN_BROWSER -> "chosen_browser_package"
            UriRecordGroupField.URI_SOURCE -> "uri_source"
            UriRecordGroupField.HOST -> "host"
        }

        val queryBuilder = StringBuilder(
            "SELECT $groupField as group_value, COUNT(*) as count "
                    + "FROM uri_records"
        )

        val whereClause = buildWhereClause(config)
        val args = mutableListOf<Any>()

        if (whereClause.isNotEmpty()) {
            queryBuilder.append(" WHERE ").append(whereClause.first)
            args.addAll(whereClause.second)
        }

        queryBuilder.append(" GROUP BY $groupField")
        queryBuilder.append(" ORDER BY count")
        queryBuilder.append(if (config.groupSortOrder == SortOrder.ASC) " ASC" else " DESC")

        Timber.d("Built group count query: $queryBuilder with ${args.size} args")
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    /**
     * Builds a WHERE clause for filtering based on all provided conditions.
     * @return Pair of (SQL clause, argument list)
     */
    private fun buildWhereClause(config: UriRecordQueryConfig): Pair<String, List<Any>> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        // Text search
        config.searchQuery?.takeIf { it.isNotBlank() }?.let { search ->
            conditions.add("(uri_string LIKE ? OR host LIKE ?)")
            val searchPattern = "%$search%"
            args.add(searchPattern)
            args.add(searchPattern)
        }

        // Filter by URI source
        config.filterByUriSource?.takeIf { it.isNotEmpty() }?.let { sources ->
            val sourceValues = sources.joinToString(", ") { it.value.toString() }
            conditions.add("uri_source IN ($sourceValues)")
        }

        // Filter by interaction action
        config.filterByInteractionAction?.takeIf { it.isNotEmpty() }?.let { actions ->
            val actionValues = actions.joinToString(", ") { it.value.toString() }
            conditions.add("interaction_action IN ($actionValues)")
        }

        // Filter by chosen browser (including null)
        config.filterByChosenBrowser?.takeIf { it.isNotEmpty() }?.let { browsers ->
            val nullInFilter = browsers.any { it == null }
            val nonNullBrowsers = browsers.filterNotNull()

            if (nonNullBrowsers.isEmpty() && nullInFilter) {
                conditions.add("chosen_browser_package IS NULL")
            } else if (nullInFilter) {
                val placeholders = nonNullBrowsers.joinToString(", ") { "?" }
                conditions.add("(chosen_browser_package IN ($placeholders) OR chosen_browser_package IS NULL)")
                args.addAll(nonNullBrowsers)
            } else {
                val placeholders = nonNullBrowsers.joinToString(", ") { "?" }
                conditions.add("chosen_browser_package IN ($placeholders)")
                args.addAll(nonNullBrowsers)
            }
        }

        // Filter by host
        config.filterByHost?.takeIf { it.isNotEmpty() }?.let { hosts ->
            val placeholders = hosts.joinToString(", ") { "?" }
            conditions.add("host IN ($placeholders)")
            args.addAll(hosts)
        }

        // Filter by date range
        config.filterByDateRange?.let { (start, end) ->
            conditions.add("timestamp BETWEEN ? AND ?")
            args.add(start.toEpochMilliseconds())
            args.add(end.toEpochMilliseconds())
        }

        // Apply advanced filters
        config.advancedFilters.forEach { filter ->
            val (condition, filterArgs) = buildAdvancedFilterCondition(filter)
            conditions.add(condition)
            args.addAll(filterArgs)
        }

        return if (conditions.isEmpty()) {
            Pair("", emptyList())
        } else {
            Pair(conditions.joinToString(" AND "), args)
        }
    }

    /**
     * Builds a SQL condition for an advanced filter with proper SQL injection prevention.
     */
    private fun buildAdvancedFilterCondition(filter: UriRecordAdvancedFilterDomain): Pair<String, List<Any>> {
        val args = mutableListOf<Any>()

        val condition = when (filter) {
            is UriRecordAdvancedFilterDomain.StringFilter -> {
                when (filter.operator) {
                    FilterOperator.EQUALS -> {
                        args.add(filter.value)
                        "${filter.fieldName} = ?"
                    }
                    FilterOperator.NOT_EQUALS -> {
                        args.add(filter.value)
                        "${filter.fieldName} <> ?"
                    }
                    FilterOperator.CONTAINS -> {
                        args.add("%${filter.value}%")
                        "${filter.fieldName} LIKE ?"
                    }
                    FilterOperator.NOT_CONTAINS -> {
                        args.add("%${filter.value}%")
                        "${filter.fieldName} NOT LIKE ?"
                    }
                    FilterOperator.STARTS_WITH -> {
                        args.add("${filter.value}%")
                        "${filter.fieldName} LIKE ?"
                    }
                    FilterOperator.ENDS_WITH -> {
                        args.add("%${filter.value}")
                        "${filter.fieldName} LIKE ?"
                    }
                    FilterOperator.IS_NULL -> "${filter.fieldName} IS NULL"
                    FilterOperator.IS_NOT_NULL -> "${filter.fieldName} IS NOT NULL"
                    else -> throw IllegalArgumentException("Operator ${filter.operator} not supported for string fields")
                }
            }
            is UriRecordAdvancedFilterDomain.NumberFilter -> {
                when (filter.operator) {
                    FilterOperator.EQUALS -> {
                        args.add(filter.value)
                        "${filter.fieldName} = ?"
                    }
                    FilterOperator.NOT_EQUALS -> {
                        args.add(filter.value)
                        "${filter.fieldName} <> ?"
                    }
                    FilterOperator.GREATER_THAN -> {
                        args.add(filter.value)
                        "${filter.fieldName} > ?"
                    }
                    FilterOperator.LESS_THAN -> {
                        args.add(filter.value)
                        "${filter.fieldName} < ?"
                    }
                    FilterOperator.GREATER_THAN_OR_EQUALS -> {
                        args.add(filter.value)
                        "${filter.fieldName} >= ?"
                    }
                    FilterOperator.LESS_THAN_OR_EQUALS -> {
                        args.add(filter.value)
                        "${filter.fieldName} <= ?"
                    }
                    FilterOperator.IS_NULL -> "${filter.fieldName} IS NULL"
                    FilterOperator.IS_NOT_NULL -> "${filter.fieldName} IS NOT NULL"
                    else -> throw IllegalArgumentException("Operator ${filter.operator} not supported for number fields")
                }
            }
            is UriRecordAdvancedFilterDomain.DateFilter -> {
                val timeMillis = filter.value.toEpochMilliseconds()
                when (filter.operator) {
                    FilterOperator.EQUALS -> {
                        // For dates, "equals" is tricky. We'll use the same day range.
                        // This depends on how the data is stored - adjust as needed.
                        args.add(timeMillis)
                        "DATE(${filter.fieldName}/1000, 'unixepoch') = DATE(?/1000, 'unixepoch')"
                    }
                    FilterOperator.NOT_EQUALS -> {
                        args.add(timeMillis)
                        "DATE(${filter.fieldName}/1000, 'unixepoch') <> DATE(?/1000, 'unixepoch')"
                    }
                    FilterOperator.GREATER_THAN -> {
                        args.add(timeMillis)
                        "${filter.fieldName} > ?"
                    }
                    FilterOperator.LESS_THAN -> {
                        args.add(timeMillis)
                        "${filter.fieldName} < ?"
                    }
                    FilterOperator.GREATER_THAN_OR_EQUALS -> {
                        args.add(timeMillis)
                        "${filter.fieldName} >= ?"
                    }
                    FilterOperator.LESS_THAN_OR_EQUALS -> {
                        args.add(timeMillis)
                        "${filter.fieldName} <= ?"
                    }
                    FilterOperator.IS_NULL -> "${filter.fieldName} IS NULL"
                    FilterOperator.IS_NOT_NULL -> "${filter.fieldName} IS NOT NULL"
                    else -> throw IllegalArgumentException("Operator ${filter.operator} not supported for date fields")
                }
            }
            is UriRecordAdvancedFilterDomain.EnumFilter<*> -> {
                // Assuming the enum value is stored as an integer in the database
                when (filter.operator) {
                    FilterOperator.EQUALS -> {
                        args.add(getEnumValueAsInt(filter.value))
                        "${filter.fieldName} = ?"
                    }
                    FilterOperator.NOT_EQUALS -> {
                        args.add(getEnumValueAsInt(filter.value))
                        "${filter.fieldName} <> ?"
                    }
                    FilterOperator.IS_NULL -> "${filter.fieldName} IS NULL"
                    FilterOperator.IS_NOT_NULL -> "${filter.fieldName} IS NOT NULL"
                    else -> throw IllegalArgumentException("Operator ${filter.operator} not supported for enum fields")
                }
            }
        }

        return Pair(condition, args)
    }

    /**
     * Converts enum values to their integer representations for database queries.
     */
    private fun getEnumValueAsInt(enumValue: Any?): Int {
        return when (enumValue) {
            is UriSource -> enumValue.value
            is InteractionAction -> enumValue.value
            is UriStatus -> enumValue.value
            is FolderType -> enumValue.value
            null -> throw IllegalArgumentException("Enum value cannot be null")
            else -> throw IllegalArgumentException("Unsupported enum type: ${enumValue::class.java.name}")
        }
    }

    /**
     * Maps domain model sort fields to database column names.
     */
    private fun getSortFieldColumn(sortField: UriRecordSortField): String {
        return when (sortField) {
            UriRecordSortField.TIMESTAMP -> "timestamp"
            UriRecordSortField.URI_STRING -> "uri_string"
            UriRecordSortField.HOST -> "host"
            UriRecordSortField.CHOSEN_BROWSER -> "chosen_browser_package"
            UriRecordSortField.INTERACTION_ACTION -> "interaction_action"
            UriRecordSortField.URI_SOURCE -> "uri_source"
        }
    }
}

@Immutable
sealed interface UriRecordAdvancedFilterDomain {
    val fieldName: String
    val operator: FilterOperator

    // Example: Filter for records that have a chosen browser
    @Immutable
    data class HasChosenBrowser(override val fieldName: String, override val operator: FilterOperator): UriRecordAdvancedFilterDomain

    // Example: Filter for records that are associated with a HostRule
    @Immutable
    data class IsAssociatedWithHostRule(override val fieldName: String, override val operator: FilterOperator): UriRecordAdvancedFilterDomain

    @Immutable
    data class IsBookmarked(override val fieldName: String, override val operator: FilterOperator): UriRecordAdvancedFilterDomain
    // Add More specific filter types if you can think of that are relevant for my UseCase

    @Immutable
    data class StringFilter(
        override val fieldName: String,
        override val operator: FilterOperator,
        val value: String
    ) : UriRecordAdvancedFilterDomain

    @Immutable
    data class NumberFilter(
        override val fieldName: String,
        override val operator: FilterOperator,
        val value: Long
    ) : UriRecordAdvancedFilterDomain

    @Immutable
    data class DateFilter(
        override val fieldName: String,
        override val operator: FilterOperator,
        val value: Instant
    ) : UriRecordAdvancedFilterDomain

    @Immutable
    data class EnumFilter<T>(
        override val fieldName: String,
        override val operator: FilterOperator,
        val value: T
    ) : UriRecordAdvancedFilterDomain

    companion object {
        const val FIELD_URI_STRING = "uri_string"
        const val FIELD_HOST = "host"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_URI_SOURCE = "uri_source"
        const val FIELD_INTERACTION_ACTION = "interaction_action"
        const val FIELD_CHOSEN_BROWSER = "chosen_browser_package"
    }
}

@Immutable
enum class FilterOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS,
    IS_NULL,
    IS_NOT_NULL
}


 */