package surawhisk

import io.swagger.client.model.*;

class ActionCreateComposer extends zk.grails.Composer {

	def afterCompose = { wnd ->

		$("#btn_create").on('click', {
			def actionName = $("#action_name").val()
			def kind = $("#kind").selectedItem().value
			def imageName = $("#image_name").val()

			def actionExec = new ActionExec(
				kind: ActionExec.KindEnum.fromValue(kind),
				image: imageName
			)
			def actionPut = new ActionPut(exec: actionExec)

			def api = Util.actions()
			def action = api.updateAction("guest", "", actionName, actionPut, "true")
			if(action) {
				alert("Action ${action.name} created successfully.")
			}
		})

	}

}