package org.elsiklab

import org.bbop.apollo.Organism

class FastaFile {

    static constraints = {
        organism nullable: true
        sequenceName nullable: true
        fileName nullable: true
    }


    String sequenceName
    String fileName
    String originalName
    //String username
    Date dateCreated
    Date lastUpdated
    Organism organism
}
