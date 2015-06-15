# Z-Wave Web Management Interface
### Author: Woodrow Barlow
### Last Modified: Mon Jun 15 15:41:25 EDT 2015

## Summary

Once the bundle launches, it automatically spawns an HTTP server running on port
8080. I've extended that HTTP server to provide a front-end interface for
controlling devices on the Z-Wave network. It should automatically detect all
nodes on the network and provide controls for any parameters which can be
controlled through the OpenHAB binding protocol.

## How it Works

The OpenHAB binding protocol manages detection of nodes, and also registers each
node with any number of "Command Classes", each of which represents a certain
"parameter" on the device. Different command classes do different things. For
instance, the ZWaveBinarySensorCommandClass is used to see if a sensor is
triggered, while the ZWaveThermostatFanModeCommandClass is used to control which
mode an HVAC fan is using.

My web gui will build a "dialogue" (a webpage, basically) for a single node at a
time. That dialogue will contain any number of "controls". A control is just a
visual representation of a command class -- in this case, each control is an
HTML form. Since command classes each have a specific purpose, each control is
unique to a certain command class. The naming scheme for the controls matches
the name of the command class.

Each time a pageload is done, the gui also checks if there are any outstanding
requests (which happens any time one of those HTML forms is submitted) and
processes them as appropriate. There are processing functions for each control
that is not read-only. The processing functions are responsible for taking
information from the GUI (user input) and passing it off to the controller in
the form of a Z-Wave command (the command classes help construct the command).

At the top of the WebGUI.java file, you will see an enum called "Control". This
is responsible for most of the automagic page serving. Each control needs to be
defined in here. Each control gets a "friendly name" (something suitable for
displaying to the user) and the associated ZWaveCommandClass.

There are also .html files in the res/controls folder of this package. These are
not used like .html files traditionally would be used on a webserver; they
aren't served directly as files, just used as file resources by the Java code.
In other words, you can't navigate to http://10.52.140.200:8080/controls/wakeup.html
and expect to see anything. The webserver is absurdly simplistic, and doesn't
even serve files. It builds the one page you ever see from scratch each time it
is requested. These HTML files do have "variables" in them: %NODEID%,
%CONTROLNAME%, and %FRIENDLYCONTROLNAME%. These "variables" get replaced via
regex when the page is being built.

Processing isn't quite as automatic. In the serve() method (which is the primary
method for this class), there is a check near the top to see whether a control
has been submitted. If it has, it checks which control was submitted, by means
of a switch-case statement, and calls an appropriate processing method. All of
the processing methods have the same naming scheme and accept two arguments: a
ZWave Node, and a key-value map containing all the values that were passed in
the request (in the form of Strings).

Along the way, the system may make use of the ArrayLists failureMessages and
successMessages. Any messages stored in here will be displayed as little pop-up
bubbles to the user when the page loads. For example, in a processing method,
when it completes successfully, it will push a message to successMessages saying
as much.

So, basically what happens is this: the user navigates to port 8080 in a web
browser. The server gets a request to build a page. If no node is specified,
it just builds a form to select a node. If a node is specified, it builds the
dialogue for that node. When building the dialogue, it checks the controller
to see which command classes are used by this node, and, for each command class,
is builds the appropriate control. It builds the control by fetching the
appropriate .html file, reading in the contents as a string, replacing variables
as necessary, then embedding that string into the string with the rest of the
HTML for the page being built.

If, in addition to a node being specified, there is also a control specified and
some relevant bits of data passed, the server then executes the appropriate
processing method for that control. This all actually happens before the HTML is
built and served to the user.

That's it! There's some extra fluff along the way, like building the basic
navigation and loading the CSS. Also, it's worth noting that if the system ever
encounters a command class that isn't considered in the Control enum, it just
builds it as an unknown control and logs the information.

If you've implemented a new ZWave command class and you need to provide a GUI
control for it (because it will otherwise just be an "unknown" control), you
only need to add an entry to the Control enum and make an appropriately named
.html file in the res/controls directory. If you also need to process input from
that control, create a processing method in the WebGUI file alongside the others
(I try to keep them in alphabetical order) that accepts the node and key-value
map, and add a case to the switch-case statement in the serve() method for your
newly created enum entry which calls your newly created processing method.
Everything else should be handled for you.

## What Still Needs to be Done

In the spirit of OSGi, the interface should probably be a separate bundle from
the protocol / backend stuff. So there should be some sort of API in the backend
which the frontend can use. That way, users who don't need the frontend (maybe
they have a really simple script which interfaces with that exposed API) don't
need the extra bloat that comes with it. Additionally, by exposing an API and
keeping functionality separate, we could make a mobile app for the frontend if
we decide to do so.
 * note: this probably means that the openhab protocol won't be exposed directly
   and the ZWaveController object won't be used directly -- the backend should
   abstract that to a less ZWave-specific model, with cache being an integral
   part.

Also, the system is "write-only" at the moment. That is to say: from the web
interface, I can send a command to tell a light to turn on, but I can't find out
if a light is on or not. So, eventually, I want to add "displays" in addition to
the controls that are already here; unfortunately, there are some back-end
troubles to overcome first. ZWave communication is not instantaneous. I can send
a command that says "hey, when you get a chance, please tell me whether this
light is on or off", but I can't wait for a response. Communication is
asynchronous. I will keep going about my business, and pretty soon the
controller will receive a "notification" back from that node, and will let the
bundle know that it got a notification. This comes in the form of a ZWaveEvent,
and it is caught by the ZWaveIncomingEvent method in BundleThread.java.

Somehow, nodes need to be polled periodically and have their values stored in a
cache, so that the WebGUI can access that cache and display relatively fresh
values. This caching mechanism needs to be designed in such a way that it
doesn't matter is nodes shuffle around or get added and dropped without warning,
because that will happen.

Another neat feature would be to be able to reset the controller from the web
interface, and do other "settings" type of operations on the controller. Perhaps
even add the ability to add / remove nodes on the network.
