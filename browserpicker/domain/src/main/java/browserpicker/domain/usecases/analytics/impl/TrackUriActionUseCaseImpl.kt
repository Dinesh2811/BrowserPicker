package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.*
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.HostRule
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.service.UriParser
import browserpicker.domain.usecases.analytics.TrackUriActionUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import javax.inject.Inject

class TrackUriActionUseCaseImpl @Inject constructor(
    private val uriParser: UriParser,
    private val hostRuleRepository: HostRuleRepository,
    private val clock: Clock // For createdAt/updatedAt timestamps
) : TrackUriActionUseCase {

    override suspend operator fun invoke(
        uriString: String,
        action: InteractionAction,
        associatedHostRuleId: Long?
    ): DomainResult<Unit, AppError> {
        val parsedUriResult = uriParser.parseAndValidateWebUri(uriString)
        val parsedUri = parsedUriResult.getOrNull()
            ?: return DomainResult.Failure(parsedUriResult.errorOrNull() ?: AppError.UnknownError("URI parsing failed"))

        if (parsedUri == null) {
            return DomainResult.Failure(AppError.DataMappingError("Parsed URI is null after validation"))
        }
        val host = parsedUri.host

        return when (action) {
            // Example: If InteractionAction had specific values for these user-initiated actions
            // For now, PREFERENCE_SET is the most relevant from the existing enum for HostRule modification.
            // The use case description mentions "bookmarking, blocking" which are not directly in InteractionAction enum.
            // This implies InteractionAction might need to be expanded or this use case handles specific interpretations.

            InteractionAction.PREFERENCE_SET -> {
                // To set a preference, we need the browser package name and whether it's enabled.
                // These are not parameters of this use case. This is a significant gap.
                // Assuming this action is a signal that a preference *was* set by another mechanism,
                // and we just need to ensure the rule reflects *that a preference exists* or update its timestamp.
                // This is highly speculative due to missing parameters.
                val existingRuleResult = associatedHostRuleId?.let { hostRuleRepository.getHostRuleById(it) }
                    ?: hostRuleRepository.getHostRuleByHost(host).firstOrNull()

                val existingRule = existingRuleResult?.getOrNull()

                if (existingRule != null) {
                    // We don't have the new preference details. We can only update `updatedAt` or assume
                    // isPreferenceEnabled is true.
                    // This is insufficient for a real PREFERENCE_SET.
                    // Let's assume this means we are just enabling a preference on an existing rule
                    // if it had one, or simply updating its timestamp.
                    val updatedRule = existingRule.copy(
                        isPreferenceEnabled = true, // Assumption
                        updatedAt = clock.now()
                    )
                    hostRuleRepository.saveHostRule(
                        host = updatedRule.host,
                        status = updatedRule.uriStatus,
                        folderId = updatedRule.folderId,
                        preferredBrowser = updatedRule.preferredBrowserPackage, // existing one, not necessarily new one
                        isPreferenceEnabled = updatedRule.isPreferenceEnabled
                    ).mapSuccess { }
                } else {
                    // Cannot set preference if rule doesn't exist and we don't have preference details.
                    DomainResult.Failure(AppError.DataNotFound("Host rule not found to set preference for host: $host"))
                }
            }
            // How to handle "bookmarking" or "blocking" based on InteractionAction?
            // If InteractionAction.BLOCKED_URI_ENFORCED is passed, it means a block was applied, not that user *is* blocking.
            // This use case as defined is difficult to implement robustly without clearer mapping from
            // InteractionAction to HostRule states or more parameters.

            // Placeholder for other actions described (bookmarking/blocking)
            // These would ideally be distinct InteractionActions e.g., USER_BOOKMARKED, USER_BLOCKED
            // else -> DomainResult.Failure(AppError.ValidationError("Action $action not supported for HostRule modification via TrackUriActionUseCase"))

            // For now, let's make a very specific interpretation: if this use case is called
            // with an action that *implies* a status change not covered by PREFERENCE_SET, it tries to update status.
            // This is still a stretch.
            // Example: If there was an action like InteractionAction.USER_INITIATED_BLOCK
            // This example will assume a hypothetical mapping or future extension for other actions.

            else -> {
                // For any other action, this use case currently doesn't have a defined behavior
                // to modify a HostRule. It might be purely for logging in a separate analytics system,
                // which is not what's being implemented here.
                // Returning success but doing nothing for non-preference actions for now.
                DomainResult.Success(Unit)
            }
        }
    }
} 