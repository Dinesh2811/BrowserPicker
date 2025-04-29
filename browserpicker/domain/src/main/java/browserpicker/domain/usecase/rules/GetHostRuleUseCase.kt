package browserpicker.domain.usecase.rules

import browserpicker.domain.model.HostRule
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface GetHostRuleUseCase {
    operator fun invoke(host: String): Flow<HostRule?>
}

class GetHostRuleUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository
) : GetHostRuleUseCase {
    override fun invoke(host: String): Flow<HostRule?> {
        Timber.d("Getting host rule for: $host")
        return repository.getHostRuleByHost(host)
    }
}
