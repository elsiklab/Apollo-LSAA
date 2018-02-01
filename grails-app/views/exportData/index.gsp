<%@ page import="org.elsiklab.AlternativeLoci" %>
<%@ page import="org.bbop.apollo.Organism" %>
<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main">
        <title>LSAA - Export scaffolds</title>
        <style>
        .scaffoldEditor {
            width: 100%;
            height: 400px
        }
        .container {
            margin-left: 20px;
        }
        .header {
            padding: 20px;
        }
        .left {
            flex: 1;
        }
        .right {
            flex: 1;
        }
        </style>
        <g:javascript library='jquery' />
    </head>
    <body>
    <g:render template="../layouts/reportHeader"/>
        <h3 class="header">Export with LSAA</h3>
        
        <div class="container">
            <g:if test="${error}">
                <div class="message" role="status">${error}</div>
            </g:if>
            <br/>
            <div class="left">
                <b>Organism:</b>
                <g:select id="organism" name="organism.commonName" from="${Organism.listOrderByCommonName()}"
                          optionKey="id"
                          optionValue="commonName"
                          noSelection="[null:'Select an organism']"
                          onchange="updateOrganism(this.value)" />
            </div>
            <br/>
            <br/>
            <div>
                <b>Category:</b>
                <span id="categoryContainer"></span>
            </div>
            <br/>
            <br/>
            <div>
                <b>Breed:</b>
                <span id="breedContainer"></span>
            </div>
            <br/>
            %{--<div>--}%
                %{--<b>Sequence:</b>--}%
                %{--<span id="sequenceContainer">N/A</span>--}%
            %{--</div>--}%
            <br/>
            <div>
                <b>Alternative Loci:</b>
                <span id="altLociContainer">N/A</span>
            </div>
            <br/>
            <div class="left">
                <g:form action="export">
                    <select name="type">
                        <option>JSON</option>
                        <option>FASTA</option>
                    </select>
                    <select name="download">
                        <option value="text">View</option>
                        <option value="download">Download</option>
                    </select>
                    <div>
                        <br/>
                        <span><b>Export Scaffolds with LSAA: </b></span><span><g:actionSubmit value="Submit" action="exportSequences"/></span>
                    </div>
                    %{--<div>--}%
                        %{--<br/>--}%
                        %{--<span><b>Export Entire Genome with LSAA: </b></span><g:actionSubmit value="Submit" action="exportGenome"/>--}%
                    %{--</div>--}%
                </g:form>
            </div>
        </div>
    <script>

        function updateOrganism(organismId) {
            if (organismId == 'null') {
                jQuery("#categoryContainer").html("N/A");
                resetValue();
            }
            else {
                jQuery.ajax({type:'POST',data:'organismId=' + organismId, url:'updateOrganism', success: function(data,textStatus){jQuery('#categoryContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
                //jQuery.ajax({type:'POST',data:'organismId=' + organismId, url:'updateOrganismSequences', success: function(data,textStatus){jQuery('#sequenceContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
            }
        }

        function updateSequences(options) {
            var selectedSequences = [];
            for (var i = 0; i < options.length; i++) {
                if (options[i].selected) {
                    selectedSequences.push(options[i].value);
                }
            }
            jQuery.ajax({type:'POST',data: 'selectedSequences=' + selectedSequences.join(','), url:'exportData/updateSequences', success: function(data,textStatus){jQuery('#breedContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }

        function updateCategory(category) {
            if (category == "null") {
                resetValue();
            }
            else if (category == "Structural Variation") {
                jQuery.ajax({type:'POST',data: 'category=' + category, url:'updateCategory', success: function(data,textStatus){jQuery('#breedContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
                resetValue();
            }
            else {
                jQuery.ajax({
                    type:'POST',
                    data: 'category=' + category,
                    url:'updateCategory',
                    success: function(data,textStatus){
                        jQuery('#altLociContainer').html(data.replace('<html>','').replace('</html>',''));
                    },
                    error: function(XMLHttpRequest,textStatus,errorThrown){

                    }
                });
                jQuery("#breedContainer").html("N/A");
            }

        }

        function updateBreed(breedId) {
            jQuery.ajax({type:'POST',data:'breedId=' + breedId, url:'exportData/updateBreed', success: function(data,textStatus){jQuery('#altLociContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }
        function updateAlternativeLoci(options) {
            var selectedAlternativeLoci = [];
            for (var i = 0; i < options.length; i++) {
                if (options[i].selected) {
                    selectedAlternativeLoci.push(options[i].value);
                }
            }
            jQuery.ajax({type:'POST',data:'selectedAlternativeLoci=' + selectedAlternativeLoci.join(','), url:'exportData/updateAlternativeLoci', success: function(data,textStatus){jQuery('#altLociContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }
        function resetValue() {
            jQuery('#breedContainer').html("N/A");
            jQuery('#altLociContainer').html("N/A");
        }

        function updateSelection(value) {
            console.log("updateSelection: " , value);
            jQuery.ajax({type:'POST',data:'selectedAlternativeLoci=' + value, url:'updateSelectionForExport'});
        }

    </script>
    </body>
</html>
