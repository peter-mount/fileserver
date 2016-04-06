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
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import onl.area51.filesystem.http.server.FileSystemFactory;
import onl.area51.httpd.HttpRequestHandlerBuilder;
import onl.area51.filesystem.http.server.PathHttpActionBuilder;
import onl.area51.httpd.action.ActionRegistry;
import uk.trainwatch.util.Functions;
import uk.trainwatch.util.config.Configuration;
import uk.trainwatch.util.config.ConfigurationService;

@Dependent
public class FileServer
{

    private static final Logger LOG = Logger.getLogger( "FileServer" );

    /**
     * Instantiate this bean on startup.
     *
     * @param registry
     * @param configurationService
     */
    public void deploy( @Observes ActionRegistry registry, ConfigurationService configurationService )
    {
        Configuration mainConfig = configurationService.getConfiguration( "fileserver" );

        LOG.log( Level.INFO, "Mounting filesystems" );

        Configuration fileSystemConfig = mainConfig.getConfiguration( "filesystem" );

        mainConfig.collection( "filesystems" )
                .map( Functions.castTo( String.class ) )
                .map( n -> fileSystemConfig.getConfiguration( n, () -> configurationService.getConfiguration( "filesystem_" + n ) ) )
                .forEach( fs -> {
                    String prefix = FileSystemFactory.getPrefix( fs );

                    LOG.log( Level.INFO, () -> "Registring filesystem " + prefix );

                    try {
                        FileSystem fileSystem = FileSystemFactory.getFileSystem( fs );

                        registry.registerHandler( prefix + "*", HttpRequestHandlerBuilder.create()
                                                  // Log all requests
                                                  .log( LOG, Level.INFO )
                                                  // Normal GET requests
                                                  .method( "GET" )
                                                  .add( FileSystemFactory.getPathAction( fileSystem,
                                                                                         PathHttpActionBuilder.create()
                                                                                         .assertPathExists()
                                                                                         .returnPathContent()
                                                                                         .build() ) )
                                                  .end()
                                                  // HEAD requests
                                                  .method( "HEAD" )
                                                  .add( FileSystemFactory.getPathAction( fileSystem,
                                                                                         PathHttpActionBuilder.create()
                                                                                         .assertPathExists()
                                                                                         .returnPathSizeOnly()
                                                                                         .build() ) )
                                                  .end()
                                                  // POST
                                                  .method( "PUT" )
                                                  .add( FileSystemFactory.getPathAction( fileSystem,
                                                                                         PathHttpActionBuilder.create()
                                                                                         .saveContent()
                                                                                         .build() ) )
                                                  .end()
                                                  //
                                                  .build() );
                    } catch( IOException | URISyntaxException ex ) {
                        LOG.log( Level.SEVERE, ex, () -> "Failed to register " + prefix );
                    }
                } );
    }
}
