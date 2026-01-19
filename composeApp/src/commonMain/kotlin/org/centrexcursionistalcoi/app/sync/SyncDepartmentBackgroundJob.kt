package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

expect class SyncDepartmentBackgroundJob : BackgroundSyncWorker<SyncDepartmentBackgroundJobLogic>

object SyncDepartmentBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_DEPARTMENT_ID = "department_id"

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val departmentId = input[EXTRA_DEPARTMENT_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing department ID")

        val department = DepartmentsRemoteRepository.get(departmentId, progressNotifier, ignoreIfModifiedSince = true)
            ?: return SyncResult.Failure("Department with ID $departmentId not found on server")
        DepartmentsRepository.insertOrUpdate(department)

        return SyncResult.Success()
    }
}
