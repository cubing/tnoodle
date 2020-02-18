package org.worldcubeassociation.tnoodle.server

import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.routing.routing
import io.ktor.serialization.serialization
import io.ktor.server.engine.ShutDownUrl
import org.worldcubeassociation.tnoodle.server.routing.JsEnvHandler
import org.worldcubeassociation.tnoodle.server.routing.StylesheetHandler
import org.worldcubeassociation.tnoodle.server.routing.VersionHandler
import org.worldcubeassociation.tnoodle.server.serial.JsonConfig
import org.worldcubeassociation.tnoodle.server.util.ServerEnvironmentConfig

class TNoodleServer(val environmentConfig: ServerEnvironmentConfig) : ApplicationHandler {
    override fun spinUp(app: io.ktor.application.Application) {
        environmentConfig.createLocalPruningCache()

        val versionHandler = VersionHandler(environmentConfig.projectTitle)

        app.routing {
            JsEnvHandler.install(this)
            StylesheetHandler.install(this)
            versionHandler.install(this)
        }

        app.install(ShutDownUrl.ApplicationCallFeature) {
            shutDownUrl = "/kill/now"
            exitCodeSupplier = { 0 }
        }

        app.install(DefaultHeaders) {
            header("Access-Control-Allow-Origin", "*")
        }

        app.install(ContentNegotiation) {
            serialization(json = JsonConfig.SERIALIZER)
        }
    }
}
