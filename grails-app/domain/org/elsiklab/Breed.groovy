package org.elsiklab

import org.bbop.apollo.Organism

class Breed {

    static constraints = {
        name nullable: true
        identifier nullable: false
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
