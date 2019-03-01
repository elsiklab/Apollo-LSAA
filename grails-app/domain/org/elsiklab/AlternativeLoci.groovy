package org.elsiklab

import org.bbop.apollo.BiologicalRegion

import groovy.transform.EqualsAndHashCode
@EqualsAndHashCode
class AlternativeLoci extends BiologicalRegion {

    static String ontologyId = "SO:0001525"
    static String cvTerm = "assembly_error_correction"
    static String alternateCvTerm = "alternative_loci"


    static constraints = {
        reversed nullable: true
        fastaFile nullable: true
        description nullable: true
        breed nullable: true
        individual nullable: true
    }

    static belongsTo = [
            Breed
    ]

    String type
    String description
    Boolean reversed

    Integer startPosition
    Integer endPosition
    Integer orientation
    FastaFile fastaFile
    Breed breed
    String individual


    static mapping = {
        description type: 'text'
        orientation nullable: true
    }
}
