<%@ page import="org.elsiklab.AlternativeLoci" %>
<%@ page import="org.bbop.apollo.Organism" %>
<%@ page import="org.elsiklab.Breed" %>
<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main">
        <title>LSAA - Export scaffolds</title>
        <style>
            .wide-container {
                margin-left: 20px;
                width: 100%;
            }
            .header {
                padding: 20px;
            }
            td, th {
                padding: 0.5em 0.6em;
            }
            #list-availableStatus {
                overflow: auto;
            }
            .controls {
                padding: 20px 10px;
                border: 1px solid grey;
                border-radius: 15px;
                margin-bottom: 20px;
            }
        </style>

    </head>
    <body>
    <g:render template="../layouts/reportHeader"/>
        <h3 class="header">Export with LSAA</h3>
        <div id="list-availableStatus" class="content scaffold-list" role="main">
            <div class="wide-container">
                <g:if test="${error}">
                    <div class="message" role="status">${error}</div>
                </g:if>
                %{--
                <div>${params}</div>
                <div>${features}</div>
                <div>${features*.breed?.unique()}</div>
                --}%
                <div class="controls">
                    <h3>Filter Table</h3>
                    <g:form method="get" controller="exportData">
                        <label>Organism</label>
                        <g:select  id="organism" 
                                    value="${params.organismId}"
                                    name="organismId" 
                                    from="${features*.featureLocation?.sequence?.organism?.unique()}"
                                    optionValue="commonName"
                                    optionKey="id"
                                    />
                        <label>Type</label>
                        <g:select  id="type" 
                                    value="${params.type}"
                                    name="type" 
                                    from="${['INSERTION', 'INVERSION', 'DELETION']}"
                                    multiple="multiple"
                                    noSelection="${['all':'All']}"
                                    />

                        <label>Breed</label>
                         <g:select  id="breed" 
                                    value="${params.breed}"
                                    name="breed" 
                                    from="${features*.breed.unique() - null}"
                                    optionValue="nameAndIdentifier"
                                    optionKey="identifier"
                                    noSelection="${['all':'All']}"
                                    /> 

                        <label>Individual</label>
                         <g:select  id="individual" 
                                    value="${params.individual}"
                                    name="individual" 
                                    from="${features*.individual.unique() - null}"
                                    multiple="multiple"
                                    noSelection="${['all':'All']}"
                                    /> 

                        <label>Owner</label>
                         <g:select  id="owner" 
                                    value="${params.owner}"
                                    name="owner" 
                                    from="${features*.owner?.username.unique()}"
                                    multiple="multiple"
                                    noSelection="${['all':'All']}"
                                    /> 

                      <!--   
                      %{--
                        <label>Chromosome Id</label>
                         <g:select  id="chromosome" 
                                    value="${params.chromosome}"
                                    name="chromosome" 
                                    from="${features*.ontologyId?.unique()}"
                                    noSelection="[ 'ALL' : 'ALL' ]"
                                    /> 

                        --}% 
                        -->
                        <g:actionSubmit value="Filter" action="index" />
                    </g:form>

                </div>
                
                <g:form action="export">
                    <div>
                        <h3>Export Table</h3>

                            <label>Format</label>
                            <g:select id="data-format" name="data-format" from="['FASTA', 'JSON']" />

                            <label>Action</label>
                            <g:select id="action" name="action" from="['View', 'Download']" />

                            <g:actionSubmit id="export-data" value="Export Scaffolds" action="exportSequences" disabled="disabled"/>

                            <g:hiddenField value="${params.organismId}" name="organismId" />

                            <g:hiddenField value="${params.type}" name="type" />

                            <g:hiddenField value="${params.breed}" name="breed"/> 

                            <g:hiddenField value="${params.individual}" name="individual" /> 
                            
                            <g:hiddenField value="${params.owner}" name="owner" /> 

                            %{--<div>--}%
                                %{--<br/>--}%
                                %{--<span><b>Export Entire Genome with LSAA: </b></span>
                                <g:actionSubmit value="Submit" action="exportGenome"/>--}%
                            %{--</div>--}%
                    </div>
                    <br/>

                    <table>
                        <thead>
                            <tr>
                                %{--<g:sortableColumn property="id" title="Edit"/>--}%
                                %{--<g:sortableColumn property="selection" title="Selected" params="${filters}"/>--}%
                                <th>Selected</th>
                                <g:sortableColumn property="created" title="Created" params="${params}"/>
                                <g:sortableColumn property="owners" title="Owner" params="${params}"/>
                                %{--<g:sortableColumn property="organismId" title="Organism" params="${params}"/>--}%
                                <th>Organism</th>
                                <g:sortableColumn property="location" title="Location" params="${params}"/>
                                %{--<g:sortableColumn property="orientation" title="Orientation" params="${params}"/>--}%
                                <g:sortableColumn property="lsaa_length" title="LSAA Length" params="${params}"/>
                                <g:sortableColumn property="input_length" title="Input Length" params="${params}"/>
                                <g:sortableColumn property="type" title="Type" params="${params}"/>
                                <g:sortableColumn property="name" title="Name" params="${params}"/>
                                <g:sortableColumn property="breed" title="Breed" params="${params}"/>
                                <g:sortableColumn property="individual" title="Individual" params="${params}"/>
                                <g:sortableColumn property="description" title="Description" params="${params}"/>
                                <g:sortableColumn property="link" title="Link" params="${params}"/>
                            </tr>
                        </thead>
                        <tbody>
                            <g:each in="${features}" status="i" var="feature">
                                <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                                    %{--<td><g:link action="edit" id="${feature.id}">Edit</g:link></td>--}%
                                    <td>
                                        %{--<g:checkBox name="${feature.id}" value="false"/>--}%
                                        %{--<g:checkBox name="${feature.name}" value="${false}" />--}%
                                        <input type="checkbox" 
                                            name="selection" 
                                            value="${feature.uniqueName}" 
                                            data-sequence="${feature.featureLocation?.sequence?.name}"
                                            data-location="[${feature.endPosition},${feature.startPosition}]"
                                            data-breed="${feature.breed?.nameAndIdentifier}"
                                            class="selector"/>
                                    </td>
                                    <td><g:formatDate format="E dd-MMM-yy" date="${feature.dateCreated}"/></td>
                                    <td>${feature.owner?.username}</td>
                                    <td>${feature.featureLocation?.sequence?.organism?.commonName}</td>
                                    <td>${feature.featureLocation?.sequence?.name}:${feature.featureLocation.fmin + 1}..${feature.featureLocation.fmax}</td>
                                    %{--<td>${(feature.orientation) == -1 ? 'Reverse' : 'Forward'}</td>--}%
                                    <td>${(feature.featureLocation.fmax - feature.featureLocation.fmin)}</td>
                                    <td>
                                        ${feature.type in ["INVERSION", "DELETION"] ? (feature.endPosition - feature.startPosition) : (feature.endPosition - feature.startPosition) + 1}
                                    </td>
                                    <td>${feature.type}</td>
                                    <td>${feature.name}</td>
                                    <td>${feature.breed?.nameAndIdentifier}</td>
                                    <td>${feature.individual}</td>
                                    <td>${feature.description}</td>
                                    <td>
                                        <g:if env="development">
                                            <a href="${g.createLink(relativeUri: + feature.featureLocation.sequence.organism.id + '/jbrowse/?loc=' + feature.featureLocation?.sequence?.name + ':' + (feature.featureLocation.fmin + 1) + '..' + feature.featureLocation.fmax)}&tracks=LSAA_annotations">JBrowse Link</a>
                                        </g:if>
                                        <g:if env="production">
                                            <a href="${g.createLink(absolute:true, uri: '/' + feature.featureLocation.sequence.organism.id +'/jbrowse/?loc=' + feature.featureLocation?.sequence?.name + ':' + (feature.featureLocation.fmin + 1) + '..' + feature.featureLocation.fmax)}&tracks=LSAA_annotations">JBrowse Link</a>
                                        </g:if>
                                    </td>
                                </tr>
                            </g:each>
                        </tbody>
                    </table>

                 </g:form>
            </div>
        </div>
    <script
  src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
  integrity="sha256-3edrmyuQ0w65f8gfBsqowzjJe2iM6n0nKciPUp8y+7E="
  crossorigin="anonymous"></script>
    <script>

        $(document).ready(function(){
            // uncheck all selected

            $('input[type=checkbox].selector').prop('checked', false)

            var enabled = false

            var sequences = {}

            $('input[type=checkbox].selector').on('click', function(event, selector){
                var seq = $(this).data('sequence');
                var loc = $(this).data('location');

                // make sure we're in lower to upper ranges
                if(loc[0] > loc[1]){
                    var tmp = loc[0]
                    loc[0] = loc[1]
                    loc[1] = tmp
                }

                if(sequences.hasOwnProperty(seq)){
                    var toAdd = true
                    for( var seq in sequences ){
                        //if the sequence is in another sequence
                        if(    loc[0] <= sequences[seq][0] 
                            && loc[1] >= sequences[seq][0]
                            && loc[1] <= sequences[seq][1]){
                            toAdd = false
                        }
                        //if the sequence is in another sequence
                        if(    loc[0] >= sequences[seq][0] 
                            && loc[0] <= sequences[seq][0]
                            && loc[1] >= sequences[seq][1]){
                            toAdd = false
                        }
                        // or a selected sequence is in this sequence
                        if(    loc[0] >= sequences[seq][0] 
                            && loc[1] <= sequences[seq][1]){
                            toAdd = false
                        }
                        // or a selected sequence is in this sequence
                        if(    loc[0] <= sequences[seq][0] 
                            && loc[1] >= sequences[seq][1]){
                            toAdd = false
                        }
                    }

                    if(!toAdd){
                        alert("Cannot add a sequence that intersects with another sequence")
                        console.log("Cannot add a sequence that intersects with another sequence")
                        $(selector).prop('checked', false)
                    }


                    // console.log($('input[type=checkbox].selector:checked'), $('input[type=checkbox].selector:checked').length, checked)
                    

                } else {
                    sequences[seq] = [loc]
                }

                $(selector).prop('checked', !($(selector).prop('checked')) );

                var checked = $('input[type=checkbox].selector:checked').length > 0;
                $('#export-data').prop('disabled', !checked)

            })

        })
        /*
                loc = [1, 20]
        sequences = { bob: [11, 12] }
        var toAdd = true
        for( seq in sequences ){

            //if the sequence is in another sequence
            if(    loc[0] <= sequences[seq][0] 
                && loc[1] >= sequences[seq][0]
                && loc[1] <= sequences[seq][1]){
                toAdd = false
            }
            //if the sequence is in another sequence
            if(    loc[0] >= sequences[seq][0] 
                && loc[0] <= sequences[seq][0]
                && loc[1] >= sequences[seq][1]){
                toAdd = false
            }
            // or a selected sequence is in this sequence
            if(    loc[0] >= sequences[seq][0] 
                && loc[1] <= sequences[seq][1]){
                toAdd = false
            }
          
              // or a selected sequence is in this sequence
            if(    loc[0] <= sequences[seq][0] 
                && loc[1] >= sequences[seq][1]){
                toAdd = false
            }

        }

        if(!toAdd){
            console.log("Cannot add a sequence that intersects with another sequence")
        }*/


    </script>
    </body>
</html>
