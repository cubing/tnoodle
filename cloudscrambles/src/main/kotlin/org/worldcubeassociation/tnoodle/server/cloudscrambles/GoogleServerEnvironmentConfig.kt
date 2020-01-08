package org.worldcubeassociation.tnoodle.server.cloudscrambles

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import org.worldcubeassociation.tnoodle.server.util.ServerEnvironmentConfig
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils.DEVEL_VERSION
import org.worldcubeassociation.tnoodle.server.util.WebServerUtils.callerClass
import java.io.File
import java.io.File.separator
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels

object GoogleServerEnvironmentConfig : ServerEnvironmentConfig {
    val GCS_SERVICE by lazy { StorageOptions.getDefaultInstance().service }

    private val CONFIG_FILE = javaClass.getResourceAsStream("/version.tnoodle")
    private val CONFIG_DATA = CONFIG_FILE?.reader()?.readLines() ?: listOf()

    val JAVA_HOME = System.getProperty("java.home")

    val FONT_CONFIG_NAME = "fontconfig.Prodimage.properties"
    val FONT_CONFIG = ("$JAVA_HOME${separator}lib${separator}$FONT_CONFIG_NAME")

    val FONT_CONFIG_PROPERTY = "sun.awt.fontconfig"

    override val projectName: String
        get() = CONFIG_DATA.getOrNull(0)
            ?: callerClass?.simpleName!!

    override val version: String
        get() = CONFIG_DATA.getOrNull(1)
            ?: DEVEL_VERSION

    override fun pruningTableExists(tableName: String): Boolean {
        val blobId = remotePruningBlob(tableName)
        return GCS_SERVICE.get(blobId)?.exists() ?: false
    }

    override fun getPruningTableInput(tableName: String): InputStream {
        val blobId = remotePruningBlob(tableName)
        val blobReader = GCS_SERVICE.get(blobId).reader()

        return Channels.newInputStream(blobReader)
    }

    override fun getPruningTableOutput(tableName: String): OutputStream {
        val blobId = remotePruningBlob(tableName)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build()

        val blobWriter = GCS_SERVICE.create(blobInfo).writer()

        return Channels.newOutputStream(blobWriter)
    }

    private fun remotePruningBlob(tableName: String) = BlobId.of(getCloudBucketName(), tableName)

    private fun getCloudHostname() = System.getProperty("com.google.appengine.application.id")
    fun getCloudBucketName() = "${getCloudHostname()}.appspot.com"

    fun overrideFontConfig() {
        if (File(FONT_CONFIG).exists()) {
            System.setProperty(FONT_CONFIG_PROPERTY, FONT_CONFIG)
        }
    }

    override fun createLocalPruningCache() = overrideFontConfig() // FIXME do we want this here implicitly?
}
