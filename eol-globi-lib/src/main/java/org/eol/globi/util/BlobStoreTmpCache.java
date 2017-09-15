package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

public class BlobStoreTmpCache implements BlobStore {
    private final static Log LOG = LogFactory.getLog(BlobStoreTmpCache.class);

    @Override
    public InputStream asInputStream(URI resource, Class clazz) throws IOException {
        return asInputStream(resource.toString(), clazz);
    }

    @Override
    public InputStream asInputStream(String resource, Class clazz) throws IOException {
        try {
            InputStream is = null;
            if (isHttpURI(resource)) {
                LOG.info("caching of [" + resource + "] started...");
                is = getCachedRemoteInputStream(resource);
                LOG.info("caching of [" + resource + "] complete.");
            } else if (StringUtils.startsWith(resource, "file:/")) {
                is = new FileInputStream(new File(URI.create(resource)));
            } else if (StringUtils.startsWith(resource, "jar:file:/")) {
                is = URI.create(resource).toURL().openStream();
            } else if (clazz != null) {
                String classpathResource = resource;
                if (StringUtils.startsWith(resource, "classpath:")) {
                    classpathResource = StringUtils.replace(resource, "classpath:", "");
                }
                is = clazz.getResourceAsStream(classpathResource);
            }
            if (is == null) {
                final URI uri = ResourceUtil.fromShapefileDir(resource);
                if (uri == null) {
                    throw new IOException("failed to open resource [" + resource + "]");
                } else {
                    is = new FileInputStream(new File(uri));
                }
            }
            if (StringUtils.endsWith(resource, ".gz")) {
                is = new GZIPInputStream(is);
            }
            return is;
        } catch (IOException ex) {
            throw new IOException("issue accessing [" + resource + "]", ex);
        }
    }

    @Override
    public URI getAbsoluteResourceURI(URI context, String resourceName) {
        URI resourceURI = URI.create(resourceName);
        return resourceURI.isAbsolute()
                ? resourceURI
                : absoluteURIFor(context, resourceName);
    }

    @Override
    public boolean resourceExists(URI descriptor) {
        boolean exists = false;
        try {
            if (isHttpURI(descriptor)) {
                HttpResponse resp = HttpUtil.getHttpClient().execute(new HttpHead(descriptor));
                exists = resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
            } else {
                InputStream input = asInputStream(descriptor, ResourceUtil.class);
                IOUtils.closeQuietly(input);
                exists = input != null;
            }
        } catch (IOException e) {
            //
        }
        return exists;
    }

    private  InputStream getCachedRemoteInputStream(String resource) throws IOException {
        URI resourceURI = URI.create(resource);
        HttpGet request = new HttpGet(resourceURI);
        try {
            HttpResponse response = HttpUtil.getHttpClient().execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            return cacheAndOpenStream(response.getEntity().getContent());
        } finally {
            request.releaseConnection();
        }

    }

    private InputStream cacheAndOpenStream(InputStream is) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(is, fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
        return new FileInputStream(tempFile);
    }

    private URI absoluteURIFor(URI context, String resourceName) {
        String resourceNameNoSlashPrefix = StringUtils.startsWith(resourceName, "/")
                ? StringUtils.substring(resourceName, 1)
                : resourceName;
        String contextString = context.toString();
        String contextNoSlashSuffix = StringUtils.endsWith(contextString, "/")
                ? StringUtils.substring(contextString, 0, contextString.length() - 1)
                : contextString;

        return URI.create(contextNoSlashSuffix + "/" + resourceNameNoSlashPrefix);
    }

    private boolean isHttpURI(String descriptor) {
        return StringUtils.startsWith(descriptor, "http:/")
                || StringUtils.startsWith(descriptor, "https:/");
    }

    private boolean isHttpURI(URI descriptor) {
        return isHttpURI(descriptor.toString());
    }
}