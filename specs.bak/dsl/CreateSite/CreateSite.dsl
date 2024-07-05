def projName = args.projName
def resName = args.resName

def params = [
  websitename: '',
  websitepath: '',
  bindings: '',
  websiteid: '',
  createDirectory: '',
]

project projName, {
    procedure 'Create Site', {
      resourceName = resName

      params.each {name, defValue ->
        formalParameter name, defaultValue: defValue, {
          type = 'textarea'
        }
      }

      formalParameter 'credential', defaultValue: '', {
        type = 'credential'
      }

      step 'Create Site', {
        description = ''
        subprocedure = 'CreateWebSite'
        subproject = '/plugins/EC-IIS/project'

        params.each { name, defValue ->
          actualParameter name, '$[' + name + ']'
        }

        actualParameter 'credential', '$[credential]'
        attachParameter(formalParameterName: 'credential')
      }



  }
}
