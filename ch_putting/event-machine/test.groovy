def pwd = new File(".").getAbsolutePath() - '.'

def solFiles = new FileNameFinder().getFileNames("contracts", "**/*.sol")
println(solFiles.collect { it - pwd })
