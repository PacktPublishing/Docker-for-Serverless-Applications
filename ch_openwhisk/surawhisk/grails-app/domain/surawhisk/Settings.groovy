package surawhisk

class Settings {

	String  endpoint
	String  apiKey
	Boolean active = false

	def makeActive() {
		Settings.withTransaction { status ->
			Settings.executeUpdate("update Settings s set s.active=false")
			this.active = true
			return this.save(flush: true)
		}
	}

}