package org.eol.globi.data;

import org.eol.globi.domain.LogMessage;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;

import java.util.List;
import java.util.logging.Level;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NodeFactoryIT extends GraphDBTestCase {

    @Test
    public void createStudy() throws NodeFactoryException {
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("bla", "source", new DOI("1111", "j.1469-7998.1966.tb02907.x"), ExternalIdUtil.toCitation(null, "descr", null)));
        assertThat(study.getDOI().toURI().toString(), is("https://doi.org/10.1111/j.1469-7998.1966.tb02907.x"));
        assertThat(study.getCitation(), is("descr"));
    }

}
