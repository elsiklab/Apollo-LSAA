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
            title: 'Create an alternative loci of type Insertion',

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
                        request(thisB.contextPath + '/../alternativeLoci/createInsertion', {
                            data: {
                                position: thisB.lsaaPosition.get('value'),
                                coordinate_format: "one_based",
                                sequence: thisB.sequence.get('value'),
                                description: thisB.description.get('value'),
                                orientation: thisB.orientation.get('value'),
                                sequence_data: thisB.sequencedata.value,
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
                }).placeAt(actionBar);

                new Button({
                    iconClass: 'dijitIconFilter',
                    label: 'Get coordinates from highlighted region',
                    onClick: function() {
                        var highlight = thisB.browser.getHighlight();
                        if (highlight) {
                            thisB.lsaaPosition.set('value', highlight.start + 1);
                            thisB.sequence.set('value', highlight.ref);
                            thisB.coordinateFormat = "one_based";
                        } else {
                            console.log('No highlight set');
                        }
                    }
                }).placeAt(actionBar);
            },

            show: function() {
                console.log(this);
                dojo.addClass(this.domNode, 'setLSAA');

                this.sequence = new TextBox({id: 'lsaa_name', value: this.browser.refSeq.name});
                this.lsaaPosition = new TextBox({id: 'lsaa_position'});
                this.orientation = new Select({
                    name: "orientation-select",
                    width: "75px",
                    options: [
                        { label: "Forward", value:  1 },
                        { label: "Reverse", value: -1 }
                    ]
                });

                this.description = new TextBox({id: 'lsaa_description'});
                this.sequencedata = dom.create('textarea', { style: { height: '60px', width: '100%' }, id: 'sequencedata' });
                this.error = dom.create('div', { 'id': 'error', 'class': 'errormsg' });
                this.coordinateFormat = "one_based";
                this.user = JSON.parse(window.parent.getCurrentUser());
                var br = function() { return dom.create('br'); };

                this.set('content', [
                    dom.create('label', { 'for': 'lsaa_name', innerHTML: 'Reference sequence: ' }), this.sequence.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_position', innerHTML: 'Position: ' }), this.lsaaPosition.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_orientation', innerHTML: 'Orientation: ' }), this.orientation.domNode, br(),
                    dom.create('label', { 'for': 'lsaa_description', innerHTML: 'Description: ' }), this.description.domNode, br(),
                    dom.create('label', { 'for': 'sequencedata', innerHTML: 'Sequence data: ' }), this.sequencedata, br(),
                    this.error, br()
                ]);

                this.inherited(arguments);
            },

            hide: function() {
                this.inherited(arguments);
                window.setTimeout(dojo.hitch(this, 'destroyRecursive'), 500);
            }

        })
    }
);