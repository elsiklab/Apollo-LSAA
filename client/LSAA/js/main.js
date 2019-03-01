define([
    'dojo/_base/declare',
    'dojo/_base/lang',
    'JBrowse/Plugin',
    'dijit/MenuItem',
    'dijit/MenuSeparator',
    'LSAA/View/Dialog/Correction',
    'LSAA/View/Dialog/Inversion',
    'LSAA/View/Dialog/Insertion',
    'LSAA/View/Dialog/Deletion',
    'LSAA/View/Dialog/SequenceSearch'
],
function(
    declare,
    lang,
    JBrowsePlugin,
    MenuItem,
    MenuSeparator,
    LsaaCorrectionDialog,
    LsaaInversionDialog,
    LsaaInsertionDialog,
    LsaaDeletionDialog,
    SequenceSearchDialog
) {
    return declare(JBrowsePlugin, {
        constructor: function(args) {
            console.log('LSAA plugin starting');
            var browser = args.browser;
            var thisB = this;
            this.contextPath = browser.config.contextPath || '..';

            browser.afterMilestone('initView', function() {
                browser.renderGlobalMenu('lsaa', { text: 'LSAA' }, browser.menuBar);

                // Insertion
                browser.addGlobalMenuItem('lsaa', new MenuItem({
                    label: 'Annotate Insertion',
                    iconClass: 'dijitEditorIcon dijitEditorIconRedo',
                    onClick: function() {
                        new LsaaInsertionDialog({ browser: thisB.browser, contextPath: thisB.contextPath }).show();
                    }
                }));

                // Deletion
                browser.addGlobalMenuItem('lsaa', new MenuItem({
                    label: 'Annotate Deletion',
                    iconClass: 'dijitIconCut',
                    onClick: function() {
                        new LsaaDeletionDialog({ browser: thisB.browser, contextPath: thisB.contextPath }).show();
                    }
                }));

                // Inversion
                browser.addGlobalMenuItem('lsaa', new MenuItem({
                    label: 'Annotate Inversion',
                    iconClass: 'dijitIconUndo',
                    onClick: function() {
                        new LsaaInversionDialog({ browser: thisB.browser, contextPath: thisB.contextPath }).show();
                    }
                }));

                // Correction
                browser.addGlobalMenuItem('lsaa', new MenuItem({
                    label: 'Annotate Other Changes',
                    iconClass: 'dijitIconEdit',
                    onClick: function() {
                        new LsaaCorrectionDialog({ browser: thisB.browser, contextPath: thisB.contextPath }).show();
                    }
                }));

                // browser.addGlobalMenuItem('lsaa', new MenuItem({
                //     label: 'Search sequence',
                //     iconClass: 'dijitIconSearch',
                //     onClick: function() {
                //         new SequenceSearchDialog({
                //             browser: thisB.browser,
                //             contextPath: thisB.contextPath,
                //             refseq: thisB.browser.refSeq.name,
                //             successCallback: function(id, fmin, fmax) {
                //                 console.log('here');
                //                 var locobj = {
                //                     ref: id,
                //                     start: fmin,
                //                     end: fmax
                //                 };
                //                 var highlightSearchedRegions = thisB.browser.config.highlightSearchedRegions;
                //                 thisB.browser.config.highlightSearchedRegions = true;
                //                 thisB.browser.showRegionWithHighlight(locobj);
                //                 thisB.browser.config.highlightSearchedRegions = highlightSearchedRegions;
                //             },
                //             errorCallback: function(response) {
                //                 console.error(response);
                //             }
                //         }).show();
                //     }
                // }));
                
                // browser.addGlobalMenuItem('lsaa', new MenuItem({
                //     label: 'View report',
                //     iconClass: 'dijitIconTable',
                //     onClick: function() {
                //         window.open(thisB.contextPath + '/../alternativeLoci' + '?organismId=' + browser.config.dataset_id);
                //     }
                // }));
                browser.addGlobalMenuItem('lsaa', new MenuItem({
                    label: 'Export LSAA',
                    iconClass: 'dijitIconSave',
                    onClick: function() {
                        window.open(thisB.contextPath + '/../exportData' + '?organismId=' + browser.config.dataset_id);
                    }
                }));
            });
        },

        getClientToken: function () {
            if (this.runningApollo()) {
                return this.getApollo().getClientToken();
            }
            else{
                var returnItem = window.sessionStorage.getItem("clientToken");
                if (!returnItem) {
                    var randomNumber = this.generateRandomNumber(20);
                    window.sessionStorage.setItem("clientToken", randomNumber);
                }
                return window.sessionStorage.getItem("clientToken");
            }
        }
    });
});
