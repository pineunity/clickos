#ifndef CLICK_TOHOST_HH
#define CLICK_TOHOST_HH
#include "elements/linuxmodule/fromhost.hh"

/*
 * =c
 *
 * ToHost([DEVNAME])
 *
 * =s sinks
 *
 * sends packets to Linux
 *
 * =d
 *
 * Hands packets to the ordinary Linux protocol stack.
 * Expects packets with Ethernet headers.
 * 
 * You should probably give Linux IP packets addressed to
 * the local machine (including broadcasts), and a copy
 * of each ARP reply.
 *
 * If DEVNAME is present, each packet is marked to appear as if it originated
 * from that network device -- that is, its device annotation is set to that
 * device. As with ToDevice, DEVNAME can be an Ethernet address.
 *
 * This element is only available in the Linux kernel module.
 *
 * =n
 *
 * Linux expects packets to have valid device annotations and packet type
 * annotations. Do not pass packets with null device annotations to Linux! Use
 * the `C<ToHost(eth0)>' syntax to supply a device annotation. Most packets
 * generated by Click will have null device annotations -- for example,
 * InfiniteSource makes packets with null device annotations. Exceptions
 * include FromDevice and PollDevice.
 *
 * Linux depends on packet type annotations as well. It will generally only
 * process packets with packet type annotation HOST. (Other packets, such as
 * packets originally sent to some other host, are sent only to packet
 * sniffers like tcpdump(1). Linux will handle some BROADCAST and MULTICAST
 * packets.) By default, packets made by Click have HOST packet type
 * annotations. However, if you modified a packet that you originally got from
 * some device, that packet may have some other type. Use SetPacketType to
 * reset the type appropriately.
 *
 * =a
 *
 * ToHostSniffers, FromHost, FromDevice, PollDevice, ToDevice,
 * SetPacketType, InfiniteSource */

class ToHost : public Element { public:
  
  ToHost();
  ~ToHost();
  
  const char *class_name() const		{ return "ToHost"; }
  const char *processing() const		{ return PUSH; }
  const char *flags() const			{ return "S2"; }
  
  int configure_phase() const	{ return FromHost::CONFIGURE_PHASE_TODEVICE; }
  int configure(Vector<String> &, ErrorHandler *);
  ToHost *clone() const;
  
  void push(int port, Packet *);
  void uninitialize();

 private:
  
  net_device *_dev;

};

#endif
