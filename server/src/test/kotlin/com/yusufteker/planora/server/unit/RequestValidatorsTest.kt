package com.yusufteker.planora.server.unit

import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AutoScheduleRequest
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.RegisterRequest
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import com.yusufteker.planora.shared.api.ValidationException
import com.yusufteker.planora.shared.validation.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests validating input payload constraints across shared request models.
 *
 * Verifies Fail-Fast behavior, ensuring invalid data (malformed emails, short passwords,
 * empty titles, inverted date ranges) is rejected with detailed field-level errors
 * before hitting downstream database or business logic.
 */
class RequestValidatorsTest {

    // ─────────────────────────────────────────
    // REGISTER REQUEST VALIDATION TESTS
    // ─────────────────────────────────────────

    /**
     * Verifies that a valid registration request passes validation without throwing any exception.
     */
    @Test
    fun registerRequestWithValidDataShouldPass() {
        val request = RegisterRequest(
            name = "Yusuf Teker",
            username = "yusuf_teker",
            email = "yusuf@example.com",
            password = "securePassword123",
            code = "123456"
        )
        request.validate()
    }

    /**
     * Verifies that an invalid email syntax throws [ValidationException] containing the "email" field error.
     */
    @Test
    fun registerRequestWithInvalidEmailShouldFail() {
        val request = RegisterRequest(
            name = "Yusuf Teker",
            username = "yusuf_teker",
            email = "not-an-email",
            password = "securePassword123",
            code = "123456"
        )
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("email"))
    }

    /**
     * Verifies that passwords shorter than the minimum threshold throw [ValidationException].
     */
    @Test
    fun registerRequestWithShortPasswordShouldFail() {
        val request = RegisterRequest(
            name = "Yusuf Teker",
            username = "yusuf_teker",
            email = "yusuf@example.com",
            password = "123", // Too short (min 6)
            code = "123456"
        )
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("password"))
    }

    /**
     * Verifies that blank verification code throws [ValidationException].
     */
    @Test
    fun registerRequestWithBlankCodeShouldFail() {
        val request = RegisterRequest(
            name = "Yusuf Teker",
            username = "yusuf_teker",
            email = "yusuf@example.com",
            password = "securePassword123",
            code = "   "
        )
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("code"))
    }

    /**
     * Verifies that username with invalid characters or out-of-bound length fails.
     */
    @Test
    fun registerRequestWithInvalidUsernameShouldFail() {
        val request = RegisterRequest(
            name = "Yusuf",
            username = "ab", // Too short (min 3)
            email = "yusuf@example.com",
            password = "securePassword123",
            code = "123456"
        )
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("username"))
    }

    // ─────────────────────────────────────────
    // AUTH (LOGIN) REQUEST VALIDATION TESTS
    // ─────────────────────────────────────────

    /**
     * Verifies that valid login credentials pass validation.
     */
    @Test
    fun authRequestWithValidCredentialsShouldPass() {
        val request = AuthRequest(
            identifier = "yusuf@example.com",
            password = "myPassword"
        )
        request.validate()
    }

    /**
     * Verifies that blank identifier or password triggers [ValidationException].
     */
    @Test
    fun authRequestWithBlankFieldsShouldFail() {
        val request = AuthRequest(identifier = "", password = "")
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertEquals(2, exception.fieldErrors.size)
        assertTrue(exception.fieldErrors.containsKey("identifier"))
        assertTrue(exception.fieldErrors.containsKey("password"))
    }

    // ─────────────────────────────────────────
    // TASK REQUEST VALIDATION TESTS
    // ─────────────────────────────────────────

    /**
     * Verifies that a well-formed task passes validation.
     */
    @Test
    fun createTaskRequestWithValidDataShouldPass() {
        val now = 1700000000000L
        val request = CreateTaskRequest(
            title = "Sprint Planning Meeting",
            description = "Plan Q3 deliverables",
            startTime = now,
            endTime = now + 3600000L,
            type = TaskType.EVENT,
            status = TaskStatus.PENDING,
            visibility = TaskVisibility.PRIVATE
        )
        request.validate()
    }

    /**
     * Verifies that an empty task title throws [ValidationException].
     */
    @Test
    fun createTaskRequestWithBlankTitleShouldFail() {
        val now = 1700000000000L
        val request = CreateTaskRequest(
            title = "   ",
            description = null,
            startTime = now,
            endTime = null,
            type = TaskType.TASK,
            status = TaskStatus.PENDING,
            visibility = TaskVisibility.PRIVATE
        )
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("title"))
    }

    /**
     * Verifies that endTime chronologically preceding startTime throws [ValidationException].
     */
    @Test
    fun createTaskRequestWithInvertedDatesShouldFail() {
        val now = 1700000000000L
        val request = CreateTaskRequest(
            title = "Invalid Date Task",
            description = null,
            startTime = now,
            endTime = now - 1000L, // Inverted! End before start
            type = TaskType.EVENT,
            status = TaskStatus.PENDING,
            visibility = TaskVisibility.PRIVATE
        )
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("endTime"))
    }

    // ─────────────────────────────────────────
    // PLAN ROOM & AUTO-SCHEDULE VALIDATION TESTS
    // ─────────────────────────────────────────

    /**
     * Verifies that empty room name fails validation.
     */
    @Test
    fun createPlanRoomRequestWithBlankNameShouldFail() {
        val request = CreatePlanRoomRequest(name = "   ")
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("name"))
    }

    /**
     * Verifies that an empty list of task IDs in [AutoScheduleRequest] throws [ValidationException].
     */
    @Test
    fun autoScheduleRequestWithEmptyTaskIdsShouldFail() {
        val request = AutoScheduleRequest(taskIds = emptyList())
        val exception = assertFailsWith<ValidationException> { request.validate() }
        assertTrue(exception.fieldErrors.containsKey("taskIds"))
    }
}
