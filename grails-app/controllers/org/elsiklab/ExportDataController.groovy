package org.elsiklab

import static org.springframework.http.HttpStatus.*

import grails.converters.JSON
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.Sequence
import org.bbop.apollo.Organism
import org.ho.yaml.Yaml

class ExportDataController {

    def grailsApplication
    def exportDataService

    def index() {
        render view: 'index'
    }

    /**
     *
     * @return
     */
    def export() {
        if(params.type == 'JSON') {
            getTransformedJSON()
        }
        else if(params.type == 'FASTA') {
            getTransformedFasta()
        }
        else {
            render text: 'Unknown download method'
        }
    }

    /**
     *
     * @return
     */
    def getTransformedJSON() {
        println "@getTransformedJson: ${params.toString()}"
        def organism = Organism.findByCommonName(params.organism)?:Organism.findById(params.organism)
        if (organism) {
            def transformedJsonObject = exportDataService.getTransformationAsJSON(organism)

            response.contentType = 'application/json'
            if(params.download == 'download') {
                response.setHeader 'Content-disposition', 'attachment;filename=output.json'
            }
            def json = transformedJsonObject as JSON
            json.prettyPrint = true
            render text: json
        }
        else {
            render ([error: "Organism not found"] as JSON)
        }
    }

    /**
     *
     * @return
     */
    def getTransformedFasta() {
        println "@getTransformedFasta: ${params.toString()}"
        def organism = Organism.findByCommonName(params.organism)?:Organism.findById(params.organism)
        if (organism) {
            def transformedFastaMap = exportDataService.getTransformationAsFASTA(organism)

            response.contentType = 'application/json'
            if (params.download == 'download') {
                response.setHeader 'Content-disposition', 'attachment;filename=output.fasta'
            }

            String responseData = ""
            for (String key : transformedFastaMap.keySet()) {
                responseData += '>' + key + '-LSAA' + ' ' + transformedFastaMap[key]["comment"] + '\n' + transformedFastaMap[key]["sequence"] + '\n'
            }

            render text: responseData
        }
        else {
            render ([error: "Organism not found"] as JSON)
        }

    }
}
