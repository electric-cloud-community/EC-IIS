# $[/myProject/preamble]
use EC::IIS::Discovery;

use warnings;
use strict;

my $discovery = EC::IIS::Discovery->new;
$discovery->discover('$[resourceName]');
