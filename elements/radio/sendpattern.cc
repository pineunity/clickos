/*
 * sendpattern.{cc,hh} -- element creates a particular kind of packet for
 * CheckPattern to check
 * Robert Morris
 *
 * Copyright (c) 1999-2000 Massachusetts Institute of Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, subject to the conditions
 * listed in the Click LICENSE file. These conditions include: you must
 * preserve this copyright notice, and you cannot mention the copyright
 * holders in advertising related to the Software without their permission.
 * The Software is provided WITHOUT ANY WARRANTY, EXPRESS OR IMPLIED. This
 * notice is a summary of the Click LICENSE file; the license in that file is
 * legally binding.
 */

#include <click/config.h>
#include "sendpattern.hh"
#include <click/confparse.hh>
#include <click/error.hh>

SendPattern::SendPattern()
  : Element(0, 1)
{
  MOD_INC_USE_COUNT;
  _len = 1;
}

SendPattern::~SendPattern()
{
  MOD_DEC_USE_COUNT;
}

SendPattern *
SendPattern::clone() const
{
  return new SendPattern();
}

int
SendPattern::configure(Vector<String> &conf, ErrorHandler *errh)
{
  return cp_va_parse(conf, this, errh,
		     cpUnsigned, "packet length", &_len,
		     0);
}

Packet *
SendPattern::pull(int)
{
  WritablePacket *p = Packet::make(_len);
  int i;
  for(i = 0; i < _len; i++)
    p->data()[i] = i & 0xff;
  return p;
}

EXPORT_ELEMENT(SendPattern)
