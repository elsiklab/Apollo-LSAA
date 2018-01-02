define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dijit/focus',
        'dijit/form/TextBox',
        'dojo/on',
        'dojo/request',
        'dijit/form/Button',
        'JBrowse/View/Dialog/WithActionBar'
    ],
    function(
        declare,
        dom,
        focus,
        TextBox,
        on,
        request,
        Button,
        ActionBarDialog
    ) {
        return declare(ActionBarDialog, {
            autofocus: false,
            title: 'Create an alternative loci of type Deletion',

            constructor: function(args) {
                this.browser = args.browser;
                this.setCallback = args.setCallback || function() {};
                this.cancelCallback = args.cancelCallback || function() {};
                this.contextPath = args.contextPath || '..';
            },

            _fillActionBar: function(actionBar) {
                var thisB = this;
                new Button({
                    label: 'Cancel',
                    onClick: function() {
                        if (thisB.cancelCallback) {
                            thisB.cancelCallback();
                        }
                        thisB.hide();
                    }
                }).placeAt(actionBar);
                new Button({
                    label: 'OK',
                    onClick: function() {
                        var valid = true;
                        var breed = thisB.breed.get('value');
                        var urlTemplate1 = 'http://bovinegenome.org/bovinemine/service/query/results?query=<query name="" model="genomic" view="LBOTerm.identifier LBOTerm.name" longDescription="" sortOrder="LBOTerm.identifier asc"><constraint path="LBOTerm.identifier" op="=" value="%IDENTIFIER%"/></query>&format=json';
                        var urlTemplate2 = 'http://bovinegenome.org/bovinemine/service/query/results?query=<query name="" model="genomic" view="LBOTerm.identifier LBOTerm.name" longDescription="" sortOrder="LBOTerm.identifier asc"><constraint path="LBOTerm.name" op="=" value="%NAME%"/></query>&format=json';
                        var queryUrl;

                        if (breed.length != 0) {
                            if (breed.startsWith("LBO:")) {
                                queryUrl = urlTemplate1.replace('%IDENTIFIER%', breed);
                            }
                            else {
                                queryUrl = urlTemplate2.replace('%NAME%', breed);
                            }

                            request(queryUrl,
                                {
                                    data: {},
                                    handleAs: 'json',
                                    method: 'get'
                                }).then(function(response) {
                                console.log("Results: ", response);
                                if (response.statusCode === 200) {
                                    if (response.results.length === 0) {
                                        // no match
                                        var message = "Could not verify Breed.\nPlease use a proper Livestock Breed Ontology (LBO) term (Ex. 'LBO:0000017' or 'Angus').";
                                        console.log(message);
                                        thisB.error.innerHTML = '<br>' + message + '<br>';
                                        valid = false;
                                    }
                                    else if (response.results.length === 1) {
                                        // exact match
                                        var message = "Found one LBOTerm for '" + breed + "'.";
                                        breed = response.results[0];
                                        console.log(message);
                                        thisB.error.innerHTML = '';
                                        valid = true;
                                    }
                                    else {
                                        // ambiguous match
                                        var message = "Found more than one LBO term for '" + breed + "'.\nPlease use one of the following LBO term identifiers instead: ";
                                        for (var i = 0; i < response.results.length; i++) {
                                            if (i !== 0) message += ', ';
                                            message += response.results[i][0]
                                        }
                                        console.log(message);
                                        thisB.error.innerHTML = '<br>' + message + '<br>';
                                        valid = false;
                                    }
                                }
                                else {
                                    console.log("Could not query BovineMine for validating Breed against known Livestock Breed Ontology (LBO) terms.");
                                }

                                var start = thisB.start.get('value');
                                var end = thisB.end.get('value');
                                if (start > end) {
                                    valid = false;
                                    window.alert("Error: Deletion Start greater than End");
                                }

                                if (valid) {
                                    request(thisB.contextPath + '/../alternativeLoci/createDeletion', {
                                        data: {
                                            start: start,
                                            end: end,
                                            coordinateFormat: "one_based",
                                            sequence: thisB.sequence.get('value'),
                                            description: thisB.description.get('value'),
                                            breed: breed.join('|'),
                                            organism: thisB.browser.config.dataset_id,
                                            username: thisB.user.email
                                        },
                                        handleAs: 'json',
                                        method: 'post'
                                    }).then(function() {
                                        thisB.hide();
                                        thisB.browser.clearHighlight();
                                        thisB.browser.view.redrawTracks();
                                    }, function(error) {
                                        thisB.error.innerHTML = error.message + '<br>' + ((error.response || {}).data || {}).error;
                                        console.error(error);
                                    });
                                }

                            }, function(error) {
                                thisB.error.innerHTML = error.message + '<br>' + ((error.response || {}).data || {}).error;
                                console.log(error);
                            });
                        }
                        else {
                            var start = thisB.start.get('value');
                            var end = thisB.end.get('value');
                            if (start > end) {
                                valid = false;
                                window.alert("Error: Deletion Start greater than End");
                            }

                            if (valid) {
                                request(thisB.contextPath + '/../alternativeLoci/createDeletion', {
                                    data: {
                                        start: start,
                                        end: end,
                                        coordinateFormat: "one_based",
                                        sequence: thisB.sequence.get('value'),
                                        description: thisB.description.get('value'),
                                        organism: thisB.browser.config.dataset_id,
                                        username: thisB.user.email
                                    },
                                    handleAs: 'json',
                                    method: 'post'
                                }).then(function() {
                                    thisB.hide();
                                    thisB.browser.clearHighlight();
                                    thisB.browser.view.redrawTracks();
                                }, function(error) {
                                    thisB.error.innerHTML = error.message + '<br>' + ((error.response || {}).data || {}).error;
                                    console.error(error);
                                });
                            }
                        }
                    }
                }).placeAt(actionBar);

                new Button({
                    iconClass: 'dijitIconFilter',
                    label: 'Get coordinates from highlighted region',
                    onClick: function() {
                        var highlight = thisB.browser.getHighlight();
                        if (highlight) {
                            thisB.start.set('value', highlight.start + 1);
                            thisB.end.set('value', highlight.end);
                            thisB.sequence.set('value', highlight.ref);
                            thisB.coordinateFormat = "one_based";
                        } else {
                            console.log('No highlight set');
                        }
                    }
                }).placeAt(actionBar);
            },

            show: function() {
                dojo.addClass(this.domNode, 'setLSAA');

                this.sequence = new TextBox({id: 'lsaa_name', value: this.browser.refSeq.name});
                this.start = new TextBox({id: 'lsaa_start'});
                this.end = new TextBox({id: 'lsaa_end'});
                this.coordinateFormat = "one_based";
                this.description = new TextBox({id: 'lsaa_description'});
                this.breed = new TextBox({id: 'lsaa_breed'});
                this.error = dom.create('div', { 'id': 'error', 'class': 'errormsg' });
                this.user = JSON.parse(window.parent.getCurrentUser());
                var br = function() { return dom.create('br'); };

                this.set('content', [
                    dom.create('label', { 'for': 'lsaa_name', innerHTML: 'Reference sequence: ' }), this.sequence.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_start', innerHTML: 'Start: ' }), this.start.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_end', innerHTML: 'End: ' }), this.end.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_description', innerHTML: 'Description: ' }), this.description.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_breed', innerHTML: 'Breed: ' }), this.breed.domNode, br(),
                    this.error, br()
                ]);

                this.inherited(arguments);
            },

            hide: function() {
                this.inherited(arguments);
                window.setTimeout(dojo.hitch(this, 'destroyRecursive'), 500);
            }
        });
    });
