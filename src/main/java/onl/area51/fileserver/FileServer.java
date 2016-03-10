/*
 * Copyright 2016 peter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package onl.area51.fileserver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import onl.area51.httpd.HttpRequestHandlerBuilder;
import org.apache.http.config.SocketConfig;
import uk.trainwatch.kernel.CommandArguments;
import onl.area51.filesystem.http.server.FileSystemMap;
import onl.area51.filesystem.http.server.PathHttpActionBuilder;
import onl.area51.httpd.HttpServer;
import onl.area51.httpd.HttpServerBuilder;
import uk.trainwatch.util.Functions;
import uk.trainwatch.util.config.Configuration;
import uk.trainwatch.util.config.ConfigurationService;

/**
 * Handles the booting of the MapGen cluster node
 *
 * @author peter
 */
@ApplicationScoped
public class FileServer
{

    private static final Logger LOG = Logger.getLogger( FileServer.class.getName() );

    @Inject
    private ConfigurationService configurationService;
    private Configuration mainConfig;
    private Configuration httpdConfig;

    private Level logLevel = Level.INFO;

    private FileSystemMap fileSystemMap;
    private HttpServer server;
    private int port;
    private String serverInfo;

    /**
     * Instantiate this bean on startup.
     *
     * @param args
     */
    public void boot( @Observes CommandArguments args )
    {
        // Nothing to do here, it's presence ensures the bean is instantiated by CDI
    }

    @PostConstruct
    void start()
    {
        mainConfig = configurationService.getConfiguration( "fileserver" );
        httpdConfig = mainConfig.getConfiguration( "httpd" );

        try {
            logLevel = Level.parse( mainConfig.getString( "logLevel", logLevel.getName() ) );
        }
        catch( NullPointerException ex ) {
        }

        mountFileSystems();
        createServer();

        LOG.log( Level.INFO, () -> "Starting http server " + serverInfo + " on port " + port );

        try {
            server.start();
        }
        catch( IOException ex ) {
            throw new UncheckedIOException( ex );
        }

        LOG.log( Level.INFO, () -> "Started http server " + serverInfo + " on port " + port );
    }

    /**
     * Mount all file systems either from "filesystem" object in the main config or individual "filesystem_" + name +".json" files.
     */
    private void mountFileSystems()
    {
        LOG.log( Level.INFO, "Mounting filesystems" );

        Configuration fileSystemConfig = mainConfig.getConfiguration( "filesystem" );
        fileSystemMap = FileSystemMap.builder()
                .addFileSystems( mainConfig.collection( "filesystems" )
                        .map( Functions.castTo( String.class ) )
                        .map( n -> fileSystemConfig.getConfiguration( n, () -> configurationService.getConfiguration( "filesystem_" + n ) ) )
                )
                .build();

        if( LOG.isLoggable( logLevel ) ) {
            fileSystemMap.prefixes()
                    .sorted( String.CASE_INSENSITIVE_ORDER )
                    .forEach( s -> LOG.log( logLevel, s ) );
        }
    }

    private void createServer()
    {
        port = httpdConfig.getInt( "port", 8080 );
        serverInfo = httpdConfig.getString( "serverInfo", "Area51/1.1" );

        LOG.log( Level.INFO, () -> "Creating http server " + serverInfo + " on port " + port );

        server = HttpServerBuilder.builder()
                .setSocketConfig( SocketConfig.custom()
                        .setSoTimeout( httpdConfig.getInt( "socket.soTimeout", 15000 ) )
                        .setTcpNoDelay( httpdConfig.getBoolean( "socket.tcpNoDelay", true ) )
                        .build() )
                .setListenerPort( port )
                .setServerInfo( serverInfo )
                .setSslContext( null )
                .setExceptionLogger( ex -> LOG.log( Level.SEVERE, null, ex ) )
                .shutdown( httpdConfig.getLong( "shutdown.time", 5L ), httpdConfig.getEnum( "shutdown.unit", TimeUnit.class, TimeUnit.SECONDS ) )
                .registerHandler( "*", HttpRequestHandlerBuilder.create()
                                  // Normal GET requests
                                  .method( "GET" )
                                  .log( LOG, logLevel )
                                  .add( PathHttpActionBuilder.create()
                                          .assertPathExists()
                                          .returnPathContent()
                                          .build( fileSystemMap ) )
                                  .end()
                                  // HEAD requests
                                  .method( "HEAD" )
                                  .log( LOG, logLevel )
                                  .add( PathHttpActionBuilder.create()
                                          .assertPathExists()
                                          .returnPathSizeOnly()
                                          .build( fileSystemMap ) )
                                  .end()
                                  //
                                  .build() )
                .build();
    }

    @PreDestroy
    void stop()
    {
        LOG.log( Level.INFO, () -> "Shutting down http server " + serverInfo + " on port " + port );

        server.stop();

        LOG.log( Level.INFO, () -> "Shut down http server " + serverInfo + " on port " + port );
    }
}
