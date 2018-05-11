package surawhisk

class NavComposer extends zk.grails.Composer {

	private desktopWidth
	private desktopHeight

	def afterCompose = { wnd ->
		$(wnd).on('clientInfo', { evt ->
			desktopWidth = evt.desktopWidth
			desktopWidth = evt.desktopHeight
		})

		$('#settings').on('click', {
			$("#namespace").parent().removeClass("active")

			$("#settings").parent().toggleClass("active")
			$d("#incmain").src("settings.zul")
		})

		$("#namespace").on("click", {
			$("#settings").parent().removeClass("active")

			$("#namespace").parent().toggleClass("active")
			$d("#incmain").src("namespaces.zul")
		})

		/*
		$("#trace").on("click", {
			$("#settings").parent().removeClass("active")

			$("#trace").parent().toggleClass("active")
			$d("#incmain").src("trace.zul")
			// $d("#incmain").redirect("trace.zul", [width: desktopWidth, height: desktopHeight])
		})
		*/

		$("#create").on("click", {
			$("#namespace").parent().removeClass("active")
			$("#settings").parent().removeClass("active")

			$d("#incmain").src("actions_create.zul")
		})

		$("#invoke").on("click", {
			$("#namespace").parent().removeClass("active")
			$("#settings").parent().removeClass("active")

			$d("#incmain").src("actions_invoke.zul")
		})

	}

}