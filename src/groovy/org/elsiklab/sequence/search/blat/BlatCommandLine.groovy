package org.elsiklab.sequence.search.blat

import org.bbop.apollo.sequence.search.AlignmentParsingException
import org.bbop.apollo.sequence.search.SequenceSearchTool
import org.bbop.apollo.sequence.search.SequenceSearchToolException
import org.bbop.apollo.BlastAlignment
import org.bbop.apollo.TabDelimitedAlignment
import java.nio.file.Files
import java.nio.file.Path
import org.codehaus.groovy.grails.web.json.JSONObject

class BlatCommandLine {

    private String blatBin
    private String database
    private String blatUserOptions
    private String tmpDir
    private String outputDir
    private String gffFormatter
    private boolean removeTmpDir
    protected String [] blatOptions

    void parseConfiguration(JSONObject config) throws SequenceSearchToolException {
        blatBin = config.search_exe
        database = config.database
        blatUserOptions = config.params
        outputDir = config.output_dir
        removeTmpDir = config.removeTmpDir
        tmpDir = config.tmp_dir
        gffFormatter = config.gff_exe
    }

    Collection<BlastAlignment> search(String uniqueToken, String query, String databaseId, StringBuilder stringBuilder) throws SequenceSearchToolException {
        Path tmpDirPath = tmpDir ? Files.createTempDirectory(new File(tmpDir).toPath(),'blat_tmp') : Files.createTempDirectory('blat_tmp')
        log.debug "blat tmpDir path: ${tmpDirPath}"
        File tmpDirFile
        try {
            tmpDirFile = tmpDirPath.toFile()
        } catch (IOException e) {
            println e.message
        }
        log.debug "successfully created file: ${tmpDirFile.canonicalPath}"

        // Perform BLAT search
        Collection<BlastAlignment> blatResults = runSearch(tmpDirFile, query, databaseId)

        // remove tmpDir created after BLAT run
        if (removeTmpDir && tmpDirFile != null) {
            deleteTmpDir(tmpDirFile)
        }
        // TODO: perhaps append canonical name instead?
        stringBuilder.append(tmpDirFile.name)

        return blatResults
    }

    private Collection<BlastAlignment> runSearch(File dir, String query, String databaseId) throws IOException, AlignmentParsingException, InterruptedException {
        String queryArg = createQueryFasta(dir, query)
        String databaseArg = database
        String outputArg = dir.absolutePath + File.separator + 'results.tab'
        String outputPsl = dir.absolutePath + File.separator + 'results.psl'
        String outputGff = dir.absolutePath + File.separator + 'results.gff'

        String command = blatBin + ' '
        for (String option : blatOptions) {
            command += option + ' '
        }

        // Using only .2bit for BLAT
        println "Running BLAT to create TAB: ${command} ${databaseArg}:${databaseId} ${queryArg} ${outputArg} -out=blast8"
        ("${command} ${databaseArg}:${databaseId} ${queryArg} ${outputArg} -out=blast8").execute().waitForProcessOutput(System.out, System.err)
        println "Running BLAT to create PSL: ${command} ${databaseArg}:${databaseId} ${queryArg} ${outputPsl}"
        ("${command} ${databaseArg}:${databaseId} ${queryArg} ${outputPsl}").execute().waitForProcessOutput(System.out, System.err)
        println "Parsing PSL to GFF3: ${gffFormatter} -f psl  -m -ver 3 -t hit -i ${outputPsl}"
        def gffContent = ("${gffFormatter} -f psl  -m -ver 3 -t hit -i ${outputPsl}").execute().text
        println "Results as GFF3:\n ${gffContent}"
        new File(outputGff).withWriterAppend('UTF-8') { it.write(gffContent) }
        ['flatfile-to-json.pl', '--config', $/{"glyph":"JBrowse/View/FeatureGlyph/Box"}/$,'--clientConfig',$/{"color":"function(feature){return(feature.get('strand')==-1?'blue':'red');}"}/$, '--trackType', 'JBrowse/View/Track/CanvasFeatures', '--trackLabel', "${dir.name}", '--gff', "${outputGff}", '--out', "${outputDir}"].execute().waitForProcessOutput(System.out, System.err)

//        def timer = new Timer()
//        def outputPath = outputDir
//        // schedule a task to run after 2 minutes
//        // TODO: make this configurable
//        def task = timer.runAfter(120 * 1000) {
//            println "removing track: ${dir.name}"
//            ("remove-track.pl --trackLabel ${dir.name} --out ${outputPath} --delete").execute().waitForProcessOutput(System.out, System.err)
//        }

        Collection<BlastAlignment> matches = new ArrayList<BlastAlignment>()
        new File(outputArg).eachLine { line ->
            matches.add(new TabDelimitedAlignment(line))
        }

        return matches
    }

    private void deleteTmpDir(File dir) {
        if (!dir.exists()) {
            return
        }
        for (File f : dir.listFiles()) {
            f.delete()
        }
        dir.delete()
    }

    private String createQueryFasta(File tmpDir, String query) throws IOException {
        String queryFileName = tmpDir.absolutePath + File.separator + 'query.fa'
        File queryFile
        try {
            queryFile = new File(queryFileName)
            FileWriter queryFileWriter = new FileWriter(queryFile)
            PrintWriter out = new PrintWriter(new BufferedWriter(queryFileWriter))
            out.println('>query')
            out.println(query)
            out.close()
            return queryFileName
        } catch (IOException e) {
            println e.stackTrace
        }
        return null
    }
}
