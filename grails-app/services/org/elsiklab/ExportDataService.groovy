package org.elsiklab

import grails.transaction.Transactional
import grails.web.JSONBuilder
import groovy.json.JsonBuilder
import org.bbop.apollo.Organism
import org.bbop.apollo.Sequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.ho.yaml.Yaml
import org.ho.yaml.exception.YamlException

@Transactional
class ExportDataService {

    def grailsApplication
    def fastaFileService

    public static final String REFERENCE = "reference"

    /**
     * Given an organism, generate a JSON representation of an alternate version of sequences that
     * have been annotated with Alternative Loci
     * @param organism
     */
    def getTransformationAsJson(Organism organism, boolean exportEntireGenome = false) {
        log.debug "organism ${organism.commonName}"
        def altLociList = getAltLoci(organism)
        log.debug "altLoci list size for organism: ${altLociList.size()}"
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = buildJson(organism, altLociList, exportEntireGenome)
        return returnObject
    }

    /**
     * Given an organism, generate a JSON representation of an alternate version of sequences that
     * have been annotated with Alternative Loci from a given breed
     * @param organism
     * @param breed
     * @return
     */
    def getTransformationAsJson(Organism organism, Breed breed, boolean exportEntireGenome = false) {
        log.debug "[ExportDataService][getTransformationAsJson] organism: ${organism.commonName} breed: ${breed}"
        def altLociList = breed.alternativeLoci
        log.debug "[ExportDataService][getTransformationAsJson] altLoci list size for organism: ${altLociList.size()}"
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = buildJson(organism, altLociList, exportEntireGenome)
        return returnObject
    }

//    def getTransformationAsJson(Organism organism, Breed breed, ArrayList<Sequence> sequences) {
//        log.debug "[ExportDataService][getTransformationAsJson] organism: ${organism.commonName} breed: ${breed} sequences: ${sequences?.size()}"
//        def altLociList = breed.alternativeLoci
//        def filteredAltLociList = []
//        altLociList.each { altLoci ->
//            if (altLoci.featureLocation.sequence in sequences) {
//                filteredAltLociList.add(altLoci)
//            }
//        }
//
//        def sequenceToAltLociMap = [:]
//        JSONObject returnObject = new JSONObject()
//
//        altLociList.each { altLoci ->
//            // get altLoci feature location sequence name
//            String sequenceName = altLoci.featureLocation.sequence.name
//            if (!sequenceToAltLociMap.get(sequenceName)) {
//                // if sequenceName doesn't exist, then create an array of altLoci for that sequence name
//                sequenceToAltLociMap[sequenceName] = [altLoci]
//            }
//            else {
//                // append to the array of altLoci
//                sequenceToAltLociMap[sequenceName] += [altLoci]
//            }
//        }
//
//        for (String sequenceName: sequenceToAltLociMap.keySet()) {
//            Sequence sequence = Sequence.findByName(sequenceName)
//            def altLociForSequenceList = sequenceToAltLociMap[sequenceName]
//            // sort the altLoci based on their fmin
//            altLociForSequenceList.sort {a,b -> a.featureLocation.fmin <=> b.featureLocation.fmin}
//            JSONArray transformedJsonArray = getTransformationAsJson(sequence, altLociForSequenceList)
//            returnObject.put(sequenceName, transformedJsonArray)
//        }
//
//        return returnObject
//    }

    /**
     * Given an organism, generate a JSON representation of an alternate version of sequences that
     * have been annotated with a given list of Alternative Loci from a given breed
     * @param organism
     * @param breed
     * @param altLociList
     * @return
     */
    def getTransformationAsJson(Organism organism, Breed breed, ArrayList<AlternativeLoci> altLociList, boolean exportEntireGenome = false) {
        log.debug "[ExportDataService][getTransformationAsJson] organism: ${organism.commonName} breed: ${breed} altLociList: ${altLociList?.size()}"
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = buildJson(organism, altLociList, exportEntireGenome)
        return returnObject
    }

    /**
     *
     * @param organism
     * @param altLociList
     * @param exportEntireGenome
     */
    def buildJson(Organism organism, def altLociList, boolean exportEntireGenome = false) {
        def returnObject = new JSONObject()
        def sequenceToAltLociMap = [:]

        if (exportEntireGenome) {
            organism.refresh()
            organism.sequences.each { sequence ->
                sequenceToAltLociMap[sequence.name] = []
            }
        }

        altLociList.each { altLoci ->
            // get altLoci feature location sequence name
            altLoci.refresh()
            String sequenceName = altLoci.featureLocation.sequence.name
            if (!sequenceToAltLociMap.get(sequenceName)) {
                // if sequenceName doesn't exist, then create an array of altLoci for that sequence name
                sequenceToAltLociMap[sequenceName] = [altLoci]
            }
            else {
                // append to the array of altLoci
                sequenceToAltLociMap[sequenceName] += [altLoci]
            }
        }

        long start = System.currentTimeMillis()

        for (String sequenceName: sequenceToAltLociMap.keySet()) {
            Sequence sequence = Sequence.findByName(sequenceName)
            def altLociForSequenceList = sequenceToAltLociMap[sequenceName]
            // sort the altLoci based on their fmin
            altLociForSequenceList.sort {a,b -> a.featureLocation.fmin <=> b.featureLocation.fmin}
            JSONArray transformedJsonArray = getTransformationAsJson(sequence, altLociForSequenceList)
            returnObject.put(sequenceName, transformedJsonArray)
        }

        long end = System.currentTimeMillis()
        log.debug "time taken to generate transformationArray: ${end - start} ms"

        return returnObject
    }

    /**
     * Given a sequence and a list of alternative loci, generate a JSON representation of an alternate version
     * of the sequence by incorporating all given alternative loci
     *
     * @param sequence
     * @param altLociList
     * @return
     */
    def getTransformationAsJson(Sequence sequence, def altLociList) {
        log.debug "generate transformation JSON for ${sequence.name} and ${altLociList.size()} loci"
        JSONArray transformationArray = new JSONArray()
        def previousAltLoci
        int previousAltLociFmax
        String genomeFasta = sequence.organism.directory + File.separator + sequence.organism.genomeFasta

        if (altLociList.size() == 0) {
            JSONObject sequenceJsonObject = new JSONObject()
            sequenceJsonObject.put("name", sequence.name)
            sequenceJsonObject.put("description", "reference genome")
            sequenceJsonObject.put("type", REFERENCE)
            sequenceJsonObject.put("source", genomeFasta)
            sequenceJsonObject.put("source_sequence", sequence.name)
            sequenceJsonObject.put("fmin", sequence.start)
            sequenceJsonObject.put("fmax", sequence.end)
            sequenceJsonObject.put("strand", 1)
            transformationArray.add(sequenceJsonObject)
        }
        else {
            for (altLoci in altLociList) {
                JSONObject sequenceJsonObject = new JSONObject()
                sequenceJsonObject.put("name", sequence.name)
                sequenceJsonObject.put("description", "reference genome")
                sequenceJsonObject.put("type", REFERENCE)
                sequenceJsonObject.put("source", genomeFasta)
                sequenceJsonObject.put("source_sequence", sequence.name)
                if (previousAltLociFmax) {
                    sequenceJsonObject.put("fmin", previousAltLociFmax)
                }
                else {
                    sequenceJsonObject.put("fmin", sequence.start)
                }
                sequenceJsonObject.put("fmax", (altLoci.featureLocation.fmin))
                sequenceJsonObject.put("strand", 1)
                transformationArray.add(sequenceJsonObject)

                JSONObject altLociJsonObject = new JSONObject()
                altLociJsonObject.put("name", altLoci.uniqueName)

                if (altLoci.type == AlternativeLociService.TYPE_INVERSION || altLoci.type == AlternativeLociService.TYPE_DELETION) {
                    altLociJsonObject.put("source_sequence", sequence.name)
                }
                else {
                    altLociJsonObject.put("source_sequence", altLoci.uniqueName)
                }

                altLociJsonObject.put("source_start", altLoci.startPosition)
                altLociJsonObject.put("source_end", altLoci.endPosition)
                altLociJsonObject.put("description", altLoci.description)
                if (altLoci.breed) altLociJsonObject.put("breed", altLoci.breed.nameAndIdentifier)

                if (altLoci.type == AlternativeLociService.TYPE_INVERSION || altLoci.type == AlternativeLociService.TYPE_DELETION) {
                    altLociJsonObject.put("source", genomeFasta)
                }
                else {
                    altLociJsonObject.put("source", altLoci.fastaFile.fileName)
                }
                altLociJsonObject.put("type", altLoci.type.toLowerCase())
                altLociJsonObject.put("fmin", altLoci.featureLocation.fmin)
                altLociJsonObject.put("fmax", altLoci.featureLocation.fmax)
                altLociJsonObject.put("strand", altLoci.featureLocation.strand)
                altLociJsonObject.put("orientation", altLoci.orientation)
                transformationArray.add(altLociJsonObject)
                previousAltLociFmax = altLoci.featureLocation.fmax
                previousAltLoci = altLoci
            }

            JSONObject finalSequenceJsonObject = new JSONObject()
            finalSequenceJsonObject.put("name", sequence.name)
            finalSequenceJsonObject.put("description", "reference genome")
            finalSequenceJsonObject.put("type", REFERENCE)
            finalSequenceJsonObject.put("source", genomeFasta)
            finalSequenceJsonObject.put("source_sequence", sequence.name)
            finalSequenceJsonObject.put("fmin", previousAltLociFmax)
            finalSequenceJsonObject.put("fmax", sequence.end)
            finalSequenceJsonObject.put("strand", 1)
            transformationArray.put(finalSequenceJsonObject)
        }

        return transformationArray
    }

    /**
     * Given an organism, return all of its alternative loci
     * @param organism
     * @return
     */
    def getAltLoci(Organism organism) {
        return AlternativeLoci.createCriteria().list {
            featureLocations {
                order('fmin', 'ascending')
                sequence {
                    eq('organism', organism)
                }
            }
            isNull("breed")
        }
    }

    /**
     * Given an organism, generate a FASTA representation of an alternate version of sequences that
     * have been annotated with Alternative Loci
     * @param organism
     */
    def getTransformationAsFasta(Organism organism, boolean exportEntireGenome = false) {
        log.debug "organism ${organism.commonName}"
        def altLociList = getAltLoci(organism)
        log.debug "altLoci list size for organism: ${altLociList.size()}"
        def transformedFastaMap = [:]
        File fastaFile = buildSequence(organism, altLociList, exportEntireGenome)
        transformedFastaMap.put(organism.id, fastaFile.getCanonicalPath())
        return transformedFastaMap
    }

    /**
     * Given an organism, generate a FASTA representation of an alternate version of sequences that
     * have been annotated with Alternative Loci from a given breed
     * @param organism
     * @param breed
     * @return
     */
    def getTransformationAsFasta(Organism organism, Breed breed, boolean exportEntireGenome = false) {
        log.debug "organism ${organism.commonName} breed: ${breed}"
        def altLociList = breed.alternativeLoci
        log.debug "altLoci list size for organism: ${altLociList.size()}"
        def transformedFastaMap = [:]
        def fastaFile = buildSequence(organism, altLociList, exportEntireGenome)
        transformedFastaMap.put(organism.id, fastaFile.getCanonicalPath())
        return transformedFastaMap
    }

    /**
     * Given an organism, generate a FASTA representation of an alternate version of sequences that
     * have been annotated with a given list of Alternative Loci from a given breed
     * @param organism
     */
    def getTransformationAsFasta(Organism organism, Breed breed, ArrayList<AlternativeLoci> altLociList, boolean exportEntireGenome = false) {
        log.debug "organism ${organism.commonName} breed: ${breed} altLociList: ${altLociList.size()}"
        log.debug "${grailsApplication.config.lsaa.exportDirectory} + ${File.separator} + ${UUID.randomUUID()} + ${FastaFileService.FA_SUFFIX}"
        def transformedFastaMap = [:]
        def fastaFile = buildSequence(organism, altLociList, exportEntireGenome)
        transformedFastaMap.put(organism.id, fastaFile.getCanonicalPath())
        return transformedFastaMap
    }

    /**
     * Given a list of Alternative Loci, create a new sequence
     * @param altLociList
     * @return
     */

     
    def buildSequence(Organism organism, def altLociList, boolean exportEntireGenome = false) {
        log.debug "${grailsApplication.config.lsaa.exportDirectory} + ${File.separator} + ${UUID.randomUUID()} + ${FastaFileService.FA_SUFFIX}"
        String fileName = grailsApplication.config.lsaa.exportDirectory.toString() + File.separator.toString() + UUID.randomUUID().toString() + FastaFileService.FA_SUFFIX.toString()
        def fastaFile = new File(fileName)
        def sequenceToAltLociMap = [:]
        def timer = new Timer()
        def task = timer.runAfter(120 * 1000) {
            log.debug"removing ${fastaFile.getCanonicalPath()}"
            fastaFile.delete()
        }

        if (exportEntireGenome) {
            organism.refresh()
            organism.sequences.each { sequence ->
                sequenceToAltLociMap[sequence.name] = []
            }
        }

        altLociList.each { altLoci ->
            println ">>>> ${altLoci.name}"
            altLoci.refresh()
            println ">>>> ${altLoci.name}"
            String sequenceName = altLoci.featureLocation.sequence.name
            if (!sequenceToAltLociMap.get(sequenceName)) {
                sequenceToAltLociMap[sequenceName] = [altLoci]
            }
            else {
                sequenceToAltLociMap[sequenceName] += [altLoci]
            }
        }

        for (String sequenceName: sequenceToAltLociMap.keySet()) {
            Sequence sequence = Sequence.findByName(sequenceName)
            def altLociForSequenceList = sequenceToAltLociMap[sequenceName]
            // sort the altLoci based on their fmin
            altLociForSequenceList.sort {a,b -> a.featureLocation.fmin <=> b.featureLocation.fmin}
            JSONArray transformedJsonArray = getTransformationAsJson(sequence, altLociForSequenceList)
            long start = System.currentTimeMillis()
            String name = "${sequenceName}-${UUID.randomUUID().toString()}"
            JSONArray descriptionArray = new JSONArray()
            transformedJsonArray.each {
                if (it.type != REFERENCE) descriptionArray.add("${it.name} (${it.type}) ${it.breed}")
            }

            fastaFile << ">${name} ${descriptionArray.join(",")}\n"
            for (JSONObject jsonObject : transformedJsonArray) {
                println "jsonObject: ${jsonObject.toString()}"
                if (jsonObject.type == REFERENCE) {
                    //fastaFile << "<START_OF_REF>"
                    fastaFile << fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.name, jsonObject.fmin, jsonObject.fmax)
                    //fastaFile <<  "<END_OF_REF>"
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_INVERSION.toLowerCase()) {
                    //fastaFile << "<START_INV>"
                    fastaFile << fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.fmin, jsonObject.fmax, true)
                    //fastaFile << "<END_INV>"
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_DELETION.toLowerCase()) {
                    //fastaFile << "<DEL>"
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_INSERTION.toLowerCase()) {
                    //fastaFile << "<START_INS>"
                    jsonObject.orientation == -1 ? fastaFile << fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1, true) : fastaFile << fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1)
                    //fastaFile << "<END_INS>"
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_CORRECTION.toLowerCase()) {
                    //fastaFile << "<START_CORRECTION>"
                    jsonObject.orientation == -1 ? fastaFile << fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1, true) : fastaFile << fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1)
                    //fastaFile << "<END_CORRECTION>"
                }
            }
            fastaFile << "\n"

            long end = System.currentTimeMillis()
            log.debug "time taken to generate an alternate version of sequence: ${end - start} ms"
        }

        return fastaFile
    }

}
