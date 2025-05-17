package browserpicker.domain.usecase.rules

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.HostRule
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface GetHostRuleUseCase {
    suspend operator fun invoke(host: String): DomainResult<HostRule?, AppError>
}

class GetHostRuleUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository
) : GetHostRuleUseCase {
    override suspend fun invoke(host: String): DomainResult<HostRule?, AppError> {
        Timber.d("Getting host rule for: $host")
        return repository.getHostRuleByHost(host)
    }
}
