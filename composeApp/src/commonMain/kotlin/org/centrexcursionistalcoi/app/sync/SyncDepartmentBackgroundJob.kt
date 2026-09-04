package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named(SyncDepartmentBackgroundJob.NAME)
class SyncDepartmentBackgroundJob(
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
) : BackgroundJob() {
    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val departmentId = input[EXTRA_DEPARTMENT_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing department ID")

        departmentsRemoteRepository.update(departmentId, progressNotifier, ignoreIfModifiedSince = true)
            ?: return SyncResult.Failure("Department with ID $departmentId not found on server")

        return SyncResult.Success()
    }

    companion object {
        const val NAME = "SyncDepartmentBackgroundJob"
        const val EXTRA_DEPARTMENT_ID = "department_id"
    }
}
