package org.elsiklab

import grails.converters.JSON
import groovy.json.JsonBuilder
import org.bbop.apollo.Sequence

class AltLociTrackController {

    def features() {
        // Have to do this since a sequence name with a period in it, ex: Group1.10, is interpreted as id: Group1 format: 10
        String sequenceName = params.id
        if (params.format && params.format != "" && params.format != " ") {
            sequenceName = sequenceName + "." + params.format
        }

        Sequence sequence = Sequence.findByName(sequenceName)
        def features = AlternativeLoci.createCriteria().list {
            featureLocations {
                eq('sequence', sequence)
            }
        }

        JsonBuilder json = new JsonBuilder ()
        json.features features, { it ->
            uniqueID it.uniqueName
            name it.name
            type it.type
            start it.featureLocation.fmin
            end it.featureLocation.fmax
            orientation it.orientation == -1 ? "Reverse" : "Forward"
            size_of_loci (it.featureLocation.fmax - it.featureLocation.fmin) + 1
            size_of_input (it.endPosition - it.startPosition) + 1
            breed it.breed.nameAndIdentifier
            description it.description
            owner it.owner.username
        }

        render json.toString()
    }

    def globalStats() {
        render ([] as JSON)
    }
}
