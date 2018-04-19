require "base64"
require "io"

bzip = Base64.decode(STDIN)

put bzip
