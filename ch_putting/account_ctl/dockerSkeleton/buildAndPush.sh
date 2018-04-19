docker build -t chanwit/account_ctl:$1 .
docker push chanwit/account_ctl:$1

wsk -i action delete account_ctl
wsk -i action create --docker=chanwit/account_ctl:$1 account_ctl
