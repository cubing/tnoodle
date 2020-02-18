package org.worldcubeassociation.tnoodle.server.webscrambles

import com.xenomachina.argparser.ArgParser
import cs.threephase.Tools
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import org.worldcubeassociation.tnoodle.server.ApplicationHandler
import org.worldcubeassociation.tnoodle.server.TNoodleServer
import org.worldcubeassociation.tnoodle.server.routing.JsEnvHandler
import org.worldcubeassociation.tnoodle.server.util.ServerEnvironmentConfig
import org.worldcubeassociation.tnoodle.server.webscrambles.server.MainLauncher.NO_REEXEC_OPT
import org.worldcubeassociation.tnoodle.server.webscrambles.routing.*
import org.worldcubeassociation.tnoodle.server.webscrambles.routing.job.JobSchedulingHandler
import org.worldcubeassociation.tnoodle.server.webscrambles.server.LocalServerEnvironmentConfig
import org.worldcubeassociation.tnoodle.server.webscrambles.server.MainLauncher
import org.worldcubeassociation.tnoodle.server.webscrambles.server.OfflineJarUtils
import java.io.DataInputStream
import java.io.DataOutputStream

class WebscramblesServer(val environmentConfig: ServerEnvironmentConfig) : ApplicationHandler {
    private val baseServer = TNoodleServer(environmentConfig)

    override fun spinUp(app: Application) {
        val scrambleHandler = ScrambleHandler(environmentConfig)
        val scrambleViewHandler = ScrambleViewHandler(environmentConfig)

        val wcifHandler = WcifHandler(environmentConfig)

        initPruning()

        app.routing {
            PuzzleListHandler.install(this)
            RouteRedirectHandler.install(this)
            scrambleHandler.install(this)
            ReadmeHandler.install(this)
            scrambleViewHandler.install(this)
            StaticContentHandler.install(this)
            wcifHandler.install(this)
            JobSchedulingHandler.install(this)
        }

        baseServer.spinUp(app)
    }

    private fun initPruning() {
        if (environmentConfig.usePruning) {
            if (environmentConfig.pruningTableExists(THREEPHASE_PRUNING)) {
                DataInputStream(environmentConfig.getPruningTableInput(THREEPHASE_PRUNING)).use {
                    Tools.initFrom(it)
                }
            } else {
                DataOutputStream(environmentConfig.getPruningTableOutput(THREEPHASE_PRUNING)).use {
                    Tools.saveTo(it)
                }
            }
        }
    }

    companion object {
        const val MIN_HEAP_SIZE_MEGS = 512

        const val THREEPHASE_PRUNING = "444-threephase"

        val LOG = LoggerFactory.getLogger(WebscramblesServer::class.java)

        fun spinUpLocally(app: Application) =
            WebscramblesServer(LocalServerEnvironmentConfig).spinUp(app)

        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser(args)

            val desiredJsEnv by parser.adding("--jsenv", help = "Add entry to global js object TNOODLE_ENV in /env.js. Treated as strings, so FOO=42 will create the entry TNOODLE_ENV['FOO'] = '42';")

            val noBrowser by parser.flagging("-n", "--nobrowser", help = "Don't open the browser when starting the server")
            // val noUpgrade by parser.flagging("-u", "--noupgrade", help = "If an instance of $NAME is running on the desired port(s), do not attempt to kill it and start up")
            val noReexec by parser.flagging(NO_REEXEC_OPT, help = "Do not reexec. This is sometimes done to rename java.exe on Windows, or to get a larger heap size.")

            OfflineJarUtils.setApplicationIcon()

            if (!noReexec) {
                MainLauncher.wrapMain(args, MIN_HEAP_SIZE_MEGS)

                // This second call to setApplicationIcon() is intentional.
                // We want different icons for the parent and child processes.
                OfflineJarUtils.setApplicationIcon()
            }

            for (jsEnv in desiredJsEnv) {
                val (key, strValue) = jsEnv.split("=", limit = 2)
                JsEnvHandler.putJsEnv(key, strValue)
            }

            val cliEnv = commandLineEnvironment(args)
            embeddedServer(Netty, cliEnv).start()

            LOG.info("${LocalServerEnvironmentConfig.projectTitle} started")

            val url = OfflineJarUtils.openTabInBrowser(!noBrowser)
            LOG.info("Visit $url for a readme and demo.")
        }
    }
}
