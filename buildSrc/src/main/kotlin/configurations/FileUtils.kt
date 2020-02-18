package configurations

import org.gradle.api.Task
import java.io.File
import java.nio.file.Files

object FileUtils {
    fun Task.symlink(linkName: File, originFile: File) {
        if (System.getenv("CI") != null) { // Travis can't do symlinks for unknown reasons
            logger.warn("Detected CI environment, not creating any symlinks!")
            return
        }

        if (!linkName.exists() || linkName.delete()) {
            Files.createSymbolicLink(linkName.toPath(), originFile.toPath())
        } else {
            logger.warn("Unable to (re-)create symlink for $linkName!")
        }
    }
}
