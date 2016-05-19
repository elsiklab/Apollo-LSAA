package org.elsiklab


import groovy.json.JsonBuilder
import grails.converters.JSON
import grails.util.Environment
import grails.transaction.Transactional
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.Sequence
import org.bbop.apollo.OrganismProperty
import org.bbop.apollo.User
import org.apache.shiro.SecurityUtils

import static org.springframework.http.HttpStatus.*

class AlternativeLociController {

    def permissionService
    def nameService
    def featureService


    def addLoci() {

        Sequence sequence = Sequence.findByName(params.sequence)
        if(!sequence) {
            response.status = 500
            render ([error: "No sequence found"] as JSON)
            return
        }

        String name = UUID.randomUUID().toString()
        AlternativeLoci altloci = new AlternativeLoci(
            description: params.description,
            name: name,
            uniqueName: name,
            residues: params.sequencedata
        ).save(flush: true)

        FeatureLocation featureLoc = new FeatureLocation(
                fmin: params.start
                ,fmax: params.end
                ,feature: altloci
                ,sequence: sequence
        ).save(flush:true)
        altloci.addToFeatureLocations(featureLoc)

        def owner = User.findByUsername(SecurityUtils.subject.principal?:"admin")
        if (!owner && Environment.current != Environment.PRODUCTION) {
            owner = new User(username: "admin", passwordHash: "admin", firstName: "admin", lastName: "admin")
            owner.save(flush: true)
        }
        altloci.addToOwners(owner)

        if(params.sequencedata.length()) {
            new Sequence(
                name: name,
                organism: sequence.organism,
                start: 0,
                end: params.sequencedata.length(),
                length: params.sequencedata.length(),
                seqChunkSize:20000 
            ).save()

            new File(sequence.organism.directory+"/"+name+".fa").with {
                log.debug "${absolutePath}"
                write(">"+name+"\n"+params.sequencedata+"\n")
                ('prepare-refseqs.pl --fasta '+absolutePath+' --out '+sequence.organism.directory).execute()
                ('generate-names.pl --completionLimit 0 --out '+sequence.organism.directory).execute()

                new OrganismProperty(key: "blatdb", organism: sequence.organism, value: name).save()
                new OrganismProperty(key: "blatdbpath", organism: sequence.organism, value: absolutePath).save()
        
                // remake fasta index, blat db, blast db
                ('makeblastdb -dbtype nucl -in '+absolutePath + " -title "+name).execute()
            }
        }
        render ([success: "create loci success"] as JSON)
    }


    def getLoci() {
        Sequence sequence = Sequence.findByName(params.sequence)
        def features = AlternativeLoci.createCriteria().list() {
            featureLocations {
                eq('sequence',sequence)
            }
        }
        JsonBuilder json = new JsonBuilder ()
        json.features features, { result ->
            start result.featureLocation.fmin
            id result.uniqueName
            end result.featureLocation.fmax
            ref result.featureLocation.sequence.name
            description result.description
            if(result.residues) {
                href true
            }
            color "rgba(50,50,190,0.2)"
            seqName result.uniqueName
        }
        
        render json.toString()
    }



    def show(AlternativeLoci alternativeLociInstance) {
        respond alternativeLociInstance
    }

    def create() {
        respond new AlternativeLoci(params)
    }

    @Transactional
    def save(AlternativeLoci alternativeLociInstance) {
        if (alternativeLociInstance == null) {
            notFound()
            return
        }

        if (alternativeLociInstance.hasErrors()) {
            respond alternativeLociInstance.errors, view:'create'
            return
        }

        alternativeLociInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'alternativeLoci.label', default: 'AlternativeLoci'), alternativeLociInstance.id])
                redirect alternativeLociInstance
            }
            '*' { respond alternativeLociInstance, [status: CREATED] }
        }
    }

    def edit(AlternativeLoci alternativeLociInstance) {
        render view: "edit", model: [alternativeLociInstance: alternativeLociInstance]
    }

    @Transactional
    def update(AlternativeLoci alternativeLociInstance) {
        if (alternativeLociInstance == null) {
            notFound()
            return
        }

        if (alternativeLociInstance.hasErrors()) {
            respond alternativeLociInstance.errors, view:'edit'
            return
        }

        alternativeLociInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'AlternativeLoci.label', default: 'AlternativeLoci'), alternativeLociInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ respond alternativeLociInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(AlternativeLoci alternativeLociInstance) {

        if (alternativeLociInstance == null) {
            notFound()
            return
        }

        alternativeLociInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'AlternativeLoci.label', default: 'AlternativeLoci'), alternativeLociInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'availableStatus.label', default: 'AlternativeLoci'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }




    def index(Integer max) {
        params.max = Math.min(max ?: 15, 100)

        def c = AlternativeLoci.createCriteria()

        def list = c.list(max: params.max, offset:params.offset) {
            
            if(params.sort=="owners") {
                owners {
                    order('username', params.order)
                }
            }
            if(params.sort=="sequencename") {
                featureLocations {
                    sequence {
                        order('name', params.order)
                    }
                }
            }
            else if(params.sort=="name") {
                order('name', params.order)
            }
            else if(params.sort=="cvTerm") {
                order('class', params.order)
            }
            else if(params.sort=="organism") {
                featureLocations {
                    sequence {
                        organism {
                            order('commonName',params.order)
                        }
                    }
                }
            }
            else if(params.sort=="lastUpdated") {
                order('lastUpdated',params.order)
            }

            if(params.ownerName!=null&&params.ownerName!="") {
                owners {
                    ilike('username', '%'+params.ownerName+'%')
                }
            }
            if(params.organismName!= null&&params.organismName != "") {
                featureLocations {
                    sequence {
                        organism {
                            ilike('commonName','%'+params.organismName+'%')
                        }
                    }
                }
            }
        }

        list.each {
            log.debug it.owner
        }

        render view: "index", model: [features: list, sort: params.sort, alternativeLociInstanceCount: list.totalCount]
    }
}
