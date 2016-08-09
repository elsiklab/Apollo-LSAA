package org.elsiklab


import grails.converters.JSON
import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import htsjdk.samtools.reference.IndexedFastaSequenceFile

@Transactional(readOnly = true)
class FastaFileController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "GET"]


    def index(Integer max) {
        params.max = Math.min(max ?: 15, 100)
 
        def list = FastaFile.createCriteria().list(max: params.max, offset:params.offset) {
            if(params.sort=="username") {
                order('username', params.order)
            }
            if(params.sort=="filename") {
                order('filename', params.order)
            }
            else if(params.sort=="lastUpdated") {
                order('lastUpdated', params.order)
            }
            else if(params.sort=="dateCreated") {
                order('dateCreated', params.order)
            }
            else if(params.sort=="organism") {
                organism {
                    order('commonName',params.order)
                }
            }
        }
 
        render view: "index", model: [features: list, featureCount: list.totalCount, sort: params.sort]
    }


    def show(FastaFile fastaFile) {
        respond fastaFile
    }

    @Transactional
    def create() {
        def fastaFile
        if(params.fastaFile) {
            def f = File.createTempFile("fasta", null, new File(grailsApplication.config.lsaa.appStoreDirectory))
            f.GithWriter { out ->
                out << params.fastaFile
            }
            fastaFile = new FastaFile(filename: f.getAbsolutePath(), username: "admin", dateCreated: new Date(), lastUpdated: new Date()).save()
        }
 
        else if(params.addFile) {
            if(new File(params.addFile).exists()) {
                fastaFile = new FastaFile(filename: params.addFile, username: "admin", dateCreated: new Date(), lastUpdated: new Date()).save()
            }
            else {
                respond "Error: file does not exist", view: 'index', error: "Error"
            }
        }

        if (fastaFile.hasErrors()) {
            respond fastaFile.errors, view:'create'
            return
        }

        fastaFile.save flush:true

        redirect(action: "index")
    }

    @Transactional
    def save(FastaFile fastaFile) {
        if (fastaFile == null) {
            notFound()
            return
        }

        if (fastaFile.hasErrors()) {
            respond fastaFile.errors, view:'create'
            return
        }

        fastaFile.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'fastaFile.label', default: 'AddFasta'), fastaFile.id])
                redirect fastaFile
            }
            '*' { respond fastaFile, [status: CREATED] }
        }
    }

    def edit(FastaFile fastaFile) {
        respond fastaFile
    }

    @Transactional
    def update(FastaFile fastaFile) {
        if (fastaFile == null) {
            notFound()
            return
        }

        if (fastaFile.hasErrors()) {
            respond fastaFile.errors, view:'edit'
            return
        }

        fastaFile.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'AddFasta.label', default: 'AddFasta'), fastaFile.id])
                redirect fastaFile
            }
            '*'{ respond fastaFile, [status: OK] }
        }
    }

    @Transactional
    def delete(FastaFile fastaFile) {
        if (fastaFile == null) {
            notFound()
            return
        }

        fastaFile.delete flush:true

        redirect(action: "index")
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'fastaFile.label', default: 'AddFasta'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }


    def readFasta() {
        def file = '/Users/biocmd/Data/pyu_data/scf1117875582023.fa'
        def genome = []
        def currentSeq
        def firstSeq = false
        def filetxt = new File(file).text

        filetxt.split("\n").each { it ->
            if (it[0] == ">") {
                if (currentSeq) {
                    genome.push(currentSeq)
                    firstSeq = true
                }
                currentSeq = [
                    name: it.substring(1),
                    seq: ''
                ]
            }
            else {
                currentSeq.seq += it
            }
        }
        if(!firstSeq) {
            genome.push(currentSeq)
        }

        render genome as JSON
    }
    def readIndexedFasta() {
        def file = '/Users/biocmd/Data/pyu_data/scf1117875582023.fa'
        def indexedFasta = new IndexedFastaSequenceFile(new File(file));
        def ret = indexedFasta.getSubsequenceAt('scf1117875582023', 0, 100);
        log.debug new String(ret.getBases());
        render "Hello world!"
    }


}