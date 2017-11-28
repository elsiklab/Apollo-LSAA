package org.elsiklab

import grails.transaction.Transactional
import grails.converters.JSON
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.bbop.apollo.Organism
import org.bbop.apollo.TabDelimitedAlignment
import org.codehaus.groovy.grails.web.json.JSONObject

class SequenceSearchController {


    def grailsApplication
    def permissionService

    def searchSequence() {
        // { track: Group1.1, search: { key: 'blast_nuc', residues: 'GATCAGCTACA...', database_id: 'Group1.1' }, operation: 'search_sequence', organism: '21' }
        // TODO: handle authentication
        //def requestObject = permissionService.handleInput(request, params)
        log.debug "Request: ${request.JSON}"
        Organism organism = Organism.findById(request.JSON.organism)

        if(organism) {
            def search = request.JSON.search
            StringBuilder stringBuilder = new StringBuilder()
            def searchUtils = grailsApplication.config.lsaa.sequence_search_tools.get(search.key)

            searchUtils.put('database', organism.blatdb)
            searchUtils.put('output_dir', organism.directory)

            // dynamically allocate a search class
            try {
                def searcher = this.class.classLoader.loadClass( searchUtils.search_class )?.newInstance()
                searcher.parseConfiguration(searchUtils as JSONObject)
                def results = searcher.search('searchid', search.residues, search.database_id ?: '', stringBuilder)

                def slurper = new JsonSlurper()
                def filetext = new File(organism.directory + File.separator + 'trackList.json').text
                def trackList = slurper.parseText(filetext)

                def newtrack = [:]
                trackList.tracks.each { result ->
                    if(result.label == stringBuilder.toString()) {
                        newtrack = result
                    }
                }
                log.debug "new track: ${newtrack.toString()}"

                // convert results into JSON
                JsonBuilder json = new JsonBuilder()
                json {
                    matches results.collect { TabDelimitedAlignment result ->
                        [
                            'identity': result.percentId,
                            'significance': result.eValue,
                            'subject': ({
                                'location' ({
                                    'fmin' result.subjectStart
                                    'fmax' result.subjectEnd
                                    'strand' result.subjectStrand
                                })
                                'feature' ({
                                    'uniquename' result.subjectId
                                    'type'({
                                        'name' 'region'
                                        'cv' ({
                                            'name' 'sequence'
                                        })
                                    })
                                })
                            }),
                            'query': ({
                                'location' ({
                                    'fmin' result.queryStart
                                    'fmax' result.queryEnd
                                    'strand' result.queryStrand
                                })
                                'feature' ({
                                    'uniquename' result.queryId
                                    'type' ({
                                        'name' 'region'
                                        'cv'({
                                            'name' 'sequence'
                                        })
                                    })
                                })
                            }),
                            'rawscore': result.bitscore
                        ]
                    }
                    track newtrack
                }

                render json
            } catch (IOException e) {
                render ([error: "Error: ${e.message}"]) as JSON
            }
        }
        else {
            render ([error: "Organism not found with ${request.JSON.organism}"] as JSON)
        }
    }

    def getSequenceSearchTools() {
        def sequenceSearchConfig = ['sequence_search_tools': grailsApplication.config.lsaa.sequence_search_tools]
        render sequenceSearchConfig as JSON
    }
}
