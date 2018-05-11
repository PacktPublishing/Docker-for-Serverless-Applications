package surawhisk

import io.swagger.client.*
import io.swagger.client.api.*
import io.swagger.client.auth.*
import io.swagger.client.model.*
import com.google.gson.*
import org.zkoss.zhtml.Text

import surawhisk.Util

class InvokeComposer extends zk.grails.Composer {

	def afterCompose = { wnd ->

		def r = Util.namespaces().getAllEntitiesInNamespace("guest")
		r.actions.each { action ->
			$('#actions').append """
				<z:listitem value=\"${action.name}\">
					<z:listcell label=\"${action.name}\"/>
				</z:listitem>
			"""
			$('#actions').setSelectedIndex(0)
		}

		$('#btn_invoke').on('click', {
			def param_size = $('#params').children.flatten().size()
			KeyValue payload = []
			param_size.times { i ->
				def key = $("#param_key_$i").val()
				def value = $("#param_value_$i").val()
				payload.put(key, value)
			}

			def action = $('#actions').val()

			def activation = Util.actions().invokeAction("guest", "", action, payload, "true", "false", 60000)
			def result = activation.response.result
			def gson = new GsonBuilder().setPrettyPrinting().create()
			$("#result")[0].setValue("${gson.toJson(result)}")
		})

		$('#btn_add_param').on('click', {
			def next_id = $('#params').children.flatten().size()

			$('#params').append """
				<x:div id=\"param_${next_id}\" class=\"row top-buffer\">
		            <div class=\"col-md-2\"/>
		            <div class=\"col-md-3\">
		               <z:textbox id=\"param_key_${next_id}\" class=\"form-control\"/>
		            </div>
		            <div class=\"col-md-3\">
		               <z:textbox id=\"param_value_${next_id}\" class=\"form-control\"/>
		            </div>
		            <div class=\"col-md-3\">
		            	<z:button id=\"btn_del_param_${next_id}\" class=\"btn btn-default\">Remove</z:button>
		            </div>
	            </x:div>
			"""
			$("#btn_del_param_${next_id}").on('click', {
				$("#param_${next_id}").detach()
			})
		})

	}

}