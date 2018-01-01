package org.elsiklab

import org.bbop.apollo.Feature
import org.bbop.apollo.User
import org.bbop.apollo.UserGroup
import org.bbop.apollo.GroupOrganismPermission
import org.bbop.apollo.Role
import org.bbop.apollo.UserOrganismPermission
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.Sequence
import org.bbop.apollo.BiologicalRegion
import org.bbop.apollo.Organism

import org.apache.shiro.SecurityUtils

import org.elsiklab.PermissionEnum
import org.elsiklab.FeatureStringEnum

@Transactional
class AlternativeLociService {

    def grailsApplication
    def featureService
    def preferenceService

    def fastaFileService


    public static final String TYPE_CORRECTION = "CORRECTION"
    public static final String TYPE_INVERSION = "INVERSION"
    public static final String TYPE_INSERTION = "INSERTION"
    public static final String TYPE_DELETION = "DELETION"

    /**
     *
     * @param jsonObject
     * @param organism
     * @param sequence
     * @return
     */
    def createCorrection(JSONObject jsonObject, Organism organism, Sequence sequence) {
        log.debug "${jsonObject.toString()}"
        String name = UUID.randomUUID()
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

        log.info "Creating FASTA file to ${grailsApplication.config.lsaa.lsaaDirectory}"
        String fastaFilePrefix = "${organism.id}-${sequenceName}-${name}"
        String fastaFileName = grailsApplication.config.lsaa.lsaaDirectory + File.separator + fastaFilePrefix + ".fa"
        def file = new File(fastaFileName)
        // TODO: a better way to write this file and move this to a method
        file << ">${name} ${TYPE_CORRECTION} ${sequenceName} ${organism.id}\n"
        file << jsonObject.sequenceData
        fastaFileService.generateFastaIndexFile(fastaFileName)

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
                type: TYPE_CORRECTION,
                uniqueName: name,
                description: description,
                startPosition: 0,
                endPosition: jsonObject.sequenceData.length() - 1,
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
        log.debug "${jsonObject.toString()}"
        String name = UUID.randomUUID()
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
                type: TYPE_INVERSION,
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
        log.debug "${jsonObject.toString()}"
        String name = UUID.randomUUID()
        String description = jsonObject.description
        String sequenceName = jsonObject.sequence
        int orientation = Integer.parseInt(jsonObject.orientation)
        int start = Integer.parseInt(jsonObject.position)
        String coordinateFormat = jsonObject.coordinateFormat
        if (coordinateFormat == "one_based") {
            // bringing the positions to internal zero-based
            start -= 1
        }

        log.info "Creating FASTA file to ${grailsApplication.config.lsaa.lsaaDirectory}"
        String fastaFilePrefix = "${organism.id}-${sequenceName}-${name}"
        String fastaFileName = grailsApplication.config.lsaa.lsaaDirectory + File.separator + fastaFilePrefix + ".fa"
        def file = new File(fastaFileName)
        // TODO: a better way to write this file and move this to a method
        file << ">${name} ${TYPE_INSERTION} ${sequenceName} ${organism.id}\n"
        file << jsonObject.sequenceData
        fastaFileService.generateFastaIndexFile(fastaFileName)

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
                type: TYPE_INSERTION,
                uniqueName: name,
                description: description,
                startPosition: 0,
                endPosition: jsonObject.sequenceData.length() - 1,
                orientation: orientation,
                fastaFile: fastaFile
        ).save(flush: true)

        // Insertion fmin and fmax will always be the same (i.e. fmax - fmin will be 0)
        FeatureLocation featureLocation = new FeatureLocation(
                fmin: start,
                fmax: start,
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
        log.debug "${jsonObject.toString()}"
        String name = UUID.randomUUID()
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
                type: TYPE_DELETION,
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
        log.debug "Sequence is: ${sequence}"
        return sequence
    }

    /**
     *
     * @param jsonObject
     * @param organism
     * @return
     */
    Boolean hasPermissions(JSONObject jsonObject, Organism organism) {
        User user = getCurrentUser(jsonObject)
        log.debug "checking if user: ${user.username} is ADMIN"
        // check if user is admin
        if (isUserAdmin(user)) {
            log.debug "user is ADMIN"
            return true
        }
        // check if user is part of a group that has ADMIN or WRITE permission on the organism
        log.debug "checking group-level permissions for user: ${user.username}"
        def groupOrganismPermissions = GroupOrganismPermission.findAllByOrganism(organism)
        for(int i = 0; i < groupOrganismPermissions.size(); i++) {
            def groupOrganismPermission = groupOrganismPermissions.get(i)
            if (user in groupOrganismPermission.group.users) {
                log.debug "user: ${user.username} is part of group: ${groupOrganismPermission.group.name}"
                if (groupOrganismPermission.permissions.contains("WRITE") || groupOrganismPermission.permissions.contains("ADMIN")) {
                    log.debug "group: ${groupOrganismPermission.group.name} has WRITE/ADMIN permissions"
                    return true
                }
            }
        }

        // check if user itself has ADMIN or WRITE permission on the organism
        log.debug "checking user-level permissions for user: ${user.username}"
        def userOrganismPermission = UserOrganismPermission.findByUserAndOrganism(user, organism)
        if (userOrganismPermission.permissions.contains("WRITE") || userOrganismPermission.permissions.contains("ADMIN")) {
            log.debug "user: ${user.username} has WRITE/ADMIN permissions"
            return true
        }

        log.debug "user does not have WRITE/ADMIN permissions"
        return false
    }

    /**
     *
     * @param inputObject
     * @return
     */
    User getCurrentUser(JSONObject inputObject = new JSONObject()) {
        String username = null
        if (inputObject?.has(FeatureStringEnum.USERNAME.value)) {
            username = inputObject.getString(FeatureStringEnum.USERNAME.value)
        }
        if (!username) {
            username = SecurityUtils.subject.principal
            println"username from Shiro: ${username}"
        }
        if (!username) {
            return null
        }

        User user = User.findByUsername(username)
        return user
    }

    /**
     *
     * @param user
     * @return
     */
    boolean isUserAdmin(User user) {
        if (user != null) {
            for (Role role in user.roles) {
                if (role.name == "ADMIN") {
                    return true
                }
            }
        }

        return false
    }
}