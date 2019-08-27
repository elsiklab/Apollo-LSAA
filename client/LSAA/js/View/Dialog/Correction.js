define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dijit/focus',
        'dijit/form/TextBox',
        'dijit/form/Select',
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
        Select,
        on,
        request,
        Button,
        ActionBarDialog
    ) {
        return declare(ActionBarDialog, {
            autofocus: false,
            title: 'Create an alternative locus of type Correction',

            constructor: function(args) {
                this.browser = args.browser;
                this.setCallback    = args.setCallback || function() {};
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
                        var individual = thisB.individual.get('value');
                        var urlTemplate1 = 'http://128.206.116.13:8080/bovinemine/service/query/results?query=<query name="" model="genomic" view="LBOTerm.identifier LBOTerm.name" longDescription="" sortOrder="LBOTerm.identifier asc"><constraint path="LBOTerm.identifier" op="=" value="%IDENTIFIER%"/></query>&format=json';
                        var urlTemplate2 = 'http://128.206.116.13:8080/bovinemine/service/query/results?query=<query name="" model="genomic" view="LBOTerm.identifier LBOTerm.name" longDescription="" sortOrder="LBOTerm.identifier asc"><constraint path="LBOTerm.name" op="=" value="%NAME%"/></query>&format=json';
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
                                if (thisB.sequencedata.value.length === 0) {
                                    valid = false;
                                    window.alert("Error: Sequence data cannot be empty");
                                }
                                if (!thisB.sequencedata.value.match(/^[ATCGatcg]+$/)) {
                                    valid = false;
                                    window.alert("Error: Sequence data contains non-nucleotide characters");
                                }
                                if (parseInt(start) > parseInt(end)) {
                                    valid = false;
                                    window.alert("Error: Correction Start greater than End");
                                }

                                if (valid) {
                                    request(thisB.contextPath + '/../alternativeLoci/createCorrection', {
                                        data: {
                                            start: start,
                                            end: end,
                                            coordinateFormat: "one_based",
                                            sequence: thisB.sequence.get('value'),
                                            description: thisB.description.get('value'),
                                            breed: breed.join('|'),
                                            individual: individual,
                                            orientation: thisB.orientation.get('value'),
                                            sequenceData: thisB.sequencedata.value,
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
                            if (thisB.sequencedata.value.length === 0) {
                                valid = false;
                                window.alert("Error: Sequence data cannot be empty");
                            }
                            if (!thisB.sequencedata.value.match(/^[ATCGatcg]+$/)) {
                                valid = false;
                                window.alert("Error: Sequence data contains non-nucleotide characters");
                            }
                            if (parseInt(start) > parseInt(end)) {
                                valid = false;
                                window.alert("Error: Correction Start greater than End");
                            }

                            if (valid) {
                                request(thisB.contextPath + '/../alternativeLoci/createCorrection', {
                                    data: {
                                        start: start,
                                        end: end,
                                        coordinateFormat: "one_based",
                                        sequence: thisB.sequence.get('value'),
                                        description: thisB.description.get('value'),
                                        individual: individual,
                                        orientation: thisB.orientation.get('value'),
                                        sequenceData: thisB.sequencedata.value,
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
                this.orientation = new Select({
                    name: "orientation-select",
                    width: "75px",
                    options: [
                        { label: "Forward", value:  1 },
                        { label: "Reverse", value: -1 }
                    ]
                });

                this.description = new TextBox({id: 'lsaa_description'});
                this.breed = new TextBox({id: 'lsaa_breed'});
                this.individual = new TextBox({id: 'lsaa_individual'});
                this.sequencedata = dom.create('textarea', { style: { height: '60px', width: '100%' }, id: 'sequencedata' });
                this.error = dom.create('div', { 'id': 'error', 'class': 'errormsg' });
                this.coordinateFormat = "one_based";
                this.user = JSON.parse(window.parent.getCurrentUser());
                var br = function() { return dom.create('br'); };

                this.set('content', [
                    dom.create('label', { 'for': 'lsaa_name', innerHTML: 'Reference sequence: ' }), this.sequence.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_start', innerHTML: 'Start: ' }), this.start.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_end', innerHTML: 'End: ' }), this.end.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_orientation', innerHTML: 'Orientation: ' }), this.orientation.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_description', innerHTML: 'Description: ' }), this.description.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_breed', innerHTML: 'Breed: ' }), this.breed.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_individual', innerHTML: 'Individual: ' }), this.individual.domNode, br(),
                    dom.create('label', { 'for': 'sequencedata', innerHTML: 'Sequence data: ' }), this.sequencedata, br(),
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
