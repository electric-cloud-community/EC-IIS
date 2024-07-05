import spock.lang.*
import com.electriccloud.spec.*

class AddSSLCertificate extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs AddSSLCertificate'
    static def iisHandler
    static def procName = 'AddSSLCertificate'

    static def certificateP12 = '''
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
'''

    static def secondCertificate = '''
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
'''

    static def certHash = '‎87 8b 81 a1 ca 81 4f 14 47 13 2a bf cd 23 74 a9 42 15 47 ab'
    static def secondHash = '‎ac d0 81 c4 b2 ec f3 07 3c e7 e3 2c 2b 49 1b 3f 18 b3 08 a3'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                ip: '',
                certHostName: '',
                port: '',
                certStore: '',
                certHash: ''
            ]
        ]
        createHelperProject(resName)
        dsl """
                project '$projectName', {
                    procedure 'Import Certificate', {
                        resourceName = '$resName'
                        formalParameter 'certificate', defaultValue: '', {
                            type = 'textarea'
                        }
                        step 'Save Certificate', {
                            command = '''
                                use strict;
                                use warnings;
                                use ElectricCommander;
                                use MIME::Base64;

                                my \$base_64 = '\$[certificate]';
                                my \$decoded = decode_base64(\$base_64);
                                open my \$fh, '>c:/certificate.p12' or die \$!;
                                binmode \$fh;
                                print \$fh \$decoded;
                                close \$fh;
                            '''
                            shell = 'cb-perl'
                        }

                        step 'Import Certificate', {
                            command = '''
                                certutil -importpfx c:\\\\certificate.p12
                            '''
                        }
                    }
                }
        """

    }

    def doCleanupSpec() {
        if (!System.getenv('NO_CLEANUP')) {
            dsl "deleteProject '$projectName'"
        }
    }

    def "add certificate to site"() {
        given:
            def port = '9999'
            def siteName = 'Site With Certificate'
            createSite(siteName, 'c:/tmp/site', port)
            createCertificate()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        ip: '0.0.0.0',
                        port: '$port',
                        certStore: 'My',
                        certHash: '$certHash'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /SSL Certificate successfully added/
        cleanup:
            removeSite(siteName)
            runCmd("netsh http delete sslcert ipport=0.0.0.0:$port")
    }


    def "add certificate to hostname"() {
        given:
            def port = '9999'
            def siteName = 'Site With Certificate'
            createSite(siteName, 'c:/tmp/site', port)
            createCertificate()
            def hostname = 'localhost'
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        certHostName: '$hostname',
                        port: '$port',
                        certStore: 'My',
                        certHash: '$certHash'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /SSL Certificate successfully added/
            def certificates = showCertificates()
            assert certificates =~ /Hostname:port\s*:\s*localhost:9999/
        cleanup:
            removeSite(siteName)
            runCmd("netsh http delete sslcert hostnameport=$hostname:$port")
    }


    def "update certificate"() {
        given:
            def port = '9999'
            def siteName = 'Site With Certificate'
            createSite(siteName, 'c:/tmp/site', port)
            createCertificate()
            def res = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        ip: '0.0.0.0',
                        port: '$port',
                        certStore: 'My',
                        certHash: '$certHash'
                    ]
                )
            """
            assert res.outcome == 'success'
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        ip: '0.0.0.0',
                        port: '$port',
                        certStore: 'My',
                        certHash: '$certHash'
                    ]
                )
            """

        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
            runCmd("netsh http delete sslcert ipport=0.0.0.0:$port")

    }

    def "change certificate"() {
        given:
            def port = '9999'
            def siteName = 'Site With Certificate'
            createSite(siteName, 'c:/tmp/site', port)
            createCertificate()
            createSecondCertificate()
            def res = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        ip: '0.0.0.0',
                        port: '$port',
                        certStore: 'My',
                        certHash: '$certHash'
                    ]
                )
            """
            assert res.outcome == 'success'
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        ip: '0.0.0.0',
                        port: '$port',
                        certStore: 'My',
                        certHash: '$secondHash'
                    ]
                )
            """

        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def output = runCmd("netsh http show sslcert")
            logger.debug(output)
            def hash = secondHash.replaceAll(/\W/, '')
            assert output =~ /$hash/
        cleanup:
            removeSite(siteName)
            runCmd("netsh http delete sslcert ipport=0.0.0.0:$port")

    }


    def "negative: wrong cert hash"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        certHostName: 'localhost',
                        port: '99',
                        certStore: 'My',
                        certHash: 'wrong'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)

    }


    def "negative: wrong store name"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        certHostName: 'localhost',
                        port: '99',
                        certStore: 'some store',
                        certHash: '$certHash'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)

    }


    def createCertificate() {
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Certificate',
                actualParameter: [
                    certificate: '''$certificateP12'''
                ]
            )
        """
        assert result.outcome == 'success'
    }

    def createSecondCertificate() {
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Certificate',
                actualParameter: [
                    certificate: '''$secondCertificate'''
                ]
            )
        """
        assert result.outcome == 'success'
    }


    def showCertificates() {
        def res = runCmd("netsh http show sslcert")
        return res
    }
}
