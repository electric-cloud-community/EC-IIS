def projName = args.projName
def resName = args.resName

def params = [
  appname: '',
  physicalpath: '',
  path: '',
  createDirectory: ''
]

project projName, {
    procedure 'Create App', {
      resourceName = resName


      params.each {name, defValue ->
        formalParameter name, defaultValue: defValue, {
          type = 'textarea'
        }
      }
      formalParameter 'credential', defaultValue: '', {
        type = 'credential'
      }
      step 'Create App', {
        description = ''
        subprocedure = 'CreateWebApplication'
        subproject = '/plugins/EC-IIS/project'

        params.each { name, defValue ->
          actualParameter name, '$[' + name + ']'
        }
        actualParameter 'credential', '$[credential]'
        attachParameter(formalParameterName: 'credential')
      }
  }
}
