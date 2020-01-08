package org.worldcubeassociation.tnoodle.server.util

import java.io.InputStream
import java.io.OutputStream

interface ServerEnvironmentConfig {
    val projectName: String
    val version: String

    val projectTitle
        get() = "$projectName-$version"

    fun pruningTableExists(tableName: String): Boolean

    fun getPruningTableInput(tableName: String): InputStream
    fun getPruningTableOutput(tableName: String): OutputStream

    fun createLocalPruningCache()
}
