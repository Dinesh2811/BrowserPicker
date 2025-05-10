package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.MonitorUriClipboardUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MonitorUriClipboardUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : MonitorUriClipboardUseCase {
    override operator fun invoke(): Flow<DomainResult<String, AppError>> {
        return systemRepository.getClipboardUriFlow()
            .catchUnexpected()
    }
} 