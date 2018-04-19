package accounting

import grails.rest.*

@Resource(uri="/entries")
class Entry {

	Account account
	Double  amount
	Date    createdDate = new Date()
	Double  balance = 0

	static constraints = {
		account(nullable: false)
		amount(nullable: false)
	}

	def beforeInsert() {
		def e = Entry.find("from Entry as e where e.account = :account order by e.createdDate desc", [account: this.account])
		if (e) {
			this.balance = e.balance + this.amount
		} else {
			this.balance = this.amount
		}
	}

}
