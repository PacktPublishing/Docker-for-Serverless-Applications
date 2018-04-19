package accounting

import grails.converters.JSON

class BootStrap {

    def init = { servletContext ->

    	JSON.registerObjectMarshaller(Account, { Account a ->
    		return [
    			id: a.id,
    			name: a.name,
    			balance: a.getBalance(),
    			transactions: a.transactions
    		]
    	})

    	def a = new Account(name:"test")
    	a.id = "A1234"
    	a = a.save()

    	def e = new Entry(account: a, amount: 1000).save()
    	a.addToTransactions(e)
    	a.save()
    }

    def destroy = {
    }
}
