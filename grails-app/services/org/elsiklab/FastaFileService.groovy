package org.elsiklab

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.samtools.reference.FastaSequenceIndex
import htsjdk.samtools.reference.FastaSequenceIndexCreator
import htsjdk.samtools.util.SequenceUtil
import htsjdk.samtools.reference.ReferenceSequence

import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths

class FastaFileService {

    public static final String FAI_SUFFIX = ".fai"

    /**
     * Generate Fasta Index for a given FASTA file
     * @param fileName
     * @return
     */
    def generateFastaIndex(String fileName) {
        log.debug "fileName: ${fileName}"
        Path filePath = Paths.get(fileName)
        FastaSequenceIndex index = FastaSequenceIndexCreator.buildFromFasta(filePath)
        log.debug "index created"
        return index
    }

    /**
     *
     * @param fileName
     * @return
     */
    def generateFastaIndexFile(String fileName) {
        Path filePath = Paths.get(fileName)
        FastaSequenceIndexCreator.create(filePath, true)
    }

    /**
     * Read an indexed FASTA and fetch the sequence for a given sequence name
     * @param fastaFile
     * @param sequenceName
     * @return
     */
    def readIndexedFasta(String fastaFile, String sequenceName, boolean reverse = false) {
        log.debug "fastaFile: ${fastaFile} sequenceName: ${sequenceName}"
        def indexedFasta
        try {
            indexedFasta = new IndexedFastaSequenceFile(new File(fastaFile))
        }
        catch(FileNotFoundException e) {
            log.debug "Index not found for: ${fastaFile}; generating one..."
            def fastaIndex = generateFastaIndex(fastaFile)
            indexedFasta = new IndexedFastaSequenceFile(new File(fastaFile), fastaIndex)
        }
        ReferenceSequence sequence = indexedFasta.getSequence(sequenceName)
        String returnSequence = sequence.getBaseString()
        if (reverse) {
            returnSequence = SequenceUtil.reverseComplement(returnSequence)
        }

        log.debug "return sequence size: ${returnSequence.length()}"
        return returnSequence
    }

    /**
     *
     * @param fastaFile
     * @param sequenceName
     * @param start zero-based
     * @param end zero-based
     * @return
     */
    def readIndexedFastaRegion(String fastaFile, String sequenceName, Integer start, Integer end, boolean reverse = false) {
        println "fastaFile: ${fastaFile} ${sequenceName}:${start}-${end}"
        def indexedFasta
        try {
            indexedFasta = new IndexedFastaSequenceFile(new File(fastaFile))
        }
        catch(FileNotFoundException e) {
            log.debug "Index not found for: ${fastaFile}; generating one..."
            def fastaIndex = generateFastaIndex(fastaFile)
            indexedFasta = new IndexedFastaSequenceFile(new File(fastaFile), fastaIndex)
        }
        // Note: start and end are expected to be 1-based
        ReferenceSequence sequence = indexedFasta.getSubsequenceAt(sequenceName, start + 1, end)
        String returnSequence = sequence.getBaseString().trim()
        if(reverse) {
            returnSequence = SequenceUtil.reverseComplement(returnSequence)
        }
        log.debug "return sequence size: ${returnSequence.length()}"
        return returnSequence
    }

}
