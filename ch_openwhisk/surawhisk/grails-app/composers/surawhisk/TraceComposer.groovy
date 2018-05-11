package surawhisk

class TraceComposer extends zk.grails.Composer {

	private process

	def afterCompose = { wnd ->
		println "doing TraceComposer"
		// def params = desktop.getAttribute('$JQ_REQUEST_PARAMS$')
		// println desktop.getQueryString()
		if(process == null) {
			process = "node /root/WITT/witt.js".execute()
		}

		def iframe = $("#iframe")[0]
		iframe.width = "100%"
		iframe.height = "50em"
		iframe.src = "http://localhost:3000"
	}

}