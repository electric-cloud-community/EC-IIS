package EC::Plugin::IISDriver;

use strict;
use warnings;
use EC::Plugin::Core;
use base qw(EC::Plugin::Core);
use subs qw(assert);
use XML::Simple qw(XMLin);

use constant {
    DEFAULT_APPCMD_PATH => ($ENV{windir} || 'C:\\').'\system32\inetsrv\appcmd',
};

sub cmd_appcmd {
    return DEFAULT_APPCMD_PATH;
};


sub after_init_hook {
    my ($self, %params) = @_;
    $self->debug_level(0);

    eval {
        my $debug_level = $self->ec->getProperty('/plugins/EC-IIS/project/debugLevel')->findvalue('//value')->string_value;
        $self->debug_level($debug_level);
        $self->logger->level($debug_level);
    } or do {
        $self->debug_level(0);
        $self->logger->level(0);
    };

    eval {
        my $log_to_property = $self->ec->getProperty('/plugins/EC-IIS/project/ec_debug_logToProperty')->findvalue('//value')->string_value;
        $self->logger->log_to_property($log_to_property);
        $self->logger->info("Logs are redirected to property $log_to_property");
    };

}


sub get_app_pool {
    my ($self, $website, $application) = @_;

    my $command = $self->get_app_cmd('list', 'apps', qq{/app.name:"$website/$application"});
    my $result = $self->run_command($command);

    if ($result->{stdout}) {
        # APP "testsite/app" (applicationPool:mypool)
        my ($pool) = $result->{stdout} =~ m/APP ".+" \(applicationPool:(.+)\)/;
        return $pool;
    }
    return;
}


sub start_site_cmd {
    my ($self, $params) = @_;

    my $name = $params->{siteName};
    unless($name) {
        die "No site name is provided";
    }

    return $self->get_app_cmd('start', 'site', qq{/site.name:"$name"});
}

sub recycle_app_pool_cmd {
    my ($self, $params) = @_;

    my $name = $params->{applicationPool};
    unless($name) {
        $self->bail_out("No application pool name is provided");
    }
    return $self->get_app_cmd('recycle', 'apppool', qq{/apppool.name:"$name"});
}


sub stop_app_pool_cmd {
    my ($self, $params) = @_;

    my $name = $params->{applicationPool};
    assert $name;

    return $self->get_app_cmd('stop', 'apppool', qq{/apppool.name:"$name"});
}

sub check_app_pool_exists {
    my ($self, $name) = @_;

    my $check_exists_command = $self->get_app_cmd('list', 'apppools', qq{/name:"$name"});
    my $result = $self->run_command($check_exists_command);
    if ($result->{stderr} ne '') {
        return $self->bail_out("Cannot list app pools: $result->{stderr}");
    }
    return $result->{stdout} ? 1 : 0;
}

sub check_vdir_exists {
    my ($self, $name) = @_;

    my $command = $self->get_app_cmd('list', 'vdirs', qq{"$name"});
    my $result = $self->run_command($command);
    $self->logger->debug($command);
    $self->logger->debug($result);
    return $result->{stdout} && $result->{stdout} =~ m/VDIR "$name"/;
}


sub create_app_cmd {
    my ($self, $params) = @_;

    my $site_name = $params->{websiteName} or die 'No site name';
    my $path = $params->{applicationPath} or die 'No application path';
    my $physical_path = $params->{physicalPath} or die 'No physicalPath';

    $physical_path = EC::Plugin::Core::canon_path($physical_path);
    # TODO create folder if it does not exists

    if ($path !~ m/^\//) {
        $path = "/$path";
    }

    my $command = $self->get_app_cmd(
        'add', 'app',
        qq{/site.name:"$site_name"},
        qq{/path:"$path"},
        qq{/physicalPath:"$physical_path"}
    );
    return $command;
}


sub check_application_exists {
    my ($self, $appname) =  @_;

    my $command = $self->get_app_cmd('list', 'apps');
    $self->logger->debug($command);
    my $result = $self->run_command($command);
    $self->logger->debug($result);
    return $result->{stdout} && $result->{stdout} =~ m/APP "$appname"/;
}

my @app_pool_settings = qw(managedRuntimeVersion enable32BitAppOnWin64 managedPipelineMode queueLength autoStart);

sub create_app_pool_cmd {
    my ($self, $params, $available_settings, $options) = @_;

    if (!$available_settings || !@$available_settings) {
        $available_settings = [ @app_pool_settings ];
    }

    my $name = $params->{applicationPool};
    unless($name) {
        return $self->bail_out("No application pool name");
    }
    my @settings = ();

    for my $setting ( @$available_settings ) {
        if ($params->{$setting}) {
            push @settings, qq{/$setting:"$params->{$setting}"};
        }
    }

    if ($params->{appPoolAdditionalSettings}) {
        push @settings, $params->{appPoolAdditionalSettings};
    }

    $options ||= {};
    if ($options->{recycling_periodic_restart}) {
        my %times = map { $_ => 1} split(/\s*,\s*/, $options->{recycling_periodic_restart});
        for my $time (keys %times) {
            push @settings, qq{/+recycling.periodicRestart.schedule.[value='$time']}
        }
    }

    my $command = $self->get_app_cmd('add', 'apppool', qq{/name:"$name"}, @settings);
    return $command;
}

sub delete_app_cmd {
    my ($self, $params) = @_;

    my $app_name = $params->{applicationName};
    my $command = $self->get_app_cmd(
        'delete', 'app',
        qq{/app.name:"$app_name"}
    );
    return $command;
}

sub get_app_cmd {
    my ($self, $action, $object, @params) = @_;

    my $executable = $self->cmd_appcmd;
    die 'No action' unless $action;
    die 'No object' unless $object;
    my $command = "\"$executable\" $action $object " . join(" ", @params);
    return $command;
}

sub update_app_pool_cmd {
    my ($self, $params, $available_settings, $options) = @_;

    if (!$available_settings || !@$available_settings) {
        $available_settings = [ @app_pool_settings ];
    }

    my $name = $params->{applicationPool};
    my @settings = ();

    for my $setting ( @$available_settings ) {
        if ($params->{$setting}) {
            push @settings, qq{/$setting:"$params->{$setting}"};
        }
    }

    if ($params->{appPoolAdditionalSettings}) {
        push @settings, $params->{appPoolAdditionalSettings};
    }

    $options ||= {};
    if ($options->{recycling_periodic_restart}) {
        my %times = map { $_ => 1} split(/\s*,\s*/, $options->{recycling_periodic_restart});
        for my $time (keys %times) {
            push @settings, qq{/+recycling.periodicRestart.schedule.[value='$time']}
        }
    }

    unless (@settings) {
        return;
    }
    my $command = $self->get_app_cmd('set', 'apppool', qq{/apppool.name:"$name"}, @settings);
    return $command;
}


sub seconds_to_time_span {
    my ($self, $seconds) = @_;

    my ($hours, $minutes, $seconds_left);

    ($hours, $seconds_left) = div_mod($seconds, 60 * 60);
    ($minutes, $seconds_left) = div_mod($seconds_left, 60);

    my $span = sprintf('%02d:%02d:%02d', $hours, $minutes, $seconds_left);
    return $span;
}


sub div_mod {
    my ($a, $b) = @_;
    return (int($a/$b), $a % $b);
}

sub check_site_exists {
    my ($self, $site) = @_;

    my $command = $self->get_app_cmd('list', 'sites', qq{/name:"$site"});
    my $result = $self->run_command($command);
    $self->logger->debug($result);
    return $result->{stdout} ? 1 : 0;
}

sub create_site_cmd {
    my ($self, $params) = @_;

    my @command_parts = qw/add site/;
    if ($params->{websiteName}) {
        push @command_parts, qq{/site.name:"$params->{websiteName}"};
    }
    else {
        return $self->bail_out("Cannot create a site without site name");
    }
    if ($params->{bindings}) {
        push @command_parts, qq{/bindings:"$params->{bindings}"};
    }

    if ($params->{physicalPath}) {
        push @command_parts, qq{/physicalPath:"$params->{physicalPath}"};
    }
    if ($params->{websiteId}) {
        push @command_parts, qq{/id:"$params->{websiteId}"};
    }
    return $self->get_app_cmd(@command_parts);
}

sub update_site_cmd {
    my ($self, $params) = @_;

    my @command_parts = qw/set site/;
    if ($params->{websiteName}) {
        push @command_parts, qq{"$params->{websiteName}"};
        push @command_parts, qq{/name:"$params->{websiteName}"};
    }
    else {
        return $self->bail_out("Cannot create a site without site name");
    }
    if ($params->{bindings}) {
        push @command_parts, qq{/bindings:"$params->{bindings}"};
    }
    if ($params->{websiteId}) {
        push @command_parts, qq{/id:$params->{websiteId}};
    }
    return $self->get_app_cmd(@command_parts);
}

sub update_vdir_cmd {
    my ($self, $params) = @_;

    my $name = $params->{vdirName};
    my @command_parts = qw/set vdir/;
    unless($name) {
        $self->bail_out("No virtual directory name is provided");
    }

    push @command_parts, qq{"$name"};
    push @command_parts, qq{/vdir.name:"$name"};
    if ($params->{physicalPath}) {
        push @command_parts, qq{/physicalPath:"$params->{physicalPath}"};
    }
    return $self->get_app_cmd(@command_parts);
}

sub create_vdir_cmd {
    my ($self, $params) = @_;

    my $cmd = $self->get_app_cmd('add', 'vdir',
        qq{/app.name:"$params->{applicationName}"},
        qq{/path:"$params->{path}"},
        qq{/physicalPath:"$params->{physicalPath}"}
    );
    return $cmd;
}

sub delete_app_pool_cmd {
    my ($self, $params) = @_;

    my $name = $params->{applicationPool};
    unless($name) {
        return $self->bail_out("No application pool name is provided");
    }

    return $self->get_app_cmd('delete', 'apppool', qq{/apppool.name:"$name"});
}


sub delete_vdir_cmd {
    my ($self, $params) = @_;

    my $name = $params->{vdirName};
    unless($name) {
        return $self->bail_out("No virtual directory name is provided");
    }

    return $self->get_app_cmd('delete', 'vdir', qq{/vdir.name:"$name"});
}


sub delete_site_cmd {
    my ($self, $params) = @_;

    my $name = $params->{websiteName};
    unless($name) {
        $self->bail_out("No website name was specified");
    }
    return $self->get_app_cmd('delete', 'site', qq{/site.name:"$name"});
}

sub list_sites_cmd {
    my ($self, $params) = @_;

    my $criteria = $params->{criteria} || '';
    return $self->get_app_cmd('list', 'site', $criteria);
}

sub list_pools_cmd {
    my ($self, $params) = @_;

    my $criteria = $params->{searchcriteria} || '';
    return $self->get_app_cmd('list', 'apppool', $criteria);
}

sub list_apps_cmd {
    my ($self, $params) = @_;

    my $site = $params->{websiteName} || '';
    my $extra = '';
    if ($site) {
        $extra = qq{/site.name:"$site"};
    }
    return $self->get_app_cmd('list', 'apps', $extra);
}

sub list_vdirs_cmd {
    my ($self, $params) = @_;

    my $vdir = $params->{vdirName} || '';
    my $extra = '';
    if ($vdir) {
        $extra = $vdir;
    }
    return $self->get_app_cmd('list', 'vdirs', $extra);
}

sub get_ssl_certificate {
    my ($self, $params) = @_;

    my $command = 'netsh http show sslcert ';
    my $port = $params->{port} || die 'No port is provided';
    if ($params->{ip}) {
        $command .= qq{ipport=$params->{ip}:$port};
    }
    elsif ($params->{certHostName}) {
        $command .= qq{hostnameport=$params->{certHostName}:$port};
    }

    my $result = $self->run_command($command);
    if ($result->{stdout}) {

        my @lines = split(/\n/, $result->{stdout});
        my $retval = {};
        for my $line (@lines) {
            $line =~ /^\s*([\w\s:]+)\s*:\s*(.+)$/;
            if ($1 && $2) {
                my $key = $1;
                my $value = $2;
                $key =~ s/\s+$//g;
                $retval->{$key} = $value;
            }
        }
        return $retval;
    }
    else {
        if ($result->{code}) {
            die "Show certificate failed: " . ($result->{stderr} || $result->{stdout});
        }
    }
    return;
}


sub add_ssl_certificate_cmd {
    my ($self, $params) = @_;

    my $verb = $params->{verb} ||= 'add';
    my @command = (qw/netsh http/);
    push @command, $verb;
    push @command, 'sslcert';
    if ($params->{ip}) {
        push @command, qq{ipport=$params->{ip}:$params->{port}};
    }
    elsif ($params->{certHostName}) {
        push @command, qq{hostnameport=$params->{certHostName}:$params->{port}};
    }
    else {
        die 'Either IP or certHostName must be provided';
    }
    my $hash = $params->{hash};
    assert($hash);
    my $appid = $params->{appid};
    assert($appid);
    assert($params->{certStore});

    push @command, qq{certstore="$params->{certStore}"};
    push @command, "certhash=$hash";
    push @command, qq{appid="{$appid}"};
    my $command = join(' ', @command);
    return $command;
}


sub delete_ssl_certificate_cmd {
    my ($self, $params) = @_;

    my @command = qw(netsh http delete sslcert);
    if ($params->{ip}) {
        push @command, qq{ipport=$params->{ip}:$params->{port}};
    }
    elsif($params->{certHostName}) {
        push @command, qq{hostnameport=$params->{certHostName}:$params->{port}};
    }
    else {
        die 'Either IP or certHostName must be provided';
    }

    my $command = join(' ', @command);
    return $command;
}

sub set_vdir_creds_cmd {
    my ($self, $params) = @_;

    my $vdir = $params->{vdirName};
    my $creds = $params->{creds};

    my $username = escape($creds->{userName});
    my $password = escape($creds->{password});

    unless($vdir) {
        die 'No virtual directory name found in params';
    }
    unless($username && $password) {
        die 'No username or password found in params';
    }
    return $self->get_app_cmd('set', 'vdir', qq{/vdir.name:"$vdir"}, qq{/username:"$username"}, qq{/password:"$password"});
    # return $self->get_app_cmd('set', 'vdir', qq{/vdir.name:"$vdir"}, qq{/username:$username}, qq{/password:$password});
}


sub add_site_binding_cmd {
    my ($self, $params) = @_;

    my $site_name = $params->{websitename};
    my $protocol = $params->{bindingprotocol};
    my $information = $params->{bindinginformation};
    assert $site_name;
    assert $information;
    assert $protocol;

    # appcmd set site /site.name: contoso /bindings.[protocol='https',bindingInformation='*:443:'].bindingInformation:*:443: marketing
    return $self->get_app_cmd('set', 'site', qq{/site.name:"$site_name"}, qq{/+bindings.[protocol='$protocol',bindingInformation='${information}']});
}

sub get_site {
    my ($self, $name) = @_;

    my $cmd = $self->get_app_cmd('list', 'site', qq{/site.name:"$name"}, '/xml');
    my $result = $self->run_command($cmd);

    if ($result->{code}) {
        if ($result->{code} == 1) {
            die qq{The site "$name" does not exist\n};
        }
        die "Cannot get site: code $result->{code}, " . ($result->{stderr} || $result->{stdout});
    }
    my $site_data = XMLin($result->{stdout});
    if ($site_data->{SITE}) {
        return $site_data->{SITE};
    }
    else {
        return $site_data;
    }
}

sub escape {
    my ($string) = @_;
    # TODO

    # Didn't work
    # $string =~ s/"/^"/g;
    # $string =~ s/"/\\"/g;
    # $string =~ s/"/\\\\\"/g;
    # Does not work either, but at least it runs
    $string =~ s/"/\\""/;
    # $string =~ s/"/""/g;
    # $string =~ s/"/\^"/;

    # Escape shell metacharacters:
    # $string =~ s/([()%!^"<>&|;, ])/\^$1/g;
    return $string;
}


sub assert($) {
    my $value = $_[0];
    unless($value) {
        my ($module, $file, $line) = caller();
        die "${module}::${line}: value is required";
    }
}

1;
