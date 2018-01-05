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

    public static Boolean exportEntireGenome = false
    public static Organism selectedOrganism
    public static Breed selectedBreed
    public static ArrayList<Sequence> selectedSequenceList = new ArrayList<Sequence>()
    public static def selectedAlternativeLociList = []

    def index() {
        exportEntireGenome = false
        selectedOrganism = null
        selectedBreed = null
        selectedSequenceList.clear()
        selectedAlternativeLociList.clear()
        render view: 'index'
    }

    /**
     *
     * @return
     */
    def exportSequences() {
        log.debug "params: ${params.toString()}"
        exportEntireGenome = false
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

    def exportGenome() {
        log.debug "exportGenome: ${params.toString()}"
        exportEntireGenome = true
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
        log.debug "selectedOrganism: ${selectedOrganism?.commonName}"
        log.debug "selectedBreed: ${selectedBreed?.nameAndIdentifier}"
        //log.debug "[exportDataController][getTransformedJson] selectedSequenceList: ${selectedSequenceList?.size()}"
        log.debug "selectedAlternativeLociList: ${selectedAlternativeLociList?.size()}"

        if (selectedOrganism) {
            def transformedJsonObject
            if (selectedAlternativeLociList.size() > 0) {
                transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, selectedBreed, selectedAlternativeLociList, exportEntireGenome)
            }
            else if (selectedBreed) {
                transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, selectedBreed, exportEntireGenome)
            }
            else {
                transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, exportEntireGenome)
            }

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
        log.debug "selectedOrganism: ${selectedOrganism.commonName}"
        log.debug "selectedBreed: ${selectedBreed?.nameAndIdentifier}"
        //log.debug "[exportDataController][getTransformedJson] selectedSequenceList: ${selectedSequenceList?.size()}"
        log.debug "selectedAlternativeLociList: ${selectedAlternativeLociList?.size()}"

        if (selectedOrganism) {
            def transformedFastaMap
            if (selectedAlternativeLociList.size() > 0) {
                transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, selectedBreed, selectedAlternativeLociList, exportEntireGenome)
            }
            else if (selectedBreed) {
                transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, selectedBreed, exportEntireGenome)
            }
            else {
                transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, exportEntireGenome)
            }
            log.debug "Transformed FASTA map: ${transformedFastaMap.toString()}"

            response.contentType = 'text/plain'
            //response.setHeader("Content-disposition", "attachment;filename=${transformedFastaMap[selectedOrganism.id]}")
            if (params.download == 'download') {
                response.setHeader("Content-disposition", "attachment;filename=output.fasta")
            }

            File file = new File(transformedFastaMap.get(selectedOrganism.id))
            if (file) {
                response.outputStream << file.bytes
            }
            else {
                render ([error: "Cannot find file"] as JSON)
            }
        }
        else {
            render ([error: "Organism not found"] as JSON)
        }

    }

    def updateOrganism() {
        log.debug "${params.toString()}"
        if (params.get("organismId") == "null") {
            render "N/A"
        }
        else {
            String organismId = params.get("organismId")
            Organism organism = Organism.findById(organismId)
            selectedOrganism = organism

            def breeds = Breed.findAllByOrganism(organism)
            log.debug "breeds: ${breeds.size()}"

            render g.select(id:'breeds', name:'breed.id',
                    from:breeds, optionKey:'id', optionValue: "nameAndIdentifier", noSelection:[null:'Select a breed'], onchange: "updateBreed(this.value)"
            )
        }
    }

    def updateOrganismSequences() {
        log.debug "params: ${params.toString()}"
        String organismId = params.get("organismId")
        Organism organism = Organism.findById(organismId)
        def sequences = []
        selectedOrganism = organism
        sequences = organism.sequences
        log.debug "[ExportDataController][updateOrganismSequences] sequences list size: ${sequences.size()}"

        render g.select(id:'sequences', name:'sequences', multiple: 'multiple', value: '${sequences?.id}',
                from:sequences, optionKey:'id', optionValue: 'name', noSelection:[All:'All'], onchange: "updateSequences(this.options)"
        )
    }

    def updateBreed() {
        log.debug "params: ${params.toString()}"
        if (params.get("breedId") == "null") {
            render "N/A"
        }
        else {
            Long breedId = params.getLong("breedId")
            log.debug "selected organism: ${selectedOrganism}"
            Breed breed = Breed.findByIdAndOrganism(breedId, selectedOrganism)
            selectedBreed = breed
            def alternativeLociList = breed.alternativeLoci
            log.debug "alternativeLociList size: ${alternativeLociList.size()}"

            if (alternativeLociList.size() > 0) {
                render g.select(id: 'alternativeLociList', name: 'alternativeLoci.id', multiple: 'multiple',
                        from: alternativeLociList, optionKey: 'id', optionValue: 'name', noSelection: [All: 'All'], onchange: "updateAlternativeLoci(this.options)"
                )
            }
            else {
                render "N/A"
            }
        }

    }

    def updateSequences() {
        log.debug "params: ${params.toString()}"
        String selectedSequences = params.selectedSequences
        selectedSequenceList = []
        if (selectedSequences.length() != 0 && selectedSequences != "All") {
            selectedSequences.split(",").each { sequenceId ->
                Sequence sequence = Sequence.findById(Long.parseLong(sequenceId))
                if (sequence) selectedSequenceList.add(sequence)
            }
        }

        log.debug "selected sequence list: ${selectedSequenceList.size()}"
    }

    def updateAlternativeLoci() {
        log.debug "params: ${params.toString()}"
        String selectedAlternativeLoci = params.selectedAlternativeLoci
        selectedAlternativeLociList = []
        if (selectedAlternativeLoci != "All") {
            if (selectedAlternativeLoci.length() != 0 && selectedAlternativeLoci != "All") {
                selectedAlternativeLoci.split(",").each { alternativeLociId ->
                    AlternativeLoci alternativeLoci = AlternativeLoci.findById(alternativeLociId)
                    selectedAlternativeLociList.add(alternativeLoci)
                }
            }
        }

        log.debug "selected alternativeLoci list: ${selectedAlternativeLociList.size()}"
    }
}
