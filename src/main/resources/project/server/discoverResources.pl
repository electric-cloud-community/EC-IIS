# $[/myProject/preamble]
use warnings;
use strict;

my $ec = new ElectricCommander( { abortOnError => 0 } );

my @resourceNames = split( /\n/, "$[resourceNames]" );

print @resourceNames;

if ( scalar(@resourceNames) < 1 ) {
    print "Error: resourceNames parameter must be provided.\n";
    exit 1;
}

# For each resource, create steps which actually execute resource-level discovery
for my $resourceName (@resourceNames) {
    my $xpath = $ec->createJobStep(
        {
            jobStepName     => "Discover Resource - $resourceName",
            subprocedure    => 'DiscoverResource',
            resourceName    => $resourceName,
        }
    );
    print "Spawned job for resource $resourceName\n";
}

1;
