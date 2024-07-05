# $[/myProject/preamble]

use strict;
use warnings;

use Carp;
use EC::IIS;

my $iis = EC::IIS->new;
$iis->step_deploy_advanced;
