package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.InputStreamFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class DatasetRegistryGitHubArchive extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubArchive(InputStreamFactory factory) {
        super(factory);
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        try {
            String commitSha = GitHubUtil.lastCommitSHA(namespace, getInputStreamFactory());
            return GitHubUtil.getArchiveDataset(namespace, commitSha, getInputStreamFactory());
        } catch (URISyntaxException | IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

}
