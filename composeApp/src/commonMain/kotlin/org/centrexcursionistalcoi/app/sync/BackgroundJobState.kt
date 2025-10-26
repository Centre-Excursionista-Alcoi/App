package org.centrexcursionistalcoi.app.sync

enum class BackgroundJobState {
    /**
     * Used to indicate that the job is enqueued and eligible to run when its constraints are met and resources are available.
     */
    ENQUEUED,

    /** Used to indicate that the job is currently being executed. */
    RUNNING,

    /**
     * Used to indicate that the job has completed in a successful state. Note that periodic jobs will never enter this state (they will simply go back to
     * [ENQUEUED] and be eligible to run again).
     */
    SUCCEEDED,

    /**
     * Used to indicate that the job has completed in a failure state. All dependent work will also be marked as [FAILED] and will never run.
     */
    FAILED,

    /**
     * Used to indicate that the job is currently blocked because its prerequisites haven't finished successfully.
     */
    BLOCKED,

    /**
     * Used to indicate that the job has been cancelled and will not execute. All dependent work will also be marked as [CANCELLED] and will not run.
     */
    CANCELLED;

    /**
     * Returns `true` if this state is considered finished: [SUCCEEDED], [FAILED], [CANCELLED]
     */
    val isFinished: Boolean
        get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
}
