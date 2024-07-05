import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class ListSiteApps extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs ListSiteApps'
    static def iisHandler
    static def procName = 'ListSiteApps'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                sitename: '',
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
    def "show one app, property #propertyName, dump format #dumpFormat"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
            def appName = 'app'
            createApp(siteName, appName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        sitename: '$siteName',
                        dumpFormat: '$dumpFormat',
                        propertyName: '$propertyName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)

            def resultProperty = propertyName ? propertyName : '/myJob/IISApps'
            switch(dumpFormat) {
                case '' :
                    validateResultPlaintext(result.jobId, resultProperty, siteName, appName)
                    break
                case 'propertySheet':
                    validateResultPropertySheet(result.jobId, resultProperty, siteName, appName)
                    break
                case 'json':
                    validateResultJson(result.jobId, resultProperty, siteName, appName)
                    break
                case 'xml':
                    validateResultXML(result.jobId, resultProperty, appName)
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

    def "Show multiple apps"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
            (1..3).each {
                createApp(siteName, "App ${it}")
            }
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        sitename: '$siteName',
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
                assert saved =~ /APP \"$siteName\/App ${it}\"/
            }
        cleanup:
            removeSite(siteName)
    }

    @Unroll
    def "No apps #failOnEmpty"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        sitename: 'no_such_site',
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


    def validateResultPlaintext(jobId, propertyName, siteName, appName) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /APP \"$siteName\/$appName\"/
    }

    def validateResultXML(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /applicationPool/
    }

    def validateResultJson(jobId, propertyName, siteName, appName) {
        def result = getJobProperty(propertyName, jobId)
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(result)
        assert object["${siteName}/${appName}"].applicationPool

    }

    def validateResultPropertySheet(jobId, propertyName, siteName, appName) {
        def result = getJobProperties(jobId)
        logger.debug(objectToJson(result))
        assert result.result[siteName][appName].applicationPool

    }

}
