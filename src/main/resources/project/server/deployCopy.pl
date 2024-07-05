#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# -------------------------------------------------------------------------
# Includes
# -------------------------------------------------------------------------
use ElectricCommander;
use ElectricCommander::PropDB;
use strict;
#use warnings;
use File::Spec;
use Carp;
use Data::Dumper;
$| = 1;

# -------------------------------------------------------------------------
# Constants
# -------------------------------------------------------------------------
use constant {
    SUCCESS => 0,
    ERROR   => 1,

    PLUGIN_NAME    => 'EC-IIS',
    CREDENTIAL_ID  => 'credential',

};

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

    my @commandArgs = ();

    #get an EC object
    my $ec = new ElectricCommander();
    $ec->abortOnError(0);

    # -------------------------------------------------------------------------
    # Fetch parameter values
    # -------------------------------------------------------------------------

    # TODO: !!!!!!!!!!!  CANNONICAL PATHS with File::Spec !!!!!!!!!!!
    my $sourcePath = ($ec->getProperty( "SourcePath" ))->findvalue('//value')->string_value;
    my $execPath = ($ec->getProperty( "ExecPath" ))->findvalue('//value')->string_value;
    my $destinationPath = ($ec->getProperty( "DestinationPath" ))->findvalue('//value')->string_value;
    my $additionalOptions = ($ec->getProperty( "AdditionalOptions" ))->findvalue('//value')->string_value;

    my %configuration;
    my %props;

    #generate command line from array of executable + arguments
    
    ## xcopy <source> <dest>  /E /K /R /H /I /Y
    #  / E - Deep copy including empty dirs
    #   /K - Copy attributes
    #   /R  - Overwrite read-only files
    #   /H - Copy hidden and system files
    #   /I - If destination does not exist and copying more than one file, assumes that destination must be a directory.
    #   /Y - Supress prompting for overwrite confirmation

    # Put quotes around paths in case of spaces
    push( @commandArgs, qq{"$execPath"} );
    push( @commandArgs, qq{"$sourcePath"} );
    push( @commandArgs, qq{"$destinationPath"} );
    # Note: no quotes around option switches
    push( @commandArgs, qq{$additionalOptions} );

    my $cmdLine = createCommandLine( \@commandArgs );
    my $content = '';

    if ( $cmdLine && $cmdLine ne '' ) {

        # execute command line
        $content = `$cmdLine`;

        print $cmdLine;
        print $content;

        # evaluates if exit was successful to mark
        # it as a success or fail the step
        if ( $? == SUCCESS ) {

            $ec->setProperty( "/myJobStep/outcome", 'success' );

         # set any additional error or warning conditions here
         # there may be cases in which an error occurs and the exit code is 0.
         # we want to set to correct outcome for the running step
         #if ( $content !~ m/has been STARTED|is already STARTED/ ) {
         #    $ec->setProperty( "/myJobStep/outcome", 'error' );
         #}

        }
        else {
            $ec->setProperty( "/myJobStep/outcome", 'error' );
        }

        #show masked command line
        print "Command Line: $cmdLine\n";

        #add masked command line to properties object
        $props{'cmdLine'} = $cmdLine;

        #set prop's hash to EC properties
        setProperties( \%props );
    }
    else {

        print "Error: could not generate command line";
        exit ERROR;

    }

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

    my $command = shift(@$arr);

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

main();

1;
