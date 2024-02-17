import buildkonfig.BuildKonfig
import kotlin.test.Test
import kotlin.test.assertTrue

class BuildKonfig {
    @Test
    fun `assert is testing`() {
        assertTrue(
            BuildKonfig.TESTING,
            "BuildKonfig.TESTING should be true. It is ${BuildKonfig.TESTING}"
        )
    }
}
