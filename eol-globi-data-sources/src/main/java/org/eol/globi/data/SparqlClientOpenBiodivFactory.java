package org.eol.globi.data;

import org.eol.globi.service.ResourceService;

public class SparqlClientOpenBiodivFactory implements SparqlClientFactory {

    @Override
    public SparqlClient create(ResourceService resourceService) {
        return new OpenBiodivClient(resourceService);
    }

}
