# jSSC - Java Simple Serial Connector
### Author: Woodrow Barlow
### Last Modified: Mon Jun 15 15:41:25 EDT 2015

The Aeon Labs Z-Stick controller works by setting up a virtual serial port over
which to communicate. The JVM doesn't have a concept of a "serial port" and the
JVM provides no abstraction for serial ports. Third-party serial port libraries
are lacking, for whatever reason.

Because there is no abstraction for serial ports, any third-party Java library
depends upon interaction with a native shared library binary using the Java
Native Interface (JNI). Each third-party library writes its own serial port
library, because of course that isn't standardized (that would be too easy!).
The result for us is that we're left in the lurch again because we run a MIPS
architecture, and no one bothers supporting that.

The OpenHAB Z-Wave binding bundle originally used a serial port library called
RxTx. This library has gone dead. The author's official site and repository was
taken down a couple of years ago, and the only source left is just handed around
between the people who are still trying to use it.

I did some shopping around for other options, and I settled upon what seems to
be the "new" open-source way of doing serial: jSSC. It has a (kind of) active
Github/Google Code community. More importantly, it bundles the natives inside
the jar file and extracts them into the filesystem at runtime -- which means the
native binary doesn't need to pre-exist on the system, it can be downloaded as
part of the OSGi bundle!

Now, they don't have a MIPS binary, but it wasn't hard to cross-compile one. I
forked the original Github repository and set up a build system for natives that
would support cross-compiling with a toolchain (like the one we have on our
deployers).

The Java source for jSSC is also included in this project. I've tied it in with
the logging system and made some changes that make it more compatible with the
previous RxTx Serial Library implementation (mostly, I added the classes
SerialInputStream and SerialOutputStream, which are a part of the experimental
next-version build of jSSC).
