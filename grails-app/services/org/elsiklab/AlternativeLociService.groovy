package org.elsiklab

import org.bbop.apollo.Feature
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.Sequence
import org.bbop.apollo.BiologicalRegion
import org.bbop.apollo.Organism

@Transactional
class AlternativeLociService {

    def grailsApplication
    def featureService

    /**
     *
     * @param jsonObject
     * @param organism
     * @param sequence
     * @return
     */
    def createCorrection(JSONObject jsonObject, Organism organism, Sequence sequence) {
        println "[DEBUG][AlternativeLociService][createCorrection] ${jsonObject.toString()}"
        String name = UUID.randomUUID()
        String type = "CORRECTION"
        String description = jsonObject.description
        String sequenceName = jsonObject.sequence
        int orientation = Integer.parseInt(jsonObject.orientation)
        int start = Integer.parseInt(jsonObject.start)
        int end = Integer.parseInt(jsonObject.end)
        String coordinateFormat = jsonObject.coordinateFormat
        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }

        println "[DEBUG][AlternaitveLociService][createCorrection] Creating FASTA file to ${grailsApplication.config.lsaa.lsaaDirectory}"
        String fastaFilePrefix = "${organism.id}-${sequenceName}-${name}"
        String fastaFileName = grailsApplication.config.lsaa.lsaaDirectory + File.separator + fastaFilePrefix + ".fa"
        def file = new File(fastaFileName)
        // TODO: a better way to write this file and move this to a method
        file << ">${name} ${type} ${sequenceName} ${organism.id}\n"
        file << jsonObject.sequence_data

        FastaFile fastaFile = new FastaFile(
                sequenceName: name, // the fasta sequence name
                fileName: file.getCanonicalPath(), // the direct file path
                originalName: fastaFilePrefix, // the file name
                //username: 'admin',
                dateCreated: new Date(),
                lastModified: new Date()
        ).save(flush: true)

        // Corrections will always default their startPosition to 0 and endPosition to len(sequence) - 1
        AlternativeLoci alternativeLoci = new AlternativeLoci(
                name: fastaFilePrefix + '-alt',
                type: type,
                uniqueName: name,
                description: description,
                startPosition: 0,
                endPosition: jsonObject.sequence_data.length() - 1,
                orientation: orientation,
                fastaFile: fastaFile
        ).save(flush: true)

        FeatureLocation featureLocation = new FeatureLocation(
                fmin: start,
                fmax: end,
                strand: 1,
                feature: alternativeLoci,
                sequence: sequence
        ).save(flush: true)
        alternativeLoci.addToFeatureLocations(featureLocation)

        return alternativeLoci
    }

    /**
     *
     * @param jsonObject
     * @param organism
     * @param sequence
     * @return
     */
    def createInversion(JSONObject jsonObject, Organism organism, Sequence sequence) {
        println "[DEBUG][AlternativeLociService][createInversion] ${jsonObject.toString()}"
        String name = UUID.randomUUID()
        String type = "INVERSION"
        String description = jsonObject.description
        String sequenceName = jsonObject.sequence
        int start = Integer.parseInt(jsonObject.start)
        int end = Integer.parseInt(jsonObject.end)
        String coordinateFormat = jsonObject.coordinateFormat
        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }

        // since an inversion is from the current genome itself, we won't be creating a fasta file
        // Instead, we will keep track of what part of the genome is involved in the inversion
        AlternativeLoci alternativeLoci = new AlternativeLoci(
                name: name,
                type: type,
                uniqueName: name,
                description: description,
                startPosition: start,
                endPosition: end,
                orientation: -1, // since the original orientation is on fwd; an inversion will always be reverse
                fastaFile: null // this is null because what we want is accessible from the genome
        ).save(flush: true)

        FeatureLocation featureLocation = new FeatureLocation(
                fmin: start,
                fmax: end,
                strand: 1, // here the orientation is fwd because the alternativeLoci representation is wrt. fwd
                feature: alternativeLoci,
                sequence: sequence
        ).save(flush: true)
        alternativeLoci.addToFeatureLocations(featureLocation)

        return alternativeLoci
    }

    /**
     *
     * @param jsonObject
     * @param organism
     * @param sequence
     * @return
     */
    def createInsertion(JSONObject jsonObject, Organism organism, Sequence sequence) {
        println "[DEBUG][AlternativeLociService][createInsertion] ${jsonObject.toString()}"
        String name = UUID.randomUUID()
        String type = "INSERTION"
        String description = jsonObject.description
        String sequenceName = jsonObject.sequence
        int orientation = Integer.parseInt(jsonObject.orientation)
        int start = Integer.parseInt(jsonObject.position)
        String coordinateFormat = jsonObject.coordinateFormat
        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }
        int end = start + 1

        println "[DEBUG][AlternaitveLociService][createInsertion] Creating FASTA file to ${grailsApplication.config.lsaa.lsaaDirectory}"
        String fastaFilePrefix = "${organism.id}-${sequenceName}-${name}"
        String fastaFileName = grailsApplication.config.lsaa.lsaaDirectory + File.separator + fastaFilePrefix + ".fa"
        def file = new File(fastaFileName)
        // TODO: a better way to write this file and move this to a method
        file << ">${name} ${type} ${sequenceName} ${organism.id}\n"
        file << jsonObject.sequence_data

        FastaFile fastaFile = new FastaFile(
                sequenceName: name, // the fasta sequence name
                fileName: file.getCanonicalPath(), // the direct file path
                originalName: fastaFilePrefix, // the file name
                //username: 'admin',
                dateCreated: new Date(),
                lastModified: new Date()
        ).save(flush: true)

        // Insertion will always default their startPosition to 0 and endPosition to len(sequence) - 1
        AlternativeLoci alternativeLoci = new AlternativeLoci(
                name: fastaFilePrefix + '-alt',
                type: type,
                uniqueName: name,
                description: description,
                startPosition: 0,
                endPosition: jsonObject.sequence_data.length() - 1,
                orientation: orientation,
                fastaFile: fastaFile
        ).save(flush: true)

        // Insertion fmin and fmax will always be next to each other (i.e. fmax - fmin will always be 1)
        FeatureLocation featureLocation = new FeatureLocation(
                fmin: start,
                fmax: end,
                strand: 1,
                feature: alternativeLoci,
                sequence: sequence
        ).save(flush: true)
        alternativeLoci.addToFeatureLocations(featureLocation)

        return alternativeLoci
    }

    /**
     *
     * @param jsonObject
     * @param organism
     * @param sequence
     * @return
     */
    def createDeletion(JSONObject jsonObject, Organism organism, Sequence sequence) {
        println "[DEBUG][AlternativeLociService][createDeletion] ${jsonObject.toString()}"
        String name = UUID.randomUUID()
        String type = "DELETION"
        String description = jsonObject.description
        String sequenceName = jsonObject.sequence
        int start = Integer.parseInt(jsonObject.start)
        int end = Integer.parseInt(jsonObject.end)
        String coordinateFormat = jsonObject.coordinateFormat
        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }

        // since a Deletion is on the current genome itself, we won't be creating a fasta file.
        // Instead, we will keep track of what part of the genome is involved in the deletion.
        AlternativeLoci alternativeLoci = new AlternativeLoci(
                name: name,
                type: type,
                uniqueName: name,
                description: description,
                startPosition: start,
                endPosition: end,
                orientation: 1,
                fastaFile: null
        ).save(flush: true)

        FeatureLocation featureLocation = new FeatureLocation(
                fmin: start,
                fmax: end,
                strand: 1, // here the orientation is fwd because the alternativeLoci representation is wrt. fwd
                feature: alternativeLoci,
                sequence: sequence
        ).save(flush: true)
        alternativeLoci.addToFeatureLocations(featureLocation)

        return alternativeLoci
    }

    def getSequence(Organism organism, String sequenceName) {
        Sequence sequence = null
        def results = Sequence.executeQuery(
                "SELECT DISTINCT s FROM Sequence s WHERE s.name =:querySequenceName AND s.organism =:queryOrganism",
                [querySequenceName: sequenceName, queryOrganism: organism])

        if (results.size() == 0) {
            //render text: ([error: 'sequence ' + sequenceName + ' not found for organism ' + organism.commonName] as JSON)
            log.error "sequence ${sequenceName} not found for organism ${organism.commonName}"
        }
        else if (results.size() == 1) {
            sequence = results.first()
        }
        else {
            log.warn "HQL query for fetching sequence returned more than one result; using the first most"
            sequence = results.first()
        }
        println "[DEBUG][AlternaitveLociService][getSequence] Sequence is: ${sequence}"
        return sequence
    }
}