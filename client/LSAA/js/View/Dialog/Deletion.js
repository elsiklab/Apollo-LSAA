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
                this.error = dom.create('div', { 'id': 'error', 'class': 'errormsg' });
                this.user = JSON.parse(window.parent.getCurrentUser());
                var br = function() { return dom.create('br'); };

                this.set('content', [
                    dom.create('label', { 'for': 'lsaa_name', innerHTML: 'Reference sequence: ' }), this.sequence.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_start', innerHTML: 'Start: ' }), this.start.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_end', innerHTML: 'End: ' }), this.end.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_descritpion', innerHTML: 'Description: ' }), this.description.domNode, br(),
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
