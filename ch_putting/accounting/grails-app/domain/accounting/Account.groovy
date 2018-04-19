package accounting

import grails.rest.*

@Resource(uri="/accounts")
class Account {

	String id
	String name
	Set<Entry> transactions

	Double getBalance() {
		def e = Entry.find("from Entry as e where e.account = :account order by e.createdDate desc", [account: this])
		if (e) {
			return e.balance
		} else {
			return 0
		}
	}

	static transients = ['balance']

    static constraints = {
    	name(nullable: false)
    }

    static mapping = {
    	id generator: 'assigned'
    }
}
