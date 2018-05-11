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


 var  chartWidth = 900,
 	  gapWidth = 2,
 	  atvInfoWidth = 350,
	  yLabelWidth = 30,
	  histogramLabelHeight = 20,
	  historgramHeight = 100,
	  historgramAxisHeight = 50,
	  histogramTicks = 30,
	  barHeight = 18,
	  barPadding = 2,
	  colHeight = barHeight + (barPadding*2),
	  trWidth = 2,
	  jsonTextLimit = 300,
	  whiskPricingRate = 0.000017,
	  whiskPricingRunningTimes = 1000000,
	  exportLinkHost = window.location.protocol + "//" + window.location.host +"?link=", // https://openwhisk.ng.bluemix.net/api/v1/experimental/web/kerry.chang_test/demo/webJs.http?link=",
	  timelineBarColor = {trigger: "orange", rule: "LightSeaGreen", sequence: "deepskyblue", action: "steelblue", hover: "yellow", selected: "orangered", success:"lightgreen", error:"palevioletred"},
	  buttonColor = {active: "LightSeaGreen", inactive:"lightgrey"};


 $(document).ready(function(){

	var socket = io(window.location.protocol + "//" + window.location.host);
	var actionList;
	var currentActionName = "";
	var atvsFullData, code = {}. selectedAtvsData;
	var TLFont = 1, paneFont = 1;
	var currentChartView = "timelineView", currentAtvView = "renderedView";
	var detailLock = false, currentItem = "";
	var dragResize = false;

	// set UI width and height and resize
	$("#atvInfoDiv").css("width", atvInfoWidth+"px");
	chartWidth = $("body").width() - yLabelWidth - atvInfoWidth - 25 - 25 - gapWidth;	//
	$("#charts").css("width", chartWidth+yLabelWidth);
	$("#gap, #gap-move").css("height", ($(window).height()-40)+"px");

	// for vertical scolls
	$("#charts, #atvInfoDiv").css({
		"height":($(window).height()-50)+"px",
		"overflow-y": "auto"
	});

	$("#gap").mousedown(e => {
		e.preventDefault();
		dragResize = true;

		$("#gap-move").css("left", e.pageX);
		$("#gap-move").css("display", "block");
		$("#gap-move").attr("start", e.pageX);
	});

	$(document).mousemove(e => {
		if(dragResize){
			$("#gap-move").css("left", e.pageX);
		}

	}).mouseup(e => {
		if(dragResize){
			dragResize = false;
			$("#gap-move").css("display", "none");

			// resize
			let end = e.pageX, start = $("#gap-move").attr("start"), diff = end - start;
			//console.log(diff);
			atvInfoWidth -= diff;
			resize();

		}
	});


	$(window).resize(function(){
		resize();
	});

	$("#chartViewDiv").children("*").click(function(){
		$(this).css("background-color", buttonColor.active);
		$(this).siblings("*").css("background-color", buttonColor.inactive);
		currentChartView = $(this).attr("id").replace("View", "Chart");
		$("#"+currentChartView).css("display", "block");
		$("#"+currentChartView).siblings("*").css("display", "none");

		if($(this).attr("id") == "timelineView"){
			$("#legend").css("display", "block");
		}
		else{
			$("#legend").css("display", "none");
		}

		if($(this).attr("id") == "summaryView"){
			$("#overviewChart").css("display", "none");
			$("#atvInfoAndButtonsDiv").css("display", "none");

			$("#summaryDetailDiv").css("display", "block");
			$("#backToSummary").css("display", "inline-block");

		}
		else{
			$("#overviewChart").css("display", "block");
			$("#atvInfoAndButtonsDiv").css("display", "block");

			$("#summaryDetailDiv").css("display", "none");
			$("#backToSummary").css("display", "none");
		}
	});


	$("#timelineChart, #listChart, #summaryChart").click(function(e){
		if(detailLock){
			detailLock = false;
			atvDetail("", false);
			// remove highlight element
			if(currentItem.length != 0){

			}
			if($("#summaryDetailDiv").prop("current") != undefined || $("#summaryDetailDiv").prop("current") != ""){
				$($("#summaryDetailDiv").prop("current")).css("text-decoration", "none");
			}
			currentItem = "";
		}

	});

	$("#backToSummary").click(function(){
		$("#atvInfoAndButtonsDiv").css("display", "none");
		$("#summaryDetailDiv").css("display", "block");
	});

	$("#infoFormatButtonsDiv").children(".toolBarButton").click(function(){
		$(this).css("background-color", buttonColor.active);
		$(this).siblings(".toolBarButton").css("background-color", buttonColor.inactive);
		currentAtvView = $(this).attr("id");
		if(currentAtvView == "renderedView"){
			$("#atvInfo").css("display", "block");
			$("#rawJSONAtv").css("display", "none");
		}
		else{
			$("#atvInfo").css("display", "none");
			$("#rawJSONAtv").css("display", "block");
		}
	});




	// tell the server to send data!
	socket.emit("init");

	// Receive data from the server
	socket.on("init", function(data){

		//console.log(atvsFullData.e);
		if(data.fontInfo != undefined){
			if(data.fontInfo.TLFont && !isNaN(data.fontInfo.TLFont))
				TLFont = data.fontInfo.TLFont;
			if(data.fontInfo.paneFont)
				paneFont = data.fontInfo.paneFont;

			histogramLabelHeight = Math.round(histogramLabelHeight*TLFont);
			historgramAxisHeight = Math.round(historgramAxisHeight*TLFont);
			barHeight = Math.round(barHeight*TLFont);
			colHeight = barHeight + (barPadding*2);

			adjustCSSFontSize(TLFont, paneFont);

		}


		// populate select widget for summary
		$("#summaryEntity").html("");
		var optionString = "<option selected>-- select here --</option>";
		Object.keys(data.e).forEach(function(name){
			if(data.e[name].kind == "action" || data.e[name].kind == "sequence")
				optionString += ("<option>"+name+"</option>");
		});
		$("#summaryEntity").html(optionString);
		var s = Object.keys(data.a), format = d3.timeFormat("%x %H:%M:%S.%L");

		// activations are latest first (decrased by time)
		$("#summaryStartTime").html(format(data.a[s[s.length-1]].start));

		$("#summaryEndTime").html(format(data.a[s[0]].start));


		atvsFullData = data;
		selectedAtvsData = data.a;

		showSummaryData($("#summaryEntity").val());
		plotOverviewGraph(selectedAtvsData);

		$("#loading").css("display", "none");
		$("#content").css("display", "block");

		$("#exportDataLocal").off().click(function(e){
			var exportData = {e: atvsFullData.e, a:selectedAtvsData}, fileName = "data.json";

			var element = document.createElement('a');
			element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(escapeHTML(JSON.stringify(exportData))));
			element.setAttribute('download', fileName);

			element.style.display = 'none';
			document.body.appendChild(element);

			element.click();

			document.body.removeChild(element);

		});

	});

	socket.on("url", function(data){
		$("#exportData").html("Export visualization data as a JSON file");
		alert("Your visulaization can now be accessed through this URL: "+exportLinkHost+data);
	});

	$("#summaryEntity").change(function(){
		showSummaryData($(this).val());
	});

	$("#successOrErrorSum").change(function(){
		populateSummaryInfo($("#summaryEntity").val(), $(this).val());
	});


	function showSummaryData(name){
		if(name == "-- select here --"){
			$("#summaryDiv").css("display", "none");
		}
		else if(atvsFullData.e[name].summary == undefined){
			$("#summaryAtvNum").html("0");
			$("#summaryDataDiv").css("display", "none");
			sumAtvs();
			showSummaryData(name);
		}
		else{

			var e = atvsFullData.e[name].summary;


			// name
			$("#summaryActionTitle").html(name);

			// activations and aggregation data
			var ss = "", totalNum = e.success.activations.length+e.error.activations.length;
			if(totalNum>1)
				ss = "s";
			$("#summaryAtvNum").html(e.success.activations.length+" successful and "+e.error.activations.length+" failed activation"+ss+" ");
			var options = "";
			if(e.success.activations.length>0)
				options += ("<option value='success'>Successful activations ("+e.success.activations.length+")</options>");
			if(e.error.activations.length>0)
				options += "<option value='error'>Failed activations ("+e.error.activations.length+")</options>";

			$("#successOrErrorSum").html(options).children("option").first().attr("selected", true);

			populateSummaryInfo(name, $("#successOrErrorSum").val());

			$("#summaryDataDiv").css("display", "block");
			$("#summaryDiv").css("display", "block");
		}

	}

	function sumAtvs(){
		var atvs = Object.keys(atvsFullData.a);
		// sort atvs
		atvs.sort(function(id1, id2){
			return atvsFullData.a[id2].start-atvsFullData.a[id1].start;
		});
		atvs.forEach(function(id, index){
			var a = atvsFullData.a[id], e = atvsFullData.e[a.name];
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
				o.pre.push(atvsFullData.a[atvs[index+1]].activationId);
			else
				o.pre.push("");

			if(index > 0)
				o.next.push(atvsFullData.a[atvs[index-1]].activationId);
			else
				o.next.push("");

		});
	}

	// called when the user selects a new action or selects to view success or failed actions
	// name is name of the action, value is success or error
	function populateSummaryInfo(name, value){

		var e = atvsFullData.e[name].summary[value];

		// peformance
		var durations = [], r = [], sum = 0;
		e.activations.forEach(function(id){
			let a = atvsFullData.a[id];
			durations.push({time:a.end-a.start, id:id});
			sum += (a.end - a.start);
			r.push({r: a.response.result, id:id});
		});
		durations.sort((a, b) => a.time-b.time);
		$("#avg").html(Math.round(sum/durations.length));
		var median;
		if(durations.length%2 == 0){
			let i = durations.length/2;
			median = Math.round((durations[i].time+durations[i-1].time)/2);
		}
		else{
			let i = (durations.length-1)/2;
			median = durations[i].time;
		}
		$("#median").html(median+"");

		$("#min").html(durations[0].time+"").attr("src", durations[0].id);

		$("#max").html(durations[durations.length-1].time+"").attr("src", durations[durations.length-1].id);

		$("#min, #max").on("mouseover", function(d){
			if(!detailLock){
				atvDetail($(this).attr("src"), true)
			}
		})
		.on("mouseout", function(d){
			if(!detailLock){
				atvDetail("", false)
			}
		})
		.on("click", function(e){
			e.stopPropagation();
			if(!detailLock){
				// hover mode, lock it
				currentItem = $(this).attr("src");
				detailLock = true;
			}
			else{
				// already locked
				if(currentItem == $(this).attr("src")){
					// if click on self, unlock
					detailLock = false;
					currentItem = "";
					atvDetail("", false);
				}
				else{
					// if click on other object, display other object
					currentItem = $(this).attr("src");
					atvDetail($(this).attr("src"), true);

				}
			}
		});;

		// pre
		var pre = {}, preString = "", preSelectString = "";
		e.pre.forEach(function(id){
			if(atvsFullData.a[id] != undefined){
				let name = atvsFullData.a[id].name;
				if(pre[name] == undefined)
					pre[name] = 0;
				pre[name]++;
			}

		});

		Object.keys(pre).forEach(function(n){
			if(preString.length>0)
				preString += ", ";
			preString += ("<span name='"+n+"'>"+n+" ("+Math.round(pre[n]/e.activations.length*100)+"%, "+pre[n]+" times)</span>");

			preSelectString += "<option>"+n+"</option>";
		});
		$("#actionsBefore").html(preString+"<br/><br/>");

		// next
		var next = {}, nextString = "", nextSelectString = "";
		e.next.forEach(function(id){
			if(atvsFullData.a[id] != undefined){
				let name = atvsFullData.a[id].name;
				if(next[name] == undefined)
					next[name] = 0;
				next[name]++;
			}

		});

		Object.keys(next).forEach(function(n){
			if(nextString.length>0)
				nextString += ", ";
			nextString += ("<span name='"+n+"'>"+n+" ("+Math.round(next[n]/e.activations.length*100)+"%, "+next[n]+" times)</span>");

			nextSelectString += "<option>"+n+"</option>";

		});
		$("#actionsAfter").html(nextString+"<br/><br/>");

		// summary

		if(Object.keys(pre).length>1)
			preSelectString += "<option>all</option>";
		if(Object.keys(next).length>1)
			nextSelectString += "<option>all</option>";

		$("#rDataSummaryBefore").off().html(preSelectString).children().first().attr("selected", "true");
		$("#rDataSummaryAfter").off().html(nextSelectString).children().first().attr("selected", "true");

		regenerateSummary($("#summaryEntity").val(), $("#successOrErrorSum").val(), $("#rDataSummaryBefore").val(), $("#rDataSummaryAfter").val());


		$("#rDataSummaryBefore, #rDataSummaryAfter").change(function(){
			regenerateSummary($("#summaryEntity").val(), $("#successOrErrorSum").val(), $("#rDataSummaryBefore").val(), $("#rDataSummaryAfter").val());
		});

	}



	function regenerateSummary(name, value, pre, next){
		if(pre == "" || next == "")
			return;

		//console.log("reprint summary: ", name, value, pre, next);
		$("#outputSummary").html("");
		var summaryResult = {}, e =atvsFullData.e[name].summary[value];

		var rObj = [], rId = [];
		e.activations.forEach(function(id, index){
			if((pre == "all" || (pre == e.pre[index]) || (atvsFullData.a[e.pre[index]]!=undefined && atvsFullData.a[e.pre[index]].name == pre)) &&
				(next == "all" || (next == e.next[index]) || (atvsFullData.a[e.next[index]] != undefined && atvsFullData.a[e.next[index]].name == next))){
				rObj.push(atvsFullData.a[id].response.result);
				rId.push(id);
			}
		});
		// r is an array that has all the responses
		summaryResult = summarize(rObj, rId);
		/*r.forEach(function(o){
			summaryResult = summarize(o.r, o.id, summaryResult);
		});*/
		atvsFullData.e[name].summaryResult[value] = summaryResult;

		//console.log(summaryResult);

		$("#outputSummary").html("<pre>"+printableSummary(summaryResult, name, value)+"</pre>");

		$("#outputSummary").find("span[type='value']").css("cursor", "pointer")
		.mouseover(function(e){
			if(!detailLock){
				$(this).css("text-decoration", "underline");
				$("#atvInfoDiv").css("visibility", "visible");
				$("#summaryDetailDiv").html(summaryDetailHTML($(this)));
			}
		}).mouseout(function(e){
			if(!detailLock){
				$(this).css("text-decoration", "none");
				$("#atvInfoDiv").css("visibility", "hidden");
				$("#summaryDetailDiv").html("");
			}
		}).click(function(e){
			e.stopPropagation();

			if(!detailLock){
				detailLock = true;
				$("#atvInfoDiv").css("visibility", "visible");
				$("#summaryDetailDiv").html(summaryDetailHTML($(this)));
				$("#summaryDetailDiv").prop("current", $(this));

			}
			else{
				// already true. if click on other things - show the same thing
				if($("#summaryDetailDiv").prop("current")!= undefined && $("#summaryDetailDiv").prop("current").is($(this))){
					// same, go back to hover mode
					detailLock = false;
					$("#summaryDetailDiv").prop("current", "");
				}
				else{
					// other

					$("#summaryDetailDiv").html(summaryDetailHTML($(this)));
					var pre = $("#summaryDetailDiv").prop("current");
					if(pre != undefined)
						$(pre).css("text-decoration", "none");
					$(this).css("text-decoration", "underline");
					$("#summaryDetailDiv").prop("current", $(this));

					$("#summaryDetailDiv").css("display", "block");
					$("#atvInfoAndButtonsDiv").css("display", "none");


				}
			}
		});
	}

	function summaryDetailHTML(element){
		var path = $(element).attr("v"), name = $(element).attr("name"), type = $(element).attr("class"), s = "";
		if(type == "type"){
			type = $(element).html();
		}

		let obj = atvsFullData.e[name].summaryResult;
		const format = d3.timeFormat("%m/%d %H:%M:%S.%L");

		//console.log(path, name, type);
		path.split("||").forEach((p, i) => {
			if(p.length>0){
				obj = obj[p];
			}
		});

		Object.keys(obj[type]).forEach(value => {
			if(s.length != 0)
				s += "<br/><br/>"

			let ss = "";
			if(obj[type][value].length>1){
				ss = "s";
			}
			s += ("<span class='"+type+"'>"+escapeHTML(value)+"</span> from "+obj[type][value].length+" activation"+ss+":<br/> <ul>");
			obj[type][value].forEach(id => {
				s += ("<li class='summaryDetailIds' attr='"+id+"'>"+format(atvsFullData.a[id].start)+"</li>");
			});
			s += "</ul>";
		});

		var r = $(s);
		$(r).find(".summaryDetailIds").click(function(e){
			// need a back button
			let id = $(this).attr("attr");
			atvDetail(id, true);
			$("#summaryDetailDiv").css("display", "none");
			$("#atvInfoAndButtonsDiv").css("display", "block");
		});

		return r;
	}


	// turn the summary object into a printable version. path is for the summary object, NOT the actual return object
	function printableSummary(summaryObj, name, path){
		var s = "";
		if(path == undefined)
			path = "";

		if(typeof summaryObj === "object"){
			if(summaryObj.wittArray){
				// is array
				s += "[<div class='obj'>";
				s += printableSummary(summaryObj.wittArray, name, path+"||wittArray");
				s += "</div>]";
			}
			else{

				if(Object.keys(summaryObj).indexOf("wittObj") == -1){
					s += "{<div class='obj'>";
					// is object
					// print all keys
					Object.keys(summaryObj).forEach((key, index) => {
						if(key != "wittMissing"){
							let perc = "";
							if(summaryObj[key].wittMissing && summaryObj[key].wittMissing < 1)
								perc = '<span class="perc">'+Math.round(summaryObj[key].wittMissing*100)+'%</span>';
							s += ('"'+key+perc+'": ');
							s += printableSummary(summaryObj[key], name, path+"||"+key);
							if(index != Object.keys(summaryObj).length-1)
								s += ",";

							s += "<br/>";
						}
					});
					s += "</div>}";

				}
				else{
					// is end object
					let obj = summaryObj.wittObj, types = Object.keys(obj), p = path+"||wittObj";
					if(types.length == 1 && Object.keys(obj[types[0]]).length == 1){
						// one value, one type
						s += ("<span class='"+types[0]+"' type='value' name='"+name+"' v='"+p+"'>");
						if(types[0] == "string"){
							s += '"'+escapeHTML(Object.keys(obj[types[0]])[0])+'"';
						}
						else{
							s += Object.keys(obj[types[0]])[0];
						}
						s += "</span>";
					}
					else{

						types.forEach((type,index) => {
							if(index != 0)
								s += " || ";

							s += ("<span class='type' type='value' name='"+name+"' v='"+p+"'>" + type + "</span>");
						});
					}



				}

			}
		}
		return s;


	}


	// rObj: array of returned objects/arrays to be summarized. rId: array of ids the returned objects are from
	function summarize(rObj, rId){

		// rObj: only all arrays or all objects
		// all arrays
		if(Array.isArray(rObj[0])){
			var returnedObj = {}, oArray = [], idArray = [];
			rObj.forEach((obj, index) => {
				// obj is an array
				obj.forEach(o => {
					oArray.push(o);
					idArray.push(rId[index]);
				});
			});

			returnedObj.wittArray = aggregateVals({val:oArray, ids:idArray});
			return returnedObj;
		}
		else{
		// all objects

			var distinctKeys = {}, returnedObj = {};
			rObj.forEach((o, i) => {
				Object.keys(o).forEach(key => {
					if(distinctKeys[key] == undefined)
						distinctKeys[key] = {ids:[], val:[]};
					distinctKeys[key].ids.push(rId[i]);
					distinctKeys[key].val.push(o[key]);
				});
			});

			Object.keys(distinctKeys).forEach(key => {
				returnedObj[key] = aggregateVals(distinctKeys[key]);
				returnedObj[key].wittMissing = distinctKeys[key].ids.length/rObj.length;
			});

			return returnedObj;
		}
	}

	function aggregateVals(values){
		var obj = {};

		let sumObj = {};
		values.val.forEach((v, i) => {
			let t = whatTypeIsIt(v), vString = t;
			if(t != "null")
				vString = v.toString();	// i'm storing the string value of the thing.
			if(Object.keys(sumObj).indexOf(t) == -1)
				sumObj[t] = {};
			if(sumObj[t][vString] == undefined)
				sumObj[t][vString] = [];
			sumObj[t][vString].push(values.ids[i]);
		});

		if(Object.keys(sumObj).length == 1){
			// same type -> see what type is it
			let t = Object.keys(sumObj)[0];
			if(t == "object"){
				// get deeper
				obj = summarize(values.val, values.ids);
			}
			else if(t == "array"){
				obj = summarize(values.val, values.ids);
			}
			else{
				// close here
				obj.wittObj = sumObj;
			}
		}
		else{
			// different types!
			obj.wittObj = sumObj;
		}

		return obj;
	}

	function whatTypeIsIt(obj){
		if(obj == null)
			return "null";
		else if(typeof obj === "object"){
			if(Array.isArray(obj))
				return "array";
			else
				return "object";
		}
		else
			return typeof obj;
	}




	function plotOverviewGraph(atvs){
		// aggregation given timeline - a barchart showing frequency, and an axis showing time
		var data = [];
		Object.keys(atvs).forEach(function(id){
			// select only necessary data
			data.push({v: atvs[id].start, n:atvs[id].name, k:atvs[id].kind, id:id, error:{success: atvs[id].response.success, status: atvs[id].response.status}});
		});

		$("#overviewChart").html("");

		var g = d3.select("#overviewChart").attr("width", chartWidth+yLabelWidth)
				.attr("height", historgramHeight+historgramAxisHeight+histogramLabelHeight)
				.append("g");

		var x0 = [d3.min(data, function(d){return d.v;}), d3.max(data, function(d){return d.v;})];

		// axis range is set to fit the ticks
		if((i = getAxisRange(d3.scaleLinear().domain(x0).ticks(histogramTicks))) != undefined){
			x0 = i;
		}

		var x = d3.scaleLinear().domain(x0).rangeRound([0, chartWidth]);
		var bins = d3.histogram().value(function(d){return d.v}).domain(x.domain()).thresholds(x.ticks(histogramTicks))(data);

		var y = d3.scaleLinear().domain([0, d3.max(bins, function(d) { return d.length; })]).range([historgramHeight, 0]);

		// brush first
		var brush = d3.brushX().on("end", brushended),
		    idleTimeout,
		    idleDelay = 350;

		// brush end

		var format = d3.timeFormat("%m/%d %H:%M:%S");
		var xAxis = d3.axisBottom(x)

	    // append brush
		g.append("g")
			.attr("class", "brush")
			.attr("transform", "translate("+yLabelWidth+", 0)")
			.call(brush);
		$("#overviewChart").find(".brush").find(".overlay").attr("width", chartWidth);

		// append y label text
		g.append("text")
		 .attr("text-anchor", "middle")
		 .attr("transform", "translate("+yLabelWidth/2+", "+(historgramHeight+historgramAxisHeight)/2+")rotate(-90)")
		 .text("Number of activations");

		// append axis
		g.append("g")
		    .attr("class", "axis axis--x")
		    .attr("transform", "translate("+yLabelWidth+"," + (historgramHeight+histogramLabelHeight) + ")");

		// append x label text
		g.append("text")
		 .attr("text-anchor", "middle")
		 .attr("transform", "translate("+(yLabelWidth+chartWidth/2)+", "+(historgramHeight+histogramLabelHeight+40)+")")
		 .text("Activation start time");

		// append reset button
		g.append("text")
		 .attr("id", "resetSelection")
		 .attr("text-anchor", "end")
		 .attr("transform", "translate("+(chartWidth-10)+", "+(historgramHeight+histogramLabelHeight+40)+")")
		 .style("display", (Object.keys(selectedAtvsData).length == Object.keys(atvsFullData.a).length ? "none" : "inline-block") )
		 .style("font-size", (12*TLFont)+"px")
		 .text("Reset selection")
		 .on("click", function(e){
		 	brushended();
		 });


		drawHistogram();
		plotCharts(processActivationData(bins));

		function brushended() {
			// call when clicking/dragging on brush, or when clicking the reset button
			// s when clicking/dragging on brush is null/range. When clicking the reset button, s is undefined
			var s = d3.event.selection;
			if (!s) {	// enters when s is null (click on canvas) or undefined (click the reset button)
				if (s === null && !idleTimeout) // return when single-clicking
					return idleTimeout = setTimeout(idled, idleDelay);

				// x0 is the initial range;
				if(data.length != Object.keys(atvsFullData.a).length){
					console.log("reset x0 range to full data");
					data = [];
					atvs = atvsFullData.a;
					Object.keys(atvs).forEach(function(id){
						// select only necessary data
						data.push({v: atvs[id].start, n:atvs[id].name, k:atvs[id].kind, id:id, error:{success: atvs[id].response.success, status: atvs[id].response.status}});
					});

					x0 = [d3.min(data, function(d){return d.v;}), d3.max(data, function(d){return d.v;})];

					// axis range is set to fit the ticks
					if((i = getAxisRange(d3.scaleLinear().domain(x0).ticks(histogramTicks))) != undefined){
						x0 = i;
					}
				}
				x.domain(x0);
				bins = d3.histogram().value(function(d){return d.v}).domain(x.domain()).thresholds(x.ticks(histogramTicks))(data);
				y.domain([0, d3.max(bins, function(d) { return d.length; })]);
				$("#resetSelection").css("display", "none");

			}
			else {
				// should just scale to the cloest data
				var selectRange = [s[0], s[1]].map(x.invert, x);
				var dataRange = [d3.min(data, function(d){
					if(d.v >= selectRange[0]){
						return d.v;
					}
					else{
						return undefined;
					}
				}), d3.max(data, function(d){
					if(d.v <= selectRange[1]){
						return d.v;
					}
					else{
						return undefined;
					}
				})];

				// then, make dataRange fit threshold using getAxisRange
				if((i = getAxisRange(d3.scaleLinear().domain(dataRange).ticks(histogramTicks))) != undefined){
					dataRange = i;
				}

				x.domain(dataRange).rangeRound([0, chartWidth]);

				bins = d3.histogram().value(function(d){return d.v;}).domain(x.domain()).thresholds(x.ticks(histogramTicks))(data);

				y.domain([0, d3.max(bins, function(d) { return d.length; })]);

				g.select(".brush").call(brush.move, null);

				$("#resetSelection").css("display", "inline-block");
			}

			// redraw historgram
			drawHistogram();
			plotCharts(processActivationData(bins));

		}

		function getAxisRange(ticksArray){

			if(ticksArray.length>2){
				var diff = ticksArray[1] - ticksArray[0];
				return [ticksArray[0]-diff+1, ticksArray[ticksArray.length-1]+diff-1];
			}
			else{
				return undefined;
			}

		}

		function idled() {
			idleTimeout = null;
		}

		function drawHistogram(isTransition){

			var t = g.transition().duration(750);

			var ticks = [], format, maxLabel;
			if(bins.length>2){
				var min = bins[0].x0, max = bins[bins.length-1].x1;
				if(max - min > 5000){
					// at least 5 second differences
					maxLabel = 5;
					format = d3.timeFormat("%m/%d %H:%M:%S");
				}
				else{
					// less than 5 seconds difference
					maxLabel = 5;
					format = d3.timeFormat("%m/%d %H:%M:%S.%L");
				}
				while(ticks.length<maxLabel){
					var v = min + (max-min)/maxLabel*ticks.length;
					ticks.push(v);
				}
				ticks.push(max);

			}

			xAxis.tickValues(ticks).tickFormat(function(d, index){
	    		return format(new Date(Math.round(d)));
	    	});


			g.select(".axis--x").call(xAxis);
			$(".axis--x").find(".tick").first().find("text").css("text-anchor", "start");
			$(".axis--x").find(".tick").last().find("text").css("text-anchor", "end");

			g.selectAll(".bar").remove();

			var b = g.selectAll(".bar").data(bins);

			bar = b.enter().append("g").attr("class", "bar")
						.attr("transform", function(d) { return "translate(" + (x(d.x0)+yLabelWidth) + "," + (y(d.length)+histogramLabelHeight) + ")"; })
						.attr("startTime", function(d) {
							return d.x0;
						})
						.attr("endTime", function(d) {
							return d.x1;
						});

			bar.append("rect")
				.attr("class", "successBars")
			    .attr("x", 1)
			    .attr("y", function(d){
			    	var error = 0;
			    	d.forEach(function(a){
			    		if(!a.error.success)
			    			error++;
			    	});
			    	return historgramHeight - y(error);
			    })
			    //.attr("width", x(bins[0].x1) - x(bins[0].x0) - 1)
			    .attr("width", chartWidth/bins.length - 1)
			    .attr("height", function(d) {
			    	// the none error bars - green color
			    	var nonerror = 0;
			    	d.forEach(function(a){
			    		if(a.error.success)
			    			nonerror++;
			    	});
			    	return historgramHeight - y(nonerror);

			    })
			    .style("fill", timelineBarColor.success)
			    /*.on("mouseover", function(d){
			    	$(this).css("fill", timelineBarColor.hover);
			    	d.forEach(function(o){
			    		if(o.error.success)
				    		$("#"+o.id).css("fill", timelineBarColor.hover);
				    });

			    })
			    .on("mouseout", function(d){
			    	$(this).css("fill", timelineBarColor.success);
			    	d.forEach(function(o){
				    	$("#"+o.id).css("fill", timelineBarColor[o.k]);
				    });
			    })*/;

			bar.append("rect")
				.attr("class", "errorBars")
			    .attr("x", 1)
			    //.attr("width", x(bins[0].x1) - x(bins[0].x0) - 1)
			    .attr("width", chartWidth/bins.length - 1)
			    .attr("height", function(d) {
			    	// the none error bars - green color
			    	var error = 0;
			    	d.forEach(function(a){
			    		if(!a.error.success)
			    			error++;
			    	});
			    	return historgramHeight - y(error);

			    })
			    .style("fill", timelineBarColor.error)
			    /*.on("mouseover", function(d){
			    	$(this).css("fill", timelineBarColor.hover);
			    	d.forEach(function(o){
			    		if(!o.error.success){
				    		$("#"+o.id).css("fill", timelineBarColor.hover);
				    		$("#"+o.id+"_error").css("fill", timelineBarColor.hover);
			    		}
				    });
			    })
			    .on("mouseout", function(d){
			    	$(this).css("fill", timelineBarColor.error);
			    	d.forEach(function(o){
				    	$("#"+o.id).css("fill", timelineBarColor[o.k]);
				    	$("#"+o.id+"_error").css("fill", timelineBarColor.error);
				    });
			    })*/;



			bar.append("text")
			    .attr("dy", "-1em")
			    .attr("y", 6)
			    .attr("x", (x(bins[0].x1) - x(bins[0].x0)) / 2)
			    .attr("text-anchor", "middle")
			    .text(function(d) {
			    	if(d.length>0)
			    		return d.length;
			    	else
			    		return "";
			    });
		}


	};


	function processActivationData(bins){
		// prepare data for the timeline vis. really not efficient...
		var atvsArray = [];
		selectedAtvsData = {};
		bins.forEach(function(bin){
			bin.forEach(function(b){
				if(atvsFullData.a[b.id] != undefined)
					atvsFullData.a[b.id].processed = undefined;
				atvsArray.push(atvsFullData.a[b.id]);
				selectedAtvsData[b.id] = atvsFullData.a[b.id];
			});
		});

		// why are there here?????
		/*$("#exportData").off().click(function(e){

			var p = prompt("Witt will generate a URL that let you share this visualization with others, or load it back later. The data will be stored as a web action under the package 'WittData' in your OpenWhisk account. Give a name to your data and click OK to continue.", "myData");
			if(p != null){
				socket.emit("export", {name:p, data:{e: atvsFullData.e, a:selectedAtvsData}});
				$(this).html('<div class="loader"></div>');
			}
		});*/


		// -------------------------------

		atvsArray.sort(function(a, b){
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

		var data = [];
		for(var i=atvsArray.length-1; i>=0; i--){
			data = data.concat(findChild(i, atvsArray));
		}

		return {timeline: data, list: atvsArray};
	}

	function findChild(i, array){
		var data = [];

		if(array[i].processed == undefined){
			array[i].processed = true;
			var o = {name: array[i].name, start:array[i].start, end:array[i].end, kind: array[i].kind, id:array[i].activationId};

			data.push(o);
			if(o.kind == "sequence"){
				var children = [];
				array[i].logs.forEach(function(id){
					// find taht atv record
					for(var j=0; j<array.length; j++){
						if(id == array[j].activationId){
							children = children.concat(findChild(j, array));
							break;
						}
					}
				});

				o.children = children.length;
				data = data.concat(children);
			}
		}

		return data;
	}

	function plotCharts(data){

		if(currentChartView == "timelineChart"){
			plotTimeline(data.timeline);
			plotList(data.list);
		}
		else{
			plotList(data.list);
			plotTimeline(data.timeline);
		}

	}

	function plotList(data){

		var format = d3.timeFormat("%x %H:%M:%S.%L");

		d3.select("#listItems").selectAll("div").remove();
		var row = d3.select("#listItems").selectAll("div").data(data).enter().append("div").attr("class", "listItem")
		.style("background-color", function(d, i){
			if(i%2 == 0){
				return "lightgrey";
			}
			else{
				return "white";
			}
		}).attr("id", function(d){
			return d.activationId+"List";
		}).style("padding", "5px")
		.style("color", function(d){
			if(d.response.success){
				return "black";
			}
			else{
				return "red";
			}
		})
		.on("mouseover", function(d){
				if(!detailLock){

					atvDetail(d.activationId, true)
				}
			})
			.on("mouseout", function(d){
				if(!detailLock){
					//$(this).children("*").css("text-decoration", "none");
					atvDetail(d.activationId, false)
				}
			})
			.on("click", function(d){
				d3.event.stopPropagation();
				if(!detailLock){
					// hover mode, lock it
					currentItem = d.activationId;
					detailLock = true;
				}
				else{
					// already locked
					if(currentItem == d.activationId){
						// if click on self, unlock
						detailLock = false;
						currentItem = "";
						atvDetail("", false);
					}
					else{
						// if click on other object, display other object's info
						currentItem = d.activationId;
						atvDetail(d.activationId, true);
					}
				}
			});;

		row.append("div").style("width", "35%").style("display", "inline-block").html(function(d){return d.activationId;});
		row.append("div").style("width", "20%").style("display", "inline-block").html(function(d){return d.name;});
		row.append("div").style("width", "10%").style("display", "inline-block").html(function(d){return d.kind;});
		row.append("div").style("width", "35%").style("text-align", "right").style("display", "inline-block").html(function(d){return format(d.start);});

		if(currentItem.length>0)
			syncStyle(currentItem, true);

	}

	function plotTimeline(data){

		// create margin for bottom axis
		const margin = {left: 30, right:0, top:0, bottom:55}, height = colHeight * data.length, graphRightMargin = 20,
			xMax = chartWidth+yLabelWidth-margin.left - graphRightMargin;

		var minX = d3.min(data, function(d){return d.start;}), maxX = d3.max(data, function(d){return d.end;});
		var x = d3.scaleLinear().domain([minX, maxX])
				.range([0, xMax]);

		var ticks = [], tickNum = 5;
		while(ticks.length<tickNum){
			ticks.push(minX+(maxX-minX)/tickNum*ticks.length);
		}
		ticks.push(maxX);

		var xAxis = d3.axisBottom(x).tickValues(ticks).tickFormat(function(d){return Math.round(d - minX);})
					.tickSizeInner(-height).tickSizeOuter(0);


		d3.select("#timelineChart").selectAll("g").remove();

		var chart = d3.select("#timelineChart")
					.attr("width", chartWidth+yLabelWidth-margin.left).attr("height", height+margin.top+margin.bottom)
					.attr("transform", "translate("+margin.left+", "+margin.top+")")
					.append("g");





		var bar = chart.selectAll("g").data(data).enter().append("g").attr("transform", function(d, i){
			return "translate(0, "+i*colHeight+")";
		});
		// timeline bars
		bar.append("rect").attr("class", "timelineBars").attr("id", function(d){return d.id;}).attr("x", function(d){return x(d.start);}).attr("y", barPadding)
			.attr("width", function(d){
				if(d.kind == "action" || d.kind == "sequence")
					return Math.max(x(d.end) - x(d.start), trWidth);
				else
					return Math.max(x(d.start+trWidth) - x(d.start), trWidth);

			})
			.attr("height", barHeight-1)
			.style("fill", function(d){
				return timelineBarColor[d.kind];
			})
			.on("mouseover", function(d){
				if(!detailLock){
					$(this).css("stroke", "orangered");
					atvDetail(d.id, true)
				}
			})
			.on("mouseout", function(d){
				if(!detailLock){
					$(this).css("stroke", "none");
					atvDetail(d.id, false)
				}
			})
			.on("click", function(d){
				d3.event.stopPropagation();
				if(!detailLock){
					// hover mode, lock it
					currentItem = d.id;
					detailLock = true;
				}
				else{
					// already locked
					if(currentItem == d.id){
						// if click on self, unlock
						detailLock = false;
						currentItem = "";
					}
					else{
						// if click on other object, display other object's info
						$("#"+currentItem).css("stroke", "none");
						$(this).css("stroke", "orangered");
						currentItem = d.id;
						atvDetail(d.id, true);
					}
				}
			});
		// error bars
		bar.append("rect").attr("id", function(d){return d.id+"_error";}).attr("x", function(d){return x(d.start);}).attr("y", barPadding+barHeight/3)
			.attr("width", function(d){
				if(d.kind == "action" || d.kind == "sequence")
					return Math.max(x(d.end) - x(d.start), trWidth);
				else
					return Math.max(x(d.start+trWidth) - x(d.start), trWidth);

			})
			.attr("height", function(d){
				if(!atvsFullData.a[d.id].response.success)
					return barHeight/3;
				else
					return 0;
			})
			.style("fill", timelineBarColor.error)
			.on("mouseover", function(d){
				d3.event.stopPropagation();
				if(!detailLock){
					$(this).prev().css("stroke", "orangered");
					atvDetail(d.id, true)
				}
			})
			.on("mouseout", function(d){
				d3.event.stopPropagation();
				if(!detailLock){
					atvDetail(d.id, false)
				}
			})
			.on("click", function(d){
				d3.event.stopPropagation();
				if(!detailLock){
					// hover mode, lock it
					currentItem = d.id;
					detailLock = true;
				}
				else{
					// already locked
					if(currentItem == d.id){
						// if click on self, unlock
						detailLock = false;
						currentItem = "";
						atvDetail(d.id, false);
					}
					else{
						// if click on other object, display other object's info
						currentItem = d.id;
						atvDetail(d.id, true);
					}
				}
			});
		// text labels
		bar.append("text").attr("class", "labels")
		.attr("x", function(d){
			var xx = x(d.start),
				textLength = ((d.kind == "action" || d.kind == "sequence")? (d.name+", "+(d.end-d.start)/1000+"s").length : d.name.length)*5;//d.name.length*5;
			if(xx+textLength>xMax)
				xx = xMax - textLength -5;
			else
				xx += 3;
			return xx;
		})
		.attr("y", colHeight/2).attr("dy", ".35em")
		.text(function(d){
				if(d.kind == "action"){
					return d.name+", "+(d.end-d.start)/1000+"s";
				}
				if(d.kind == "sequence")
					return d.name+", "+(d.end-d.start)/1000+"s";
				else
					return d.name;
		})// make text hover/clickable. same thing
		.on("mouseover", function(d){
			d3.event.stopPropagation();
			if(!detailLock){
				atvDetail(d.id, true)
			}
		})
		.on("mouseout", function(d){
			d3.event.stopPropagation();
			if(!detailLock){
				atvDetail(d.id, false)
			}
		})
		.on("click", function(d){
			d3.event.stopPropagation();
			if(!detailLock){
				// hover mode, lock it
				currentItem = d.id;
				detailLock = true;
			}
			else{
				// already locked
				if(currentItem == d.id){
					// if click on self, unlock
					detailLock = false;
					currentItem = "";
					atvDetail(d.id, false);
				}
				else{
					// if click on other object, display other object's info
					currentItem = d.id;
					atvDetail(d.id, true);
				}
			}
		});


		// left
		bar.append("line").attr("class", "sequenceLine").attr("x1", function(d){return x(d.start)+1;}).attr("y1", colHeight).attr("x2", function(d){return x(d.start)+1;}).attr("y2", function(d){
			if(d.kind == "sequence"){
				return (d.children+1)*colHeight;
			}
			else{
				return colHeight;
			}
		}).style("stroke", timelineBarColor.sequence).style("stroke-width", 1).style("stroke-dasharray", "10, 5");

		// right
		bar.append("line").attr("class", "sequenceLine")
		.attr("x1", function(d){
			if(d.end == undefined)
				return x(d.start)-1;
			else
				return x(d.start)+(x(d.end) - x(d.start))-1;
		}).attr("y1", colHeight)
		.attr("x2", function(d){
			if(d.end == undefined)
				return x(d.start)-1;
			else
				return x(d.start)+(x(d.end) - x(d.start))-1;
		}).attr("y2", function(d){
			if(d.kind == "sequence"){
				return (d.children+1)*colHeight;
			}
			else{
				return colHeight;
			}
		}).style("stroke", timelineBarColor.sequence).style("stroke-width", 1).style("stroke-dasharray", "10, 5");

		// bottom
		bar.append("line").attr("class", "sequenceLine").attr("x1", function(d){return x(d.start);}).attr("y1", function(d){
			if(d.kind == "sequence"){
				return (d.children+1)*colHeight-2;
			}
			else{
				return colHeight;
			}

		}).attr("x2", function(d){
			if(d.kind == "sequence"){
				return x(d.start)+(x(d.end) - x(d.start))-1;
			}
			else{
				return x(d.start);
			}

		}).attr("y2", function(d){
			if(d.kind == "sequence"){
				return (d.children+1)*colHeight-2;
			}
			else{
				return colHeight;
			}
		}).style("stroke", timelineBarColor.sequence).style("stroke-width", 1).style("stroke-dasharray", "10, 5");

		bar.exit().remove();


		chart.append("g").attr("class", "x axis").attr("transform", "translate (0,"+(height+5)+")").call(xAxis)
			.append("text").attr("x", chartWidth/2).attr("dy", "40px").style("text-anchor", "center").text("ms");

		$(".x").find(".tick").first().find("text").css("text-anchor", "start");
		$(".x").find(".tick").last().find("text").css("text-anchor", "end");

		//var xAxis = d3.axisBottom(x).ticks(10).tickFormat(function(d){return format(new Date(d));})
		if(currentItem.length>0)
			syncStyle(currentItem, true);

	};

	$("#activationIdLink").click(function(e){
		var id = $(this).html();
		if(atvsFullData.a[id] != undefined){
			writeNewPageJSON(atvsFullData.a[id], "activation "+id);
		}
	});

	function syncStyle(id, show){
		if(!show){
			$(".timelineBars").css("stroke", "none");
			$(".labels").css("text-decoration", "none");
			$(".listItem").children("*").css("text-decoration", "none");
			$("#atvInfoDiv").css("visibility", "hidden");
			$("#jsonDetail").html("");
			$("#min, #max").css("text-decoration", "none");

			$("#atvInfoAndButtonsDiv").css("display", "none");
			$(".bar[currentItem=true]").children("rect").each(function(){
				if($(this).attr("class") == "successBars"){
					$(this).css("fill", timelineBarColor.success);
				}
				else{
					$(this).css("fill", timelineBarColor.error);
				}
				$(this).removeAttr("currentItem");
			});
		}
		else{

			// do the object highlighting here
			$(".timelineBars").css("stroke", "none");
			$(".labels").css("text-decoration", "none");
			$(".listItem").children("*").css("text-decoration", "none");
			$("#min, #max").css("text-decoration", "none");
			let rect = $("#"+id);
			$(rect).css("stroke", "orangered");
			$(rect).next().next().css("text-decoration", "underline");
			$("#"+id+"List").children("*").css("text-decoration", "underline");
			$("#min[src='"+id+"']").css("text-decoration", "underline");
			$("#max[src='"+id+"']").css("text-decoration", "underline");

			$("#atvInfoAndButtonsDiv").css("display", "block");
			$(".bar").each(function(){
				var st = atvsFullData.a[id].start;
				//console.log(st, $(this).attr("startTime"), $(this).attr("endTime"));
				if($(this).attr("startTime")<=st && $(this).attr("endTime")>=st){
					// highlight this
					if(atvsFullData.a[id].response.success){
						$(this).children(".successBars").css("fill", timelineBarColor.hover);
					}
					else{
						$(this).children(".errorBars").css("fill", timelineBarColor.hover);
					}
					$(this).attr("currentItem", true);
				}
				else{
					$(this).children(".successBars").css("fill", timelineBarColor.success);
					$(this).children(".errorBars").css("fill", timelineBarColor.error);
					$(this).removeAttr("currentItem");
				}
			});

		}
	}

	function atvDetail(id, show){
		syncStyle(id, show);
		if(show){

			syncStyle(id, show);
			// ----- end ----------

			$("#rawJSONAtv").html("");

			var a = atvsFullData.a[id];

			var format = d3.timeFormat("%x %H:%M:%S.%L");


			$("#activationIdLink").html(id);
			$("#actionName").html(a.name);
			if(a.kind == "action"){
				let msg = a.kind;
				if(atvsFullData.e[a.name] && atvsFullData.e[a.name].exec && atvsFullData.e[a.name].exec.kind)
					msg = atvsFullData.e[a.name].exec.kind + " " + msg;
				$("#actionType").html(msg);
			}
			else{
				$("#actionType").html(a.kind);
			}

			$("#actionType").css("background-color", timelineBarColor[a.kind]);

			// any warning text to show?
			if(atvsFullData.e[a.name] && atvsFullData.e[a.name].deleted){
				var s = "<i><b>Note</b>: The system could not retrieve details of this "+a.kind+" from the server. The "+a.kind+" was probably deleted. </i><br/><br/>";
				$("#warningDiv").html(s);
				$("#warningDiv").css("display", "inline-block");
			}
			else{
				$("#warningDiv").css("display", "none");
			}


			// start time - every item has one
			$("#startTime").html(format(new Date(a.start)));
			// end time and duration - only for sequences and actions
			if(a.end != undefined){
				//$("#endTime").html(format(new Date(a.end)));
				$("#duration").html(a.end - a.start);
				// calculate cost
				var memory;
				for(var i=0; i<a.annotations.length; i++){
					if(a.annotations[i].key == "limits"){
						memory = a.annotations[i].value.memory;
						break;
					}
				}
				if(memory == undefined){
					$("#costSpan").css("display", "none");
				}
				else{
					$("#costTimes").html(whiskPricingRunningTimes/1000000+"M");
					var fullRate = (memory/1000)*((a.end-a.start))/1000*whiskPricingRunningTimes;
					var freeTier = 400000;
					freeTier = 0;
					var cost = (fullRate-freeTier)*whiskPricingRate;
					if(fullRate - freeTier >0)
						$("#cost").html("$"+(Math.round(cost*100)/100));
					else
						$("#cost").html("free");

					$("#costSpan").css("display", "inline-block");
				}
				$("#endTimeDiv").css("display", "block");
			}
			else{
				$("#endTimeDiv").css("display", "none");
			}
			var cause;
			// caused by
			if(a.kind == "action" || a.kind == "sequence"){
				var msg = "";
				if(a.rule != undefined){
					var atv_rule = atvsFullData.a[a.rule], atv_trigger = atvsFullData.a[atv_rule.cause];
					msg += "Trigger "+domActivationText(atv_trigger.activationId)+" through rule "+domActivationText(atv_rule.activationId)+". ";
					cause = atv_trigger.activationId;
				}
				if(a.cause != undefined){
					// caused by a sequence
					var atv_seq = atvsFullData.a[a.cause];

					if(atv_seq != undefined){
						msg += "Sequence "+domActivationText(atv_seq.activationId);
						var i = atv_seq.logs.indexOf(a.activationId);
						// if the action is not the first one in its action
						if(i != 0){
							var atv_before = atvsFullData.a[atv_seq.logs[i-1]];
							msg += ", after "+domActivationText(atv_before.activationId);
							cause = atv_before.activationId;
						}
						else{
							// first action in a sequence. trace up - till it's not the first action anymore
							while(true){
								if(atv_seq.cause == undefined){
									// reach the top, always the first, get the trigger of the sequence
									if(atv_seq.rule != undefined){
										cause = atvsFullData.a[atv_seq.rule].cause;
									}
									break;
								}
								else if(atvsFullData.a[atv_seq.cause] == undefined){
									// stop here - the cause is not in the retrieved activations
										cause = undefined;
										break;
								}
								else if(atvsFullData.a[atv_seq.cause].logs.indexOf(atv_seq.activationId) != 0){
									var index = atvsFullData.a[atv_seq.cause].logs.indexOf(atv_seq.activationId);
									// done here. caused by the action before the index
									cause = atvsFullData.a[atv_seq.cause].logs[index-1];
									break;
								}
								atv_seq = atvsFullData.a[atv_seq.cause];
							}
						}
					}
				}

				if(msg.length>0){
					$("#causedByContent").html(msg);
					$("#causedByDiv").css("display", "block");
				}
				else{
					$("#causedByDiv").css("display", "none");
				}
			}
			else if(a.kind == "rule" && a.cause != undefined){
				var atv_trigger = atvsFullData.a[a.cause];
				var dom_trigger = $("<a></a>").attr("class", "triggerText").attr("source", atv_trigger.activationId).html(atv_trigger.name);
				$("#causedByContent").html(dom_trigger);
				$("#causedByDiv").css("display", "block");
				cause = a.cause;
			}
			else{
				$("#causedByDiv").css("display", "none");
			}


			var showInputDiv = false, inputObj;
			if(atvsFullData.e[a.name].parameters && atvsFullData.e[a.name].parameters.length>0){
				var params = {};
				if(atvsFullData.e[a.name].wittParams == undefined){
					atvsFullData.e[a.name].parameters.forEach(function(obj){
					if(obj.key.indexOf("_") == -1){
							params[obj.key] = obj.value;
						}
					});
					atvsFullData.e[a.name].wittParams = params;
				}
				else{
					params = atvsFullData.e[a.name].wittParams;
				}

				if(Object.keys(params).length>0){
					var s = JSON.stringify(params, undefined, 4);
					if(s.length>jsonTextLimit){
						s = s.substring(0, jsonTextLimit);
						s += "... </pre><span class='viewMore' r='"+a.name+"'>(view all)</span>";
					}
					else{
						s += "</pre>"
					}
					$("#prebindedParams").html("<br/>Bound parameters<br/><pre>"+s+"<br/>");
					$("#prebindedParams").find(".viewMore").click(function(){
						var n = $(this).attr("r");
						writeNewPageJSON(atvsFullData.e[n].wittParams, n);
					});
					$("#prebindedParams").css("display", "block");
					showInputDiv = true;
				}
				else{
					$("#prebindedParams").css("display", "none");
				}
			}
			else{
				$("#prebindedParams").css("display", "none");
			}

			// input from source
			if(cause != undefined){

				var inputSource = "From "+atvsFullData.a[cause].kind +" ";
				inputSource += domActivationText(cause);
				if(a.kind == "rule" && a.action != undefined){
					inputSource += ", passing to ";
					inputSource += domActivationText(a.action);
				}

				$("#inputSource").html(inputSource);

				var inputData =  "<pre>"+escapeHTML(JSON.stringify(atvsFullData.a[cause].response.result, undefined, 4));
				if(inputData.length > jsonTextLimit){
					inputData = inputData.substring(0, jsonTextLimit);
					inputData += "... </pre><span class='viewMore' r='"+cause+"'>(view all)</span>";
				}
				else{
					inputData += "</pre>"
				}

				$("#inputData").html(inputData);

				$("#inputData").find(".viewMore").click(function(e){
					var id = $(this).attr("r"), name = atvsFullData.a[id].name;
					var title = name+", activation Id "+id;
					writeNewPageJSON(atvsFullData.a[id].response.result, title);
				});

				inputObj = JSON.parse(JSON.stringify(atvsFullData.a[cause].response.result));

				showInputDiv = true;
				$("#inputSource").css("display", "inline-block");
				$("#inputData").css("display", "inline-block");
			}
			else{
				$("#inputSource").css("display", "none");
				$("#inputData").css("display", "none");
			}

			if(showInputDiv){
				$("#inputDiv").css("display", "block");
			}
			else{
				$("#inputDiv").css("display", "none");
			}

			// invoke what - I would only know if it's a sequence, an action in a sequence, or a trigger
			var invokeMsg = "";
			if(a.kind == "trigger"){
				if(a.rules != undefined){
					a.rules.forEach(function(rid){
						if(invokeMsg.length>0)
							invokeMsg += ", ";
						invokeMsg += domActivationText(rid);
						/*if(atvsFullData.a[rid].action != undefined){
							invokeMsg += " and "+domActivationText(atvsFullData.a[rid].action);
						}*/
					});
				}
			}
			else if(a.kind == "rule"){
				invokeMsg += domActivationText(a.action);
			}
			else if(a.kind == "sequence"){
				invokeMsg = $("<span><select id='sortSeq' src='"+a.activationId+"'><option value='start' selected>Sort by start time</option><option value='perf'>Sort by execution time</option></select><div style='padding-top:5px;'></div></span>");

				$(invokeMsg).find("#sortSeq").change(function(){
					let id = $(this).attr("src"), msg = "";
					if($(this).val() == "start"){
						atvsFullData.a[id].logs.forEach(function(aid){
							if(msg.length>0)
								msg += ", ";
							msg += domActivationText(atvsFullData.a[aid].activationId);
						});
					}
					else{
						let array = [];
						atvsFullData.a[id].logs.forEach(function(aid){
							array.push({id:aid, time:atvsFullData.a[aid].duration});
						});
						array.sort((a,b) => {return b.time - a.time;});

						array.forEach(function(o){
							let aid = o.id, t = o.time;
							if(msg.length>0)
								msg += ", ";
							msg += (domActivationText(atvsFullData.a[aid].activationId)+" ("+t+"ms, "+Math.round(t/atvsFullData.a[id].duration*100)+"%)");
						});
					}

					$(this).next().html(msg);
					$(this).next().find("a").mouseover(function(e){
						var id = $(this).attr("source");
						if(id)
							$("#"+id).css("fill", timelineBarColor.hover);
					}).mouseout(function(e){
						var id = $(this).attr("source");
						if(id)
							$("#"+id).css("fill", timelineBarColor[atvsFullData.a[id].kind]);
						/*var color = $("#"+id).css("stroke");
						if(color == "rgb(255, 255, 0)")
							$("#"+id).css("stroke", "none");*/

					}).click(function(e){
						e.stopPropagation();
						var id = $(this).attr("source");
						if(id){
							$("#"+currentItem).css("stroke", "none");

							$("#"+id).css("fill", timelineBarColor[atvsFullData.a[id].kind])
							$("#"+id).css("stroke", "orangered");

							currentItem = id;
							atvDetail(id, true);
						}
					});

				});

				$(invokeMsg).find("#sortSeq").trigger("change");



			}
			else if(a.kind == "action"){
				if(a.cause != undefined){
					// in a sequence, find the next action in the sequence
					var atv_pat = atvsFullData.a[a.cause];
					if(atv_pat != undefined && atv_pat.logs != undefined){
						var index = atv_pat.logs.indexOf(a.activationId);
						if(index != atv_pat.logs.length-1){
							invokeMsg += domActivationText(atv_pat.logs[index+1]);
						}
					}
				}
			}

			if(invokeMsg.length>0){
				$("#invokeContent").html(invokeMsg);
				$("#invokeDiv").css("display", "block");
			}
			else{
				$("#invokeDiv").css("display", "none");
			}

			var outputObj;
			// output data
			if(a.response != undefined && a.response.result != undefined){
				var outputData = "<pre>"+escapeHTML(JSON.stringify(a.response.result, undefined, 4));
				if(outputData.length > jsonTextLimit){
					outputData = outputData.substring(0, jsonTextLimit)+"... ";
					outputData += "</pre><span class='viewMore' r='"+a.activationId+"'>(view all)</span>"
				}
				else{
					outputData += "</pre>"
				}

				$("#outputData").html(outputData);
				$
				$("#outputData").find(".viewMore").click(function(e){
					var id = $(this).attr("r"), name = atvsFullData.a[id].name;
					var title = name+", activation Id "+id;
					writeNewPageJSON(atvsFullData.a[id].response.result, title);

				});

				outputObj = JSON.parse(JSON.stringify(a.response.result));

				$("#outputDiv").css("display", "block");
				$("#outputData").css("display", "inline-block");
			}
			else{
				$("#outputDiv").css("display", "none");
				$("#outputData").css("display", "none");
			}

			//diff
			if(inputObj != undefined && outputObj != undefined && jsondiffpatch.formatters != undefined){
				// cauclate diff using the diff library
				var delta = jsondiffpatch.diff(inputObj, outputObj);
				var deltaHTML = jsondiffpatch.formatters.html.format(delta, inputObj);
				var body = "<head><title>Diff</title><link rel='stylesheet' text='text/css' href='/lib/jsonDiffStyle/html.css'/></head><body>"+deltaHTML+"</body>";
				$("#diffLink").off().click(function(e){
					writeExternalPage(body);
				});
				$("#diffLink").css("display", "inline-block");
			}
			else{
				$("#diffLink").css("display", "none");
			}

			// dev logs
			if(a.logs != undefined && a.logs.length>0){
				var msg = [];
				for(var i=0; i<a.logs.length; i++){
					var index = a.logs[i].indexOf("stdout");
					if(index != -1){
						msg.push(a.logs[i].substring(index+"stdout".length+	1));
					}
				}

				if(msg.length>0){
					var s = "";
					for(var i=0; i<msg.length; i++){
						s += msg[i]+"<br/>";
					}
					$("#devLogData").html(s);
					$("#devLogsDiv").css("display", "block");
				}
				else{
					$("#devLogsDiv").css("display", "none");
				}
			}

			// code
			if(a.kind == "action"){
				if(atvsFullData.e[a.name] && atvsFullData.e[a.name].exec && atvsFullData.e[a.name].exec.code){
					$("#codeLink").attr("name", a.name).off().on("click", function(e){
						var p = $(this).attr("path");
						writeNewPageCode(atvsFullData.e[a.name].exec.code, a.name);
					}).css("display", "inline-block");
				}
				else{
					$("#codeLink").css("display", "none");
				}
			}
			else{
				$("#codeLink").css("display", "none");
			}

			// all a
			$("#atvInfo").find("a").mouseover(function(e){
				var id = $(this).attr("source");
				if(id)
					$("#"+id).css("fill", timelineBarColor.hover);
			}).mouseout(function(e){
				var id = $(this).attr("source");
				if(id)
					$("#"+id).css("fill", timelineBarColor[atvsFullData.a[id].kind]);
				/*var color = $("#"+id).css("stroke");
				if(color == "rgb(255, 255, 0)")
					$("#"+id).css("stroke", "none");*/

			}).click(function(e){
				e.stopPropagation();
				var id = $(this).attr("source");
				if(id){
					$("#"+currentItem).css("stroke", "none");

					$("#"+id).css("fill", timelineBarColor[atvsFullData.a[id].kind])
					$("#"+id).css("stroke", "orangered");

					currentItem = id;
					atvDetail(id, true);
				}


			});

			$("#rawJSONAtv").html("<pre>"+syntaxHighlight(a)+"</pre>");


			$("#atvInfoDiv").css("visibility", "visible");
			//$("#jsonDetail").html(JSON.stringify(a));

		}

	}

	function domActivationText(id){
		if(id == undefined)
			return ""
		else{
			return "<a class='"+atvsFullData.a[id].kind+"Text' style='color:"+timelineBarColor[atvsFullData.a[id].kind]+";' source='"+id+"'>"+atvsFullData.a[id].name+"</a>";
		}
	}


	function writeNewPageCode(code, title){
		if(title == undefined)
			title = "";
		var link='<link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.10.0/styles/default.min.css">\n<script src="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.10.0/highlight.min.js"></script>\n<script>hljs.initHighlightingOnLoad();</script>';
		var data = "<head>\n<title>"+title+"</title>\n"+link+"\n</head><body><pre><code style='background-color:white'>"+code+"</code></pre></body>";
		writeExternalPage(data);
	}

	function writeNewPageJSON(obj, title){
		if(title == undefined)
			title = "";
		var data = "<head><title>"+title+"</title><style>pre {padding: 5px; margin: 5px; } .string { color: red; } .number{ color: blue; } .boolean{ color: magenta; } .null{ color: magenta; } .key{ color: purple; }</style></head><body><pre>"
		data += syntaxHighlight(obj);
		data += "</pre></body>"
		writeExternalPage(data);
	}

	function writeExternalPage(html){
		var x=window.open();
		x.document.open();
		x.document.write(html);
		x.document.close();
	}

	function adjustCSSFontSize(TLFont, paneFont){
		if(document.styleSheets.length>0){
			var ss = document.styleSheets[0];
			for(var i=0; i<ss.cssRules.length; i++){
				var rule = ss.cssRules[i];
				if(rule.selectorText == "#overviewChart text"
					|| rule.selectorText == "#resetSelection"
					|| rule.selectorText == "#timelineChart text"){

					var oldSize = rule.style["font-size"].substring(0, rule.style["font-size"].length-"px".length);
					rule.style["font-size"] = Math.round(oldSize*TLFont)+"px";
				}
				else if(rule.selectorText == "#atvInfoDiv"
					|| rule.selectorText == "#activationIdDiv"
					|| rule.selectorText == "#costSpan"
					|| rule.selectorText == "#actionName"
					|| rule.selectorText == "#actionType"
					|| rule.selectorText == "#codeLink"
					|| rule.selectorText == ".viewMore"
					|| rule.selectorText == "pre"){

					var oldSize = rule.style["font-size"].substring(0, rule.style["font-size"].length-"px".length);
					rule.style["font-size"] = Math.round(oldSize*paneFont)+"px";
				}
			}

		}

	}

	function escapeHTML(s){
		return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
	}

	// helper code to beautify JSON
	function syntaxHighlight(obj) {
		var json = JSON.stringify(obj, undefined, 4);
		json = escapeHTML(json);
		return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
			var cls = 'number';
			if (/^"/.test(match)) {
			if (/:$/.test(match)) {
			cls = 'key';
			} else {
			cls = 'string';
			}
			} else if (/true|false/.test(match)) {
			cls = 'boolean';
			} else if (/null/.test(match)) {
			cls = 'null';
			}
			return '<span class="' + cls + '">' + match + '</span>';
		});
	}


	function resize(){
		$("#atvInfoDiv").css("width", atvInfoWidth+"px");
		chartWidth = $("body").width() - yLabelWidth - atvInfoWidth - 25 - 25 - gapWidth;
		$("#charts").css("width", chartWidth+yLabelWidth);
		// for vertical scolls
		$("#charts, #atvInfoDiv").css({
			"height":($(window).height()-50)+"px",
			"overflow-y": "auto"
		});
		plotOverviewGraph(selectedAtvsData);
	}

});




