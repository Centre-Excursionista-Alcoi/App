package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named("SyncDepartmentBackgroundJobLogic")
class SyncDepartmentBackgroundJobLogic(
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val departmentsRepository: DepartmentsRepository
) : BackgroundSyncWorkerLogic() {
    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val departmentId = input[EXTRA_DEPARTMENT_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing department ID")

        val department = departmentsRemoteRepository.get(departmentId, progressNotifier, ignoreIfModifiedSince = true)
            ?: return SyncResult.Failure("Department with ID $departmentId not found on server")
        departmentsRepository.insertOrUpdate(department)

        return SyncResult.Success()
    }

    companion object {
        const val EXTRA_DEPARTMENT_ID = "department_id"
    }
}
