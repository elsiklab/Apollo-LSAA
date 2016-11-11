package org.elsiklab

import grails.converters.JSON
import groovy.json.JsonBuilder
import org.bbop.apollo.Sequence

class AltLociTrackController {

    def features() {
        log.info "@altLociTrack::features() with params: ${params}"
        String sequenceName = params.id
        // Somehow the a sequence name with a '.', ex: Group1.10, is split into
        // id and format with values Group1 and 10, respectively
        // This is a quick solution to this issue
        if (params.format && params.format != "" && params.format != " ") {
            sequenceName = sequenceName + "." + params.format
        }
        Sequence sequence = Sequence.findByName(sequenceName)
        def features = AlternativeLoci.createCriteria().list {
            featureLocations {
                eq('sequence', sequence)
            }
        }

        // TODO: Return more information about the AlternateLoci
        // TODO: Are the start and the end properly sent to the client?
        JsonBuilder json = new JsonBuilder ()
        json.features features, { it ->
            uniqueID it.uniqueName
            start it.featureLocation.fmin - 1
            end it.featureLocation.fmax - 1
            ref it.featureLocation.sequence.name
            description it.description
        }

        render json.toString()
    }

    def globalStats() {
        render ([] as JSON)
    }
}
