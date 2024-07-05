#$[/myProject/preamble]
use strict;
use warnings;
use EC::IIS;

my $ec_iis = EC::IIS->new;
$ec_iis->step_stop_server;
