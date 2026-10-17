import ComposeApp

// The types of the `:shared` module (the data the server and the app both use) come with a `Shared` prefix: these
// are the names the rest of the app uses.
typealias ProfileResponse = SharedProfileResponse
typealias Department = SharedDepartment
typealias Qualification = SharedQualification
typealias QualificationGrant = SharedQualificationGrant
typealias ReferencedEvent = SharedReferencedEvent
typealias ReferencedPost = SharedReferencedPost
