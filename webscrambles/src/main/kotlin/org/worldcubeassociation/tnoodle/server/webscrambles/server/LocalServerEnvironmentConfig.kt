package org.worldcubeassociation.tnoodle.server.webscrambles.server

import org.worldcubeassociation.tnoodle.server.util.ServerEnvironmentConfig
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils.DEVEL_VERSION
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils.PRUNING_FOLDER
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils.jarFile
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils.programDirectory
import java.io.*

object LocalServerEnvironmentConfig : ServerEnvironmentConfig {
    override val projectName
        get() = LocalServerEnvironmentConfig::class.java.getPackage()?.implementationTitle
            ?: WebServerUtils.callerClass?.simpleName!!

    override val version
        get() = LocalServerEnvironmentConfig::class.java.getPackage()?.implementationVersion
            ?: DEVEL_VERSION

    private fun getPruningTableCache(assertExists: Boolean = true): File {
        val baseDir = File(programDirectory, PRUNING_FOLDER)

        // Each version of tnoodle extracts its pruning tables
        // to its own subdirectory of PRUNING_FOLDER
        val file = baseDir.takeIf { version == DEVEL_VERSION }
            ?: File(baseDir, version)

        if (assertExists && !file.isDirectory) {
            throw FileNotFoundException("${file.absolutePath} does not exist, or is not a directory")
        }

        return file
    }

    override fun pruningTableExists(tableName: String) = localPruningFile(tableName).exists()

    override fun getPruningTableInput(tableName: String) = localPruningFile(tableName).inputStream()

    override fun getPruningTableOutput(tableName: String) =
        localPruningFile(tableName).takeIf { it.parentFile.isDirectory || it.parentFile.mkdirs() }?.outputStream()
            ?: error("Unable to create pruning file for table '$tableName'")

    private fun localPruningFile(tableName: String) = File(getPruningTableCache(false), "$tableName.prun")

    override fun createLocalPruningCache() {
        val jarFile = jarFile

        if (jarFile != null) {
            val pruningTableDirectory = getPruningTableCache(false)

            if (pruningTableDirectory.isDirectory) {
                // If the pruning table folder already exists, we don't bother re-extracting the
                // files.
                return
            }

            assert(pruningTableDirectory.mkdirs())
        }
    }
}
