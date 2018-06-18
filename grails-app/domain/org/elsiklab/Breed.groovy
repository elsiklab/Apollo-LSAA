package org.elsiklab

import org.bbop.apollo.Organism

class Breed {

    static constraints = {
        name nullable: false
        identifier nullable: true
    }

    static transients = [ 'nameAndIdentifier' ]

    static hasMany = [
            alternativeLoci: AlternativeLoci
    ]

    String identifier
    String name
    Organism organism

    public String getNameAndIdentifier() {
        return "${name} (${identifier})"
    }

}
