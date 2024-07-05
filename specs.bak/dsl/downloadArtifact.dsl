def projName = args.projName
def url = args.url
def resName = args.resName

project projName, {

    procedure 'Download And Unpack', {
        resourceName = resName
        step 'Download & Unpack', {
            command = '''
use strict;
use warnings;
use Archive::Zip;
use LWP::UserAgent;
use Data::Dumper;
use Cwd qw(chdir);
use File::Basename;

my $ua = LWP::UserAgent->new;
my $url = \'$[url]\';

my $request = HTTP::Request->new(GET => $url);
print Dumper $request;

my $path = \'$[artifactPath]\';
my $dir = dirname($path);
chdir($dir);

if (-f $path) {
    unlink $path;
}
print "$path\\n";
open my $fh, \">$path\" or die $!;
binmode($fh);
my $size = 0;
my $response = $ua->request($request, sub {
    my ($bytes, $res) = @_;

    $size += length($bytes);

    if ($res->is_success) {
        print $fh $bytes;
    }
    else {
        print $res->code;
        exit -1;
    }
});

close $fh;


my $zip = Archive::Zip->new;
unless($zip->read($path) == Archive::Zip::AZ_OK) {
    die \'read error\';
}
$zip->extractTree('');

print "\\nExtracted\\n";
            '''
            shell = 'cb-perl'
        }
        formalParameter 'url', defaultValue: '', {
            type = "textarea"
        }

        formalParameter 'artifactPath', defaultValue: '', {
            type = 'entry'
        }
    }

}
