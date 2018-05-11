package surawhisk

import io.swagger.client.*;
import io.swagger.client.api.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import com.google.gson.*;

class Util {

	// private static final String API_KEY="23bc46b1-71f6-4ed5-8c54-816aa4f8c502"
	// private static final String API_PASS="123zO3xZCLrMN6v2BKK1dXYFpXlPkccOFqm12CdAsMgRU4VrNZ9lyGVCGuMDGIwP"
	// https://192.168.33.13/api/v1

	private static cli() {
		Settings.withNewSession {
			def s = Settings.findByActive(true)
			if (s) {
				def (username, pass) = s.apiKey.split(":")
				return new ApiClient(
					basePath: s.endpoint,
					verifyingSsl: false,
					debugging: true,
					username: username,
					password: pass)
			}
		}
	}

	public static actions() {
		return new ActionsApi(cli())
	}

	public static namespaces() {
		return new NamespacesApi(cli())
	}


}