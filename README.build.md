# How To Build and Execute the Bundle
### Author: Woodrow Barlow
### Last Modified: Mon Jun 15 15:41:25 EDT 2015

## Z-Wave Dongle Driver

If your Z-Wave controller is a USB dongle (I'm using the Aeon Labs Z-Stick), you
need a USB-serial driver that creates a TTY stream for the device. If you are
using a full-featured computer, this will probably install when you plug in the
device. For other use cases (like embedded linux), you'll need to install the
driver manually.

The cp210x driver worked for my purposes.

## Export the Bundle as a Jar File (Eclipse)

1. Right click on the project and choose export.
2. Choose "Plug-in development / Deployable plug-ins and fragments"
3. In the "Destination" tab, specify a directory (I just use my home directory).
   * note: Eclipse will make a "plugins" directory inside your specified
     directory, and the packaged jar will end up there.
4. All other options should be fine. Review them, then click finish.
