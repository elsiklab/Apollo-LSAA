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
    def sequenceService

    def serviceMethod() {

    }

    /**
     *
     * @param orgaism
     */
    def getTransformationAsJSON(Organism organism) {
        log.debug "organism ${organism.name}"
        def altLociList = getAltLoci(organism)
        def sequenceToAltLociMap = [:]
        JSONObject returnObject = new JSONObject()

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
            JSONArray transformedJsonArray = getTransformationAsJSON(sequence, altLociForSequenceList).get("transformation")
            returnObject.put(sequenceName, transformedJsonArray)
        }

        return returnObject
    }

    /**
     *
     *
     * @param sequence
     * @param altLociList
     * @return
     */
    def getTransformationAsJSON(Sequence sequence, def altLociList) {

        JSONObject returnObject = new JSONObject()
        JSONArray array = new JSONArray()
        int previousAltLociFmax

        for (altLoci in altLociList) {
            JSONObject sequenceJsonObject = new JSONObject()
            sequenceJsonObject.put("name", sequence.name)
            sequenceJsonObject.put("description", "reference genome")
            sequenceJsonObject.put("type", "reference")
            sequenceJsonObject.put("source", sequence.organism.blatdb.replace(".2bit", ".fa"))
            sequenceJsonObject.put("source_contig", sequence.name)
            if (previousAltLociFmax) {
                sequenceJsonObject.put("fmin", previousAltLociFmax + 1)
            }
            else {
                sequenceJsonObject.put("fmin", sequence.start)
            }
            sequenceJsonObject.put("fmax", (altLoci.featureLocation.fmin - 1))
            sequenceJsonObject.put("strand", 1)
            array.add(sequenceJsonObject)

            JSONObject altLociJsonObject = new JSONObject()
            altLociJsonObject.put("name", altLoci.uniqueName)
            if (altLoci.reversed) {
                altLociJsonObject.put("source_contig", sequence.name)
            }
            else {
                altLociJsonObject.put("source_contig", altLoci.uniqueName)
            }
            altLociJsonObject.put("description", altLoci.description)
            if (altLoci.reversed) {
                // TODO: add a proper attribute to organism to hold the actual genome fasta name
                altLociJsonObject.put("source", sequence.organism.blatdb.replace(".2bit", ".fa"))
                altLociJsonObject.put("type", "inversion")
            }
            else {
                altLociJsonObject.put("source", altLoci.fasta_file.filename)
                altLociJsonObject.put("type", "correction")
            }
            altLociJsonObject.put("fmin", altLoci.featureLocation.fmin)
            altLociJsonObject.put("fmax", altLoci.featureLocation.fmax)
            altLociJsonObject.put("strand", altLoci.featureLocation.strand)
            altLociJsonObject.put("orientation", altLoci.orientation)
            array.add(altLociJsonObject)
            previousAltLociFmax = altLoci.featureLocation.fmax
        }

        JSONObject finalSequenceJsonObject = new JSONObject()
        finalSequenceJsonObject.put("name", sequence.name)
        finalSequenceJsonObject.put("description", "reference genome")
        finalSequenceJsonObject.put("type", "reference")
        finalSequenceJsonObject.put("source", sequence.organism.blatdb.replace(".2bit", ".fa"))
        finalSequenceJsonObject.put("source_contig", sequence.name)
        finalSequenceJsonObject.put("fmin", previousAltLociFmax + 1)
        finalSequenceJsonObject.put("fmax", sequence.end)
        array.put(finalSequenceJsonObject)

        returnObject.put("transformation", array)
        return returnObject
    }

    /**
     *
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
     *
     *
     * @param organism
     */
    def getTransformationAsFASTA(Organism organism) {
        def altLociList = getAltLoci(organism)
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
            JSONArray transformedJsonArray = getTransformationAsJSON(sequence, altLociForSequenceList).get("transformation")

            String fastaSequence = ""
            for (JSONObject segment : transformedJsonArray) {
                // TODO: a better way of handling the jump between 0-based and 1-based
                if (segment.type == "reference") {
                    if (segment.fmax + 1 > sequence.end) {
                        log.debug "[X1] Fetching for ${segment.fmin} ${segment.fmax}"
                        fastaSequence += fastaFileService.readIndexedFastaRegion(segment.source, segment.name, segment.fmin, segment.fmax)
                    }
                    else {
                        log.debug "[X2] Fetching for ${segment.fmin} ${segment.fmax + 1}"
                        fastaSequence += fastaFileService.readIndexedFastaRegion(segment.source, segment.name, segment.fmin, segment.fmax + 1)
                    }
                    fastaSequence += "|"
                }
                else if (segment.type == "inversion") {
                    log.debug "[Y] Fetching for ${segment.fmin + 1} ${segment.fmax}"
                    String sequenceString = fastaFileService.readIndexedFastaRegion(segment.source, segment.source_contig, segment.fmin + 1, segment.fmax)
                    fastaSequence += sequenceString.reverse()
                    fastaSequence += '|'
                }
                else if (segment.type == "correction") {
                    // calling readIndexedFasta because the correction assumes that the entire sequence will be used
                    if (segment.orientation == "REVERSE") {
                        fastaSequence += reverseComplementSequence(fastaFileService.readIndexedFasta(segment.source, segment.source_contig))
                    }
                    else {
                        fastaSequence += fastaFileService.readIndexedFasta(segment.source, segment.source_contig)
                    }
                    fastaSequence += '|'
                }
            }

            transformedFastaMap[sequenceName] = [sequence: fastaSequence, comment: "TRANSFORMED"]
        }

        return transformedFastaMap
    }


    // TODO: Make use of SequenceTranslationHandler; not sure why its not accessible
    /** Reverse complement a nucleotide sequence.
     *
     * @param sequence - String for the nucleotide sequence to be reverse complemented
     * @return Reverse complemented nucleotide sequence
     */
    public static String reverseComplementSequence(String sequence) {
        StringBuilder buffer = new StringBuilder(sequence);
        buffer.reverse();
        for (int i = 0; i < buffer.length(); ++i) {
            switch (buffer.charAt(i)) {
                case 'A':
                    buffer.setCharAt(i, 'T' as char);
                    break;
                case 'C':
                    buffer.setCharAt(i, 'G' as char);
                    break;
                case 'G':
                    buffer.setCharAt(i, 'C' as char);
                    break;
                case 'T':
                    buffer.setCharAt(i, 'A' as char);
                    break;
            }
        }
        return buffer.toString();
    }
}
