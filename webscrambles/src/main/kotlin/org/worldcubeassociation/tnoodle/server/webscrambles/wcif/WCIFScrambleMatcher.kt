package org.worldcubeassociation.tnoodle.server.webscrambles.wcif

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.worldcubeassociation.tnoodle.server.webscrambles.plugins.EventPlugins
import org.worldcubeassociation.tnoodle.server.webscrambles.plugins.PuzzlePlugins
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.*
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension.ExtensionBuilder
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension.ExtraScrambleCountExtension
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension.FmcExtension
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension.MultiScrambleCountExtension
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object WCIFScrambleMatcher {
    const val ID_PENDING = 0 // FIXME should this be -1?

    // SCRAMBLE SET FILLING -----
    const val CIPHER_ALGORITHM = "AES"
    const val CIPHER_SALT = "TNOODLE_WCIF"
    const val CIPHER_KEY_ITERATIONS = 1000
    const val CIPHER_KEY_LENGTH = 128 * 8

    val CIPHER_CHARSET = Charsets.UTF_8

    val CIPHER_KEY_FACTORY = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

    fun encryptScrambleSets(wcif: Competition, password: String): Competition {
        return cryptScrambleSets(wcif, password, Cipher.ENCRYPT_MODE, WCIFScrambleMatcher::applyCipherEncrypt)
    }

    fun decryptScrambleSets(wcif: Competition, password: String): Competition {
        return cryptScrambleSets(wcif, password, Cipher.DECRYPT_MODE, WCIFScrambleMatcher::applyCipherDecrypt)
    }

    private fun cryptScrambleSets(wcif: Competition, password: String, cipherOpMode: Int, cipherMethod: (List<Scramble>, Cipher) -> List<Scramble>): Competition {
        val cipherKey = generateKey(password)

        val cipherInstance = Cipher.getInstance(CIPHER_ALGORITHM)
            .apply { init(cipherOpMode, cipherKey) }

        val scrambledEvents = wcif.events.map { e ->
            val scrambledRounds = e.rounds.map { r ->
                val cryptedSets = r.scrambleSets.map { scr ->
                    val cryptedStdScrambles = cipherMethod(scr.scrambles, cipherInstance)
                    val cryptedExtraScrambles = cipherMethod(scr.extraScrambles, cipherInstance)

                    scr.copy(scrambles = cryptedStdScrambles, extraScrambles = cryptedExtraScrambles)
                }

                r.copy(scrambleSets = cryptedSets)
            }

            e.copy(rounds = scrambledRounds)
        }

        return wcif.copy(events = scrambledEvents)
    }

    private fun applyCipherEncrypt(scrambles: List<Scramble>, cipher: Cipher): List<Scramble> {
        return scrambles.map {
            val contentBytes = it.scrambleString.toByteArray(CIPHER_CHARSET)

            val cipherContent = cipher.doFinal(contentBytes)
            val cipherString = Base64.getEncoder().encodeToString(cipherContent)

            it.copy(scrambleString = cipherString)
        }
    }

    private fun applyCipherDecrypt(scrambles: List<Scramble>, cipher: Cipher): List<Scramble> {
        return scrambles.map {
            val contentBytes = Base64.getDecoder().decode(it.scrambleString)

            val cipherContent = cipher.doFinal(contentBytes)
            val cipherString = cipherContent.toString(CIPHER_CHARSET)

            it.copy(scrambleString = cipherString)
        }
    }

    private fun generateKey(password: String): SecretKey {
        val saltBytes = CIPHER_SALT.toByteArray(CIPHER_CHARSET)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, CIPHER_KEY_ITERATIONS, CIPHER_KEY_LENGTH)

        return CIPHER_KEY_FACTORY.generateSecret(spec)
    }

    suspend fun fillScrambleSetsAsync(wcif: Competition, onUpdate: (PuzzlePlugins, String) -> Unit): Competition {
        val scrambledEvents = wcif.events.map { e ->
            val scrambledRounds = coroutineScope {
                e.rounds.map { r -> async { scrambleRound(r, onUpdate) } }.awaitAll()
            }

            e.copy(rounds = scrambledRounds)
        }

        val indexedEvents = reindexScrambleSets(scrambledEvents)
        val scrambled = wcif.copy(events = indexedEvents)

        return matchActivities(scrambled)
    }

    // helper fn for synchronously running tests
    fun fillScrambleSets(wcif: Competition) = runBlocking { fillScrambleSetsAsync(wcif) { _, _ -> Unit } }

    private suspend fun scrambleRound(round: Round, onUpdate: (PuzzlePlugins, String) -> Unit): Round {
        val scrambles = coroutineScope {
            List(round.scrambleSetCount) { async { generateScrambleSet(round, onUpdate) } }.awaitAll()
        }

        return round.copy(scrambleSets = scrambles)
    }

    private fun generateScrambleSet(round: Round, onUpdate: (PuzzlePlugins, String) -> Unit): ScrambleSet {
        val puzzle = Event.findPuzzlePlugin(round.idCode.eventId)
            ?: error("Unable to load scrambler for Round ${round.idCode}")

        val standardScrambleNum = standardScrambleCountPerSet(round)

        val scrambles = if (round.idCode.eventPlugin == EventPlugins.THREE_MULTI_BLD) {
            val countPerAttempt = standardScrambleNum / round.scrambleSetCount

            List(round.expectedAttemptNum) { _ ->
                val scrambles = puzzle.generateEfficientScrambles(countPerAttempt) { onUpdate(puzzle, it) }
                    .joinToString(Scramble.WCIF_NEWLINE_CHAR)

                Scramble(scrambles)
            }
        } else {
            puzzle.generateEfficientScrambles(standardScrambleNum) { onUpdate(puzzle, it) }.map(::Scramble)
        }

        val extraScrambleNum = extraScrambleCountPerSet(round)
        val extraScrambles = puzzle.generateEfficientScrambles(extraScrambleNum) { onUpdate(puzzle, it) }.map(::Scramble)

        // dummy ID -- indexing happens afterwards
        return ScrambleSet(ID_PENDING, scrambles, extraScrambles)
    }

    private fun standardScrambleCountPerSet(round: Round): Int {
        return if (round.idCode.eventPlugin == EventPlugins.THREE_MULTI_BLD) {
            val multiExtCount = round.findExtension<MultiScrambleCountExtension>()
                ?.requestedScrambles ?: error("No multiBLD number for round $round specified")

            round.expectedAttemptNum * multiExtCount
        } else {
            round.expectedAttemptNum
        }
    }

    private fun extraScrambleCountPerSet(round: Round): Int {
        return round.findExtension<ExtraScrambleCountExtension>()?.extraAttempts
            ?: defaultExtraCount(round.idCode.eventPlugin)
    }

    private fun defaultExtraCount(event: EventPlugins?): Int {
        return when (event) {
            EventPlugins.THREE_MULTI_BLD, EventPlugins.THREE_FM -> 0
            else -> 2
        }
    }

    // JOB RESULT SCRAMBLE COUNT COMPUTATION -----

    fun getScrambleCountsPerEvent(wcif: Competition): Map<String, Int> {
        return wcif.events.associateWith { it.rounds }
            .mapValues { (_, rs) ->
                rs.map { it.scrambleSetCount * totalScrambleCountPerSet(it) }.sum()
            }.mapKeys { it.key.id }
    }

    private fun totalScrambleCountPerSet(round: Round): Int {
        return standardScrambleCountPerSet(round) + extraScrambleCountPerSet(round)
    }

    // EXTENSION HANDLING -----

    fun installExtensions(wcif: Competition, ext: Map<ExtensionBuilder, EventPlugins>) =
        ext.entries.fold(wcif) { acc, e -> installExtensionForEvents(acc, e.key, e.value) }

    fun installExtensionForEvents(wcif: Competition, ext: ExtensionBuilder, event: EventPlugins): Competition {
        fun installRoundExtension(e: Event): Event {
            val extendedRounds = e.rounds.map { r ->
                r.copy(extensions = r.withExtension(ext))
            }

            return e.copy(rounds = extendedRounds)
        }

        val extendedEvents = wcif.events.map { e ->
            e.takeUnless { it.id == event.key }
                ?: installRoundExtension(e)
        }

        return wcif.copy(events = extendedEvents)
    }

    // INDEXING -----

    private fun reindexActivities(venues: List<Venue>): List<Venue> {
        val allActivities = venues.flatMap { it.rooms }
            .flatMap { it.activities }
            .flatMap { it.selfAndChildActivities }

        val idIndex = buildReindexingMap(allActivities)

        return venues.map { v ->
            val matchedRooms = v.rooms.map { r ->
                val matchedActivities = r.activities.map { a ->
                    reindexActivityAndChildren(a, idIndex)
                }

                r.copy(activities = matchedActivities)
            }

            v.copy(rooms = matchedRooms)
        }
    }

    private fun reindexActivityAndChildren(activity: Activity, index: Map<Activity, Int>): Activity {
        val reindexedChildren = activity.childActivities.map { reindexActivityAndChildren(it, index) }
        val reindexId = index[activity] ?: activity.id

        return activity.copy(id = reindexId, childActivities = reindexedChildren)
    }

    private fun reindexScrambleSets(events: List<Event>): List<Event> {
        val allScrambleSets = events.flatMap { it.rounds }
            .flatMap { it.scrambleSets }

        val indexTable = buildReindexingMap(allScrambleSets)

        return events.map { e ->
            val reindexedRounds = e.rounds.map { r ->
                val reindexedScrambleSets = r.scrambleSets.map { s ->
                    indexTable[s]?.let { s.copy(id = it) } ?: s
                }

                r.copy(scrambleSets = reindexedScrambleSets)
            }

            e.copy(rounds = reindexedRounds)
        }
    }

    private fun <T : IndexingIdProvider> buildReindexingMap(candidates: List<T>): Map<T, Int> {
        val forReindexing = candidates.filter { it.id == ID_PENDING }
        val maxAssignedId = (candidates - forReindexing)
            .maxBy { it.id }?.id ?: 1

        return forReindexing.mapIndexed { i, elem -> elem to i + maxAssignedId + 1 }
            .toMap()
    }

    // ACTIVITY MATCHING -----

    fun matchActivities(wcif: Competition): Competition {
        val matchedVenues = wcif.schedule.venues.map { v ->
            val matchedRooms = v.rooms.map { r ->
                val matchedActivities = r.activities.map { a ->
                    matchActivity(a, wcif.events)
                }

                r.copy(activities = matchedActivities)
            }

            v.copy(rooms = matchedRooms)
        }

        val matchedVenuesWithIndex = reindexActivities(matchedVenues)
        val matchedSchedule = wcif.schedule.copy(venues = matchedVenuesWithIndex)

        return wcif.copy(schedule = matchedSchedule)
    }

    private fun matchActivity(activity: Activity, events: List<Event>): Activity {
        if (activity.activityCode.eventId in ActivityCode.IGNORABLE_KEYS) {
            return activity
        }

        val children = activity.childActivities

        // we have children that need to be specified!
        if (children.isNotEmpty()) {
            val matchedChildren = children.map { matchActivity(it, events) }
            return activity.copy(childActivities = matchedChildren)
        }

        val matchedRound = findRound(events, activity)
        val actGroup = activity.activityCode.groupNumber

        // uh oh. no child activities where there should be some.
        if (actGroup == null) {
            val actAttempt = activity.activityCode.attemptNumber

            // resort to creating them ourselves…
            if (actAttempt == null) {
                val inventedChildren = List(matchedRound.scrambleSetCount) {
                    val copiedActCode = activity.activityCode.copyParts(groupNumber = it + 1)
                    val childSetId = matchedRound.scrambleSets[it].id

                    activity.copy(id = ID_PENDING, activityCode = copiedActCode, childActivities = listOf(), scrambleSetId = childSetId)
                }

                return activity.copy(childActivities = inventedChildren)
            }

            if (matchedRound.scrambleSetCount > 1) {
                error("Attempt-only specification ${activity.activityCode} for activity ${activity.id} is impossible to match")
            }

            val onlyPossibleSet = matchedRound.scrambleSets.single()
            return activity.copy(scrambleSetId = onlyPossibleSet.id)
        }

        val scrambleSet = matchedRound.scrambleSets[actGroup - 1]

        return activity.copy(scrambleSetId = scrambleSet.id)
    }

    private fun findRound(events: List<Event>, activity: Activity): Round {
        return events
            .filter { it.id == activity.activityCode.eventId }
            .flatMap { it.rounds }
            .find { it.idCode.isParentOf(activity.activityCode) }
            ?: error("An activity of the schedule did not match an event: $activity")
    }
}
