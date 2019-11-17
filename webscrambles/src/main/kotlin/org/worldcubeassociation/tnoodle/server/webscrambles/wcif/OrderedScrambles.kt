package org.worldcubeassociation.tnoodle.server.webscrambles.wcif

import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFHelper.Companion.filterForActivity
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFHelper.Companion.atStartOfDay
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest.Companion.putFileEntry
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.WCIFHelper.Companion.WCIF_DATE_FORMAT
import java.time.LocalDateTime
import java.time.Period
import java.time.ZonedDateTime

import java.util.Date

object OrderedScrambles {
    fun generateOrderedScrambles(scrambleRequests: List<ScrambleRequest>, globalTitle: String?, generationDate: Date, zipOut: ZipOutputStream, parameters: ZipParameters, wcifHelper: WCIFHelper) {
        if (wcifHelper.venues.isEmpty()) {
            return
        }

        var hasMultipleDays = wcifHelper.hasMultipleDays
        val hasMultipleVenues = wcifHelper.hasMultipleVenues

        // We consider the competition start date as the earlier activity from the schedule.
        // This prevents miscalculation of dates for multiple timezones.
        val competitionStartString = wcifHelper.earliestActivityString

        for (venue in wcifHelper.venues) {
            val venueName = venue.fileSafeName
            val hasMultipleRooms = venue.hasMultipleRooms

            val timezone = venue.dateTimeZone
            val competitionStartDate = ZonedDateTime.of(LocalDateTime.parse(competitionStartString, WCIF_DATE_FORMAT), timezone)

            for (room in venue.rooms) {
                val roomName = room.fileSafeName

                val requestsPerDay = room.activities
                    .flatMap { scrambleRequests.filterForActivity(it, timezone) }
                    .groupBy { Period.between(competitionStartDate.atStartOfDay(), it.second.atStartOfDay()).days + 1 }

                // hasMultipleDays gets a variable assigned on the competition creation using the website's form.
                // Online schedule fit to it and the user should not be able to put events outside it, but we double check here.
                // The next assignment fix possible mistakes (eg. a competition is assigned with 1 day, but events are spread among 2 days).
                hasMultipleDays = hasMultipleDays || requestsPerDay.size > 1

                for ((day, scrambles) in requestsPerDay) {
                    if (scrambles.isNotEmpty()) {
                        val parts = listOfNotNull(
                            "Printing/Ordered Scrambles/",
                            "$venueName/".takeIf { hasMultipleVenues },
                            "Day $day/".takeIf { hasMultipleDays },
                            "Ordered Scrambles",
                            " - $venueName".takeIf { hasMultipleVenues },
                            " - Day $day".takeIf { hasMultipleDays },
                            " - $roomName".takeIf { hasMultipleRooms },
                            ".pdf"
                        )

                        if (hasMultipleVenues || hasMultipleDays || hasMultipleRooms) {
                            // In addition to different folders, we stamp venue, day and room in the PDF's name
                            // to prevent different files with the same name.
                            val pdfFileName = parts.joinToString("")

                            val sortedScrambles = scrambles.sortedBy { it.second }.map { it.first }

                            val sheet = ScrambleRequest.requestsToCompletePdf(globalTitle, generationDate, sortedScrambles)
                            zipOut.putFileEntry(pdfFileName, sheet.render(), parameters)
                        }
                    }
                }
            }
        }
    }
}
