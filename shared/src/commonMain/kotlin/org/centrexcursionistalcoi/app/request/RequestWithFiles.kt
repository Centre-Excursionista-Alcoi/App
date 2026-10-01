package org.centrexcursionistalcoi.app.request

import org.centrexcursionistalcoi.app.data.FileWithContext

/**
 * A request that can carry files ([FileWithContext]).
 *
 * A request with the contents of any file is sent as `multipart/form-data`, so that files are not encoded, and the
 * server can stream them instead of loading them into memory:
 * - the first part, named [REQUEST_PART], holds the request as JSON, where each file with contents has no
 *   [FileWithContext.bytes], but the name of the part holding them in [FileWithContext.part].
 * - each file is a part of its own, with a `filename` in its `Content-Disposition`.
 *
 * Otherwise (its files only refer to existing ones, by [FileWithContext.id]), it's sent as plain JSON. The server
 * rejects contents in [FileWithContext.bytes], encoded as Base64 in the JSON.
 */
interface RequestWithFiles<Self : RequestWithFiles<Self>> {
    companion object {
        /**
         * The name of the part holding the request in a multipart request.
         */
        const val REQUEST_PART = "request"
    }

    /**
     * Returns a copy of this request with each of its files replaced by [transform].
     */
    fun mapFiles(transform: (FileWithContext) -> FileWithContext): Self
}
