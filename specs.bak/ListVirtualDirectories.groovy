import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class ListVirtualDirectories extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs ListVirtualDirectories'
    static def iisHandler
    static def procName = 'ListVirtualDirectories'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                vdirName: '',
                propertyName: '',
                dumpFormat: '',
                failOnEmpty: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    @Unroll
    def "show one vdir, property #propertyName, dump format #dumpFormat"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        vdirName: '$siteName/',
                        dumpFormat: '$dumpFormat',
                        propertyName: '$propertyName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)

            def resultProperty = propertyName ? propertyName : '/myJob/IISVirtualDirectories'
            switch(dumpFormat) {
                case '' :
                    validateResultPlaintext(result.jobId, resultProperty, siteName)
                    break
                case 'propertySheet':
                    validateResultPropertySheet(result.jobId, resultProperty, siteName)
                    break
                case 'json':
                    validateResultJson(result.jobId, resultProperty, siteName)
                    break
                case 'xml':
                    validateResultXML(result.jobId, resultProperty, siteName)
                    break
                default:
                    throw new RuntimeException("Don't know how to validate $dumpFormat")
            }
        cleanup:
            removeSite(siteName)
        where:
            dumpFormat << ['', 'propertySheet', 'json', 'xml']
            propertyName = dumpFormat ? '/myJob/result' : ''
    }

    @Unroll
    def "Show multiple vdirs #criteria"() {
        given:
            (1..3).each {
                createSite("Site ${it}")
            }
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        vdirName: '$criteria',
                        propertyName: '/myJob/result'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.log)
            def saved = getJobProperty('/myJob/result', result.jobId)
            logger.debug(saved)
            (1..3).each {
                assert saved =~ /VDIR \"Site ${it}\/\"/
            }
        cleanup:
            (1..3).each {
                removeSite("Site ${it}")
            }
        where:
            criteria << ['', '/path:/']
    }


    @Unroll
    def "Empty list #failOnEmpty"() {
        given:
            def criteria = '/path:no_such_path'
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        vdirName: '$criteria',
                        propertyName: '/myJob/result',
                        failOnEmpty: '$failOnEmpty'
                    ]
                )
            """
        then:
            if (failOnEmpty == '1') {
                assert result.outcome == 'error'
            }
            else {
                assert result.outcome == 'warning'
            }
        where:
            failOnEmpty << ['1', '0']
    }


    def validateResultPlaintext(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /VDIR \"$name\/\"/
    }

    def validateResultXML(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /physicalPath/
    }

    def validateResultJson(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(result)
        assert object["${name}/"].physicalPath
    }

    def validateResultPropertySheet(jobId, propertyName, name) {
        def result = getJobProperties(jobId)
        assert result.result["${name}"].physicalPath
    }

}
