# OpenHAB and the Z-Wave Binding
### Author: Woodrow Barlow
### Last Modified: Mon Jun 15 15:41:25 EDT 2015

## Summary

The binding we use to communicate with the Z-Wave network comes from OpenHAB. In
an attempt to keep this bundle lightweight, we've pulled in just the bare
minimum to make it work.

## Description of the OpenHAB Protocol Library

The controller is obviously the USB stick that you plug into the iMG. This
controller is the "primary" or "master" controller. Supposedly, there can also
be "secondary" or "slave" controllers on the network, breaking it up into a
network tree. I've never tried doing that. Each node on the network is just a
Z-Wave enabled device like a door lock or motion sensor or light switch. Each
node is assigned any number of "command classes" which describe what sort of
actions and parameters that node has. A command class is just a way of
representing an action (such as "turn on" for a light switch) or a parameter
(such as the temperature for a thermostat) as a Java object.

Interfacing with the Z-Wave network is done through the ZWaveController object.
During controller initialization, the nodes on the network are automatically
detected and added to the controller.

To initialize a controller instance, construct the object (you'll need to pass
the serial port the controller is using) and call the initialize() method on
that object. The initialize() method will return immediately, but it might take
a few more seconds for the controller to finish detecting and adding all the
nodes. This object will need to be passed around to other classes which want to
directly operate on the Z-Wave network (in this case, I've passed it to the
WebGUI class).

To get the nodes (or a certain node) use the getNodes() or getNode(nodeId)
methods of the controller instance. To get the command classes for a node, use
the getCommandClasses() method on an instance of a node. You will need to use
an `instanceof` check to see what type of command class you are dealing with.

Command classes are composed mostly of methods which return a SerialMessage
object. This is an object containing the formatted data of a command or request
in the Z-Wave protocol. Command classes help you construct the command for a
certain action or parameter request, but they don't actually send it. To send a
command, use the sendData() method of your controller instance, and pass it the
SerialMessage object which was constructed by the command class.

Receiving data from a node is done in the form of "notification events", which
are similar to a "trap" in SNMP or a "signal" in Linux or C programming. In
order to handle notifications, you need to register a certain class with the
controller as an "event listener" and implement an event listener method in that
class. Use the addEventListener() method of the controller instance. You can
only pass a class which implements the interface `ZWaveEventListener`. To
process incoming events, that class must have a method called
ZWaveIncomingEvent(), which takes a ZWaveEvent as an argument. That event can be
cast to its more specific type -- the type you will probably be most interested
in is the ZWaveCommandClassValueEvent, which is a notification providing the
value for a certain command class parameter, and which is sent as a response to
any "GET" requests sent to a node.

## Useful Resources

 * [Description of OpenHAB Bindings in General]
   (https://github.com/openhab/openhab/wiki/Bindings)

 * [Description of the Z-Wave Binding]
   (https://github.com/openhab/openhab/wiki/Z-Wave-Binding)

 * [Tutorial for Creating a New Binding]
   (https://github.com/openhab/openhab/wiki/How-To-Implement-A-Binding)

 * [The File Which Heavily Used the Protocol in the Z-Wave Binding]
   (openhab/bundles/binding/org.openhab.binding.zwave/src/main/java/org/openhab
   /binding/zwave/internal/ZWaveActiveBinding.java)
    * note: this file is not in our project here, get it from the OpenHAB source
      repository.
