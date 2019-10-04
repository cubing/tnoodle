package dependencies

object Versions {
    val JUNIT_JUPITER = "5.4.2"
    val BATIK = "1.12"
    val KOTLIN = "1.3.61"
    val KTOR = "1.3.0"
    val PROGUARD = "6.2.2"
    val KOTLESS = "0.1.3"
    val TNOODLE_LIB = "0.17.0"

    val MARKDOWNJ_CORE = "0.4"
    val ZIP4J = "2.3.1"
    val ITEXTPDF = "5.5.13.1"
    val BATIK_TRANSCODER = BATIK
    val SNAKEYAML = "1.25"
    val SYSTEM_TRAY = "3.17"
    val BOUNCYCASTLE = "1.64"
    val JUNIT_JUPITER_API = JUNIT_JUPITER
    val JUNIT_JUPITER_ENGINE = JUNIT_JUPITER
    val KOTLIN_STDLIB_JVM = KOTLIN
    val KOTLIN_STDLIB_JS = KOTLIN
    val KOTLIN_STDLIB_COMMON = KOTLIN
    val KOTLIN_SERIALIZATION_JVM = "0.14.0"
    val KTOR_SERVER_NETTY = KTOR
    val KTOR_SERVER_SERVLET = KTOR
    val KTOR_SERIALIZATION = KTOR
    val KTOR_SERVER_HOST_COMMON = KTOR
    val LOGBACK_CLASSIC = "1.2.3"
    val KOTLIN_ARGPARSER = "2.0.7"
    val PROGUARD_GRADLE = PROGUARD
    val WCA_I18N = "0.4.3"
    val GOOGLE_APPENGINE_GRADLE = "2.2.0"
    val GOOGLE_CLOUD_STORAGE = "1.103.0"
    val TNOODLE_SCRAMBLES = TNOODLE_LIB
    val TNOODLE_SCRAMBLER_THREEPHASE = TNOODLE_LIB
    val APACHE_COMMONS_LANG3 = "3.9"
    val KOTLESS_KTOR = KOTLESS

    object Plugins {
        val SHADOW = "5.2.0"
        val NODEJS = "2.2.1"
        val DEPENDENCY_VERSIONS = "0.27.0"
        val GIT_VERSION_TAG = "0.12.2"

        val KOTLIN = Versions.KOTLIN

        val KOTLIN_JVM = KOTLIN
        val KOTLIN_MULTIPLATFORM = KOTLIN
        val KOTLIN_SERIALIZATION = KOTLIN

        val KOTLESS = Versions.KOTLESS
    }
}
