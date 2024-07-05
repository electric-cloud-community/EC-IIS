package EC::IIS::Discovery;

use strict;
use warnings;
use ElectricCommander;
use EC::Plugin::Core;
use base qw(EC::Plugin::Core);

sub new {
    my ( $class, $resourceName ) = @_;

    my $ec = ElectricCommander->new;
    return bless { ec => $ec, resourceName => $resourceName }, $class;
}

my $DISCOVERY_PROPERTIES_ROOT = '/plugins/@PLUGIN_NAME@/project/ec_discovery/discovered_data';

my @POSSIBLE_PATHS = ('C:\Program Files (x86)\IIS\Microsoft Web Deploy V3\msdeploy.exe', 'C:\Program Files\IIS\Microsoft Web Deploy V3\msdeploy.exe');

sub discover {
    my ($self, $resourceName) = @_;

    # $self->ec->deleteProperty("/resources[$resourceName]/ec_discovery");
    for my $path ( @POSSIBLE_PATHS) {
        print "Checking path $path\n";
        if (-f $path) {
            $self->ec->setProperty("$DISCOVERY_PROPERTIES_ROOT/msdeploy_path/$path", $path);
            print "Found msdeploy.exe: $path\n";
        }
    }
}

1;
