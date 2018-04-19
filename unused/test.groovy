100000.times {
	def ps = "echo 'How are you?'".execute() | "faas-cli invoke hello".execute()
	ps.waitFor()
}