environments {
    development {
        // sample config to turn on debug logging in development e.g. for apollo run-local
        dataSource{
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "biocmd"
            url = "jdbc:postgresql://localhost/apollo_lsaa"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.hibernate.dialect.PostgresPlusDialect"
        }
    }
    test {
        dataSource{
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "biocmd"
            url = "jdbc:postgresql://localhost/apollo_lsaa_test"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.elsiklab.ImprovedPostgresDialect"
        }
    }
    production {
        dataSource{
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "biocmd"
            url = "jdbc:postgresql://localhost/apollo_lsaa"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.elsiklab.ImprovedPostgresDialect"
        }
    }
}


// can use full paths to blat or bp_search2gff.pl if not in path
lsaa {
    bootstrap = true
    exportDirectory = [
        dir: '/tmp'
    ]
    sequence_search_tools = [
        blat_nuc: [
            search_exe: 'blat',
            search_class: 'org.elsiklab.sequence.search.blat.BlatCommandLineNucleotideToNucleotide',
            name: 'Blat nucleotide',
            params: '',
            gff_exe: 'bp_search2gff.pl',
            tmp_dir: '/tmp',
            removeTmpDir: false,
        ],
        blat_prot: [
            search_exe: 'blat',
            search_class: 'org.elsiklab.sequence.search.blat.BlatCommandLineProteinToNucleotide',
            name: 'Blat protein',
            gff_exe: 'bp_search2gff.pl',
            params: '',
            tmp_dir: '/tmp',
            removeTmpDir: false
        ],
        blast_prot: [
            search_exe: '/usr/local/ncbi/blast/bin/tblastn',
            formatter_exe: '/usr/local/ncbi/blast/bin/blast_formatter',
            gff_exe: 'bp_search2gff.pl',
            search_class: 'org.elsiklab.sequence.search.blast.BlastCommandLine',
            name: 'Blast protein',
            params: ''
        ]
    ]
}
