#$[/myProject/preamble]
use strict;
use warnings;
use EC::IIS;

my $iis = EC::IIS->new;
$iis->step_delete_app_pool;
