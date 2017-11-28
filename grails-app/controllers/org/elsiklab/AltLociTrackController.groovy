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
            start it.featureLocation.fmin
            end it.featureLocation.fmax
            type it.type
            description it.description
        }

        render json.toString()
    }

    def globalStats() {
        render ([] as JSON)
    }
}
