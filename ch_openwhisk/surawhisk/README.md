# surawhisk-ui

A User Interface portal for OpenWhisk.
Recommended only for local development.

To run:
```
$ docker volume create data_vol

$ docker run -d -p 8080:8080 -v data_vol:/root/data surawhisk/ui
```

Open the Setting page and set the endpoint to your the API URL, for example `http://192.168.100.1/api/v1`.
The credential, please obtain from `wsk property get --auth` for your `guest` namespace.
