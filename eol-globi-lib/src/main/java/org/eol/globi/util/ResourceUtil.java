package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
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
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class ResourceUtil {

    public static final String SHAPEFILES_DIR = "shapefiles.dir";
    private final static Log LOG = LogFactory.getLog(ResourceUtil.class);

    public static URI fromShapefileDir(String shapeFile) {
        URI resourceURI = null;
        String shapeFileDir = System.getProperty(SHAPEFILES_DIR);
        if (StringUtils.isNotBlank(shapeFileDir)) {
            File file = new File(shapeFileDir + shapeFile);
            resourceURI = file.toURI();
        }
        return resourceURI;
    }

    public static InputStream asInputStream(String resource) throws IOException {
        return asInputStream(resource, inStream -> inStream);
    }

    public static InputStream asInputStream(String resource, InputStreamFactory factory) throws IOException {
        try {
            InputStream is;
            if (isHttpURI(resource)) {
                LOG.info("caching of [" + resource + "] started...");
                is = getCachedRemoteInputStream(resource, factory);
                LOG.info("caching of [" + resource + "] complete.");
            } else if (StringUtils.startsWith(resource, "file:/")) {
                is = factory.create(new FileInputStream(new File(URI.create(resource))));
            } else if (StringUtils.startsWith(resource, "jar:file:/")) {
                URL url = URI.create(resource).toURL();
                URLConnection urlConnection = url.openConnection();
                // Prevent leaking of jar file descriptors by disabling jar cache.
                // see https://stackoverflow.com/a/36518430
                urlConnection.setUseCaches(false);
                is = factory.create(urlConnection.getInputStream());
            } else if (StringUtils.startsWith(resource, "ftp:/")) {
                URI uri = URI.create(resource);
                FTPClient ftpClient = new FTPClient();
                try {
                    ftpClient.connect(uri.getHost());
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.login("anonymous", "info@globalbioticinteractions.org");
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);
                    ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

                    is = ftpClient.isConnected()
                            ? cacheAndOpenStream(ftpClient.retrieveFileStream(uri.getPath()), factory)
                            : null;
                } finally {
                    if (ftpClient.isConnected()) {
                        ftpClient.disconnect();
                    }
                }
            } else {
                String classpathResource = resource;
                if (StringUtils.startsWith(resource, "classpath:")) {
                    classpathResource = StringUtils.replace(resource, "classpath:", "");
                }
                is = factory.create(ResourceUtil.class.getResourceAsStream(classpathResource));
            }

            if (is == null) {
                final URI uri = fromShapefileDir(resource);
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

    public static URI getAbsoluteResourceURI(URI context, URI resourceName) {
        return resourceName.isAbsolute()
                ? resourceName
                : absoluteURIFor(context, resourceName);
    }

    static boolean resourceExists(URI descriptor) {
        return resourceExists(descriptor, inStream -> inStream);
    }

    public static boolean resourceExists(URI descriptor, InputStreamFactory factory) {
        boolean exists = false;
        if (null != descriptor) {
            try {
                if (isHttpURI(descriptor)) {
                    HttpResponse resp = HttpUtil.getHttpClient().execute(new HttpHead(descriptor));
                    exists = resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
                } else {
                    try (InputStream input = asInputStream(descriptor.toString(), factory)) {
                        exists = input != null;
                    }
                }
            } catch (IOException e) {
                //
            }
        }
        return exists;
    }

    private static InputStream getCachedRemoteInputStream(String resource, InputStreamFactory factory) throws IOException {
        URI resourceURI = URI.create(resource);
        HttpGet request = new HttpGet(resourceURI);
        try {
            HttpResponse response = HttpUtil.getHttpClient().execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            try (InputStream content = response.getEntity().getContent()) {
                return cacheAndOpenStream(content, factory);
            }
        } finally {
            request.releaseConnection();
        }

    }

    private static InputStream cacheAndOpenStream(InputStream is, InputStreamFactory factory) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            IOUtils.copy(factory.create(is), fos);
            fos.flush();
        }
        return new FileInputStream(tempFile);
    }

    private static URI absoluteURIFor(URI context, URI resourceName) {
        String resourceNameNoSlashPrefix = StringUtils.startsWith(resourceName.toString(), "/")
                ? StringUtils.substring(resourceName.toString(), 1)
                : resourceName.toString();
        String contextString = context.toString();
        String contextNoSlashSuffix = StringUtils.endsWith(contextString, "/")
                ? StringUtils.substring(contextString, 0, contextString.length() - 1)
                : contextString;

        return URI.create(contextNoSlashSuffix + "/" + resourceNameNoSlashPrefix);
    }

    private static boolean isHttpURI(String descriptor) {
        return StringUtils.startsWith(descriptor, "http:/")
                || StringUtils.startsWith(descriptor, "https:/");
    }

    private static boolean isHttpURI(URI descriptor) {
        return isHttpURI(descriptor.toString());
    }
}
