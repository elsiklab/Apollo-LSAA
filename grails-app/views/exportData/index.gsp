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
        <h3 class="header">Export Scaffolds with LSAA</h3>
        
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
                          onchange="updateOrganism(this.value);resetValue()" />
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
                    <g:submitButton name="Submit"></g:submitButton>
                </g:form>
            </div>
        </div>
    <script>
        function updateOrganism(organismId) {
            jQuery.ajax({type:'POST',data:'organismId=' + organismId, url:'updateOrganism', success: function(data,textStatus){jQuery('#breedContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
            //jQuery.ajax({type:'POST',data:'organismId=' + organismId, url:'exportData/updateOrganismSequences', success: function(data,textStatus){jQuery('#sequenceContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }

        function updateSequences(options) {
            var selectedSequences = [];
            for (var i = 0; i < options.length; i++) {
                if (options[i].selected) {
                    selectedSequences.push(options[i].value);
                }
            }
            jQuery.ajax({type:'POST',data: 'selectedSequences=' + selectedSequences.join(','), url:'updateSequences', success: function(data,textStatus){jQuery('#breedContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }
        function updateBreed(breedId) {
            jQuery.ajax({type:'POST',data:'breedId=' + breedId, url:'updateBreed', success: function(data,textStatus){jQuery('#altLociContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }
        function updateAlternativeLoci(options) {
            var selectedAlternativeLoci = [];
            for (var i = 0; i < options.length; i++) {
                if (options[i].selected) {
                    selectedAlternativeLoci.push(options[i].value);
                }
            }
            jQuery.ajax({type:'POST',data:'selectedAlternativeLoci=' + selectedAlternativeLoci.join(','), url:'updateAlternativeLoci', success: function(data,textStatus){jQuery('#altLociContainer').html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
        }
        function resetValue() {
            jQuery('#breedContainer').html("N/A");
            jQuery('#altLociContainer').html("N/A");
        }
    </script>
    </body>
</html>
