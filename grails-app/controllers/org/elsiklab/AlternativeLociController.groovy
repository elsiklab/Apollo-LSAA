package org.elsiklab

import org.bbop.apollo.Feature
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

import grails.converters.JSON
import grails.util.Environment
import grails.transaction.Transactional
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.Sequence
import org.bbop.apollo.BiologicalRegion
import org.bbop.apollo.OrganismProperty
import org.bbop.apollo.Organism
import org.bbop.apollo.User
import org.apache.shiro.SecurityUtils

class AlternativeLociController {

    def permissionService
    def featureService
    def fastaFileService
    def alternativeLociService


    def index(Integer max) {
        if (alternativeLociService.getCurrentUser()) {
            params.max = Math.min(max ?: 20, 100)

            def c = Feature.createCriteria()

            def list = c.list(max: params.max, offset:params.offset) {

                if(params.sort == 'owners') {
                    owners {
                        order('username', params.order)
                    }
                }
                if(params.sort == 'sequencename') {
                    featureLocations {
                        sequence {
                            order('name', params.order)
                        }
                    }
                }
                else if(params.sort == 'name') {
                    order('name', params.order)
                }
                else if(params.sort == 'cvTerm') {
                    order('class', params.order)
                }
                else if(params.sort == 'organism') {
                    featureLocations {
                        sequence {
                            organism {
                                order('commonName', params.order)
                            }
                        }
                    }
                }
                else if(params.sort == 'lastUpdated') {
                    order('lastUpdated', params.order)
                }

                if(params.ownerName != null && params.ownerName != '') {
                    owners {
                        ilike('username', '%' + params.ownerName + '%')
                    }
                }
                if(params.organismName != null && params.organismName != '') {
                    featureLocations {
                        sequence {
                            organism {
                                ilike('commonName', '%' + params.organismName + '%')
                            }
                        }
                    }
                }
                'eq'('class', AlternativeLoci.class.name)
            }
            log.debug list.toString()
            render view: 'index', model: [features: list, sort: params.sort, alternativeLociInstanceCount: list.totalCount]
        }
        else {
            render status: 401, text: 'Failed user authentication'
        }
    }

    def addLoci() {
        Sequence sequence = Sequence.findByName(params.sequence)
        if(!sequence) {
            response.status = 500
            render ([error: 'No sequence found'] as JSON)
            return
        }

        if(!params.sequencedata.length()) {
            response.status = 500
            render ([error: 'No sequencedata provided'] as JSON)
            return
        }

        new Sequence(
                name: name,
                organism: sequence.organism,
                start: 0,
                end: params.sequencedata.length(),
                length: params.sequencedata.length(),
                seqChunkSize: 20000
        ).save()

        def filename

        new File("${sequence.organism.directory}/${name}.fa").with {
            write('>' + name + '\n' + params.sequencedata + '\n')
            filename = absolutePath
            ("prepare-refseqs.pl --fasta ${absolutePath} --out ${sequence.organism.directory}").execute()
            ("generate-names.pl --completionLimit 0 --out ${sequence.organism.directory}").execute()

            new OrganismProperty(key: 'blatdb', organism: sequence.organism, value: name).save()
            new OrganismProperty(key: 'blatdbpath', organism: sequence.organism, value: absolutePath).save()

            // remake fasta index, blat db, blast db
            ("makeblastdb -dbtype nucl -in ${absolutePath} -title ${name}").execute()
        }
        String name = UUID.randomUUID()
        def fastaFile = new FastaFile(
                filename: filename,
                dateCreated: new Date(),
                dateModified: new Date(),
                username: 'admin',
                originalname: 'admin-' + new Date()
        ).save(flush: true)

        AlternativeLoci altloci = new AlternativeLoci(
                description: params.description,
                name: name,
                uniqueName: name,
                start_file: 0,
                end_file: params.sequencedata.length(),
                name_file: params.name_file,
                fastaFile: fastaFile
        ).save(flush: true)

        FeatureLocation featureLoc = new FeatureLocation(
                fmin: params.start,
                fmax: params.end,
                feature: altloci,
                sequence: sequence
        ).save(flush: true)
        altloci.addToFeatureLocations(featureLoc)

        def owner = User.findByUsername(SecurityUtils.subject.principal ?: 'admin')
        if (!owner && Environment.current != Environment.PRODUCTION) {
            owner = new User(username: 'admin', passwordHash: 'admin', firstName: 'admin', lastName: 'admin')
            owner.save(flush: true)
        }
        altloci.addToOwners(owner)

        render ([success: 'create loci success'] as JSON)
    }

    def show(AlternativeLoci alternativeLociInstance) {
        respond alternativeLociInstance
    }

    def edit(AlternativeLoci alternativeLociInstance) {
        render view: 'edit', model: [alternativeLociInstance: alternativeLociInstance]
    }

    @Transactional
    def update(AlternativeLoci instance) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.debug "requestObject: ${requestObject.toString()}"
        def sequence = Sequence.findById(requestObject.sequence)
        if (sequence) {
            // sequence exsits
            def fastaFile = FastaFile.findById(requestObject.fastaFile)
            if (fastaFile) {
                // fastaFile exists
                def file = new File(fastaFile.fileName)
                if (file) {
                    // file exists
                    instance.name = requestObject.name
                    instance.description = requestObject.description
                    instance.startPosition = Integer.parseInt(requestObject.startPosition - 1) ?: 0
                    instance.endPosition = Integer.parseInt(requestObject.endPosition) ?: file.length()
                    instance.featureLocation.fmin = Integer.parseInt(requestObject.start) - 1
                    instance.featureLocation.fmax = Integer.parseInt(requestObject.end)
                    instance.featureLocation.sequence = sequence
                    instance.orientation = requestObject.orientation
                    instance.save(flush: true, failOnError: true)

                    render view: 'edit', model: [alternativeLociInstance: instance]
                }
                else {
                    render text: ([error: 'FASTA file does not exist'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'No FastaFile found'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No Sequence found'] as JSON), status: 500
        }
    }

    @Transactional
    def delete(AlternativeLoci alternativeLociInstance) {
        if (alternativeLociInstance == null) {
            notFound()
            return
        }

        if (!alternativeLociInstance.reversed) {
            log.info "removing fasta_file ${alternativeLociInstance.fastaFile}"
            if (alternativeLociInstance.fastaFile) {
                FastaFile fastaFile = alternativeLociInstance.fastaFile
                File file = new File(fastaFile.fileName)
                if (file.exists()) file.delete()
                fastaFile.delete()
            }
        }
        alternativeLociInstance.delete(flush:true)

        redirect(action: 'index')
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'availableStatus.label', default: 'AlternativeLoci'), params.id])
                redirect action: 'index', method: 'GET'
            }
            '*' { render status: NOT_FOUND }
        }
    }

    def createCorrection() {
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.debug "${requestObject.toString()}"
        Organism organism = Organism.findById(params.organism)
        log.debug "Organism: ${organism}"
        if (organism) {
            if (alternativeLociService.hasPermissions(requestObject, organism)) {
                Sequence sequence = alternativeLociService.getSequence(organism, requestObject.sequence)
                if (sequence) {
                    if (!alternativeLociService.checkForOverlappingAlternativeLoci(requestObject)) {
                        def alternativeLoci = alternativeLociService.createCorrection(requestObject, organism, sequence)
                        User user = alternativeLociService.getCurrentUser(requestObject)
                        featureService.setOwner(alternativeLoci, user)
                        String name = "${user.username.split("@").first()}_${alternativeLoci.featureLocation.sequence.name}:${alternativeLoci.fmin}-${alternativeLoci.fmax}_${alternativeLoci.type}_${alternativeLoci.dateCreated}"
                        alternativeLoci.name = name
                        alternativeLoci.save(flush: true)
                        render ([success: true] as JSON)
                    }
                    else {
                        render text: ([error: 'Cannot create overlapping Alternative Loci'] as JSON), status: 500
                    }
                }
                else {
                    render text: ([error: 'No sequence found'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'User does not have sufficient permissions'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No organism found'] as JSON), status: 500
        }
    }

    def createInversion() {
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.debug "${requestObject.toString()}"
        Organism organism = Organism.findById(params.organism)
        log.debug "Organism: ${organism}"
        if (organism) {
            if (alternativeLociService.hasPermissions(requestObject, organism)) {
                Sequence sequence = alternativeLociService.getSequence(organism, requestObject.sequence)
                if (sequence) {
                    if (!alternativeLociService.checkForOverlappingAlternativeLoci(requestObject)) {
                        def alternativeLoci = alternativeLociService.createInversion(requestObject, organism, sequence)
                        User user = alternativeLociService.getCurrentUser(requestObject)
                        featureService.setOwner(alternativeLoci, user)
                        String name = "${user.username.split("@").first()}_${alternativeLoci.featureLocation.sequence.name}:${alternativeLoci.fmin}-${alternativeLoci.fmax}_${alternativeLoci.type}_${alternativeLoci.dateCreated}"
                        alternativeLoci.name = name
                        alternativeLoci.save(flush: true)
                        render ([success: true] as JSON)
                    }
                    else {
                        render text: ([error: 'Cannot create overlapping Alternative Loci'] as JSON), status: 500
                    }
                }
                else {
                    render text: ([error: 'No sequence found'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'User does not have sufficient permissions'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No organism found'] as JSON), status: 500
        }
    }

    def createInsertion() {
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.debug "${requestObject.toString()}"
        Organism organism = Organism.findById(params.organism)
        log.debug "Organism: ${organism}"
        if (organism) {
            if (alternativeLociService.hasPermissions(requestObject, organism)) {
                Sequence sequence = alternativeLociService.getSequence(organism, requestObject.sequence)
                if (sequence) {
                    if (!alternativeLociService.checkForOverlappingAlternativeLoci(requestObject)) {
                        def alternativeLoci = alternativeLociService.createInsertion(requestObject, organism, sequence)
                        User user = alternativeLociService.getCurrentUser(requestObject)
                        featureService.setOwner(alternativeLoci, user)
                        String name = "${user.username.split("@").first()}_${alternativeLoci.featureLocation.sequence.name}:${alternativeLoci.fmin}-${alternativeLoci.fmax}_${alternativeLoci.type}_${alternativeLoci.dateCreated}"
                        alternativeLoci.name = name
                        alternativeLoci.save(flush: true)
                        render ([success: true] as JSON)
                    }
                    else {
                        render text: ([error: 'Cannot create overlapping Alternative Loci'] as JSON), status: 500
                    }
                }
                else {
                    render text: ([error: 'No sequence found'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'User does not have sufficient permissions'] as JSON), status: 500
            }

        }
        else {
            render text: ([error: 'No organism found'] as JSON), status: 500
        }
    }

    def createDeletion() {
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.debug "${requestObject.toString()}"
        Organism organism = Organism.findById(params.organism)
        log.debug "Organism: ${organism}"
        if (organism) {
            if (alternativeLociService.hasPermissions(requestObject, organism)) {
                Sequence sequence = alternativeLociService.getSequence(organism, requestObject.sequence)
                if (sequence) {
                    if (!alternativeLociService.checkForOverlappingAlternativeLoci(requestObject)) {
                        def alternativeLoci = alternativeLociService.createDeletion(requestObject, organism, sequence)
                        User user = alternativeLociService.getCurrentUser(requestObject)
                        featureService.setOwner(alternativeLoci, user)
                        String name = "${user.username.split("@").first()}_${alternativeLoci.featureLocation.sequence.name}:${alternativeLoci.fmin}-${alternativeLoci.fmax}_${alternativeLoci.type}_${alternativeLoci.dateCreated}"
                        alternativeLoci.name = name
                        alternativeLoci.save(flush: true)
                        render ([success: true] as JSON)
                    }
                    else {
                        render text: ([error: 'Cannot create overlapping Alternative Loci'] as JSON), status: 500
                    }
                }
                else {
                    render text: ([error: 'No sequence found'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'User does not have sufficient permissions'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No organism found'] as JSON), status: 500
        }
    }

    def getAlternativeLoci() {
        log.debug "${params.toString()}"
    }

    def viewFastaFile() {
        JSONObject requestObject = permissionService.handleInput(request, params)
        FastaFile ff = FastaFile.findById(requestObject.fastaFile)
        String fastaSequence = new File(ff.fileName).text
        render(text: fastaSequence, contentType: "text/plain", encoding: "UTF-8")
    }
}
