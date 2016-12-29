package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.join;

public class DatasetImpl implements Dataset {

    private String namespace;
    private URI archiveURI;
    private JsonNode config;
    private URI configURI;

    public DatasetImpl(String namespace, URI archiveURI) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
    }

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        return ResourceUtil.asInputStream(getResourceURI(resourceName), DatasetImpl.class);
    }

    @Override
    public URI getResourceURI(String resourceName) {
        return ResourceUtil.getAbsoluteResourceURI(getArchiveURI(), resourceName);
    }

    @Override
    public URI getArchiveURI() {
        return archiveURI;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setConfig(JsonNode node) {
        this.config = node;
    }

    @Override
    public JsonNode getConfig() {
        return config;
    }

    @Override
    public String getCitation() {
        String citation = getOrDefault("dcterms:bibliographicCitation", getOrDefault("citation", getArchiveURI().toString()));

        if (!StringUtils.contains(citation, "doi.org") && !StringUtils.contains(citation, "doi:")) {
            List<String> citationParts = Arrays.asList(StringUtils.trim(defaultString(citation)),
                    defaultString(getDOI()));
            citation = join(citationParts, ". ");
        }
        return citation;
    }

    @Override
    public String getFormat() {
        return getOrDefault("format", "globi");
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return DatasetUtil.getValueOrDefault(getConfig(), key, defaultValue);
    }

    @Override
    public String getDOI() {
        return getOrDefault("doi", "");
    }

    public void setConfigURI(URI configURI) {
        this.configURI = configURI;
    }

    @Override
    public URI getConfigURI() {
        return configURI;
    }
}
