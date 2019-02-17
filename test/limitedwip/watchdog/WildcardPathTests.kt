package limitedwip.watchdog

import limitedwip.shouldEqual
import org.junit.Test

class WildcardPathTests {
    @Test fun `file name`() {
        val pattern = convertToRegexp("*.approved")
        pattern.matches("foo.approved") shouldEqual true
        pattern.matches("bar.approved") shouldEqual true
        pattern.matches("some/path/bar.approved") shouldEqual true
        pattern.matches("foo.kt") shouldEqual false
    }

    @Test fun `dir name`() {
        val pattern = convertToRegexp("/some/path/*.approved")
        pattern.matches("/some/path/bar.approved") shouldEqual true
    }
}