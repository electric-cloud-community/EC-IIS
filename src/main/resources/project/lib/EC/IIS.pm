#
#  Copyright 2017 Electric Cloud, Inc.
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

package EC::IIS;

use strict;
use warnings;

=head1 NAME

EC::IIS - Electric Commander Microsoft IIS integration plugin core.

=cut

use Carp;
use File::Temp qw(tempfile);
require Win32 if $^O eq 'MSWin32';
use EC::Plugin::Core;
use base qw(EC::Plugin::Core);
use Data::Dumper;
use LWP::UserAgent;
use IO::Socket::INET;
use JSON;
use XML::Simple qw(XMLout);
use File::Path qw(mkpath);

use ElectricCommander;
use ElectricCommander::PropDB;
use EC::Plugin::IISDriver;


use constant {
    SUCCESS => 0,
    ERROR   => 1,

    PLUGIN_NAME => 'EC-IIS',
    CREDENTIAL_ID => 'credential',
    CHECK_COMMAND => '/status',

    GENERATE_REPORT => 1,
    DO_NOT_GENERATE_REPORT => 0,

    # IIS defaults
    DEFAULT_APPCMD_PATH => ($ENV{windir} || 'C:\\').'\system32\inetsrv\appcmd',

};


sub after_init_hook {
    my ($self, %params) = @_;

    my $is_win = EC::Plugin::Core::is_win();
    unless($is_win) {
        $self->bail_out("Non-windows system detected. Please run the plugin on Windows resource.");
    }
    $self->logger->info('Using plugin @PLUGIN_NAME@');
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

sub driver {
    my ($self) = @_;

    unless($self->{_iis_driver}) {
        $self->{_iis_driver} = EC::Plugin::IISDriver->new;
    }
    return $self->{_iis_driver};
}

sub trim {
    my ($string) = shift;

    # kill leading & trailing spaces
    $string =~ s/^\s+//;
    $string =~ s/\s+$//;

    return $string;
};

sub iisreset {
    return 'iisreset'; #TODO configurable
};

sub cmd_appcmd {
    return DEFAULT_APPCMD_PATH;
};


my @version = ($^O eq 'MSWin32') ? Win32::GetOSVersion() : ();

sub iis_version {
    # TODO add better autodetection
    # For now, 6/7 division is the most crucial.
    croak "EC::IIS: Not a Windows system, aborting!"
        unless @version;
    return $version[1] >= 6 ? 7 : 6;

    # Here's the table:

    # OS                        ID  MAJOR   MINOR   IIS
    # Win32s                    0   -       -       ?
    # Windows 95                1   4       0       ?
    # Windows 98                1   4       10      ?
    # Windows Me                1   4       90      ?
    #
    # Windows NT 3.51           2   3       51      ?
    # Windows NT 4              2   4       0       ?
    #
    # Windows 2000              2   5       0       5
    # Windows XP                2   5       1       5
    # Windows Server 2003       2   5       2       6
    # Windows Server 2003 R2    2   5       2       6
    # Windows Home Server       2   5       2       ?
    #
    # Windows Vista             2   6       0       ?
    # Windows Server 2008       2   6       0       7
    # Windows 7                 2   6       1       7
    # Windows Server 2008 R2    2   6       1       7 || 7.5
    # Windows 8                 2   6       2       ?
    # Windows Server 2012       2   6       2       ?
};

sub outcome_error {
    my ($self, $fail) = @_;

    # TODO Add error details? do  we need it at all?
    return $self->ec->setProperty( "/myJobStep/outcome", $fail ? 'error' : 'success' );
};

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
sub setProperties {
    my ($self, $propHash) = @_;

    # get an EC object
    my $ec = $self->ec;

    foreach my $key (keys %$propHash) {
        my $val = $propHash->{$key};
        $ec->setProperty("/myCall/$key", $val);
    };
}


sub getConfiguration($){
    my ($self, $configName) = @_;

    # get an EC object
    my $ec = $self->ec;

    my %configToUse;

    my $proj = "$[/myProject/projectName]";
    my $pluginConfigs = new ElectricCommander::PropDB($ec,"/projects/$proj/iis_cfgs");

    my %configRow;
    eval {
        %configRow = $pluginConfigs->getRow($configName);
        1;
    } or do {
        $self->bail_out(qq{Configuration "$configName" does not exist});
    };

    # Check if configuration exists
    unless(keys(%configRow)) {
        croak "No config for '$proj' named '$configName'";
    }

    # Get user/password out of credential
    my $xpath = $ec->getFullCredential($configRow{credential});
    $configToUse{'user'} = $xpath->findvalue("//userName");
    $configToUse{'password'} = $xpath->findvalue("//password");
    $self->logger->add_secrets($configToUse{password});

    foreach my $c (keys %configRow) {
        #getting all values except the credential that was read previously
        if($c ne CREDENTIAL_ID){
            $configToUse{$c} = $configRow{$c};
        }
    }

    return \%configToUse;
}

sub create_msdeploy_command {
    my ($self, $params) = @_;

    my $exec = EC::Plugin::Core::canon_path($params->{msdeployPath});
    my $command = "\"$exec\" -verb:$params->{verb}";

    if ($params->{sourceProvider}) {
        $command .= " -source:$params->{sourceProvider}";
        if ($params->{sourceProviderObjectPath}) {
            my $path = EC::Plugin::Core::canon_path($params->{sourceProviderObjectPath});
            $command .= qq{="$path"};
        }
        if ($params->{sourceProviderSettings}) {
            $command .= ",$params->{sourceProviderSettings}";
        }
    }
    if ($params->{destProvider}) {
        $command .= " -dest:$params->{destProvider}";
        if ($params->{destProviderObjectPath}) {
            my $path = EC::Plugin::Core::canon_path($params->{destProviderObjectPath});
            $command .= qq{="$path"};
        }
        if ($params->{destProviderSettings}) {
            $command .= ",$params->{destProviderSettings}";
        }
    }
    if ($params->{allowUntrusted}) {
        $command .= " -allowUntrusted";
    }
    if ($params->{postSync}) {
        $command .= " -postSync:runCommand=\"$params->{postSync}\"";
    }
    if ($params->{preSync}) {
        $command .= " -preSync:runCommand=\"$params->{preSync}\"";
    }
    if ($params->{setParamFile}) {
        if (_is_xml($params->{setParamFile}) && $params->{setParamFile} =~ m/parameters/) {
            my $filename = _save_params_file($params->{setParamFile});
            $command .= " -setParamFile:$filename";
        }
        elsif (-f $params->{setParamFile}) {
            $command .= " -setParamFile:$params->{setParamFile}";
        }
        else {
            $self->bail_out("The file $params->{setParamFile} is not found");
        }
    }
    if ($params->{declareParamFile}) {
        if (_is_xml($params->{declareParamFile}) && $params->{declareParamFile} =~ m/parameters/) {
            my $filename = _save_params_file($params->{declareParamFile});
            $command .= " -declareParamFile:\"$filename\"";
        }
        elsif (-f $params->{declareParamFile}) {
            $command .= " -declareParamFile:\"$params->{declareParamFile}\"";
        }
        else {
            $self->bail_out("The file $params->{declareParamFile} is not found");
        }
    }
    if ($params->{additionalOptions}) {
        $command .= " $params->{additionalOptions}";
    }

    return $command;
}

sub step_deploy_advanced {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        msdeployPath verb sourceProvider
        sourceProviderObjectPath sourceProviderSettings destProvider
        destProviderObjectPath destProviderSettings allowUntrusted
        preSync postSync
        additionalOptions
        setParamFile declareParamFile/
    );

    my $command = $self->create_msdeploy_command($params);
    $self->set_cmd_line($command);

    my $result = $self->run_command($command);
    if ($result->{code} != 0) {
        $self->bail_out("Error: $result->{stderr}");
    }
    else {
        print $result->{stdout};
    }
}


sub step_create_or_update_site {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw(websitename
        bindings
        websitepath
        websiteid
        createDirectory
        credential
    ));

    my $website_name = $params->{websitename};
    $params->{websiteName} = $params->{websitename};
    $params->{physicalPath} = EC::Plugin::Core::canon_path($params->{websitepath});
    $params->{websiteId} = $params->{websiteid};

    my $creds;
    if ($params->{credential}) {
        $creds = $self->_get_credentials($params->{credential});
    }

    if ($self->driver->check_site_exists($params->{websiteName})) {
        $self->logger->info("Site $params->{websiteName} already exists.");

        if ($params->{bindings} || $params->{websiteId}) {
            $self->logger->info("Going to update site bindings for $website_name");
            my $command = $self->driver->update_site_cmd($params);
            $self->set_cmd_line($command, 'updateSite');
            my $result = $self->run_command($command);
            $self->_process_result($result);
        }

        if ($params->{physicalPath}) {
            $self->logger->info("Going to update virtual directory $website_name");
            my $command = $self->driver->update_vdir_cmd({ %$params, vdirName => "$website_name/"});
            $self->set_cmd_line($command, 'updateVdir');
            my $result = $self->run_command($command);
            $self->_process_result($result);
        }

    }
    else {
        $self->logger->info("Site $params->{websiteName} does not exist");
        my $command = $self->driver->create_site_cmd($params);
        $self->set_cmd_line($command);
        my $result = $self->run_command($command);
        $self->_process_result($result);
    }

    if ($params->{createDirectory}) {
        $self->_create_directory($params->{physicalPath});
    }

    if ($creds) {
        my $vdir_name = "$params->{websiteName}/";
        $self->logger->info("Going to set credentails for directory $vdir_name");
        my $cmd =  $self->driver->set_vdir_creds_cmd({vdirName => $vdir_name, creds => $creds});
        my $res = $self->run_command($cmd);
        $self->_process_result($res);
    }
}

sub step_undeploy {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        msdeployPath
        websiteName
        strictMode
        applicationName
        deleteVirtualDirectories
    /);

    my $website_name = $params->{websiteName};
    unless ($self->driver->check_site_exists($website_name)) {
        if ($params->{strictMode}) {
            return $self->bail_out("Website $website_name does not exist");
        }
        else {
            return $self->warning("Website $website_name does not exist");
        }
    }

    my $app_name = $params->{applicationName};
    if ($app_name) {
        unless($self->driver->check_application_exists("$website_name/$app_name")) {
            my $message = qq{Application "$app_name" does not exist in the site "$website_name"};
            if ($params->{strictMode}) {
                return $self->bail_out($message);
            }
            else {
                return $self->warning($message);
            }
        }
    }

    my $command = $self->create_undeploy_command($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    my $output = $result->{stderr} || $result->{stdout};
    if ($result->{code} != 0) {
        # This is not an error, just a warning
        if ($output =~ m/Error:\s*Provider\s*rootWebConfig32\s*is\s*blocked/i) {
            $self->warning($output);
            return;
        }
        else {
            $self->bail_out("Error: $result->{stderr}");
        }
    }
    else {
        $self->_process_result($result);
    }
    # $self->_process_result($result);
}

sub step_deploy {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        msdeployPath
        websiteName
        source
        applicationPath
        applicationPool
        managedRuntimeVersion
        enable32BitAppOnWin64
        managedPipelineMode
        queueLength
        autoStart
        appPoolAdditionalSettings
        additionalOptions
    /);

    my $source_provider;
    my $source = $params->{source};
    if ( -d $source ) {
        $source_provider = 'iisApp';
    }
    else {
        $source_provider = 'package';
    }
    $self->logger->info(qq{Source provider: $source_provider});
    my $destination_object_path = $params->{websiteName};
    if ($params->{applicationPath}) {
        $destination_object_path .= "/$params->{applicationPath}";
    }
    my $deploy_command = $self->create_msdeploy_command({
        msdeployPath => $params->{msdeployPath},
        sourceProvider => $source_provider,
        sourceProviderObjectPath => $source,
        destProvider => 'iisApp',
        destProviderObjectPath => $destination_object_path,
        verb => 'sync',
        additionalOptions => $params->{additionalOptions}
    });
    $self->set_cmd_line($deploy_command);
    my $result = $self->run_command($deploy_command);

    $self->logger->info($result->{stdout});

    $self->_process_result($result);

    my $application = $params->{applicationPath} || '';
    my $app_pool_name = $params->{applicationPool};
    my $app_name = $application;
    $app_name =~ s{^/}{};

    my $app_path = $application;
    $app_path = '/' . $app_path unless $app_path =~ m{^/};

    if(!$app_pool_name && $application) {
        $app_pool_name = $self->get_site_app_pool($params->{websiteName}, $app_name);
        unless($app_pool_name) {
            # There is no app pool, so one will be created and it will have the site name
            $app_pool_name = $params->{websiteName};
        }
        else {
            $self->logger->info(qq{Application "$app_name" is in app pool "$app_pool_name"});
        }
        $params->{applicationPool} = $app_pool_name;
    }

    if ($app_pool_name) {
        $self->create_or_update_app_pool($params);
        my $cmd = $self->driver->get_app_cmd(
            'set',
            'site',
            qq{/site.name:"$params->{websiteName}"},
            qq{/[path='$app_path'].applicationPool:"$app_pool_name"}
        );
        $self->logger->info("Going to move app $app_name to app pool $app_pool_name");
        my $result = $self->run_command($cmd);
        if ($result->{code} != 0) {
            return $self->bail_out("Failed to move application $app_name to app pool $app_pool_name: " . _message_from_result($result));
        }
        $self->logger->info($result->{stdout});
    }

}

# application should not start with slash
sub get_site_app_pool {
    my ($self, $website, $application) = @_;

    my $command = $self->driver->get_app_cmd('list', 'apps', qq{/app.name:"$website/$application"});
    my $result = $self->run_command($command);

    if ($result->{stdout}) {
        # APP "testsite/app" (applicationPool:mypool)
        my ($pool) = $result->{stdout} =~ m/APP ".+" \(applicationPool:(.+)\)/;
        return $pool;
    }
    return;
}

sub create_or_update_app_pool {
    my ($self, $params, $settings, $options) = @_;

    my $name = $params->{applicationPool};
    if ($self->driver->check_app_pool_exists($name)) {
        $self->logger->info(qq{Application pool "$name" already exists, going to update});
        # Application pool exists

        if ($options->{recycling_periodic_restart}) {
            my $cmd = $self->driver->get_app_cmd('set', 'apppool', qq{/apppool.name:"$name"}, qq{/-recycling.periodicRestart.schedule});
            $self->run_command($cmd);
        }
        $self->update_app_pool($params, $settings, $options);
    }
    else {
        $self->logger->info(qq{Application pool "$name" has not been created yet. Proceeding to adding it.});
        $self->create_app_pool($params, $settings, $options);
    }
}

=head2 create_app_pool

Creates app pool.

    applicationPool - the name of the pool

=cut

# TODO
my @app_pool_settings = qw(managedRuntimeVersion enable32BitAppOnWin64 managedPipelineMode queueLength autoStart);

sub create_app_pool {
    my ($self, $params, $available_settings, $options) = @_;

    my $command = $self->driver->create_app_pool_cmd($params, $available_settings, $options);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);
}


sub update_app_pool {
    my ($self, $params, $available_settings, $options) = @_;

    my $command = $self->driver->update_app_pool_cmd($params, $available_settings, $options);

    if ($command) {
        $self->set_cmd_line($command);
        my $result = $self->run_command($command);
        $self->_process_result($result);
    }
    else {
        $self->logger->info("No changes found for application pool");
        return;
    }
}


sub create_undeploy_command {
    my ($self, $params) = @_;

    my $provider = $params->{deleteVirtualDirectories} ? 'appHostConfig' : 'iisApp';

    my $exec = EC::Plugin::Core::canon_path($params->{msdeployPath});
    my $command = qq{"$exec" -verb:delete};
    my $dest = $params->{websiteName};
    if ($params->{applicationName}) {
        $dest .= "/$params->{applicationName}";
    }
    $command .= qq{ -dest:$provider="$dest"};
    return $command;
}

sub step_create_application {
    my ($self) = @_;

    # TODO rename form fields
    my $params = $self->get_params_as_hashref(qw/
        appname
        path
        physicalpath
        createDirectory
        credential/);
    $params = {
        websiteName => $params->{appname},
        applicationPath => $params->{path},
        physicalPath => EC::Plugin::Core::canon_path($params->{physicalpath}),
        createDirectory => $params->{createDirectory},
        credential => $params->{credential}
    };

    my $creds;
    if ($params->{credential}) {
        $creds = $self->_get_credentials($params->{credential});
    }
    my $application_name = "$params->{websiteName}/$params->{applicationPath}";
    $application_name =~ s/\/+/\//g;
    $self->logger->info("Application full name: $application_name");
    my $vdir_name = "$application_name/";

    if ($self->driver->check_application_exists($application_name)) {
        $self->logger->info(qq{Application "$application_name" exists});
        if ($params->{physicalPath}) {
            $self->logger->info("Going to update virtual directory $vdir_name");
            my $command = $self->driver->update_vdir_cmd({ %$params, vdirName => $vdir_name});
            $self->set_cmd_line($command, 'updateVdir');
            my $result = $self->run_command($command);
            $self->_process_result($result);
        }
    }
    else {
        my $command = $self->driver->create_app_cmd($params);
        $self->set_cmd_line($command);
        my $result = $self->run_command($command);
        $self->_process_result($result);
    }
    if ($params->{createDirectory}) {
        $self->_create_directory($params->{physicalPath});
    }
    if ($creds) {
        $self->logger->info("Going to set credentails for directory $vdir_name");
        my $cmd =  $self->driver->set_vdir_creds_cmd({vdirName => $vdir_name, creds => $creds});
        my $res = $self->run_command($cmd);
        $self->_process_result($res);
    }
}


sub step_start_website {
    my ($self) = @_;

    eval {
        my $params = $self->get_params_as_hashref(qw/sitename/);
        my $command = $self->driver->start_site_cmd({siteName => $params->{sitename}});
        $self->set_cmd_line($command);

        my $result = $self->run_command($command);
        $self->_process_result($result);
        1;
    } or do {
        $self->bail_out($@);
    };
}

sub step_delete_application {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/appname strictMode/);

    unless ($self->driver->check_application_exists($params->{appname})) {
        if ($params->{strictMode}) {
            return $self->bail_out("Application $params->{appname} does not exist");
        }
        else {
            return $self->warning("Application $params->{appname} does not exist");
        }
    }

    my $command = $self->driver->delete_app_cmd({applicationName => $params->{appname}});
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);

    if ($result->{code} != 0) {
        my $message = _message_from_result($result);
        $self->bail_out("Cannot delete application: $message");
    }
    print $result->{stdout};
}


sub step_create_app_pool {
    my ($self) = @_;

    my @settings = qw/
        queueLength
        autoStart
        enable32BitAppOnWin64
        managedRuntimeVersion
        managedPipelineMode
        processModel.identityType
        processModel.loadUserProfile
        processModel.idleTimeout
        processModel.maxProcesses
        processModel.shutdownTimeLimit
        processModel.startupTimeLimit
        processModel.pingingEnabled
        processModel.pingInterval
        processModel.pingResponseTime
        recycling.disallowOverlappingRotation
        recycling.disallowRotationOnConfigChange
        recycling.periodicRestart.memory
        recycling.periodicRestart.privateMemory
        recycling.periodicRestart.requests
        recycling.periodicRestart.time
        failure.loadBalancerCapabilities
        failure.orphanWorkerProcess
        failure.orphanActionExe
        failure.orphanActionParams
        failure.rapidFailProtection
        failure.rapidFailProtectionInterval
        failure.rapidFailProtectionMaxCrashes
        failure.autoShutdownExe
        failure.autoShutdownParams
        cpu.limit
        cpu.action
        cpu.resetInterval
        cpu.smpAffinitized
        cpu.smpProcessorAffinityMask
    /;

    my $params = $self->get_params_as_hashref(
        'apppoolname',
        @settings,
        'recycling.periodicRestart.schedule',
        'appPoolAdditionalSettings');
    $params->{applicationPool} = $params->{apppoolname};

    my $recycling = delete $params->{'recycling.periodicRestart.schedule'};

    my %options = (
        recycling_periodic_restart => $recycling
    );

    my @time_span_parameters = qw(
        processModel.startupTimeLimit
        processModel.shutdownTimeLimit
        processModel.idleTimeout
        processModel.pingInterval
        processModel.pingResponseTime
        recycling.periodicRestart.time
        cpu.resetInterval
        failure.rapidFailProtectionInterval
    );

    my @minutes_parameters = qw(
        processModel.idleTimeout
        cpu.resetInterval
        recycling.periodicRestart.time
        failure.rapidFailProtectionInterval
    );

    # Some parameters require special format
    for my $time_param (@time_span_parameters) {
        if (is_int($params->{$time_param})) {
            my $seconds;
            if (grep { $time_param eq $_ } @minutes_parameters) {
                $seconds = $params->{$time_param} * 60;
            }
            else {
                $seconds = $params->{$time_param};
            }

            my $span = $self->driver->seconds_to_time_span($seconds);
            $params->{$time_param} = $span;
        }
    }

    $self->create_or_update_app_pool($params, \@settings, \%options);
}


sub set_cmd_line {
    my ($self, $cmd_line, $property) = @_;

    $property = 'cmdLine' unless $property;
    $self->ec->setProperty("/myCall/$property", $cmd_line);
    print "Wrote command to property $property\n";
}

sub step_recycle_app_pool {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/applicationPool/);
    my $command = $self->driver->recycle_app_pool_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);
}

sub step_create_or_update_vdir {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/appname
        path
        physicalpath
        createDirectory
        credential
    /);
    $params = {
        applicationName => $params->{appname},
        path => $params->{path},
        physicalPath => EC::Plugin::Core::canon_path($params->{physicalpath}),
        createDirectory => $params->{createDirectory},
        credential => $params->{credential},
    };
    $params->{path} = '/' . $params->{path} unless $params->{path} =~ m/^\//;

    my $creds;
    if ($params->{credential}) {
        $creds = $self->_get_credentials($params->{credential});
    }

    my $vdir = "$params->{applicationName}$params->{path}";
    $vdir =~ s/\/+/\//g;
    $params->{vdirName} = $vdir;
    my $command;
    if ($self->driver->check_vdir_exists($vdir)) {
        $self->logger->info("Virtual directory $vdir already exists, going to update it");
        $command = $self->driver->update_vdir_cmd($params);
    }
    else {
        $self->logger->info("Virtual directory $vdir does not exists, proceeding to creating it");
        $command = $self->driver->create_vdir_cmd($params);
    }
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);

    if ($params->{createDirectory}) {
        $self->_create_directory($params->{physicalPath});
    }

    if ($creds) {
        my $vdir = $params->{vdirName};
        $vdir =~ s/\/$//;
        $self->logger->info(qq{Going to set credentails for directory "$vdir"});
        my $cmd =  $self->driver->set_vdir_creds_cmd({vdirName => $vdir, creds => $creds});
        my $res = $self->run_command($cmd);
        $self->_process_result($res);
    }
}


sub step_add_ssl_certificate {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        port
        certStore
        certHash
        ip
        certHostName
    /);

    unless($params->{ip} || $params->{certHostName}) {
        $self->bail_out('Either IP or Hostname should be provided');
    }

    my $hash = $params->{certHash};
    $hash =~ s/\s+//g;
    $hash =~ s/\W//gi;
    $hash = uc $hash;

    my $guid = $self->ec->getProperty('/myJob/id')->findvalue('//value');
    my $appid = $guid;

    my $certificate = $self->driver->get_ssl_certificate($params);
    my $result;
    if ($certificate && $certificate->{'Certificate Hash'}) {
        $self->logger->info("Certificate already exists, with hash $certificate->{'Certificate Hash'}");
        my $delete_command = $self->driver->delete_ssl_certificate_cmd($params);
        $result = $self->run_command($delete_command);
        $self->_process_result($result);
        my $command = $self->driver->add_ssl_certificate_cmd({hash => $hash, appid => $appid, %$params});
        $result = $self->run_command($command);
    }
    else {
        my $command = $self->driver->add_ssl_certificate_cmd({hash => $hash, appid => $appid, %$params});
        $result = $self->run_command($command);
    }
    $self->_process_result($result);
}

sub _is_xml {
    my ($content) = @_;

    return $content =~ m/[<>]/;
}

sub _save_params_file {
    my ($content) = @_;

    my $filename = "param_file_" . EC::Plugin::Core::gen_random_numbers(42) . ".xml";
    open my $fh, ">" . $filename or die "Cannot open file $filename: $!";
    print $fh $content;
    close $fh;
    return $filename;
}

sub step_delete_app_pool {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/apppoolname strictMode/);
    $params->{applicationPool} = $params->{apppoolname};
    my $name = $params->{applicationPool};

    unless($self->driver->check_app_pool_exists($name)) {
        if ($params->{strictMode}) {
            return $self->bail_out("Application pool $name does not exist");
        }
        else {
            return $self->warning("Application pool $name does not exist");
        }
    }
    my $command = $self->driver->delete_app_pool_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);
}

sub step_delete_web_site {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/websitename strictMode/);
    $params->{websiteName} = $params->{websitename};
    my $name = $params->{websiteName};

    unless($self->driver->check_site_exists($name)) {
        $self->logger->info(qq{Website "$name" does not exist});
        if ($params->{strictMode}) {
            return $self->bail_out("Website $name does not exist");
        }
        else {
            return $self->warning("Website $name does not exist");
        }
    }
    my $command = $self->driver->delete_site_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);
}

sub step_delete_vdir {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/appname strictMode/);
    $params->{vdirName} = $params->{appname};
    my $name = $params->{vdirName};

    unless ($self->driver->check_vdir_exists($name)) {
        $self->logger->info(qq{Virtual directory "$name" does not exist});
        if ($params->{strictMode}) {
            return $self->bail_out("Virtual directory $name does not exist");
        }
        else {
            return $self->warning("Virtual directory $name does not exist");
        }
    }
    my $command = $self->driver->delete_vdir_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);
}

sub step_list_sites {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        searchcriteria
        propertyName
        dumpFormat
        failOnEmpty
    /);
    $params->{criteria} = $params->{searchcriteria};
    my $command = $self->driver->list_sites_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);

    if ($result->{code} && $result->{code} eq '1') {
        if ($params->{failOnEmpty}) {
            $self->bail_out('No sites found');
        }
        else {
            $self->warning('No sites found');
            return;
        }
    }
    $self->_process_result($result);

    my $stdout = $result->{stdout};
    my @lines = split /[\n\r]/ => $stdout;

    my %data = map { /SITE\s"(.*)"\s\(id:(\d+),bindings:(.+),state:(\w+)\)/; $1 => {
        id => $2,
        bindings => [ split ',' => ($3 || '') ],
        state => $4,
    }} @lines;

    $self->logger->debug(\%data);
    $params->{propertyName} ||= '/myJob/IISSiteList';

    my $xml_handler = sub {
        my ($hashref) = @_;

        my @list = ();
        for my $sitename (keys %$hashref) {
            my $v = $hashref->{$sitename};
            $v->{name} = $sitename;
            push @list, $v;
        }
        return {site => \@list};
    };

    $self->save_retrieved_data(
        data => \%data,
        raw => $stdout,
        format => $params->{dumpFormat},
        property => $params->{propertyName},
        xml_handler => $xml_handler
    );
}


sub step_add_site_binding {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        websitename
        bindingprotocol
        bindinginformation
        hostHeader
    /);

    for my $req_param (qw/websitename bindingprotocol bindinginformation/) {
        unless($params->{$req_param}) {
            $self->bail_out(qq{Required parameter "$req_param" is missing});
        }
    }

    # TODO update existing binding wtih host header
    my $binding_info = $params->{bindinginformation};
    $binding_info .= ':' unless $binding_info =~ /:$/;
    if ($params->{hostHeader}) {
        $binding_info .= $params->{hostHeader};
    }
    my $site;
    eval {
        $site = $self->driver->get_site($params->{websitename});
        1;
    } or do {
        my $err = $@;
        $self->bail_out($err);
    };
    if ($site->{bindings}) {
        my $exists = 0;
        for my $binding_str (split(',', $site->{bindings})) {
            my ($protocol, $info) = split('/', $binding_str);
            my ($host, $port, $header) = split(':', $info);

            $self->logger->info("Found binding: protocol $protocol, info: $info");
            if ($protocol eq $params->{bindingprotocol}
                && $info eq $binding_info) {
                $exists = 1;
            }
        }
        if ($exists) {
            $self->logger->info("Binding already exists, skipping");
            return;
        }
    }

    my $command = $self->driver->add_site_binding_cmd({%{$params}, bindinginformation => $binding_info});
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);
    $self->_process_result($result);
}


sub step_list_pools {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/searchcriteria propertyName dumpFormat failOnEmpty/);
    my $command = $self->driver->list_pools_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);

    if ($result->{code} && $result->{code} eq '1') {
        if ($params->{failOnEmpty}) {
            $self->bail_out('No application pools found');
        }
        else {
            $self->warning('No application pools found');
            return;
        }
    }

    $self->_process_result($result);

    # APPPOOL "site" (MgdVersion:v4.0,MgdMode:Integrated,state:Started)
    my $stdout = $result->{stdout};
    my @lines = split /[\n\r]/ => $stdout;
    my %data = map {
        m/APPPOOL\s"(.*)"\s\(MgdVersion:(.+),MgdMode:(\w+),state:(\w+)\)/;
        $1 => {
            managedVersion => $2,
            managedPipelineMode => $3,
            state => $4,
        }
    } @lines;

    $params->{propertyName} ||= '/myJob/IISSiteList';

    my ($total, $started, $stopped, $other) = (0, 0, 0, 0);
    for my $name (keys %data) {
        $total ++;
        if ($data{$name}->{state} eq 'Started') {
            $started++;
        }
        elsif ($data{$name}->{state} eq 'Stopped') {
            $stopped++;
        }
        else {
            $other++;
        }
    }

    my $summary = "Pools: $total detected\nStarted: $started detected\nStopped: $stopped detected\nOther: $other detected";
    $self->ec->setProperty('/myJobStep/summary', $summary);

    my $xml_handler = sub {
        my ($hashref) = @_;

        my @list = ();
        for my $name (keys %$hashref) {
            my $v = $hashref->{$name};
            $v->{name} = $name;
            push @list, $v;
        }
        return {applicationPool => \@list};
    };

    $self->save_retrieved_data(
        data => \%data,
        raw => $stdout,
        property => $params->{propertyName},
        format => $params->{dumpFormat},
        xml_handler => $xml_handler
    );
}

sub step_list_apps {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        sitename
        propertyName
        dumpFormat
        failOnEmpty
    /);
    $params->{websiteName} = $params->{sitename};
    my $command = $self->driver->list_apps_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);

    if ($result->{code} && $result->{code} eq '1') {
        if ($params->{failOnEmpty}) {
            $self->bail_out('No applications found');
        }
        else {
            $self->warning("No applications found");
            return;
        }
    }
    $self->_process_result($result);


    my $stdout = $result->{stdout};
    my @lines = split /[\n\r]/ => $stdout;

    my %data = map {
        m/APP\s"(.*)"\s\(applicationPool:(.*)\)/;
        $1 => {applicationPool => $2}
    } @lines;

    $params->{propertyName} ||= '/myJob/IISApps';

    my $xml_handler = sub {
        my ($hashref) = @_;

        my @list = ();
        for my $name (keys %$hashref) {
            my $v = $hashref->{$name};
            $v->{name} = $name;
            push @list, $v;
        }
        return {application => \@list};
    };


    $self->save_retrieved_data(
        data => \%data,
        property => $params->{propertyName},
        format => $params->{dumpFormat},
        raw => $stdout,
        xml_handler => $xml_handler,
    );

    my $total = scalar keys %data;
    my $summary = "Found $total applications";
    $self->ec->setProperty('/myJobStep/summary', $summary);
}

sub step_list_vdirs {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/vdirName propertyName dumpFormat failOnEmpty/);
    my $command = $self->driver->list_vdirs_cmd($params);
    $self->set_cmd_line($command);
    my $result = $self->run_command($command);

    if ($result->{code} && $result->{code} eq '1') {
        if ($params->{failOnEmpty}) {
            $self->bail_out('No virtual directories found');
        }
        else {
            $self->warning('No virtual directories found');
            return;
        }
    }

    if ($result->{code}) {
        return $self->bail_out("Cannot list vdirs: " . ($result->{stderr} || $result->{stdout}));
    }

    my $stdout = $result->{stdout};
    my @lines = split /[\n\r]/ => $stdout;

    my %data = map {
        m/VDIR\s"(.+)"\s\(physicalPath:(.+)\)/;
        $1 => {physicalPath => $2}
    } @lines;

    $params->{propertyName} ||= '/myJob/IISVirtualDirectories';

    my $xml_handler = sub {
        my ($hashref) = @_;
        my @list = ();
        for my $name (keys %$hashref) {
            my $v = $hashref->{$name};
            $v->{name} = $name;
            push @list, $v;
        }
        return {vdirs => \@list};
    };


    $self->save_retrieved_data(
        data => \%data,
        raw => $stdout,
        format => $params->{dumpFormat},
        property => $params->{propertyName},
        xml_handler => $xml_handler,
    );

    my $found = scalar keys %data;
    my $summary = "Found $found virtual directories";

    $self->ec->setProperty('/myJobStep/summary', $summary);
}


sub step_stop_server {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        additionalParams
        execpath
    /);

    my $cmd = $params->{execpath} ? qq{"$params->{execpath}"} : $self->iisreset;
    $cmd .= ' /STOP';

    if ($params->{additionalParams}) {
        $cmd .= ' ' . $params->{additionalParams};
    }

    my $result = $self->run_command($cmd);
    $self->_process_result($result);
}

sub step_stop_application_pool {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        apppoolname
        strictMode
    /);

    my $cmd = $self->driver->stop_app_pool_cmd({applicationPool => $params->{apppoolname}});
    my $result = $self->run_command($cmd);
    if (!$params->{strictMode}
        && $result->{code}
        && $result->{stdout} =~ /already stopped/) {
        $self->warning($result->{stdout});
    }
    else {
        $self->_process_result($result);
    }
}


sub step_reset_server {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        additionalParams
        execpath
    /);

    my $cmd = $params->{execpath} ? qq{"$params->{execpath}"} : $self->iisreset;

    if ($params->{additionalParams}) {
        $cmd .= ' ' . $params->{additionalParams};
    }

    my $result = $self->run_command($cmd);
    $self->_process_result($result);
}


sub step_start_server {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        additionalParams
        execpath
    /);

    my $cmd = $params->{execpath} ? qq{"$params->{execpath}"} : $self->iisreset;
    $cmd .= ' /START';

    if ($params->{additionalParams}) {
        $cmd .= ' ' . $params->{additionalParams};
    }

    my $result = $self->run_command($cmd);
    $self->_process_result($result);
}

sub save_data_to_property_sheet {
    my ($self, %params) = @_;

    my $data = $params{data};
    my $property = $params{property};
    my $expand = $params{expand};

    $self->logger->debug($data);
    if ($expand) {
        my $flat_map = _flatten_map($data, $property);
        for my $key ( sort keys %$flat_map) {
            $self->ec->setProperty($key, $flat_map->{$key});
        }
    }
    else {
        my $json = JSON::encode_json($data);
        $self->ec->setProperty($property, $json);
    }
    $self->logger->info("Retrieved data was written to $property");
    $self->success("Retrieved data was written to $property");
}


sub save_retrieved_data {
    my ($self, %param) = @_;

    my $format = $param{format};
    my $property = $param{property};

    my $data = $param{data};
    my $raw = $param{raw};

    my $xml_handler = $param{xml_handler};

    my $message;
    if ($format eq 'json') {
        my $json = JSON::encode_json($data);
        $message = "Data has been saved as JSON under $property";
        $self->logger->info("JSON to save", JSON->new->pretty->encode($data));
        $self->ec->setProperty($property, $json);
    }
    elsif ($format eq 'xml') {
        unless($xml_handler && ref $xml_handler eq 'CODE') {
            $self->bail_out('No xml_handler provided for XML output');
        }
        my $refined = $xml_handler->($data);
        my $xml = XMLout($refined, NoAttr => 1, RootName => 'data', XMLDecl => 1);
        $message = "Data has been saved as XML under $property";
        $self->logger->info("XML to save", $xml);
        $self->ec->setProperty($property, $xml);
    }
    elsif ($format eq 'propertySheet') {
        my $flat = _flatten_map($data, $property);
        for my $key ( sort keys %$flat ) {
            $self->ec->setProperty($key, $flat->{$key});
            $self->logger->info("Wrote property: $key -> $flat->{$key}");
        }
        $message = "Data has been saved as property sheet under $property";
    }
    else {
        $self->ec->setProperty($property, $raw);
        $message = "Raw data has been saved under property $property"
    }
    $self->success($message);
}

sub _process_result {
    my ($self, $result) = @_;

    $self->logger->debug($result);
    if ($result->{code} || $result->{stderr}) {
        $self->logger->error($result->{stderr});
        return $self->bail_out($result->{stderr} || $result->{stdout});
    }
    if ($result->{stdout} =~ m/ERROR\s*\(\s*message:(.+)\)/ms) {
        $self->warning($1);
    }
    elsif($result->{stdout} =~ m/Error:\s*(.+)/) {
        $self->warning($1);
    }
    else {
        # $self->logger->info("Result: $result->{stdout}");
        my $message = $result->{stdout};
        my $MAX_LENGTH = 255;
        if (length($message) > $MAX_LENGTH) {
            $message = substr($message, 0, $MAX_LENGTH) . '...';
        }
        $self->success($message);
    }
}

sub _message_from_result {
    my ($result) = @_;

    my $code = $result->{code} || 0;
    my $message = "Exit code: $code";
    if ($code == 0) {
        return $message;
    }
    if ($result->{stderr}) {
        return  "$message, $result->{stderr}";
    }
    if ($result->{stdout}) {
        return "$message, $result->{stdout}";
    }
    return $message;
}


sub is_int {
    my ($number) = @_;

    return $number && $number =~ m/^\d+$/;
}

sub run_command {
    my ($self, $command) = @_;

    $self->logger->info("Going to run command: $command");
    my $result = $self->SUPER::run_command($command);
    my $code = $result->{code} || 0;
    $self->logger->info("Exit code: $code");
    chomp $result->{stderr};
    chomp $result->{stdout};
    my $stderr = $result->{stderr} || 'N/A';
    my $stdout = $result->{stdout} || 'N/A';
    $stdout =~ s/(Error:|\[ERROR\])//;
    $stderr =~ s/(Error:|\[ERROR\])//;
    $self->logger->info('STDOUT: ' . $stdout);
    $self->logger->info('STDERR: ' . $stderr);
    return $result;
}



sub step_check_server_status {
    my ($self) = @_;

    my $params = $self->get_params_as_hashref(qw/
        configname
        checkUrl
        expectStatus
        unavailable
        checkTimeout
        checkRetries
        credential
    /);
    my $url = '';
    my $port = '';
    my $user;
    my $pass;
    if ($params->{configname}) {
        my $config = $self->getConfiguration($params->{configname});
        if ($config->{iis_url}) {
            $url = $config->{iis_url};
        }
        else {
            $self->bail_out(qq{Cannot get IIS URL from config "$params->{configname}"});
        }

        if ($config->{iis_port}) {
            $port = $config->{iis_port};
        }

        if ($config->{user}) {
            $user = $config->{user};
        }
        if ($config->{password}) {
            $pass = $config->{password};
        }

    }

    if ($params->{credential}) {
        # Get user/password out of credential
        my $xpath = $self->ec->getFullCredential($params->{credential});
        $user = $xpath->findvalue('//userName')->string_value;
        $pass = $xpath->findvalue('//password')->string_value;
    }

    if ( $port ne '' ) {
        $url =~ s/(\/*)$/:$port/;
    }

    my %opt = (
        url => $params->{checkUrl} || $url,
        status => $params->{expectStatus},
        unavailable => $params->{unavailable},
        timeout => $params->{checkTimeout},
        tries => $params->{checkRetries},
        user => $user,
        pass => $pass
    );

    unless($opt{url}) {
        $self->bail_out("URL must be specified either in configuration or in the procedure parameters.");
    }


    my $error = $self->check_http_status(%opt);

    # Check the outcome of the response
    if ( !$error ) {
        $self->logger->info("URL successful (expected $opt{status}): $url");
    }
    else {
        $self->logger->info("Error: $error");
    }
    my %props = ();
    $props{'checkServerStatusLine'} = $url;
    $props{'checkServerStatusError'} = $error;
    $self->setProperties( \%props );
    if ($error) {
        $self->bail_out($error);
    }
}

=head2 check_http_status( %options )

%options may include:

=over

=item * url - what server & path we're interested in
=item * status - http status code (default 200, but we may be expecting others as well).
=item * unavailable [_] - if checked, regard failure to connect at all as expected result (e.g. we just stopped server and want to make sure it is down now).
=item * content = regex - if given, check that such text is available on the page
=item * timeout - connect timeout
=item * tries - try again if timed out

=back

=cut

sub check_http_status {
    my ($self, %opt) = @_;

    my $url = $opt{url};
    defined $url or die "check_http_status(): url parameter is required";
    $url =~ m#^https?://# or $url = "http://$url";
    $opt{timeout} ||= 30;
    $opt{tries}   ||= 1;
    $opt{status}  ||= 200;
    if (defined $opt{user} xor defined $opt{pass}) {
        carp sprintf "check_http_status(): ignoring %s without %s - both must be defined"
            , (defined $opt{user})?('user', 'pass'):('pass', 'user');
    };

    # TODO check that server resolves first and DIE if not

    $self->logger->info("Using timeout: $opt{timeout} seconds");
    # check port availability if asked to do so
    if ($opt{unavailable}) {
        my ($host, $port) = $url =~ m#https?://([\w\-\.]+)(?::(\d+))?(?:[/?]|$)#;

        croak "check_http_status(): malformed URL $url"
            unless $host;
        $port ||= 80;

        my $outcome;
        for (1 .. $opt{tries}) {
            local $SIG{ALRM} = sub { die "timeout" };
            eval {
                alarm $opt{timeout};
                my $sock = IO::Socket::INET->new(
                    Proto => 'tcp', PeerHost => $host, PeerPort => $port );
                $sock and $outcome = "Server available at $host:$port";
                close $sock if $sock;
            };
            alarm 0;
            return $outcome if $outcome;
        };

        return '';
    };

    # Finally, go for HTTP request
    my $agent = LWP::UserAgent->new(
        env_proxy => 1,
        keep_alive => 1,
        timeout => $opt{timeout}
    );
    my $request = HTTP::Request->new( GET => $url );

    $self->logger->info("Request: " . $request->as_string);

    if (defined $opt{user} and defined $opt{pass}) {
        $self->logger->info("Username: $opt{user}, password: ****");
        $request->authorization_basic( $opt{user}, $opt{pass} );
    };

    my $response;
    my $status = qr/$opt{status}/;
    my $success;
    do {
        $response = $agent->request($request);
        $opt{tries}--;
        $self->logger->info("Retries left: $opt{tries}");
        $success = ($response->code =~ $status);
        sleep 10;
    } while ($opt{tries} > 0 && !$success);


    return $success ? '' : "Expected $opt{status}, got ".$response->status_line;
}

sub _flatten_map {
    my ($map, $prefix) = @_;

    $prefix ||= '';
    my %retval = ();
    for my $key (keys %$map) {
        my $value = $map->{$key};
        if (ref $value eq 'ARRAY') {
            my $counter = 1;
            my %copy = map { my $key = ref $_ ? $counter ++ : $_; $key => $_ } @$value;
            $value = \%copy;
        }
        if (ref $value) {
            %retval = (%retval, %{_flatten_map($value, "$prefix/$key")});
        }
        else {
            $retval{"$prefix/$key"} = $value;
        }
    }
    return \%retval;
}


sub _create_directory {
    my ($self, $path) = @_;

    $self->logger->info(qq{Going to create directory "$path"});
    my $normalized = EC::Plugin::Core::canon_path($path);
    if (-e $normalized) {
        $self->logger->info(qq{Directory "$normalized" already exists, skipping});
        return;
    }
    my $ok = mkpath($normalized);
    unless($ok) {
        $self->logger->warning("Cannot create directory: $!");
    }
    else {
        $self->logger->info(qq{Created directory "$normalized"});
    }
}


sub _get_credentials {
    my ($self, $credential) = @_;

    # Get user/password out of credential
    my $xpath = $self->ec->getFullCredential($credential);
    my $user = $xpath->findvalue('//userName')->string_value;
    my $pass = $xpath->findvalue('//password')->string_value;
    $self->logger->add_secrets($pass);
    return {userName => $user, password => $pass};
}

1;
