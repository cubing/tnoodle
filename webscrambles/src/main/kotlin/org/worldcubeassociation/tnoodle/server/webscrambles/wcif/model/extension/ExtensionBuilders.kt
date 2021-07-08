package org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.worldcubeassociation.tnoodle.server.serial.JsonConfig
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.Extension

@Serializable
sealed class ExtensionBuilder {
    abstract val id: String
    protected abstract val specUrl: String

    fun build(): Extension {
        val serialJson = JsonConfig.SERIALIZER.encodeToJsonElement(serializer(), this)

        val extValues = serialJson.jsonObject - JsonConfig.CLASS_DISCRIMINATOR
        val extSerial = JsonObject(extValues)

        return Extension(id, specUrl, extSerial)
    }

    companion object {
        val NESTED_DESCRIPTOR_INDEX by lazy { serializer().descriptor.getElementIndex("value") }
        val NESTED_DESCRIPTOR by lazy { serializer().descriptor.getElementDescriptor(NESTED_DESCRIPTOR_INDEX) }

        val REGISTERED_CHILDREN by lazy {
            List(NESTED_DESCRIPTOR.elementsCount) {
                NESTED_DESCRIPTOR.getElementName(it)
            }
        }
    }
}

@Serializable
@SerialName(FmcExtension.ID)
data class FmcExtension(val isFmc: Boolean) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.FmcFlag"
        const val SPEC_URL = "TODO"
    }
}

@Serializable
@SerialName(FmcLanguagesExtension.ID)
data class FmcLanguagesExtension(val languageTags: List<String>) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.FmcLanguages"
        const val SPEC_URL = "TODO"
    }
}

@Serializable
@SerialName(FmcAttemptCountExtension.ID)
data class FmcAttemptCountExtension(val totalAttempts: Int) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.FmcAttemptCount"
        const val SPEC_URL = "TODO"
    }
}

@Serializable
@SerialName(ExtraScrambleCountExtension.ID)
data class ExtraScrambleCountExtension(val extraAttempts: Int) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.ExtraScrambleCount"
        const val SPEC_URL = "TODO"
    }
}

@Serializable
@SerialName(MultiScrambleCountExtension.ID)
data class MultiScrambleCountExtension(val requestedScrambles: Int) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.MultiScrambleCount"
        const val SPEC_URL = "TODO"
    }
}

@Serializable
@SerialName(SheetCopyCountExtension.ID)
data class SheetCopyCountExtension(val numCopies: Int) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.SheetCopyCount"
        const val SPEC_URL = "TODO"
    }
}

@Serializable
@SerialName(TNoodleStatusExtension.ID)
data class TNoodleStatusExtension(val isStaging: Boolean, val isManual: Boolean, val isSignedBuild: Boolean, val isAllowedVersion: Boolean) : ExtensionBuilder() {
    override val id get() = ID
    override val specUrl get() = SPEC_URL

    companion object {
        const val ID = "org.worldcubeassociation.tnoodle.CompetitionStatus"
        const val SPEC_URL = "TODO"
    }
}
