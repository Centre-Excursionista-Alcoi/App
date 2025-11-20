package org.centrexcursionistalcoi.app.exception

import kotlin.uuid.Uuid

class DepartmentNotFoundException(id: Uuid): NoSuchElementException("Department with id '$id' not found")
