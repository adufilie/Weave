This repository is for Weave version 1.9.

The latest desktop version can be found under [releases](https://github.com/adufilie/Weave/releases/).

A live demo of the Flash version can be found [here](http://weaveteam.github.io/Weave-Binaries/weave.html).

Weave supports integration from multiple data sources including: **CSV, GeoJSON, SHP/DBF, CKAN**  
  
Developer documentation can be found [here](http://WeaveTeam.github.com/Weave-Binaries/asdoc/)

Components in this repository:

 * WeaveAPI: ActionScript interface classes.
 * WeaveCore: Core sessioning framework.
 * WeaveData: Data framework. Non-UI features.
 * WeaveUISpark: User interface classes (Spark components).
 * WeaveUI: User interface classes (Halo components).
 * WeaveClient: Flex application for Weave UI.
 * WeaveDesktop: Adobe AIR application front-end for Weave UI.
 * WeaveAdmin: Flex application for admin activities.
 * WeaveServletUtils: Back-end Java webapp libraries.
 * WeaveServices: Back-end Java webapp for Admin and Data server features.
 * GeometryStreamConverter: Java library for converting geometries into a streaming format. Binary included in WeaveServices/lib.
 * JTDS_SqlServerDriver: Java library for handling connections to Microsoft SQL Server. Binary included in WeaveServletUtils/lib.

The bare minimum you need to build Weave is [Flex 4.5.1.A](http://fpdownload.adobe.com/pub/flex/sdk/builds/flex4.5/flex_sdk_4.5.1.21328A.zip) and [Java EE](http://www.oracle.com/technetwork/java/javaee/downloads/index.html).

To build the projects on the command line, use the **build.xml** Ant script. To create a ZIP file for deployment on another system (much like the nightlies,) use the **dist** target.

See install-linux.md for detailed linux install instructions.

Weave is distributed under the [MPL-2.0](https://www.mozilla.org/en-US/MPL/2.0/) license.

