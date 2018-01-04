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

    def fastaFileService

    public static final String REFERENCE = "reference"

    /**
     * Given an organism, generate a JSON representation of an alternate version of sequences that
     * have been annotated with Alternative Loci
     * @param organism
     */
    def getTransformationAsJson(Organism organism) {
        log.debug "organism ${organism.commonName}"
        def altLociList = getAltLoci(organism)
        log.debug "altLoci list size for organism: ${altLociList.size()}"
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = new JSONObject()

        altLociList.each { altLoci ->
            // get altLoci feature location sequence name
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

        for (String sequenceName: sequenceToAltLociMap.keySet()) {
            Sequence sequence = Sequence.findByName(sequenceName)
            def altLociForSequenceList = sequenceToAltLociMap[sequenceName]
            // sort the altLoci based on their fmin
            altLociForSequenceList.sort {a,b -> a.featureLocation.fmin <=> b.featureLocation.fmin}
            JSONArray transformedJsonArray = getTransformationAsJson(sequence, altLociForSequenceList)
            returnObject.put(sequenceName, transformedJsonArray)
        }

        return returnObject
    }

    /**
     * Given an organism, generate a JSON representation of an alternate version of sequences that
     * have been annotated with Alternative Loci from a given breed
     * @param organism
     * @param breed
     * @return
     */
    def getTransformationAsJson(Organism organism, Breed breed) {
        log.debug "[ExportDataService][getTransformationAsJson] organism: ${organism.commonName} breed: ${breed}"
        def altLociList = breed.alternativeLoci
        log.debug "[ExportDataService][getTransformationAsJson] altLoci list size for organism: ${altLociList.size()}"
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = new JSONObject()

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

        for (String sequenceName: sequenceToAltLociMap.keySet()) {
            Sequence sequence = Sequence.findByName(sequenceName)
            def altLociForSequenceList = sequenceToAltLociMap[sequenceName]
            // sort the altLoci based on their fmin
            altLociForSequenceList.sort {a,b -> a.featureLocation.fmin <=> b.featureLocation.fmin}
            JSONArray transformedJsonArray = getTransformationAsJson(sequence, altLociForSequenceList)
            returnObject.put(sequenceName, transformedJsonArray)
        }

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
    def getTransformationAsJson(Organism organism, Breed breed, ArrayList<AlternativeLoci> altLociList) {
        log.debug "[ExportDataService][getTransformationAsJson] organism: ${organism.commonName} breed: ${breed} altLociList: ${altLociList?.size()}"
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = new JSONObject()

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

        for (String sequenceName: sequenceToAltLociMap.keySet()) {
            Sequence sequence = Sequence.findByName(sequenceName)
            def altLociForSequenceList = sequenceToAltLociMap[sequenceName]
            // sort the altLoci based on their fmin
            altLociForSequenceList.sort {a,b -> a.featureLocation.fmin <=> b.featureLocation.fmin}
            JSONArray transformedJsonArray = getTransformationAsJson(sequence, altLociForSequenceList)
            returnObject.put(sequenceName, transformedJsonArray)
        }

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
        long start = System.currentTimeMillis()

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
        transformationArray.put(finalSequenceJsonObject)

        long end = System.currentTimeMillis()
        log.debug "time taken to generate transformationArray: ${end - start} ms"

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
        }
    }

    /**
     * Given an organism, generate a FASTA representation of an alternate version of the sequence by
     * incorporating all alternative loci
     * @param organism
     */
    def getTransformationAsFasta(Organism organism) {
        log.debug "organism ${organism.commonName}"
        def altLociList = getAltLoci(organism)
        log.debug "altLoci list size for organism: ${altLociList.size()}"
        def sequenceToAltLociMap = [:]
        def transformedFastaMap = [:]

        altLociList.each { altLoci ->
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
            String fastaSequence = ""
            for (JSONObject jsonObject : transformedJsonArray) {
                println "jsonObject: ${jsonObject.toString()}"
                if (jsonObject.type == REFERENCE) {
                    fastaSequence += '<START_OF_REF>'
                    fastaSequence += fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.name, jsonObject.fmin, jsonObject.fmax)
                    fastaSequence += '<END_OF_REF>'
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_INVERSION.toLowerCase()) {
                    fastaSequence += '<START_INV>'
                    fastaSequence += fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.fmin, jsonObject.fmax, true)
                    fastaSequence += '<END_INV>'
                    descriptionArray.add("${jsonObject.name} (${jsonObject.type})")
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_DELETION.toLowerCase()) {
                    fastaSequence += "<DEL>"
                    descriptionArray.add("${jsonObject.name} (${jsonObject.type})")
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_INSERTION.toLowerCase()) {
                    fastaSequence += "<START_INS>"
                    fastaSequence += jsonObject.orientation == -1 ? fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1, true) : fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1)
                    fastaSequence += "<END_INS>"
                    descriptionArray.add("${jsonObject.name} (${jsonObject.type})")
                }
                else if (jsonObject.type == AlternativeLociService.TYPE_CORRECTION.toLowerCase()) {
                    fastaSequence += '<START_CORRECTION>'
                    fastaSequence += jsonObject.orientation == -1 ? fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1, true) : fastaFileService.readIndexedFastaRegion(jsonObject.source, jsonObject.source_sequence, jsonObject.source_start, jsonObject.source_end + 1)
                    fastaSequence += '<END_CORRECTION>'
                    descriptionArray.add("${jsonObject.name} (${jsonObject.type})")
                }
            }

            // TODO: instead of storing in map, write to a file to have a smaller memory footprint and provide file for download
            // Expire the file in 120 seconds to clear disk storage

            long end = System.currentTimeMillis()
            log.debug "time taken to generate transformationArray: ${end - start} ms"
            transformedFastaMap[sequenceName] = [name: name, description: descriptionArray, sequence: fastaSequence]
        }

        return transformedFastaMap
    }

}
