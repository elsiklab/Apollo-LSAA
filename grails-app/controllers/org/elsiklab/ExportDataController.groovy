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
            getTransformedJson()
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
    def getTransformedJson() {
        log.debug "params: ${params.toString()}"
        def organism = Organism.findByCommonName(params.organism) ?: Organism.findById(params.organism)
        if (organism) {
            def transformedJsonObject = exportDataService.getTransformationAsJson(organism)
            response.contentType = 'text/plain'
            if(params.download == 'download') {
                response.setHeader ('Content-disposition', 'attachment;filename=output.json')
            }
            def responseJson = transformedJsonObject as JSON
            responseJson.prettyPrint = true
            render text: responseJson
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
        log.debug "params: ${params.toString()}"
        def organism = Organism.findByCommonName(params.organism) ?: Organism.findById(params.organism)
        if (organism) {
            def transformedFastaMap = exportDataService.getTransformationAsFasta(organism)
            response.contentType = 'text/plain'
            if (params.download == 'download') {
                response.setHeader('Content-disposition', 'attachment;filename=output.fasta')
            }

            String responseData = ""
            for (String key : transformedFastaMap.keySet()) {
                responseData += ">${transformedFastaMap[key]["name"]} ${transformedFastaMap[key]["description"]}\n${transformedFastaMap[key]["sequence"]}\n"
            }

            render text: responseData
        }
        else {
            render ([error: "Organism not found"] as JSON)
        }

    }
}
