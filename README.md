# Simple Z-Wave Management OSGi Bundle
### Author: Woodrow Barlow
### Last Modified: Mon Jun 15 15:41:25 EDT 2015

## Some History

This project intends to provide a self-contained OSGi bundle which can perform
cursory management tasks on a Z-Wave network. The bundle is intended to be
cross-platform and targets Java 1.5 in order to run on older and limited
embedded devices. It has been succesfully tested on a router running JamVM with
GNU Classpath libraries and a MIPSEL CPU. The bundle interfaces with a USB or
serial Z-Wave dongle in order to control the network, and provides an interface
via HTTP on port 8080.

## How to Build and Execute this Bundle

See README.setup to set up your IDE, then see README.build to see how to build
and run the bundle.

## Module Structure

**MANIFEST.MF**: Every OSGi bundle must have a MANIFEST.MF file (it's in the
META-INF folder). This tells the execution environment what the bundle's
dependencies are, what the minimum Java runtime requirements are, &c. The Java
runtime requirements can be no higher than J2SE-1.5 (Java 5). Also of note:
The jSSC-native executables are packaged with the bundle in order to avoid
dependencies.

**Activator.java**: This is the bundle's "activator". This contains
initialization and deconstruction logic for the bundle, to be executed when
the bundle changes to or from the "Active" state. The best design for this
would probably to spawn a thread upon initialization (since the "start" method
isn't really supposed to stay running for more than a moment) which will do all
of the actual bundle's work, then join and stop that thread in the
deconstruction.

**BundleThread.java**: This is the worker thread which was spawned by the
activator. This initializes the ZWave network, starts the HTTP server, opens a
new Logger, etc. For now, this is also handling all incoming ZWave events. It
will soon be in charge of delegating events to appropriate areas of the
software (for now it just logs the event).

**zwavemanagement.http package**: This is the web-based GUI for controlling the
Z-Wave network. See the README.webgui file for more on that.

**zwavemanagement.cache package**: This is basically empty for now. It will
eventually be a caching mechanism for the values of each parameter on each node,
so that a new Z-Wave request doesn't need to be made each time a value is needed.

**zwavemanagement.logging package**: OpenHAB originally had its own logging
system, which was based on SLF4J. We don't need anything that heavy duty, so I
made my own that used the same method/object names (so I didn't have to change
anything in the OpenHAB code). Over time I added more features as I deemed them
useful.

**zwave.internal.protocol package**: This is the almighty protocol library. It
comes from the OpenHAB codebase. This handles communication between this bundle
and the Z-Wave controller (which, in turn, communicates with the network). See
the README.openhab file for more information about this.

**elonen package**: This is the code for the HTTP server which powers the Web
GUI. It's absurdly light-weight and doesn't do much more than serve a request.
There's the original author's readme file inside that package if you need to
know more.

**jssc package**: Since the JRE doesn't have any abstraction for serial ports,
I needed to bring in a third-party library: jSSC (Java Simple Serial Connector).
This library is actually mostly written in C, with Java acting as a wrapper
using the Java Native Interface. OpenHAB originally used the RxTx library, but
for reasons described in README.jssc, I replaced it with this.

**lib/linux folder**: This contains the C-code shared object binary for the jSSC
library. This folder gets packed into the exported JAR file, and OSGi
automatically handles extraction and insertion of the .so file.

## Stumbling Blocks and Resources

I have been unable to find much documentation about the protocol library, and
my only code example has been the binding module itself. It's heavy reading so
far.

This binding is licensed under the Eclipse Common License. I don't know enough
about licensing to know if this is compatible with our setup (apparently it
doesn't play nice with GPL).

The OpenHAB wiki has some useful information about the Z-Wave binding bundle.
When reading this, take note that I've stripped out most of the binding bundle,
such as loading configurations and automatically locating nodes, &c. (since it
had dependencies upon other parts of OpenHAB that we didn't want and made
assumptions about the setup that weren't true in our atypical use case), and
kept only the protocol functionality.
 * https://github.com/openhab/openhab/wiki/Z-Wave-Binding
