<%@ page import="org.elsiklab.AlternativeLoci" %>
<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main">
        <title>LSAA - Alternative Loci</title>
        <style>
        .container {
            margin-left: 20px;
        }
        .header {
            padding: 20px;
        }
        </style>
    </head>
    <body>
    <g:render template="../layouts/reportHeader"/>
    <div class="container">
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
                        <g:sortableColumn property="id" title="Delete"/>
                        <g:sortableColumn property="id" title="Edit"/>
                        <g:sortableColumn property="lastUpdated" title="Last updated" params="${filters}"/>
                        <g:sortableColumn property="organism" title="Organism" params="${filters}"/>
                        <g:sortableColumn property="location" title="Location" params="${filters}"/>
                        <g:sortableColumn property="owners" title="Owner" params="${filters}"/>
                        <g:sortableColumn property="cvterm" title="CV Term" params="${filters}"/>
                        %{--<g:sortableColumn property="sizeof_ref" title="sizeof REF" params="${filters}"/>--}%
                        %{--<g:sortableColumn property="orientation_ref" title="orient. REF" params="${filters}"/>--}%
                        %{--<g:sortableColumn property="sizeof_lsaa" title="sizeof LSAA" params="${filters}"/>--}%
                        %{--<g:sortableColumn property="orientation_lsaa" title="orient. LSAA" params="${filters}"/>--}%
                        <g:sortableColumn property="description" title="Description" params="${filters}"/>
                        <g:sortableColumn property="reversed" title="Reversed" params="${filters}"/>
                        <g:sortableColumn property="link" title="Link" params="${filters}"/>
                    </tr>
                </thead>
                <tbody>
                    <g:each in="${features}" status="i" var="feature">
                        <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                            <td><g:link action="delete" id="${feature.id}">Delete</g:link></td>
                            <td><g:link action="edit" id="${feature.id}">Edit</g:link></td>
                            <td><g:formatDate format="E dd-MMM-yy" date="${feature.lastUpdated}"/></td>
                            <td>${feature.featureLocation?.sequence?.organism?.commonName}</td>
                            <td>${feature.featureLocation?.sequence?.name}:${feature.featureLocation.fmin + 1}..${feature.featureLocation.fmax}</td>
                            %{--<td>${feature.name_file}:${feature.start_file}..${feature.end_file}</td>--}%
                            <td>${feature.owner?.username}</td>
                            <td>${feature.cvTerm}</td>
                            %{--<td>${feature.featureLocation.fmax - feature.featureLocation.fmin}</td>--}%
                            %{--<td>${feature.featureLocation.strand}</td>--}%
                            %{--<td>${feature.end_file - feature.start_file + 1}</td>--}%
                            %{--<td>+</td>--}%
                            <td>${feature.description}</td>
                            <td>${feature.reversed}</td>
                            <td>
                                <g:if env="development">
                                    <a href="${g.createLink(relativeUri: + feature.featureLocation.sequence.organism.id + '/jbrowse/?loc=' + feature.featureLocation?.sequence?.name + ':' + (feature.featureLocation.fmin + 1) + '..' + feature.featureLocation.fmax)}&tracks=LSAA_annotations">JBrowse Link</a>
                                </g:if>
                                <g:if env="production">
                                    <a href="${g.createLink(absolute:true, uri: '/' + feature.featureLocation.sequence.organism.commonName+'/jbrowse/?loc=' + feature.name + '&organism='+feature.featureLocation.sequence.organism.id)}&tracks=LSAA_annotations">JBrowse Link</a>
                                </g:if>
                            </td>
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
