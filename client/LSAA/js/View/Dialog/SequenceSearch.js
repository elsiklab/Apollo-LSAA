define([
    'dojo/_base/declare',
    'dojo/_base/array',
    'dojo/request',
    'dojo/dom-construct',
    'dojo/on',
    'dojo/query',
    'dijit/Dialog',
    'dijit/layout/ContentPane',
    'dijit/_TemplatedMixin',
    'dijit/_WidgetsInTemplateMixin',
    'dojo/text!./SequenceSearch.html'
],
function(
    declare,
    array,
    request,
    dom,
    on,
    query,
    Dialog,
    ContentPane,
    TemplatedMixin,
    WidgetsInTemplateMixin,
    DialogTemplate
) {
    var Panel = declare([ContentPane, TemplatedMixin, WidgetsInTemplateMixin], {
        templateString: DialogTemplate
    });

    return declare(Dialog, {
        title: 'Sequence search',
        constructor: function(args) {
            this.browser = args.browser;
            this.contextPath = args.contextPath || '..';
            this.seqContext = args.browser.config.seqContext || '..';
            this.errorCallback = args.errorCallback || function() { };
            this.successCallback = args.successCallback || function() { };
            this.refSeqName = args.refseq;
        },

        hide: function() {
            this.inherited(arguments);
            window.setTimeout(dojo.hitch(this, 'destroyRecursive'), 500);
        },
        show: function() {
            var thisB = this;
            this.set('content', new Panel());
            this.inherited(arguments);

            on(dojo.byId('sequence_search'), 'submit', function(event) {
                thisB.search(thisB.refSeqName);
                dojo.stopEvent(event);
            });

            this.getSequenceSearchTools(this.refSeqName);
        },

        getSequenceSearchTools: function(refSeqName) {
            var postobj = {
                'track': refSeqName,
                'operation': 'get_sequence_search_tools'
            };
            return request(this.seqContext + '/../sequenceSearch/getSequenceSearchTools', {
                data: JSON.stringify(postobj),
                method: 'post',
                headers: { 'Content-Type': 'application/json' },
                handleAs: 'json',
                timeout: 5000 * 1000
            }).then(function(response) {
                if (response.error) {
                    console.error(response.error);
                }
                if (response.sequence_search_tools.length === 0) {
                    console.error('No sequence search tools available');
                    return;
                }

                var sequenceToolsSelect = dojo.byId('sequence_tools_select');
                for (var key in response.sequence_search_tools) {
                    if (response.sequence_search_tools.hasOwnProperty(key)) {
                        dojo.create('option', { innerHTML: response.sequence_search_tools[key].name, id: key }, sequenceToolsSelect);
                    }
                }
            }, function(response) {
                console.error(response);
            });
        },

        search: function(refSeqName) {
            var thisB = this;
            var residues = dojo.byId('sequence_field').value.toUpperCase();
            if (residues.length === 0) {
                console.error('No sequence entered');
                return;
            } else if (residues.match(/[^ACDEFGHIKLMNPQRSTVWXY\n]/)) {
                console.error('The sequence should only contain non redundant IUPAC nucleotide or amino acid codes (except for N/X)');
                return;
            }

            var sequenceToolsSelect = dojo.byId('sequence_tools_select');
            console.log("Search all sequences ??? ", dojo.byId('search_all_refseqs').checked);
            // Note: this.browser.config.dataset_id holds the organism ID
            var postobj = {
                track: refSeqName,
                search: {
                    key: sequenceToolsSelect.options[sequenceToolsSelect.selectedIndex].id,
                    residues: residues.replace(/(\r\n|\n|\r)/gm, ''),
                    database_id: dojo.byId('search_all_refseqs').checked ? null : refSeqName
                },
                operation: 'search_sequence',
                organism: this.browser.config.dataset_id
            };
            console.log("SS post object: ", postobj);

            query('#sequence_search_waiting').style('display', '');
            request(this.seqContext + '/../sequenceSearch/searchSequence', {
                data: JSON.stringify(postobj),
                handleAs: 'json',
                method: 'post',
                headers: { 'Content-Type': 'application/json' },
                timeout: 5000 * 1000
            }).then(function(response) {
                if (response.error) {
                    console.error(response.error);
                    return;
                }
                query('#sequence_search_waiting').style('display', 'none');
                console.log(query('#sequence_search_matches'));
                var matches = dojo.byId('sequence_search_matches');
                dom.empty(matches);
                if (response.matches.length === 0) {
                    query('#sequence_search_message').style('display', '');
                    query('#sequence_search_matches').style('display', 'none');
                    query('#sequence_search_header').style('display', 'none');
                    return;
                }
                query('#sequence_search_message').style('display', 'none');
                query('#sequence_search_matches').style('display', '');
                query('#sequence_search_header').style('display', '');
                var matches = dojo.byId('sequence_search_matches');

                var returnedMatches = response.matches;
                returnedMatches.sort(function(match1, match2) {
                    return match2.rawscore - match1.rawscore;
                });
                var maxNumberOfHits = 100;

                for (var i = 0; i < returnedMatches.length && i < maxNumberOfHits; ++i) {
                    var match = returnedMatches[i];
                    var subject = match.subject;
                    var subjectStart = subject.location.fmin + 1;
                    var subjectEnd = subject.location.fmax + 1;
                    if (subject.location.strand === -1) {
                        var tmp = subjectStart;
                        subjectStart = subjectEnd;
                        subjectEnd = tmp;
                    }
                    var row = dojo.create('div', { className: 'search_sequence_matches_row' }, matches);
                    dojo.create('span', { innerHTML: subject.feature.uniquename, className: 'search_sequence_matches_field search_sequence_matches_generic_field', title: subject.feature.uniquename }, row);
                    dojo.create('span', { innerHTML: subjectStart, className: 'search_sequence_matches_field search_sequence_matches_generic_field' }, row);
                    dojo.create('span', { innerHTML: subjectEnd, className: 'search_sequence_matches_field search_sequence_matches_generic_field' }, row);
                    dojo.create('span', { innerHTML: match.rawscore, className: 'search_sequence_matches_field search_sequence_matches_generic_field' }, row);
                    dojo.create('span', { innerHTML: match.significance, className: 'search_sequence_matches_field search_sequence_matches_generic_field' }, row);
                    dojo.create('span', { innerHTML: match.identity, className: 'search_sequence_matches_field search_sequence_matches_generic_field' }, row);
                    on(row, 'click', (function(id, fmin, fmax) {
                        return function() {
                            thisB.successCallback(id, fmin, fmax);
                        };
                    })(subject.feature.uniquename, subject.location.fmin, subject.location.fmax));
                }
                if (response.track) {
                    thisB.createCombinationTrack(response.track);
                }
                thisB.resize();
                thisB._position();
            }, function(err) {
                query('#sequence_search_waiting').style('display', 'none');
                dojo.byId('sequence_search_message').val(err);
                console.error(err);
            });
        },
        createCombinationTrack: function(trackConf) {
            var storeConf = {
                browser: this.browser,
                refSeq: this.browser.refSeq,
                type: trackConf.storeClass,
                baseUrl: this.browser.config.baseUrl + 'data/',
                urlTemplate: trackConf.urlTemplate
            };
            var storeName = this.browser.addStoreConfig(null, storeConf);
            var thisB = this;
            storeConf.name = storeName;
            this.browser.getStore(storeName, function() {
                trackConf.store = storeName;
                if (trackConf.style && trackConf.style.color) {
                    trackConf.style.color = eval('(' + trackConf.style.color + ')');
                }
                thisB.browser.publish('/jbrowse/v1/v/tracks/new', [trackConf]);
                thisB.browser.publish('/jbrowse/v1/v/tracks/show', [trackConf]);
            });
        }
    });
});
