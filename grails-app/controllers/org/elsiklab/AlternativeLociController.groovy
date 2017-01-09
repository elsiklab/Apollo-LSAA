package org.elsiklab

import org.bbop.apollo.Feature
import org.codehaus.groovy.grails.web.json.JSONObject

import java.sql.Timestamp

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
    def nameService
    def featureService
    def fastaFileService

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

    def create() {
        respond new AlternativeLoci(params)
    }

    @Transactional
    def save() {
        def sequence = Sequence.findById(params.name)
        if(sequence) {
            def fastaFile = FastaFile.findById(params.fasta_file)
            if(fastaFile) {
                def file = new File(fastaFile.filename)
                if(file.isFile()) {
                    String name = UUID.randomUUID()
                    AlternativeLoci alternativeLociInstance = new AlternativeLoci(
                            description: params.description,
                            name: name,
                            uniqueName: name,
                            start_file: params.start_file == '' ? 0 : params.start_file,
                            end_file: params.end_file == '' ? file.length() : params.end_file,
                            name_file: params.name_file,
                            fasta_file: fastaFile,
                            reversed: params.reversed
                    ).save(flush: true, failOnError: true)

                    FeatureLocation featureLoc = new FeatureLocation(
                            fmin: params.start,
                            fmax: params.end,
                            feature: alternativeLociInstance,
                            sequence: sequence
                    ).save(flush:true, failOnError: true)
                    alternativeLociInstance.addToFeatureLocations(featureLoc)

                    redirect(action: 'index')
                }
                else {
                    render text: ([error: 'FASTA file path was moved'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'No FASTA file found'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No sequence found'] as JSON), status: 500
        }
    }

    def edit(AlternativeLoci alternativeLociInstance) {
        render view: 'edit', model: [alternativeLociInstance: alternativeLociInstance]
    }

    @Transactional
    def update(AlternativeLoci instance) {
        def sequence = Sequence.findById(params.name)
        if(sequence) {
            def fastaFile = FastaFile.findById(params.fasta_file)
            if(fastaFile) {
                def file = new File(fastaFile.filename)
                if(file) {
                    instance.description = params.description
                    instance.start_file = Integer.parseInt(params.start_file) ?: 0
                    instance.end_file = Integer.parseInt(params.end_file) ?: file.length()
                    instance.name_file = params.name_file
                    instance.fasta_file = fastaFile
                    instance.featureLocation.fmin = Integer.parseInt(params.start)
                    instance.featureLocation.fmax = Integer.parseInt(params.end)
                    instance.featureLocation.sequence = sequence
                    // gives error on save currently
                    instance.save(flush: true, failOnError: true)
                    redirect(action: 'edit')
                }
                else {
                    render text: ([error: 'FASTA file path was moved'] as JSON), status: 500
                }
            }
            else {
                render text: ([error: 'No FASTA file found'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No sequence found'] as JSON), status: 500
        }
    }

    @Transactional
    def delete(AlternativeLoci alternativeLociInstance) {
        if (alternativeLociInstance == null) {
            notFound()
            return
        }

        if (!alternativeLociInstance.reversed) alternativeLociInstance.fasta_file.delete()
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

    def index(Integer max) {
        params.max = Math.min(max ?: 15, 100)

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
        println list.toString()
        render view: 'index', model: [features: list, sort: params.sort, alternativeLociInstanceCount: list.totalCount]
    }
    def createReversal() {
        println "@createReversal: ${params.toString()}"
        String name = UUID.randomUUID()
        String type = "REVERSAL"
        String description = params.description
        String sequenceName = params.sequence
        // TODO: this is a crude way of dealing with username
        String username = params.username
        User user = User.findByUsername(username)

        Organism organism = Organism.findById(params.organism)
        int start = Integer.parseInt(params.start)
        int end = Integer.parseInt(params.end)
        String coordinateFormat = params.coordinateFormat

        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }

        println "Organism is: ${organism}"
        if (organism) {
            // TODO: HQL - check for more than one
            Sequence seq = null
            def results = Sequence.executeQuery(
                    "SELECT DISTINCT s FROM Sequence s WHERE s.name =:querySequenceName AND s.organism =:queryOrganism",
                    [querySequenceName: sequenceName, queryOrganism: organism])

            if (results.size() == 0) {
                render text: ([error: 'sequence ' + sequenceName + ' not found for organism ' + organism.commonName] as JSON)
            }
            else if (results.size() == 1) {
                seq = results.iterator().next()
            }
            else {
                log.warn "HQL for fetching sequence returned more than one result; using the first most"
                seq = results.iterator().next()
            }
            print "Sequence is : ${seq}"

            if (seq) {
                // expects a FastaFile representation for the current organism which is impossible since an
                // organism consists of multiple fasta; so either we pre-prepare fasta files with
                // organismName + sequenceName as file name OR something else

                //FastaFile fastaFile = FastaFile.findByOrganism(organism) // right now this will always return null
                // TODO: HQL - check for more than one
                println "Checking if FastaFile representation of ${sequenceName} exists"
                def fastaFileQueryResults = FastaFile.executeQuery(
                        "SELECT DISTINCT f FROM FastaFile f WHERE f.originalname =:querySequenceName AND f.organism =:queryOrganism",
                        [querySequenceName: sequenceName, queryOrganism: organism])

                FastaFile fastaFile
                if (fastaFileQueryResults.size() > 0) {
                    println "yes it does exist"
                    fastaFile = fastaFileQueryResults.iterator().next()
                }

                if (fastaFile) {
                    println "[ A ] FastaFile already exists with ${sequenceName} for organism ${organism.commonName}"
                    println "[ A ] creating altLoci and its featureLocation"
                    AlternativeLoci altLoci = new AlternativeLoci(
                            name: params.sequence,
                            uniqueName: name,
                            description: params.description,
                            reversed: true,
                            start_file: start,
                            end_file: end,
                            orientation: "REVERSE",
                            fasta_file: fastaFile,
                            name_file: seq.name
                    ).save(flush: true, failOnError: true)

                    featureService.setOwner(altLoci, user)

                    FeatureLocation featureLoc = new FeatureLocation(
                            fmin: start,
                            fmax: end,
                            strand: 1,
                            feature: altLoci,
                            sequence: seq
                    ).save(flush: true, failOnError: true)

                    altLoci.addToFeatureLocations(featureLoc)

                    render ([success: true] as JSON)
                }
                else {
                    // read existing split genome fasta
                    println organism.directory
                    println "[ B ] expecting split FASTA to be in ${organism.directory}/fasta/"
                    String path = organism.directory + "/fasta/" + organism.commonName + '-' + sequenceName + ".fa"

                    FastaFile newFastaFile
                    File file = new File(path)

                    if (file.exists() && !file.isDirectory()) {
                        println "[ B1 ] FASTA representation of ${sequenceName} already exists"
                    }
                    else {
                        println "[ B2 ] FASTA representation of ${sequenceName} does not exist in directory"
                        String fileName = organism.commonName + '-' + sequenceName + ".fa"
                        file = new File(${grailsApplication.config.lsaa.appStoreDirectory} + "/" + sequenceName)
                        String genomeFile = organism.blatdb.replace(".2bit", ".fa")
                        file.withWriter { temp ->
                            temp << ">${sequenceName}\n"
                            temp << fastaFileService.readIndexedFasta(genomeFile, sequenceName)
                        }
                    }

                    println "[ B ] creating FastaFile for ${file.getAbsolutePath()}"
                    newFastaFile = new FastaFile(
                            filename: file.getAbsolutePath(),
                            username: 'admin',
                            dateCreated: new Date(),
                            lastUpdated: new Date(),
                            originalname: sequenceName,
                            organism: organism
                    ).save(flush: true)

                    // Now create the AltLoci for reversal
                    println "[ B ] creating altLoci and its featureLocation"
                    AlternativeLoci altLoci = new AlternativeLoci(
                            name: sequenceName,
                            uniqueName: name,
                            description: description,
                            reversed: true,
                            start_file: start,
                            end_file: end,
                            orientation: "REVERSE",
                            fasta_file: newFastaFile,
                            name_file: newFastaFile.filename
                    ).save(flush: true, failOnError: true)

                    FeatureLocation featureLoc = new FeatureLocation(
                            fmin: start,
                            fmax: end,
                            strand: 1,
                            feature: altLoci,
                            sequence: seq
                    ).save(flush: true, failOnError: true)

                    altLoci.addToFeatureLocations(featureLoc)

                    render ([success: true] as JSON)
                }
            }
            else {
                render text: ([error: 'No sequence found'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No organism found'] as JSON), status: 500
        }
    }

    def createCorrection() {
        println "@createCorrection: ${params.toString()}"
        String name = UUID.randomUUID()
        String type = "CORRECTION"
        String description = params.description
        String sequenceName = params.sequence
        String orientation = params.orientation
        // TODO: this is a crude way of dealing with username
        String username = params.username
        User user = User.findByUsername(username)

        Organism organism = Organism.findById(params.organism)
        int start = Integer.parseInt(params.start)
        int end = Integer.parseInt(params.end)
        String coordinateFormat = params.coordinateFormat

        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }

        println "Organism: ${organism}"
        println "Seq Length: ${params.sequencedata.length()}"

        if (organism) {
            Sequence seq = null
            def results = Sequence.executeQuery(
                    "SELECT DISTINCT s FROM Sequence s WHERE s.name =:querySequenceName AND s.organism =:queryOrganism",
                    [querySequenceName: sequenceName, queryOrganism: organism])

            if (results.size() == 0) {
                render text: ([error: 'sequence ' + sequenceName + ' not found for organism ' + organism.commonName] as JSON)
            }
            else if (results.size() == 1) {
                seq = results.iterator().next()
            }
            else {
                log.warn "HQL for fetching sequence returned more than one result; using the first most"
                seq = results.iterator().next()
            }
            println "Sequence is: ${seq}"


            if (seq) {
                println "Creating FASTA file to ${grailsApplication.config.lsaa.appStoreDirectory}"
                String filePrefix = organism.commonName + '-' + sequenceName + '-' + name
                def file = new File(${grailsApplication.config.lsaa.appStoreDirectory} + "/" + filePrefix + ".fa")
                file << ">${name} ${type} ${sequenceName} ${organism.commonName}\n"
                file << params.sequencedata

                fastaFileService.generateFastaIndex(file.absolutePath)

                FastaFile fastaFile = new FastaFile(
                        filename: file.getAbsolutePath(),
                        originalname: filePrefix,
                        username: 'admin',
                        dateCreated: new Date(),
                        lastModified: new Date(),

                ).save(flush: true)

                // Corrections will always default their start_file and end_file to 0 and len(sequence) - 1, respectively
                AlternativeLoci altLoci = new AlternativeLoci(
                        name: filePrefix + '-alt',
                        uniqueName: name,
                        description: description,
                        start_file: 0,
                        end_file: params.sequencedata.length() - 1,
                        orientation: orientation,
                        name_file: fastaFile.filename,
                        fasta_file: fastaFile,
                ).save(flush: true)

                featureService.setOwner(altLoci, user)

                FeatureLocation featureLoc = new FeatureLocation(
                        fmin: start,
                        fmax: end,
                        strand: 1,
                        feature: altLoci,
                        sequence: seq
                ).save(flush: true)

                altLoci.addToFeatureLocations(featureLoc)

                render ([success: true] as JSON)
            }
            else {
                render text: ([error: 'No sequence found'] as JSON), status: 500
            }
        }
        else {
            render text: ([error: 'No organism found'] as JSON), status: 500
        }
    }

    def getAlternativeLoci() {
        println "@getAlternativeLoci: ${params.toString()}"
    }
}
