package org.elsiklab

import static org.springframework.http.HttpStatus.*

import grails.converters.JSON
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.Sequence
import org.bbop.apollo.Organism
import org.ho.yaml.Yaml
import java.io.BufferedReader
import java.util.zip.GZIPOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ExportDataController {

    def grailsApplication
    def exportDataService
    def alternativeLociService

    public static Boolean exportEntireGenome = false
    public  Organism selectedOrganism
    public  Breed selectedBreed
    public static ArrayList<Sequence> selectedSequenceList = new ArrayList<Sequence>()
    public def selectedAlternativeLociList = []

    def index() {
        if (alternativeLociService.getCurrentUser()) {
            updateOrganism()

            // log.debug "params: ${params.toString()}"

            def criteria = AlternativeLoci.createCriteria()

            selectedOrganism = Organism.findById(params.organismId)

            def list = criteria.list() {

                featureLocations {
                    sequence {
                        organism {
                            eq('id', params.organismId.toLong())
                        }
                    }
                }

                // eq('type', params.type)

                // eq('breed', params.breed)\

                // log.debug "\n\n\n\nindividual ${params.individual} ${params.individual == 'all'}\n\n\n\n\n"
                and {

                    if(params.individual != null && params.individual != 'all'){
                        'in'('individual', params.individual)
                    }

                    if(params.type != null && params.type != 'all'){
                        'in'('type', params.type)
                    }

                    if(params.breed != null && params.breed != 'all'){
                        breed {
                            'in'('identifier', params.breed)
                        }
                    }

                    if(params.owner != null && params.owner != 'all'){
                       owners {
                            'in'('username', params.owner)
                       }
                    }

                    if(params.chromosome && params.chromosome.length > 0 && params.chromosome != 'all'){
                        'in'('ontologyId', params.chromosome)
                    }

                    // if(params.organismId && params.organismId != 'all'){
                    //     featureLocation {
                    //         sequence {
                    //             'in'order('organism', params.organismId)
                    //         }
                    //     }
                    // }
                    // featureLocations {
                    //     sequence {
                    //         'eq'('organism', params.organismId)
                    //     }
                    // }

                }

                if(params.sort == 'created') {
                    order('dateCreated', params.order)
                }

                if(params.sort == 'owners') {
                    owners {
                        order('username', params.order)
                    }
                }

                if(params.sort == 'location') {
                    featureLocation {
                        sequence {
                            order('name', params.order)
                        }
                        order('fmin', params.order)
                    }
                }

                if(params.sort == 'type') {
                    order('type', params.order)
                }

                if(params.sort == 'name') {
                    order('name', params.order)
                }

                if(params.sort == 'breed') {
                    breed {
                        order('name', params.order)
                    }
                }

                if(params.sort == 'individual') {
                    order('individual', params.order)
                }

                if(params.sort == 'description') {
                    order('description', params.order)
                }

                // if(params.sort == 'organismId') {
                //     featureLocations {
                //         sequence {
                //             organismId {
                //                 order('commonName', params.order)
                //             }
                //         }
                //     }
                // }

                if(params.sort == 'lsaa_length') {
                    featureLocations {
                        order('fmax' - 'fmin', params.order)
                    }
                }

                if(params.sort == 'input_length') {
                    order('endPosition' - 'startPosition', params.order)
                }

                // if(params.sort == 'organismId'){
                //      featureLocations {
                //         sequence {
                //             order('organism', params.order)
                //         }
                //     }
                //     // breed {
                //     //     order('organism', params.order)
                //     // }
                // }

            }

            selectedAlternativeLociList = list

            // log.debug "list: ${list.toString()}"
            // log.debug "selectedList: ${selectedAlternativeLociList.toString()}"


            // selectedBreed = Breed.findByName(params.breed)

            //             {  
            //    [  
            //       Feature      {  
            //          id=21657,
            //          symbol='null',
            //          description='null',
            //          name='letourneaujj_Group1:9759847-9759847         _INS_2018-02-20_13:47:52         ', 
            //          uniqueName='         657         dc749-b66a-4302-90c5-7b55e8dd0100',
            //          sequenceLength=null,
            //          status=null,
            //          dateCreated=2018-02-20 13:47:52.787,
            //          lastUpdated=2018-02-20 13:47:52.858
            //       },
            //       Feature      {  
            //          id=21659,
            //          symbol='null',
            //          description='null',
            //          name='letourneaujj_Group1:9760208-9760291         _DEL_2018-02-21_13:02:51         ', 
            //          uniqueName='         94         ae902c-f54f-481c-be7f-9b1a2a520065',
            //          sequenceLength=null,
            //          status=null,
            //          dateCreated=2018-02-21 13:02:51.489,
            //          lastUpdated=2018-02-21 13:02:51.555
            //       }
            //    ]
            // }

            render view: 'index', model: [ features: selectedAlternativeLociList ]
        }
        else {
            //render status: 401, text: 'Failed user authentication'
	    render status: 401, text: '<p style="text-align: center">You must be logged in to submit LSAA and access the Export Table</p><p style="text-align: center">Please see instructions <a href="https://bovinegenome.elsiklab.missouri.edu/annotator_login">here</a></p>'

        }
        exportEntireGenome = false
        // selectedOrganismId = null
        // selectedBreed = null
        // selectedSequenceList.clear()
        // selectedAlternativeLociList.clear()
        // render view: 'index'
    }

    /**
     *
     * @return
     */
    def exportSequences() {
        log.debug "params: ${params.toString()}"
        exportEntireGenome = false


            def criteria = AlternativeLoci.createCriteria()

            def list = criteria.list() {

                // featureLocations {
                //     sequence {
                //         organism {
                //             eq('id', params.organism)
                //         }
                //     }
                // }

                featureLocations {
                    sequence {
                        organism {
                            eq('id', params.organismId.toLong())
                        }
                    }
                }
                // eq('type', params.type)

                // eq('breed', params.breed)
                and {

                    if(params.individual != null){
                        'in'('individual', params.individual)
                    }

                    if(params.type != null ){
                        'in'('type', params.type)
                    }

                    if(params.chromosome != null){
                        'in'('ontologyId', params.chromosome)
                    }

                    if(params.breed != null){
                        breed {
                            'in'('identifier', params.breed)
                        }
                    }

                    if(params.owner != null){
                       owners {
                            'in'('username', params.owner)
                       }
                    }

                    if(params.chromosome && params.chromosome.length > 0){
                        'in'('ontologyId', params.chromosome)
                    }

                }

                if(params.sort == 'created') {
                    order('dateCreated', 'ASC')
                }
                if(params.sort == 'owners') {
                    owners {
                        order('username', 'ASC')
                    }
                }
                if(params.sort == 'location') {
                    featureLocations {
                        sequence {
                            order('name', 'ASC')
                        }
                        order('fmin', 'ASC')
                    }
                }
                if(params.sort == 'type') {
                    order('type', 'ASC')
                }
                if(params.sort == 'name') {
                    order('name', 'ASC')
                }
                if(params.sort == 'breed') {
                    breed {
                        order('name', 'ASC')
                    }
                }
                if(params.sort == 'individual') {
                    order('individual', 'ASC')
                }
                if(params.sort == 'description') {
                    order('description', 'ASC')
                }
                // if(params.sort == 'organismId') {
                //     featureLocations {
                //         sequence {
                //             organism {
                //                 order('commonName', 'ASC')
                //             }
                //         }
                //     }
                // }
            }


        if(params['data-format'] == 'JSON') {
            getTransformedJson()
        }
        else if(params['data-format'] == 'FASTA') {
            getTransformedFasta()
        }
        else {
            render text: 'Unknown download method'
        }
    }

    def exportGenome() {
        // log.debug "exportGenome: ${params.toString()}"
        // exportEntireGenome = true

        if(params['data-format'] == 'JSON') {
            getTransformedJson()
        }
        else if(params['data-format'] == 'FASTA') {
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
        // log.debug "params: ${params.toString()}"
        // log.debug "selectedOrganism: ${selectedOrganism?.commonName}"
        // log.debug "selectedBreed: ${selectedBreed?.nameAndIdentifier}"
        //log.debug "[exportDataController][getTransformedJson] selectedSequenceList: ${selectedSequenceList?.size()}"
        // log.debug "selectedAlternativeLociList: ${selectedAlternativeLociList?.size()}"
        
            def criteria = AlternativeLoci.createCriteria()

            def list = criteria.list() {
                'in'('uniqueName', params.selection)
            }

            // log.debug "\n\nlist: ${list}\n\n"


        if (selectedOrganism) {

            def transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, selectedBreed, list, false)

            // log.debug "organism: ${organism.toString()} breed: ${breed.toString()} list: ${selectedAlternativeLociList.toString()} params: ${params.toString()}"
            response.contentType = 'text/plain'
            if(params.download == 'download') {
                response.setHeader ('Content-disposition', 'attachment;filename=output.json')
            }
            def responseJson = transformedJsonObject as JSON
            responseJson.prettyPrint = true
            render text: responseJson
        //     def transformedJsonObject
        //     if (selectedAlternativeLociList.size() > 0) {
        //         transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, selectedBreed, selectedAlternativeLociList, exportEntireGenome)
        //     }
        //     else if (selectedBreed) {
        //         transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, selectedBreed, exportEntireGenome)
        //     }
        //     else {
        //         transformedJsonObject = exportDataService.getTransformationAsJson(selectedOrganism, exportEntireGenome)
        //     }

        //     response.contentType = 'text/plain'
        //     if(params.download == 'download') {
        //         response.setHeader ('Content-disposition', 'attachment;filename=output.json')
        //     }
        //     def responseJson = transformedJsonObject as JSON
        //     responseJson.prettyPrint = true
        //     render text: responseJson
        // }
        } else {
            render ([error: "Organism not found"] as JSON)
        }
    }

    /**
     *
     * @return
     */
    def getTransformedFasta() {
        // log.debug "params: ${params.toString()}"
        // log.debug "selectedOrganism: ${selectedOrganism.commonName}"
        // log.debug "selectedBreed: ${selectedBreed?.nameAndIdentifier}"
        // //log.debug "[exportDataController][getTransformedJson] selectedSequenceList: ${selectedSequenceList?.size()}"
        // log.debug "selectedAlternativeLociList: ${selectedAlternativeLociList?.size()}"



        def criteria = AlternativeLoci.createCriteria()

        def list = criteria.list() {
            'in'('uniqueName', params.selection)
        }


        if (selectedOrganism) {

            def transformedFastaMap = [:]

            if(list.breed.name == null){
                log.debug "\n\n\norganism: ${selectedOrganism.toString()} list: ${list.toString()} params: ${params.toString()}\n\n\n"
                transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, false)
            } else {
                selectedBreed = Breed.findByName(list.breed.name)
                log.debug "\n\n\norganism: ${selectedOrganism.toString()} breed: ${selectedBreed.toString()} list: ${list.toString()} params: ${params.toString()}\n\n\n"
                transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, selectedBreed, list, false)
            }

            log.debug transformedFastaMap.dump()
            
            //response.contentType = 'text/plain'
	    response.setContentType("application/octet-stream")
	    response.setHeader("Content-disposition", "attachment;filename=output.fa.gz")
            if (params.download == 'download') {
                response.setHeader("Content-disposition", "attachment;filename=output3.fa.gz")
            }

            log.debug "ORDID: ${selectedOrganism.id}, BREEDID: ${list.breed.name}"
            File file = new File(transformedFastaMap.getAt(selectedOrganism.id))
           // FileInputStream file = new FileInputStream(transformedFastaMap.getAt(selectedOrganism.id))
            // File file = new File(transformedFastaMap.get(selectedOrganism.id))
            if (file) {
		new GZIPOutputStream(response.outputStream).withWriter { it << file.text }
		log.debug "\n\n SENDING FILE GZIP  \n\n"
                //GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(file))
                //response.outputStream = new GZIPOutputStream(new FileOutputStream(file))
		//response.outputStream << gzip.bytes
		//response.outputStream = gzip
               	//BufferedReader br = new BufferedReader(gzip)
                //response.outputStream << br.bytes
                //response.outputStream << file.bytes
            } else {
                render ([error: "Cannot find file"] as JSON)
            }

        //     def transformedFastaMap
            // if (selectedAlternativeLociList.size() > 0) {
            //     transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, selectedBreed, selectedAlternativeLociList, exportEntireGenome)
            // }
            // else if (selectedBreed) {
            //     transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, selectedBreed, exportEntireGenome)
            // }
            // else {
            //     transformedFastaMap = exportDataService.getTransformationAsFasta(selectedOrganism, exportEntireGenome)
            // }
        //     log.debug "Transformed FASTA map: ${transformedFastaMap.toString()}"

        //     response.contentType = 'text/plain'
        //     //response.setHeader("Content-disposition", "attachment;filename=${transformedFastaMap[selectedOrganism.id]}")
        //     if (params.download == 'download') {
        //         response.setHeader("Content-disposition", "attachment;filename=output.fasta")
        //     }

        //     File file = new File(transformedFastaMap.get(selectedOrganism.id))
        //     if (file) {
        //         response.outputStream << file.bytes
        //     }
        //     else {
        //         render ([error: "Cannot find file"] as JSON)
        //     }
        } else {
            render ([error: "Organism not found"] as JSON)
        }

    }

    def updateOrganism() {
        // log.debug "updateOrganism: ${params.toString()}"
        // if (params.get("organismId") == "null") {
        //     render "N/A"
        // }
        // else {
            String organismId = params.get("organismId")
            Organism organism = Organism.findById(organismId)
            selectedOrganism = organism

//            def breeds = Breed.findAllByOrganism(organism)
//            log.debug "breeds: ${breeds.size()}"

            // def categories = ['Assembly Correction', 'Structural Variation']
            // render g.select(id: 'categories', name: 'category',
            //             from: categories, noSelection:[null:'Select a category'], onchange: "updateCategory(this.value)"
            // )
//            render g.select(id:'breeds', name:'breed.id',
//                    from:breeds, optionKey:'id', optionValue: "nameAndIdentifier", noSelection:[null:'Select a breed'], onchange: "updateBreed(this.value)"
//            )
        // }
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

    def updateCategory() {
        log.debug "params: ${params.toString()}"
        String category = params.get("category")
        log.debug "Category: ${category}"

        if (category == "Structural Variation") {
            def breeds = Breed.findAllByOrganism(selectedOrganism)
            render g.select(id:'breeds', name:'breed.id',
                    from:breeds, optionKey:'id', optionValue: "nameAndIdentifier", noSelection:[null:'Select a breed'], onchange: "updateBreed(this.value)"
            )
        }
        else {
            def alternativeLociList = AlternativeLoci.findAllByBreed(null)
            if (alternativeLociList.size() > 0) {
                // return a table instead of a g.select
                println "Alt loci list: ${alternativeLociList}"
                println "Calling render";
                render g.select(id: 'alternativeLociList', name: 'alternativeLoci.id', multiple: 'multiple',
                        from: alternativeLociList, optionKey: 'id', optionValue: 'name', noSelection: [All: 'All'], onchange: "updateAlternativeLoci(this.options)"
                )
            }
            else {
                render "N/A"
            }
        }
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
                // return a table instead of a g.select
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
        println "uAL: params: ${params.toString()}"
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
