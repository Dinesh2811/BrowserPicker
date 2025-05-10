package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.MonitorSystemBrowserChangesUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MonitorSystemBrowserChangesUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : MonitorSystemBrowserChangesUseCase {
    override operator fun invoke(): Flow<DomainResult<List<String>, AppError>> {
        return systemRepository.getSystemBrowserChangesFlow()
            .catchUnexpected()
    }
} 