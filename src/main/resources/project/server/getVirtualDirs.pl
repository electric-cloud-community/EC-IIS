#!/usr/bin/env perl
# include $[/myProject/preamble]
# line 4 "[EC]/@PLUGIN_KEY@-@PLUGIN_VERSION@/getVirtualDirs.pl"
# -------------------------------------------------------------------------
# File
#    getVirtualDirs.pl
#
# Dependencies
#    None
#
# Template Version
#    1.0
#
# Date
#    07/14/2011
#
# Engineer
#    Ed Cardinal
#
# Copyright (c) 2011 Electric Cloud, Inc.
# All rights reserved
# -------------------------------------------------------------------------

use ElectricCommander;
use Data::Dumper;
use File::Temp qw/tempfile/;

my $ec = new ElectricCommander();

# Parameters
my $host = ( $ec->getProperty("HostName") )->findvalue("//value");
print "Host: $host\n";

# If GetWebSiteIDs is used prior to this, then you can use the
# property on the job <dollarsign>[/myJob/iiswebsites/<sitename>]
my $websiteid = ( $ec->getProperty("WebSiteID") )->findvalue("//value");
print "WebsiteID: $websiteid\n\n";

# Create and open a temp file for the JScript code
my ( $scriptfh, $scriptfilename ) = tempfile( DIR => '.', SUFFIX => '.js' );

# See "GetWebSiteIDs" for notes about IIS and ADSI.

# IMPORTANT: This is JScript code. If you change it to use VBScript or
# PowerShell (or whatever) you need to adjust the cscript commndline below and
# probably the SUFFIX above (although the suffix will be ignored when a /E argument
# is passed to cscript).
my $jscript = <<"EOSCRIPT";
// Get the site in question
var website = GetObject("IIS://$host/W3SVC/$websiteid");
// Iterate through all virtual directories for this site
getVDirs(website, "");

function getVDirs(parent, indent) {
    var vdirenum = new Enumerator(parent);
    for (; !vdirenum.atEnd(); vdirenum.moveNext()) {
        var xvdir = vdirenum.item();
        // Don't bother with anything but virtual or real directories
        if (xvdir.Class != "IIsWebVirtualDir" && xvdir.Class != "IIsWebDirectory") continue;
        WScript.Echo(indent + "--Name:  " + xvdir.Name);
        WScript.Echo(indent + "  Type:  " + xvdir.Class);
        WScript.Echo(indent + "  Path:  " + xvdir.Path);
        WScript.Echo(indent + "  App:   " + xvdir.AppFriendlyName);
        var state = xvdir.AppGetStatus2();
        var statename = stateName(state);
        WScript.Echo(indent + "  State: " + state + " (" + statename + ")");
        // This needs to be recursive...
        getVDirs(xvdir, indent + "  ");
    }
}

function stateName(appState) {
   switch (appState) {
      case 0:
         return "Stopped";
         break;
      case 1:
         return "Running";
         break;
      case 2:
         return "NotAnApplication";
         break;
      default: 
         return "Unknown";
    }
}

EOSCRIPT

print $scriptfh $jscript;
close($scriptfh);

my @vdirs = `cscript /E:jscript /NoLogo $scriptfilename`;

print "Virtual Directories:\n";
print @vdirs;
