/*
 * locfromfile.{cc,hh} -- play a trace of locations.
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
#include "locfromfile.hh"
#include <click/glue.hh>
#include <click/confparse.hh>
#include <click/router.hh>
#include <click/error.hh>

LocFromFile::LocFromFile()
{
  // no MOD_INC_USE_COUNT; rely on GridLocationInfo
  _next = 0;
  _move = 1;
}

LocFromFile::~LocFromFile()
{
  // no MOD_DEC_USE_COUNT; rely on GridLocationInfo
}

void *
LocFromFile::cast(const char *name)
{
  if(strcmp(name, "GridLocationInfo") == 0)
    return(this);
  return(GridLocationInfo::cast(name));
}

int
LocFromFile::configure(Vector<String> &conf, ErrorHandler *errh)
{
  String filename;
  int res = cp_va_parse(conf, this, errh,
                        cpFilename, "filename",  &filename,
                        cpEnd);
  if(res >= 0){
    FILE *fp = fopen(filename.cc(), "r");
    if(fp == 0)
      return(errh->error("cannot open file %s", filename.cc()));
    char buf[512];
    while(fgets(buf, sizeof(buf), fp)){
      struct delta d;
      if(sscanf(buf, "%lf%lf%lf", &d.interval, &d.lat, &d.lon) == 3){
        _deltas.push_back(d);
      } else {
        fclose(fp);
        return(errh->error("cannot parse a line in file %s", filename.cc()));
      }
    }
    fclose(fp);
    if(_deltas.size() < 1)
      return(errh->error("no locations in file %s", filename.cc()));
    click_chatter("read %d deltas from %s",
                  _deltas.size(),
                  filename.cc());
  }
  return res;
}

// Pick a new place to move to, and a time by which we want
// to arrive there.
void
LocFromFile::choose_new_leg(double *nlat, double *nlon, double *nt)
{
  *nlat = _deltas[_next].lat;
  *nlon = _deltas[_next].lon;
  *nt = _t0 + _deltas[_next].interval;

  _next += 1;
  if(_next >= _deltas.size())
    _next = 0;
}

ELEMENT_REQUIRES(userlevel GridLocationInfo)
EXPORT_ELEMENT(LocFromFile)

#include <click/vector.cc>
template class Vector<LocFromFile::delta>;
