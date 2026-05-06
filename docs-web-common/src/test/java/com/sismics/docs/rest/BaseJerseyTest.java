package com.sismics.docs.rest;

import com.sismics.docs.rest.util.ClientUtil;
import com.sismics.util.filter.HeaderBasedSecurityFilter;
import com.sismics.util.filter.RequestContextFilter;
import com.sismics.util.filter.TokenBasedSecurityFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.Before;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Base class of integration tests with Jersey.
 * 
 * @author jtremeaux
 */
public abstract class BaseJerseyTest extends JerseyTest {
    protected static final String FILE_APACHE_PPTX = "file/apache.pptx";
    protected static final String FILE_DOCUMENT_DOCX = "file/document.docx";
    protected static final String FILE_DOCUMENT_ODT = "file/document.odt";
    protected static final String FILE_DOCUMENT_TXT = "file/document.txt";
    protected static final String FILE_EINSTEIN_ROOSEVELT_LETTER_PNG = "file/Einstein-Roosevelt-letter.png";
    protected static final long FILE_EINSTEIN_ROOSEVELT_LETTER_PNG_SIZE = 292641L;
    protected static final String FILE_PIA_00452_JPG = "file/PIA00452.jpg";
    protected static final long FILE_PIA_00452_JPG_SIZE = 163510L;
    protected static final String FILE_VIDEO_WEBM = "file/video.webm";
    protected static final String FILE_WIKIPEDIA_PDF = "file/wikipedia.pdf";
    protected static final String FILE_WIKIPEDIA_ZIP = "file/wikipedia.zip";

    /**
     * Test HTTP server.
     */
    private HttpServer httpServer;
    
    /**
     * Utility class for the REST client.
     */
    protected ClientUtil clientUtil;

    /**
     * Test mail server.
     */
    private Wiser wiser;

    /**
     * Test SMTP server port.
     */
    private int smtpPort;

    public String adminToken() {
        return clientUtil.login("admin", "admin", false);
    }

    protected int getSmtpPort() {
        return smtpPort;
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }
    
    @Override
    protected Application configure() {
        String travisEnv = System.getenv("TRAVIS");
        if (!Objects.equals(travisEnv, "true")) {
            // Travis doesn't like big logs
            enable(TestProperties.LOG_TRAFFIC);
            enable(TestProperties.DUMP_ENTITY);
        }
        return new Application();
    }
    
    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("docs").build();
    }
    
    @Override
    @Before
    public void setUp() throws Exception {
        forceSet(TestProperties.CONTAINER_PORT, String.valueOf(findAvailablePort()));
        smtpPort = findAvailablePort();
        super.setUp();
        System.setProperty("docs.header_authentication", "true");

        clientUtil = new ClientUtil(target());

        httpServer = HttpServer.createSimpleServer(getClass().getResource("/").getFile(), "localhost", getPort());
        WebappContext context = new WebappContext("GrizzlyContext", "/docs");
        context.addListener("com.sismics.util.listener.IIOProviderContextListener");
        context.addFilter("requestContextFilter", RequestContextFilter.class)
                .addMappingForUrlPatterns(null, "/*");
        context.addFilter("tokenBasedSecurityFilter", TokenBasedSecurityFilter.class)
                .addMappingForUrlPatterns(null, "/*");
        context.addFilter("headerBasedSecurityFilter", HeaderBasedSecurityFilter.class)
                .addMappingForUrlPatterns(null, "/*");
        ServletRegistration reg = context.addServlet("jerseyServlet", ServletContainer.class);
        reg.setInitParameter("jersey.config.server.provider.packages", "com.sismics.docs.rest.resource");
        reg.setInitParameter("jersey.config.server.provider.classnames", "org.glassfish.jersey.media.multipart.MultiPartFeature");
        reg.setInitParameter("jersey.config.server.response.setStatusOverSendError", "true");
        reg.setLoadOnStartup(1);
        reg.addMapping("/*");
        reg.setAsyncSupported(true);
        context.deploy(httpServer);
        httpServer.start();

        wiser = new Wiser();
        wiser.setPort(smtpPort);
        wiser.start();
    }

    /**
     * Extract an email from the list and consume it.
     *
     * @return Email content
     * @throws MessagingException e
     * @throws IOException e
     */
    protected String popEmail() throws MessagingException, IOException {
        List<WiserMessage> wiserMessageList = wiser.getMessages();
        long timeout = System.currentTimeMillis() + 5000L;
        while (wiserMessageList.isEmpty() && System.currentTimeMillis() < timeout) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            wiserMessageList = wiser.getMessages();
        }
        if (wiserMessageList.isEmpty()) {
            return null;
        }
        WiserMessage wiserMessage = wiserMessageList.get(wiserMessageList.size() - 1);
        wiserMessageList.remove(wiserMessageList.size() - 1);
        MimeMessage message = wiserMessage.getMimeMessage();
        return getMessageContent(message);
    }

    private String getMessageContent(Part part) throws MessagingException, IOException {
        Object content = part.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                contentBuilder.append(getMessageContent(multipart.getBodyPart(i)));
            }
            return contentBuilder.toString();
        }
        return "";
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            if (wiser != null) {
                wiser.stop();
                wiser = null;
            }
            if (httpServer != null) {
                httpServer.shutdownNow();
                httpServer = null;
            }
        } finally {
            super.tearDown();
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName("localhost"))) {
            serverSocket.setReuseAddress(true);
            return serverSocket.getLocalPort();
        }
    }
}
