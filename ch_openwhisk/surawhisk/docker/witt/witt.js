#!/usr/bin/env node


/*
 * Copyright 2015-2016 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


const url = require("url"),
	path = require("path"),
	fs = require('fs'),
	https = require('https'),
	http = require('http'),
	io = require('socket.io'),
	argv = require('minimist')(process.argv.slice(2)),
	propertiesParser = require('properties-parser'),
    expandHomeDir = require('expand-home-dir'),
    wskprops = propertiesParser.read(process.env.WSK_CONFIG_FILE || expandHomeDir('~/.wskprops')),
	owProps = {
		apihost: wskprops.APIHOST || 'openwhisk.ng.bluemix.net',
		api_key: argv["key"] || wskprops.AUTH,
		namespace: wskprops.NAMESPACE || '_',
		ignore_certs: process.env.NODE_TLS_REJECT_UNAUTHORIZED == "0"
    },
    ow = require('openwhisk')(owProps),
    opn = require('opn');
    // exposedPort = process.env.EXPOSED_PORT || 8080

const removeBinaryCodeMsg = "Removed binary code.",
	  defaultLimit = 20,
	  MAX_ACTS = 2000;



var owData, fontInfo;


// Start a web server, listen to port 8080
var server = http.createServer(
	function (request, response){
		var uri = url.parse(request.url).pathname;
		var dir = path.resolve(__dirname);
		//var filename = path.join(process.cwd(), uri);
		var filename = path.join(dir, uri);

		fs.exists(filename, function(exists) {

			if(!exists) {
  				response.writeHead(404, {"Content-Type": "text/plain"});
  				response.write("404 Not Found\n");
  				response.end();
  				return;
			}

			if (fs.statSync(filename).isDirectory())
				filename += '/index.html';

			fs.readFile(filename, "binary", function(err, file) {
				if(err) {
					response.writeHead(500, {"Content-Type": "text/plain"});
					response.write(err + "\n");
					response.end();
					return;
				}

				response.writeHead(200);
				response.write(file, "binary");
				response.end();
			});

		});

	}
).listen(9090, function(e){
	//console.log(JSON.stringify(argv));
	if(argv["TLFont"] || argv["paneFont"]){
		fontInfo = {"TLFont": argv["TLFont"], "paneFont": argv["paneFont"]};
	}

	if(argv["h"] || argv["help"]){
		console.log("Use 'limit' to specify the number of activations (default 20, max 200) that you want to view (ex. witt --limit=50)");
		console.log("Use 'file' to show data in an exported JSON file (ex. witt --file=data.json)");

		server.close();
		return;
	}
	else if(argv["file"]){

		var filename;
		if(argv["file"].indexOf("~") == 0){
			var home = process.env.HOME || process.env.USERPROFILE;
			filename = path.join(home, argv["file"].substring(1));
		}
		else{
			filename = path.join(process.cwd(), argv["file"]);
		}

		console.log("Reading file "+filename+"...");

		fs.exists(filename, function(exists) {

			if(!exists) {
  				console.log("File does not exist");
  				server.close();
  				return;
			}

			if (fs.statSync(filename).isDirectory()){
				console.log("Path is a directory, not a file.");
				server.close();
  				return;
			}

			fs.readFile(filename, "binary", function(err, file) {
				if(err) {
					console.log("Cannot read file. Error: "+err);
					server.close();
  					return;
				}

 				// parse file to an object
 				try{

 					file = file.replace(/[\/]/g, '\\/').
 					replace(/[\b]/g, '\\b').replace(/[\f]/g, '\\f').replace(/[\n]/g, '\\n')
 					.replace(/[\r]/g, '\\r').replace(/[\t]/g, '\\t');


 					owData = JSON.parse(file);

 					// sort activations
 					var x = [];
 					Object.keys(owData.a).forEach(function(id){
 						x.push(owData.a[id]);
 					});

 					x.sort(function(a, b){
						if(b.start-a.start != 0)
							return b.start-a.start;
						else{
							if(b.cause == undefined && a.cause != undefined){
								return -1;
							}
							else if(b.cause != undefined && a.cause == undefined){
								return 1;
							}
							else
								return 0;
						}
					});

					owData.a = {};
					x.forEach(function(a){
						owData.a[a.activationId] = a;
					});

					//console.log(owData.e);
					Object.keys(owData.e).forEach(name => {
						if(owData.e[name].kind == "action"){
							if(removeBinaryCode(owData.e[name])){
								owData.e[name].exec.code = removeBinaryCodeMsg;
							}
						}
					})


 					console.log("Done reading the file");
 					console.log("Opening client web page at http://localhost:9090...");
 					console.log("To display a new set of data, Ctrl+C to stop the server and restart it again. ")
 					opn("http://localhost:9090");
 				}
 				catch(e){
 					console.log("File is not in a valid JSON format. Error: "+e);
 					server.close();
 					return;
 				}

			});

		});
	}
	else{

		getOpenwhiskData(argv["limit"]);
		var x = setInterval(function(){
			if(owData != undefined){
				clearInterval(x);
				console.log("Opening client web page at http://localhost:9090...");
				console.log("To display a new set of data, Ctrl+C to stop the server and restart it again. ")
				opn("http://localhost:9090");
			}
		}, 1000);
	}


});



//init socket.io
io(server).sockets.on("connection", function(socket){

	socket.on("init", function(){
		if(argv["file"]){
			// data read from file. always display the same thing
			owData.fontInfo = fontInfo;
			socket.emit("init", owData);
		}
		else{
			if(Object.keys(owData).length == 0){
				getOpenwhiskData(argv["limit"]);
				var x = setInterval(function(){
					if(Object.keys(owData).length > 0){
						clearInterval(x);
						socket.emit("init", owData);
						owData = {};
					}
				}, 1000);
			}
			else{
				owData.fontInfo = fontInfo;
				socket.emit("init", owData);
				owData = {};
			}

		}



	});

	socket.on("export", function(data){
		var name = data.name,
			content = 'function main(params){ return '+JSON.stringify(data.data)+'; };',
			action = {
	            exec: {
	                kind: 'nodejs:6',
	                code: content
	            },
	            annotations: [{ key: 'web-export', value: true }]
    		};

		ow.packages.list().then(function(packages){
			var isWittPackage = false;
			for(var i=0; i<packages.length; i++){
				if(packages[i].name.indexOf("wittData") != -1){
					isWittPackage = packages[i].name;
					break;
				}
			}
			if(isWittPackage != false){
				createWebAction(isWittPackage);
			}
			else{
				ow.packages.create({packageName: "wittData"}).then(function(package){
					createWebAction(package.name);
				}).catch(function(err){
					console.log("failed to create witt package", err);
				});
			}

		}).catch(err => {
		  console.error('failed to list packages', err)
		});

		function createWebAction(packageName){
			var fullName = packageName+"/"+name;
			ow.actions.list().then(function(actions){
				var isCreate = true;
				for(var i=0; i<actions.length; i++){
					if(actions[i].name == name){
						isCreate = false;
						break;
					}
				}

				if(isCreate){
					ow.actions.create({actionName: fullName, action: action}).then(function(r){
						var url = "https://"+owProps.apihost+"/api/v1/experimental/web/"+r.namespace+"/"+name+".json";
						socket.emit("url", url);
						console.log("created web action ", url);
					}).catch(function(err){
						console.log("failed to create a web action", err);
					});
				}
				else{
					ow.actions.update({actionName: fullName, action: action}).then(function(r){
						var url = "https://"+owProps.apihost+"/api/v1/experimental/web/"+r.namespace+"/"+name+".json";
						socket.emit("url", url);
						console.log("updated web action ", url);
					}).catch(function(err){
						console.log("failed to update a web action", err);
					});
				}

			});

		}


	});

});

// reuse the code from Raymond Camden's blog post
// https://www.raymondcamden.com/2017/05/15/my-own-openwhisk-stat-tool/
function getAllActivations(limit, cb, acts) {
    if(!acts) acts=[];
    let lim = ((limit-acts.length)>200? 200 : limit-acts.length);
    ow.activations.list({limit:lim, docs:true, skip:acts.length}).then(result => {
    	acts = acts.concat(result);
        console.log("Retrieved "+acts.length+" activations...");

        if(result.length === 0 || acts.length >= limit)
        	return cb(acts);
        else
        	getAllActivations(limit, cb, acts);
    }).catch(function(e){
		console.log(e);
	});
}



function getOpenwhiskData(limit, fontInfo){

	if(limit == undefined)
		limit = defaultLimit;
	if(limit > MAX_ACTS)
		limit = MAX_ACTS;

	var atvsDic, atvsDicId;
	var entities = [];

	console.log("Getting data from OpenWhisk server...");

	getAllActivations(limit, (atvs) => {
		/*console.log(result[0]);
		var rr = [];

		// atvs is the detailed activation records
		result.forEach(function(r){
			rr.push(ow.activations.get({activation:r.activationId}).then(function(data){
				atvs.push(data);
			}));
		});		*/

		if(atvs.length == 0){
			console.log("Witt retrieved 0 activations. Check if the wskprops file is picked up correctly.");
			process.exit(0);
		}

		atvs.sort(function(a, b){
			if(b.start-a.start != 0)
				return b.start-a.start;
			else{
				if(b.cause == undefined && a.cause != undefined){
					return -1;
				}
				else if(b.cause != undefined && a.cause == undefined){
					return 1;
				}
				else
					return 0;
			}
		});


		var entityCalls = [], tempNames = [];
		atvs.forEach(function(atv, index){
			// decide activation kind for every activation
			var k = "action";
			if(atv.end == undefined){
				// rule or trigger
				if(atv.cause == undefined)
					k = "trigger";
				else
					k = "rule";
			}
			else{
				// see if it's a sequence
				atv.annotations.forEach(function(a){
					if(a.key == "kind" && a.value == "sequence")
						k = "sequence";
				});
			}
			atvs[index].kind = k;

			if(tempNames.indexOf(atv.name) == -1){
				// if the entity name has not appeaered in tempNames, fetch its detail from the server
				tempNames.push(atv.name);

				var path, name=atv.name, namespace=atv.namespace;
				if(atv.annotations.length>0){
					for(var i=0; i<atv.annotations.length; i++){
						if(atv.annotations[i].key == "path"){
							path = atv.annotations[i].value;
							break;
						}
					}
				}
				if(path != undefined && path.lastIndexOf("/")>0){
					var i = path.lastIndexOf("/");
					name = path.substring(i+1);
					namespace = path.substring(0, i);
				}

				if(k == "action"){
				    entityCalls.push(ow.actions.get({actionName:name, namespace:namespace}).then(function(r){
				    	r.kind = "action";
				    	entities.push(r);
					}).catch(function(e){
						entities.push({name: name, kind: "action", deleted: true});
					}));
				}
				else if(k == "sequence"){
					entityCalls.push(ow.actions.get({actionName:name, namespace:namespace}).then(function(r){
				    	r.kind = "sequence";
				    	entities.push(r);
					}).catch(function(e){
						entities.push({name: name, kind: "sequence", deleted: true});
					}));
				}
				else if(k == "trigger"){
					 entityCalls.push(ow.triggers.get({triggerName:name, namespace:namespace}).then(function(r){
					 	r.kind = "trigger";
				    	entities.push(r);
					}).catch(function(e){
						entities.push({name: name, kind: "trigger", deleted: true});
					}));
				}
				else if(k == "rule"){
					 entityCalls.push(ow.rules.get({ruleName:name, namespace:namespace}).then(function(r){
					 	r.kind = "rule";
				    	entities.push(r);
					}).catch(function(e){
						entities.push({name: name, kind: "rule", deleted: true});
					}));
				}
			}

		});


		Promise.all(entityCalls).then(function(){

			console.log("Done retrieving all the required data from OpenWhisk server. Processing the data now... ")
			// turn entities into dictionary. key is name
			entities = arrayToDictionary(entities, "name", true);

			// turn activations into dictionary. atvsDicName's key is name (entity name). an entity can have multiple activation records
			// atvsDicId uses activationId as keys
			atvsDicName = arrayToDictionary(atvs);
			atvsDicId = arrayToDictionary(atvs, "activationId", true);

			//var aIds = Object.keys(atvsDicId);

			// construct relationship among entities from activation records
			// iterate each entity
			Object.keys(entities).forEach(function(key){
				var e = entities[key];


				// entity loop on RULES
				if(e.kind == "rule" && e.trigger != undefined && e.action != undefined){
					var triggerName = e.trigger.name, actionName = e.action.name;
					//console.log("rule found: "+e.name+". Searching activations of this rule");
					// for rule, show the trigger and the action it connects
					// find all the triggers associate with it (should only be one!)

					e.triggers = {};
					atvsDicName[e.name].forEach(function(a){
						// for each activation record
						if(a.cause != undefined && atvsDicId[a.cause] != undefined){
							// a.cause is the activation id of the trigger activation record
							var tn = atvsDicId[a.cause].name;
							if(tn != triggerName){
								console.log("Warning: a rule connects to a trigger different from what's in its trigger property specified. Rule: "+e.name+", trigger: "+tn+", specified trigger: "+triggerName);
							}

							if(e.triggers[tn] == undefined){
								e.triggers[tn] = [];
							}
							e.triggers[tn].push(a.cause);

							// filled in the rules property for a trigger. same way - key is rule name, value is an array that has all this rule's activationId
							if(entities[tn].rules == undefined){
								entities[tn].rules = {};
							}
							if(entities[tn]["rules"][e.name] == undefined){
								entities[tn]["rules"][e.name] = [];
							}
							entities[tn]["rules"][e.name].push(a.activationId);

							// index the activation records for trigger: what rule(s) it triggers? (for rule activation the cause already recorded)
							if(atvsDicId[a.cause]["rules"] == undefined)
								atvsDicId[a.cause]["rules"] = [];

							atvsDicId[a.cause]["rules"].push(a.activationId);

						}
					});

					// for associate action, use the action name
					// get the activation with the same action name that shows up right after the rule
					var isRule = -1, isThisRule = false, tempIndex;
					e.actions = {};
					// iterating over activations
					for(var i=atvs.length-1; i>=0; i--){
						// lastest data on top
						if(atvs[i].name == e.name){
							// found a rule entry! get the next activation that has the same name
							for(var j=i-1; j>=0; j--){
								if(atvs[j].name == actionName){
									// set this activation's action property to be the id of the activation it triggers
									atvsDicId[atvs[i].activationId].action = atvs[j].activationId;
									// set this action's rule proprety to be the id of this rule
									atvsDicId[atvs[j].activationId].rule = atvs[i].activationId;

									// set the rule's entity to include this action activation
									if(e.actions[atvs[j].name] == undefined)
										e.actions[atvs[j].name] = [];
									e.actions[atvs[j].name].push(atvs[j].activationId);

									// set the action's rules property to include this rule activation
									if(entities[atvs[j].name] && entities[atvs[j].name].rules == undefined)
										entities[atvs[j].name].rules = [];
									entities[atvs[j].name].rules.push(atvs[i].activationId);

									// set the action's triggers property to include the trigger activation that triggers the rule
									if(entities[atvs[j].name] && entities[atvs[j].name].triggers == undefined)
										entities[atvs[j].name].triggers = [];
									entities[atvs[j].name].rules.push(atvs[i].cause);

									break;
								}
							}
						}
					}
				}
				else if(e.kind == "action"){
					if(removeBinaryCode(e))
						e.exec.code = removeBinaryCodeMsg;
				}
			});
			console.log("Finished linking triggers, rules and actions. Processing summary data...");

			atvs.forEach(function(a, index){

				var e = entities[a.name];
				if(e.summary == undefined){
					e.summary = {
									success:{activations:[], pre:[], next:[]},
									error:{activations:[], pre:[], next:[]}
								};

					e.summaryResult = {};
				}
				var o;
				if(a.response.success){
					o = e.summary.success;
				}
				else{
					o = e.summary.error;
				}

				o.activations.push(a.activationId);

				// activation in before index happens after the current one! descending time
				if(index < atvs.length-1)
					o.pre.push(atvs[index+1].activationId);
				else
					o.pre.push("");

				if(index > 0)
					o.next.push(atvs[index-1].activationId);
				else
					o.next.push("");

			});

			// end here
			console.log("Done processing the data. Ready to send to the client.");
			owData = {e: entities, a:atvsDicId};

		}).catch(function(e){
			console.log(e);
		});

	});

	/*ow.activations.list({limit:limit, docs:true}).then(function(atvs){


	}).catch(function(e){
		console.log(e);
	});*/
}

function removeBinaryCode(entity){
	// how to check if something's code is binary?? :/

	if(entity.kind == "action"){
		if(entity.exec != undefined){
			if(entity.exec.binary){
				return true;
			}
			if(entity.exec.kind.indexOf("nodejs") != -1 && entity.exec.code.indexOf("function") == -1){
				// is nodejs but the code does not contain the word function
				return true;
			}
		}

	}

	return false;
};


function arrayToDictionary(array, key, flatten){
	var o = {}, i = 1;
	array.forEach(function(a){
		if(key == undefined)
			key = "name";

		if(flatten){
			o[a[key]] = a;
		}
		else{
			if(o[a[key]] == undefined){
				o[a[key]] = [a];
			}
			else{
				o[a[key]].push(a);
			}
		}
	});

	return o;
}


