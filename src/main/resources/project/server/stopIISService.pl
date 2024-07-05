#!/usr/bin/env perl
# include $[/myProject/preamble]
# line 4 "[EC]/@PLUGIN_KEY@-@PLUGIN_VERSION@/stopIISService.pl"
# -------------------------------------------------------------------------
# File
#    stopIISServices.pl
#
# Dependencies
#    None
#
# Template Version
#    1.0
#
# Date
#    11/05/2010
#
# Engineer
#    Alonso Blanco
#
# Copyright (c) 2011 Electric Cloud, Inc.
# All rights reserved
# -------------------------------------------------------------------------

# -------------------------------------------------------------------------
# Includes
# -------------------------------------------------------------------------
use ElectricCommander;
use warnings;
use strict;
use Cwd;
use File::Spec;
use Data::Dumper;
use ElectricCommander::PropDB;
$| = 1;

# -------------------------------------------------------------------------
# Constants
# -------------------------------------------------------------------------
use constant {
    SUCCESS => 0,
    ERROR   => 1,

    PLUGIN_NAME    => 'EC-IIS',
    WIN_IDENTIFIER => 'MSWin32',
    IIS_VERSION_6  => 'iis6',
    IIS_VERSION_7  => 'iis7',

    STOP_SERVICE_DEFAULT_COMMAND => 'net stop',

    CREDENTIAL_ID => 'credential',
    SERVICE_COUNT => 6,
};

########################################################################
# trim - deletes blank spaces before and after the entered value in
# the argument
#
# Arguments:
#   -untrimmedString: string that will be trimmed
#
# Returns:
#   trimmed string
#
########################################################################
sub trim($) {

    my ($untrimmedString) = @_;

    my $string = $untrimmedString;

    #removes leading spaces
    $string =~ s/^\s+//;

    #removes trailing spaces
    $string =~ s/\s+$//;

    #returns trimmed string
    return $string;
}

# -------------------------------------------------------------------------
# Variables
# -------------------------------------------------------------------------

# -------------------------------------------------------------------------
# Main functions
# -------------------------------------------------------------------------

########################################################################
# main - contains the whole process to be done by the plugin, it builds
#        the command line, sets the properties and the working directory
#
# Arguments:
#   none
#
# Returns:
#   none
#
########################################################################
sub main() {

    # create args array
    my @args = ();
    my %props;
    my %configuration;

    my $iisVersion                = '';
    my $url                       = '';
    my $user                      = '';
    my $password                  = '';
    my $successfulStoppedServices = 0;
    my $content;

    my $ec = new ElectricCommander();
    $ec->abortOnError(0);

    my $w3svcLine      = STOP_SERVICE_DEFAULT_COMMAND . ' w3svc';
    my $smtpsvcLine    = STOP_SERVICE_DEFAULT_COMMAND . ' Smtpsvc';
    my $httpFilterLine = STOP_SERVICE_DEFAULT_COMMAND . ' HTTPFilter';
    my $msftpLine      = STOP_SERVICE_DEFAULT_COMMAND . ' Msftpsvc';
    my $nntpsvcLine    = STOP_SERVICE_DEFAULT_COMMAND . ' Nntpsvc';
    my $adminLine      = STOP_SERVICE_DEFAULT_COMMAND . ' iisadmin';

    #execute command line
    $content = `$w3svcLine`;

    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step
    if ( $? == SUCCESS ) {

        #set any additional error or warning conditions here
        #there may be cases in which an error occurs and the exit code is 0.
        #we want to set to correct outcome for the running step
        if ( $content =~ m/service was stopped successfully/ ) {
            $successfulStoppedServices++;
        }
        else {

        }

    }

    #execute command line
    $content = `$httpFilterLine`;

    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step
    if ( $? == SUCCESS ) {

        #set any additional error or warning conditions here
        #there may be cases in which an error occurs and the exit code is 0.
        #we want to set to correct outcome for the running step
        if ( $content =~ m/service was stopped successfully/ ) {
            $successfulStoppedServices++;
        }
        else {

        }

    }

    #execute command line
    $content = `$smtpsvcLine`;

    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step
    if ( $? == SUCCESS ) {

        #set any additional error or warning conditions here
        #there may be cases in which an error occurs and the exit code is 0.
        #we want to set to correct outcome for the running step
        if ( $content =~ m/service was stopped successfully/ ) {

            $successfulStoppedServices++;
        }
        else {

        }

    }

    #execute command line
    $content = `$msftpLine`;

    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step
    if ( $? == SUCCESS ) {

        #set any additional error or warning conditions here
        #there may be cases in which an error occurs and the exit code is 0.
        #we want to set to correct outcome for the running step
        if ( $content =~ m/service was stopped successfully/ ) {

            $successfulStoppedServices++;

        }
        else {

        }

    }

    #execute command line
    $content = `$nntpsvcLine`;

    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step
    if ( $? == SUCCESS ) {

        #set any additional error or warning conditions here
        #there may be cases in which an error occurs and the exit code is 0.
        #we want to set to correct outcome for the running step
        if ( $content =~ m/service was stopped successfully/ ) {

            $successfulStoppedServices++;
        }
        else {

        }

    }

    #execute command line
    $content = `$adminLine`;

    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step
    if ( $? == SUCCESS ) {

        #set any additional error or warning conditions here
        #there may be cases in which an error occurs and the exit code is 0.
        #we want to set to correct outcome for the running step
        if ( $content =~ m/service was stopped successfully/ ) {
            $successfulStoppedServices++;
        }
        else {

        }

    }

    print "$w3svcLine\n";
    print "$httpFilterLine\n";
    print "$smtpsvcLine\n";
    print "$msftpLine\n";
    print "$nntpsvcLine\n";
    print "$adminLine\n";

    my $services = SERVICE_COUNT;

    if ( $services == $successfulStoppedServices ) {

        $ec->setProperty( "/myJobStep/outcome", 'success' );
        print "Successfully stopped all services\n";

    }
    elsif ( ( $successfulStoppedServices > 0 ) ) {

        $ec->setProperty( "/myJobStep/outcome", 'success' );
        print
"Successfully stopped $successfulStoppedServices service(s) out of $services\n";

    }
    else {

        $ec->setProperty( "/myJobStep/outcome", 'error' );
        print "Could not stop the IIS Services\n";

    }

    #add command line to properties object
    $props{'w3svcLine'}      = $w3svcLine;
    $props{'httpFilterLine'} = $httpFilterLine;
    $props{'smtpsvcLine'}    = $smtpsvcLine;
    $props{'msftpLine'}      = $msftpLine;
    $props{'nntpsvcLine'}    = $nntpsvcLine;
    $props{'adminLine'}      = $adminLine;

    #set prop's hash to EC properties
    setProperties( \%props );

}

########################################################################
# createCommandLine - creates the command line for the invocation
# of the program to be executed.
#
# Arguments:
#   -arr: array containing the command name (must be the first element)
#         and the arguments entered by the user in the UI
#
# Returns:
#   -the command line to be executed by the plugin
#
########################################################################
sub createCommandLine($) {

    my ($arr) = @_;

    my $commandName = @$arr[0];

    my $command = $commandName;

    shift(@$arr);

    foreach my $elem (@$arr) {
        $command .= " $elem";
    }

    return $command;

}

########################################################################
# setProperties - set a group of properties into the Electric Commander
#
# Arguments:
#   -propHash: hash containing the ID and the value of the properties
#              to be written into the Electric Commander
#
# Returns:
#   none
#
########################################################################
sub setProperties($) {

    my ($propHash) = @_;

    # get an EC object
    my $ec = new ElectricCommander();
    $ec->abortOnError(0);

    foreach my $key ( keys %$propHash ) {
        my $val = $propHash->{$key};
        $ec->setProperty( "/myCall/$key", $val );
    }
}

##########################################################################
# getConfiguration - get the information of the configuration given
#
# Arguments:
#   -configName: name of the configuration to retrieve
#
# Returns:
#   -configToUse: hash containing the configuration information
#
#########################################################################
sub getConfiguration($) {

    my ($configName) = @_;

    # get an EC object
    my $ec = new ElectricCommander();
    $ec->abortOnError(0);

    my %configToUse;

    my $proj = "$[/myProject/projectName]";
    my $pluginConfigs =
      new ElectricCommander::PropDB( $ec, "/projects/$proj/iis_cfgs" );

    my %configRow = $pluginConfigs->getRow($configName);

    # Check if configuration exists
    unless ( keys(%configRow) ) {
        print "Configuration $configName doesn't exist\n";
        exit ERROR;
    }

    # Get user/password out of credential
    my $xpath = $ec->getFullCredential( $configRow{credential} );
    $configToUse{'user'}     = $xpath->findvalue("//userName");
    $configToUse{'password'} = $xpath->findvalue("//password");

    foreach my $c ( keys %configRow ) {

        #getting all values except the credential that was read previously
        if ( $c ne CREDENTIAL_ID ) {
            $configToUse{$c} = $configRow{$c};
        }

    }

    return %configToUse;

}

main();

1;
