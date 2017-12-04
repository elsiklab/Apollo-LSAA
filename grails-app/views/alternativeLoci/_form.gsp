<%@ page import="org.elsiklab.AlternativeLoci" %>
<%@ page import="org.elsiklab.FastaFile" %>
<%@ page import="org.bbop.apollo.Sequence" %>

<div class="fieldcontain ${hasErrors(bean: alternativeLociInstance, field: 'value', 'error')} required">

    <label for="name">
        <g:message code="alternativeLoci.value.label" default="Name" />
        <span class="required-indicator">*</span>
    </label>
    <g:textField name="name" required="" value="${alternativeLociInstance?.name}"/>
    <br />
    <label for="type">
        <g:message code="alternativeLoci.value.label" default="Type" />
        <span class="required-indicator">*</span>
    </label>
    <g:field type="text" name="type" readonly="readonly" value="${alternativeLociInstance?.type}"/>
    <br />
    <label for="sequence">
        <g:message code="alternativeLoci.value.label" default="Sequence" />
        <span class="required-indicator">*</span>
    </label>
    <g:select name="sequence" required="" value="${alternativeLociInstance?.featureLocation?.sequence?.name}" from="${alternativeLociInstance?.featureLocation?.sequence}" optionValue="name" optionKey="id" />
    <br />
    <label for="start">
        <g:message code="alternativeLoci.value.label" default="Start" />
        <span class="required-indicator">*</span>
    </label>
    <g:textField name="start" required="" value="${alternativeLociInstance?.featureLocation?.fmin + 1}"/>
    <br />
    <label for="end">
        <g:message code="alternativeLoci.value.label" default="End" />
        <span class="required-indicator">*</span>
    </label>
    <g:textField name="end" required="" value="${alternativeLociInstance?.featureLocation?.fmax}"/>
    <br />
    <label for="orientation">
        <g:message code="alternativeLoci.value.label" default="Orientation" />
        <span class="required-indicator">*</span>
    </label>
    <g:textField name="orientation" required="" value="${alternativeLociInstance?.featureLocation?.strand}"/>
    <br />
    <g:unless test="${alternativeLociInstance?.type == 'INVERSION'}">
        <label for="fastaFile">
            <g:message code="alternativeLoci.value.label" default="Fasta file" />
        </label>
        <span class="required-indicator">*</span>
        <g:select name="fastaFile" required="" value="${alternativeLociInstance?.fastaFile?.originalName}" from="${alternativeLociInstance?.fastaFile}" optionKey="id" optionValue="originalName" />
        <br />
    </g:unless>
    <label for="startPosition">
        <g:message code="alternativeLoci.value.label" default="Start Position (within Fasta)" />
    </label>
    <g:textField name="startPosition" value="${alternativeLociInstance?.startPosition + 1}"/>
    <br />
    <label for="endPosition">
        <g:message code="alternativeLoci.value.label" default="End Position (within Fasta)" />
    </label>
    <g:textField name="endPosition" value="${alternativeLociInstance?.endPosition}"/>
    <br />
    <label for="description">
        <g:message code="alternativeLoci.value.label" default="Description" />
    </label>
    <g:textField name="description" value="${alternativeLociInstance?.description}"/>
    <br />
    <br />
</div>

