package start;

import filters.AuthorizationFilter;
import filters.CORSFilter;
import filters.TokenInvalidationFilter;
import filters.ValidateTokenFilter;
import messaging.outflow.SwitchAlgorithmApprove;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import service.servlet.*;
import services.*;
import services.fileservlets.Approveupload;
import services.fileservlets.UploadServlet;
import services.outflow.OutflowSwitchSequence;
import services.outflow.SwitchAlgApprove;
import services.outflow.SwitchAlgorithmController;
import services.outflow.SwitchAlgorithmDeactivate;
import services.performance.PerformanceServlet;
import services.performance.endpoints.ApproveEndpointServlet;
import services.performance.endpoints.Endpoint;
import services.serviceProvider.ApproveServiceServlet;
import services.serviceProvider.Service;
import services.switchController.ApproveSwitchServlet;
import services.switchController.Switch;
import services.transactions.*;
import services.transactions.PendingTransactionServlet;
import services.virtualaccount.ApproveVirtualAccountServlet;
import services.virtualaccount.VirtualAccount;
import util.Encrypter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class SwitchApplication {


    static final private Tomcat tomcat = new Tomcat();


    public static void start(String[] args) {

        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        getConfigProperties();

        try {


            System.out.println("Hostname: " +
                    host);

            String apiContext = "/" + System.getProperty("service.context");
            String contextPath = "";
            tomcat.setBaseDir(".");
            String appBase = ".";
            String basePath = null;
            try {
                basePath = new File(appBase).exists()
                        ? new File(appBase).getCanonicalPath()
                        : appBase;
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            String confBase = null;

            try {

                confBase = new File("../conf").getCanonicalPath();

            } catch (Exception e1) {

                e1.printStackTrace();

            }

            System.out.println("app.conf.home :: " + confBase);

            System.setProperty("app.conf.home", confBase);


            if ("https".equalsIgnoreCase(System.getProperty("transpt.scheme"))) {

                tomcat.getService().addConnector(getConnector());

            } else {
                tomcat.setPort(Integer.valueOf(System.getProperty("http.port") == null ? "9090" : System.getProperty("http.port")));
            }
            tomcat.getConnector();


            System.out.println(basePath);
            Context context = tomcat.addWebapp(contextPath, basePath);
            context.setAddWebinfClassesResources(false);
            FilterDef fd = new FilterDef();
            fd.setFilterClass(CORSFilter.class.getName());
            fd.setFilterName("CorsFilter");
            FilterMap fm = new FilterMap();
            fm.setFilterName("CorsFilter");
            fm.addURLPattern("/*");

            FilterDef tokenFilterDef = new FilterDef();
            tokenFilterDef.setFilterClass(ValidateTokenFilter.class.getName());
            tokenFilterDef.setFilterName("TokenFilter");
            FilterMap tokenFilterMap = new FilterMap();
            tokenFilterMap.setFilterName("TokenFilter");
            tokenFilterMap.addURLPattern("/switch/*");


            FilterDef authFilterDef = new FilterDef();
            authFilterDef.setFilterClass(AuthorizationFilter.class.getName());
            authFilterDef.setFilterName("authFilter");
            FilterMap authFilterMap = new FilterMap();
            authFilterMap.setFilterName("authFilter");
            authFilterMap.addURLPattern("/switch/*");

            FilterDef tokenInvalidationDef = new FilterDef();
            tokenInvalidationDef.setFilterClass(TokenInvalidationFilter.class.getName());
            tokenInvalidationDef.setFilterName("invalidationFilter");
            FilterMap tokenInvalidationMap = new FilterMap();
            tokenInvalidationMap.setFilterName("invalidationFilter");
            tokenInvalidationMap.addURLPattern("/switch/*");

            context.addFilterDef(fd);
            context.addFilterMap(fm);

            context.addFilterDef(tokenFilterDef);
            context.addFilterMap(tokenFilterMap);

            context.addFilterDef(authFilterDef);
            context.addFilterMap(authFilterMap);

            context.addFilterDef(tokenInvalidationDef);
            context.addFilterMap(tokenInvalidationMap);

//            MultipartConfigElement configElement = new MultipartConfigElement("C:/uploads", 10485760, 52428800, 10240);
            context.setAllowCasualMultipartParsing(true);

            Tomcat.addServlet(context, "userServlet", new UserManagementServlet());
            context.addServletMappingDecoded(apiContext + "/users", "userServlet");

            Tomcat.addServlet(context, "ApproveServiceServlet", new ApproveServiceServlet());
            context.addServletMappingDecoded(apiContext + "/approve-service", "ApproveServiceServlet");

            Tomcat.addServlet(context, "ServiceServlet", new Service());
            context.addServletMappingDecoded(apiContext + "/service-provider", "ServiceServlet");

            Tomcat.addServlet(context, "ApproveVirtualAccountServlet", new ApproveVirtualAccountServlet());
            context.addServletMappingDecoded(apiContext + "/approve-virtual-account", "ApproveVirtualAccountServlet");

            Tomcat.addServlet(context, "VirtualAccountServlet", new VirtualAccount());
            context.addServletMappingDecoded(apiContext + "/virtual-account", "VirtualAccountServlet");

            Tomcat.addServlet(context, "refreshServlet", new RefreshTokenController());
            context.addServletMappingDecoded( "/auth/refresh", "refreshServlet");

            Tomcat.addServlet(context, "authenticationServlet", new AuthenticationController());
            context.addServletMappingDecoded( "/auth", "authenticationServlet");

            Tomcat.addServlet(context, "logoutServlet", new AuthenticationController());
            context.addServletMappingDecoded( apiContext + "/auth/logout", "logoutServlet");

            Tomcat.addServlet(context, "getusersServlet", new GetUsers());
            context.addServletMappingDecoded(apiContext + "/getusers", "getusersServlet");

            Tomcat.addServlet(context, "getrolesServlet", new GetRoles());
            context.addServletMappingDecoded(apiContext + "/getroles", "getrolesServlet");

            Tomcat.addServlet(context, "updateuserrole", new UpdateUserRoleController());
            context.addServletMappingDecoded(apiContext + "/updateuserrole", "updateuserrole");

            Tomcat.addServlet(context, "messageServlet", new MessageServlet());
            context.addServletMappingDecoded(apiContext + "/message/update-read-status", "messageServlet");

            Tomcat.addServlet(context, "GetMessageServlet", new MessageServlet());
            context.addServletMappingDecoded(apiContext + "/message", "GetMessageServlet");

            Tomcat.addServlet(context, "SwitchServlet", new Switch());
            context.addServletMappingDecoded(apiContext + "/switch", "SwitchServlet");

            Tomcat.addServlet(context, "ApproveSwitchServlet", new ApproveSwitchServlet());
            context.addServletMappingDecoded(apiContext + "/approve-switch", "ApproveSwitchServlet");

            Tomcat.addServlet(context, "EndpointServlet", new Endpoint());
            context.addServletMappingDecoded(apiContext + "/endpoint", "EndpointServlet");

            Tomcat.addServlet(context, "ApproveEndpointServlet", new ApproveEndpointServlet());
            context.addServletMappingDecoded(apiContext + "/approve-endpoint", "ApproveEndpointServlet");

            Tomcat.addServlet(context, "PerformanceStatServlet", new PerformanceServlet());
            context.addServletMappingDecoded(apiContext + "/perf-stat", "PerformanceStatServlet");

            Tomcat.addServlet(context, "TransactionServlet", new TransactionServlet());
            context.addServletMappingDecoded(apiContext + "/transactions", "TransactionServlet");

            Tomcat.addServlet(context, "ApproveFileServlet", new Approveupload());
            context.addServletMappingDecoded(apiContext + "/uploadfile/approve", "ApproveFileServlet");

            Tomcat.addServlet(context, "FileServlet", new UploadServlet());
            context.addServletMappingDecoded(apiContext + "/uploadfile", "FileServlet");

            // Add request servlet
            Tomcat.addServlet(context, "apiRegistryServlet", new ApiRegistryServlet());
            context.addServletMappingDecoded(apiContext +"/apiRegistry/*", "apiRegistryServlet");

            Tomcat.addServlet(context, "switch_RegistryServlet", new SwitchRegistryServlet());
            context.addServletMappingDecoded(apiContext +"/switchregistry/*", "switch_RegistryServlet");

            Tomcat.addServlet(context, "cryptoConfigServlet", new CryptoConfigServlet());
            context.addServletMappingDecoded(apiContext +"/crypto_config/*", "cryptoConfigServlet");

            Tomcat.addServlet(context, "thirdPartyAddrServlet", new ThirdPartyAddrServlet());
            context.addServletMappingDecoded(apiContext +"/thirdPartyAddr/*", "thirdPartyAddrServlet");

            Tomcat.addServlet(context, "apiConfigServlet", new ApiConfigServlet());
            context.addServletMappingDecoded(apiContext +"/apiConfig/*", "apiConfigServlet");

            Tomcat.addServlet(context, "auditLogServlet", new AuditLogServlet());
            context.addServletMappingDecoded(apiContext +"/auditLog/*", "auditLogServlet");

            Tomcat.addServlet(context, "FailedRetrialServlet", new FailedRetrialServlet());//
            context.addServletMappingDecoded(apiContext + "/transaction/failed", "FailedRetrialServlet");

            Tomcat.addServlet(context, "PendingTransactionServlet", new PendingTransactionServlet());//
            context.addServletMappingDecoded(apiContext + "/transaction/pending", "PendingTransactionServlet");

            Tomcat.addServlet(context, "FetchPendingTransactionServlet", new services.PendingTransactionServlet());
            context.addServletMappingDecoded( apiContext +"/support/transaction", "FetchPendingTransactionServlet");

            Tomcat.addServlet(context, "BatchApprovalServlet", new BatchApprovalServlet());//
            context.addServletMappingDecoded(apiContext + "/transaction/approve", "BatchApprovalServlet");

            Tomcat.addServlet(context, "retrialController", new RetrialInterfaceController());
            context.addServletMappingDecoded( apiContext + "/transactions/create", "retrialController");

            Tomcat.addServlet(context, "switchAlgorithm", new SwitchAlgorithmController());
            context.addServletMappingDecoded( apiContext + "/outflow/algorithm", "switchAlgorithm");

            Tomcat.addServlet(context, "switchAlgorithmDeactivate", new SwitchAlgorithmDeactivate());
            context.addServletMappingDecoded( apiContext + "/outflow/algorithm/activate", "switchAlgorithmDeactivate");

            Tomcat.addServlet(context, "switchAlgorithmApprove", new SwitchAlgApprove());
            context.addServletMappingDecoded( apiContext + "/outflow/algorithm/approve", "switchAlgorithmApprove");

            Tomcat.addServlet(context, "switchSequence", new OutflowSwitchSequence());
            context.addServletMappingDecoded( apiContext + "/outflow/sequence", "switchSequence");

            Tomcat.addServlet(context, "CreateTimetableServlet", new CreateTimetableServlet());
            context.addServletMappingDecoded(apiContext + "/outflow/create/timetable", "CreateTimetableServlet");

            Tomcat.addServlet(context, "ApproveTimetableServlet", new ApproveTimetableServlet());
            context.addServletMappingDecoded(apiContext + "/outflow/time-table/approve", "ApproveTimetableServlet");

            StandardContext standardContext = (StandardContext) context;
            standardContext.setClearReferencesObjectStreamClassCaches(false);
            standardContext.setClearReferencesRmiTargets(false);
            //standardContext.setClearReferencesThreadLocals(false);


            //create default servlet
            createDefaultServlet(context);
            addErrorServlet(context);

            //connect to database
            addLocalDBesource(standardContext);
            tomcat.enableNaming();

            // need for proper destroying servlets
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(" Executing shutdown hook");
                    stop(null);
                    System.out.println(" Done with shutdown");
                }
            }));

            System.out.println("Starting cbs container ");

            tomcat.start();
            System.out.println("Done starting container ");
            System.out.println("Server running on " + host
                    + ":" + tomcat.getConnector().getPort());
            tomcat.getServer().await();
            System.out.println("Done.. ");

        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LifecycleException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    public static void stop(String[] args) {
        try {
            //System.out.println("Ending container process " );
            if (tomcat.getServer() != null
                    && tomcat.getServer().getState() != LifecycleState.DESTROYED) {
                if (tomcat.getServer().getState() != LifecycleState.STOPPED) {
                    try {
                        System.out.println("Executing stop command");
                        tomcat.stop();
                        System.out.println("Done with execution ");
                    } catch (LifecycleException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    System.out.println("Executing destroy command ");
                    tomcat.destroy();
                    System.out.println("Done with execution ");
                } catch (LifecycleException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        start(args);
    }


    private final static void getConfigProperties() {
        InputStream in = null;
        Properties p = null;
        try {
            in = SwitchApplication.class.getClassLoader().getResourceAsStream("System.properties");
            p = new Properties(System.getProperties());
            p.load(in);
            System.setProperties(p);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //System.getProperties().list(System.out);
    }

    private final static File getKeyStoreFile() {
        InputStream in = null;
        File file = null;
        //System.out.println("Loading keystore");
        try {
            //.
            in = SwitchApplication.class.getClassLoader().getResourceAsStream("service.keystore.jks");
            file = File.createTempFile("tmp", "mtc");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println("Done loading keystore");
        return file;
    }


    private final static Connector getConnector() {

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(System.getProperty("https.port") == null ? "9093" : System.getProperty("https.port")));
        httpsConnector.setSecure(true);
        httpsConnector.setAttribute("keystorePass", new Encrypter().decryptWithKey(System.getProperty("keystore.password")));
        httpsConnector.setAttribute("keystoreFile", getKeyStoreFile().getAbsoluteFile());
        httpsConnector.setAttribute("clientAuth", "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);
        httpsConnector.setAttribute("maxThreads", System.getProperty("max.threads"));

        httpsConnector.setAttribute("acceptCount", System.getProperty("accept.count"));
        httpsConnector.setAttribute("connectionTimeout", System.getProperty("conn.timeout"));
        httpsConnector.setAttribute("keepAliveTimeout", System.getProperty("keepalive.timeout"));

        return httpsConnector;


    }

    private final static void addErrorServlet(Context context) {
        Tomcat.addServlet(context, "errorServlet", new ErrorServlet());
        context.addServletMappingDecoded("/error", "errorServlet");
        ErrorPage errorPage = new ErrorPage();
        errorPage.setLocation("/error");
        System.out.println(errorPage.toString());
        context.addErrorPage(errorPage);
    }

    private final static void createDefaultServlet(Context context) {
        final String defaultServletName = "default1";
        Wrapper defaultServlet = context.createWrapper();
        defaultServlet.setName(defaultServletName);
        defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.addInitParameter("debug", "0");
        defaultServlet.addInitParameter("listings", "false");
        defaultServlet.setLoadOnStartup(1);
        context.addChild(defaultServlet);
        context.addServletMappingDecoded("/", defaultServletName);

    }

    private static void addLocalDBesource(StandardContext standardContext) {

        ContextResource contextResource = new ContextResource();

        contextResource.setName(System.getProperty("localdb.jndi.context"));

        contextResource.setAuth("Container");

        contextResource.setType("javax.sql.DataSource");

        contextResource.setProperty("driverClassName", System.getProperty("localdb.jdbc.driver.class"));

        contextResource.setProperty("url", System.getProperty("localdb.jdbc.url"));

        contextResource.setProperty("factory", "org.apache.tomcat.jdbc.pool.DataSourceFactory");

        contextResource.setProperty("username", System.getProperty("localdb.jdbc.username"));

        contextResource.setProperty("password", new Encrypter().decryptWithKey(System.getProperty("localdb.jdbc.pwd")));

//        contextResource.setProperty("maxActive", System.getProperty("localdb.jdbc.maxActive"));
        contextResource.setProperty("validationQuery", System.getProperty("localdb.jdbc.validationQuery"));
        contextResource.setProperty("removeAbandonedTimeout", System.getProperty("localdb.jdbc.removeAbandonedTimeout"));
        contextResource.setProperty("testWhileIdle", System.getProperty("localdb.jdbc.testWhileIdle"));
        contextResource.setProperty("timeBetweenEvictionRunsMillis", System.getProperty("localdb.jdbc.timeBetweenEvictionRunsMillis"));

        standardContext.getNamingResources().addResource(contextResource);


    }

}