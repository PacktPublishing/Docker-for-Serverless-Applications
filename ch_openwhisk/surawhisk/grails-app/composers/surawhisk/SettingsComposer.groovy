package surawhisk

import surawhisk.Settings

class SettingsComposer extends zk.grails.Composer {

	def afterCompose = { wnd ->

		Settings.withNewSession {
			def curSetting = Settings.findByActive(true)
			if(curSetting) {
				$("#endpoint").val(curSetting.endpoint)
				$("#api_key").val(curSetting.apiKey)
			}
		}

		$("#btn_save").on('click', {
			// load the active one, if existed
			def settings = new Settings()
			settings.endpoint = $("#endpoint").val()
			settings.apiKey = $("#api_key").val()
			settings = settings.makeActive()
			if(settings == null) {
				alert("Cannot save")
			} else {
				alert("Endpoint saved successfully")
			}

		})

	}

}