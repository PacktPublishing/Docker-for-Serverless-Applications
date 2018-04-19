def pwd = new File(".").getAbsolutePath() - '.'

def solFiles = new FileNameFinder().getFileNames("contracts", "**/*.sol")
files = solFiles.collect { it - pwd }

truffleCompile = ['truffle', 'compile'].execute()

def (output, err) = new StringWriter().with { o -> // For the output
    new StringWriter().with { e ->                     // For the error stream
        truffleCompile.waitForProcessOutput( o, e )
        [ o, e ]*.toString()                             // Return them both
    }
}

if (truffleCompile.exitValue() == 0)
	println "Truffle compiled successful"
else {
    println output
    return -1
}

files.collect { it.split('/')[-1] - '.sol' }.each {
	web3 = [
	"./web3/bin/web3j",
		"truffle",
		"generate",
		"build/contracts/${it}.json",
		"-o", "src/main/java",
		"-p", "event.machine"
	].execute()

	web3.waitFor()

	if (web3.exitValue() == 0) println "Generated $it done ..."
}

/*

# docker run --rm --net=host parity/parity:stable-release --geth --chain dev --force-ui --reseal-min-period 0 --jsonrpc-cors http://localhost --jsonrpc-apis all

*/