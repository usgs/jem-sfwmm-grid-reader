# JEM SFWMM Grid Reader & NetCDF IOSP Library
Joint Ecosystem Modeling (JEM)
http://jem.usgs.gov
JEM is a partnership among federal agencies (USGS/USFWS/NPS/USACE), universities and other organizations.

[![Build Status](https://travis-ci.org/usgs/jem-sfwmm-grid-reader.svg?branch=master)](https://travis-ci.org/usgs/jem-sfwmm-grid-reader)

## DISCLAIMER
This Software and any support from the JEM Community are provided
"AS IS" and without warranty, express or implied. JEM specifically
disclaim any implied warranties of merchantability for a particular
purpose. In no event will JEM be liable for any damages, including
but not limited to any lost profits, lost savings or any incidental
or consequential damages, whether resulting from impaired or
lost data, software or computer failure or any other cause, or
for any other claim by the user or for any third party claim.
Although this program has been used by the USGS, no warranty,
expressed or implied, is made by the USGS or the United States
Government as to the accuracy and functioning of the program
and related program material nor shall the fact of distribution
constitute any such warranty, and no responsibility is assumed
by the USGS in connection therewith.  This software is provisional.

## Build
To compile this application, ensure you have the Java SDK 8+ installed and Maven. Use the Maven tool in the root of this project:
 * http://www.oracle.com/technetwork/java/javase/downloads/index.html
 * https://maven.apache.org/
 
To compile standard jar dependencies and run unit tests:
$ mvn install

To explicitly use standard jar packaging use:
$ mvn install -P packaging-jar

To compile for Eclipse-based applications using Tycho, use: 
$ mvn install -P packaging-eclipse

JavaDocs can be generated with the following command:

$ mvn javadoc:javadoc -P packaging-jar

External dependencies:
 * See project pom.xml files, all dependencies are Maven projects

## Example Maven build
[INFO] ------------------------------------------------------------------------
[INFO] Building JEM SFWMM Grid Reader Parent 1.0.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- maven-install-plugin:2.4:install (default-install) @ parent ---
[INFO] Installing C:\Users\mckelvym\workspace\jem_sfwmm\pom.xml to C:\Users\mckelvym\.m2\repository\gov\usgs\jem\sfwmm\grid\group\parent\1.0.0-SNAPSHOT\parent-1.0.0-SNAPSHOT.pom
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] JEM SFWMM Grid Reader Library Plug-in .............. SUCCESS [  4.668 s]
[INFO] JEM SFWMM Grid Reader NetCDF IOSP Plug-in .......... SUCCESS [  2.523 s]
[INFO] JEM SFWMM Grid Reader Parent ....................... SUCCESS [  0.031 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 8.444 s
[INFO] Finished at: 2016-11-04T11:37:11-04:00
[INFO] Final Memory: 23M/219M
[INFO] ------------------------------------------------------------------------

## Usage
 - See Main.java in gov.usgs.jem.sfwmm.grid/example
 - See Main.java in gov.usgs.jem.netcdf.iosp.sfwmm.grid/example