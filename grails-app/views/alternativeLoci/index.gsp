<%@ page import="org.elsiklab.AlternativeLoci" %>
<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main">
        <title>LSAA - Alternative Loci</title>
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

        </style>
    </head>
    <body>
    <g:render template="../layouts/reportHeader"/>
    <div class="wide-container">
        <h3 class="header">Alternative loci</h3>
        <div id="list-availableStatus" class="content scaffold-list" role="main">

            <g:if test="${flash.message}">
                <div class="message" role="status">${flash.message}</div>
            </g:if>

            %{--Disabling create option--}%
            %{--<g:link action="create">Create</g:link>--}%
            <table>
                <thead>
                    <tr>
                        %{--<g:sortableColumn property="id" title="Edit"/>--}%
                        <g:sortableColumn property="created" title="Created" params="${filters}"/>
                        <g:sortableColumn property="owners" title="Owner" params="${filters}"/>
                        %{--<g:sortableColumn property="organism" title="Organism" params="${filters}"/>--}%
                        <g:sortableColumn property="location" title="Location" params="${filters}"/>
                        %{--<g:sortableColumn property="orientation" title="Orientation" params="${filters}"/>--}%
                        <g:sortableColumn property="lsaa_length" title="LSAA Length" params="${filters}"/>
                        <g:sortableColumn property="input_length" title="Input Length" params="${filters}"/>
                        <g:sortableColumn property="type" title="Type" params="${filters}"/>
                        <g:sortableColumn property="name" title="Name" params="${filters}"/>
                        <g:sortableColumn property="breed" title="Breed" params="${filters}"/>
                        <g:sortableColumn property="individual" title="Individual" params="${filters}"/>
                        <g:sortableColumn property="description" title="Description" params="${filters}"/>
                        <g:sortableColumn property="link" title="Link" params="${filters}"/>
                        <g:sortableColumn property="id" title="Delete"/>
                    </tr>
                </thead>
                <tbody>
                    <g:each in="${features}" status="i" var="feature">
                        <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                            %{--<td><g:link action="edit" id="${feature.id}">Edit</g:link></td>--}%
                            <td><g:formatDate format="E dd-MMM-yy" date="${feature.dateCreated}"/></td>
                            <td>${feature.owner?.username}</td>
                            %{--<td>${feature.featureLocation?.sequence?.organism?.commonName}</td>--}%
                            <td>${feature.featureLocation?.sequence?.name}:${feature.featureLocation.fmin + 1}..${feature.featureLocation.fmax}</td>
                            %{--<td>${(feature.orientation) == -1 ? 'Reverse' : 'Forward'}</td>--}%
                            <td>${(feature.featureLocation.fmax - feature.featureLocation.fmin)}</td>
                            <td>${feature.type in ["INVERSION", "DELETION"] ? (feature.endPosition - feature.startPosition) : (feature.endPosition - feature.startPosition) + 1}</td>
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
                            <td><g:link action="delete" id="${feature.id}">Delete</g:link></td>
                        </tr>
                    </g:each>
                </tbody>
            </table>
            <div class="pagination">
                <g:paginate total="${alternativeLociInstanceCount ?: 0}" />
            </div>
        </div>
        </div>
    </body>
</html>
