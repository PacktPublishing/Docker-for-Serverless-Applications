package surawhisk

import io.swagger.client.*;
import io.swagger.client.api.*;
import io.swagger.client.auth.*;

class NamespacesComposer extends zk.grails.Composer {

	final String API_KEY="23bc46b1-71f6-4ed5-8c54-816aa4f8c502"
	final String API_PASS="123zO3xZCLrMN6v2BKK1dXYFpXlPkccOFqm12CdAsMgRU4VrNZ9lyGVCGuMDGIwP"

	private cli() {
		def auth = new HttpBasicAuth(username: API_KEY, password: API_PASS)

		def client = new ApiClient(basePath: "https://192.168.33.13/api/v1", verifyingSsl: false, debugging: true)
		client.authentications["basic"] = auth

		return new NamespacesApi(client)
	}

	def afterCompose = { wnd ->
		def tbody = $("tbody")
		cli().allNamespaces.eachWithIndex { n, i ->
			tbody.append("""
				<tr>
					<td>${i+1}</td>
					<td>${n}</td>
				</tr>
			""")
		}
	}

}