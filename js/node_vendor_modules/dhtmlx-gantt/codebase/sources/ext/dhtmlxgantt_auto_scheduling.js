/*!
 * @license
 * 
 * dhtmlxGantt v.5.1.5 Professional
 * This software is covered by DHTMLX Enterprise License. Usage without proper license is prohibited.
 * 
 * (c) Dinamenta, UAB.
 * 
 */
Gantt.plugin(function(gantt){
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 2);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */,
/* 1 */
/***/ (function(module, exports) {

module.exports = function(gantt) {
// helpers for building chain of dependencies, used for critical path calculation and for auto scheduling

	gantt._get_linked_task = function (link, getTarget) {
		var task = null;
		var taskId = getTarget ? link.target : link.source;

		if (gantt.isTaskExists(taskId)) {
			task = gantt.getTask(taskId);
		}

		return task;
	};
	gantt._get_link_target = function (link) {
		return gantt._get_linked_task(link, true);
	};

	gantt._get_link_source = function (link) {
		return gantt._get_linked_task(link, false);
	};

	var caching = false;
	var formattedLinksStash = {};
	var inheritedSuccessorsStash = {};
	var inheritedPredecessorsStash = {};
	var getPredecessorsCache = {};


	gantt._isLinksCacheEnabled = function(){
		return caching;
	};
	gantt._startLinksCache = function(){
		formattedLinksStash = {};
		inheritedSuccessorsStash = {};
		inheritedPredecessorsStash = {};
		getPredecessorsCache = {};
		caching = true;
	};
	gantt._endLinksCache = function(){
		formattedLinksStash = {};
		inheritedSuccessorsStash = {};
		inheritedPredecessorsStash = {};
		getPredecessorsCache = {};
		caching = false;
	};

	gantt._formatLink = function (link) {


		if(caching && formattedLinksStash[link.id]){
			return formattedLinksStash[link.id];
		}

		var relations = [];
		var target = this._get_link_target(link);
		var source = this._get_link_source(link);

		if (!(source && target)) {
			return relations;
		}

		if ((gantt.isSummaryTask(target) && gantt.isChildOf(source.id, target.id)) || (gantt.isSummaryTask(source) && gantt.isChildOf(target.id, source.id))) {
			return relations;
		}


		// there are three kinds of connections at this point
		// task -> task - regular link
		// task -> project - transform it into set of regular links (task -> [each subtask]), use offset beetween subtask and project dates as lag, in order not to change mutual positions of subtasks inside a project
		// project -> task - transform it into ([each subtask] -> task) links
		// project -> project - transform it into ([each subtask of p1] -> [each subtask of p2]) links

		var from = this._getImplicitLinks(link, source, function (c) {
			return 0;
		}, true);

		var respectTargetOffset = gantt.config.auto_scheduling_move_projects;
		var targetDates = this.isSummaryTask(target) ? this.getSubtaskDates(target.id) : {
			start_date: target.start_date,
			end_date: target.end_date
		};
		var to = this._getImplicitLinks(link, target, function (c) {
			if (!respectTargetOffset) {
				return 0;
			} else {

				if (!c.$target.length && !(gantt.getState().drag_id == c.id)) {// drag_id - virtual lag shouldn't restrict task that is being moved inside project
					return gantt.calculateDuration({
						start_date: targetDates.start_date,
						end_date: c.start_date,
						task: source
					});
				} else {
					return 0;
				}
			}
		});

		for (var i = 0, fromLength = from.length; i < fromLength; i++) {
			var fromTask = from[i];
			for (var j = 0, toLength = to.length; j < toLength; j++) {
				var toTask = to[j];

				var lag = fromTask.lag * 1 + toTask.lag * 1;

				var subtaskLink = {
					id: link.id,
					type: link.type,
					source: fromTask.task,
					target: toTask.task,
					lag: (link.lag * 1 || 0) + lag
				};

				relations.push(gantt._convertToFinishToStartLink(toTask.task, subtaskLink, source, target, fromTask.taskParent, toTask.taskParent));
			}
		}

		if(caching)
			formattedLinksStash[link.id] = relations;

		return relations;
	};

gantt._isAutoSchedulable = function(task){
	return task.auto_scheduling !== false;
};

gantt._getImplicitLinks = function(link, parent, selectOffset, selectSourceLinks){
	var relations = [];

	if(this.isSummaryTask(parent)){

		// if the summary task contains multiple chains of linked tasks - no need to consider every task of the chain,
		// it will be enough to check the first/last tasks of the chain
		// special conditions if there are unscheduled tasks in the chain, or negative lag values
		var children = {};
		this.eachTask(function(c){
			if(!this.isSummaryTask(c)){
				children[c.id] = c;
			}
		}, parent.id);

		var skipChild;

		for(var c in children){
			var task = children[c];
			var linksCollection = selectSourceLinks ? task.$source : task.$target;

			skipChild = false;

			for(var l = 0; l < linksCollection.length; l++){
				var siblingLink = gantt.getLink(linksCollection[l]);
				var siblingId = selectSourceLinks ? siblingLink.target : siblingLink.source;

				if(children[siblingId] && task.auto_scheduling !== false && children[siblingId].auto_scheduling !== false && siblingLink.lag >= 0){
					skipChild = true;
					break;
				}
			}
			if(!skipChild){
				relations.push({task: task.id, taskParent: task.parent, lag: selectOffset(task)});
			}
		}

	}else{
		relations.push({task:parent.id, taskParent: parent.parent, lag: 0});
	}

		return relations;
};

	gantt._getDirectDependencies = function (task, selectSuccessors) {

		var links = [],
			successors = [];

		var linksIds = selectSuccessors ? task.$source : task.$target;

	for(var i = 0; i < linksIds.length; i++){
		var link = this.getLink(linksIds[i]);
		if(this.isTaskExists(link.source) && this.isTaskExists(link.target)) {
			var target = this.getTask(link.target);
			if(this._isAutoSchedulable(target)){
				links.push(this.getLink(linksIds[i]));
			}
		}
	}

		for (var i = 0; i < links.length; i++) {
			successors = successors.concat(this._formatLink(links[i]));
		}

		return successors;
	};

	gantt._getInheritedDependencies = function (task, selectSuccessors) {

		//var successors = [];
	var stop = false;
	var inheritedRelations = [];
	var cacheCollection;
	if(this.isTaskExists(task.id)){

		var parent = this.getParent(task.id);


		this.eachParent(function(parent){
			if(stop)
				return;

			if(caching){
				cacheCollection = selectSuccessors ? inheritedSuccessorsStash : inheritedPredecessorsStash;
				if(cacheCollection[parent.id]){
					inheritedRelations.push.apply(inheritedRelations, cacheCollection[parent.id]);
					return;
				}
			}

			var parentDependencies;
			if(this.isSummaryTask(parent)){
				if(!this._isAutoSchedulable(parent)){
					stop = true;
				}else{
					parentDependencies = this._getDirectDependencies(parent, selectSuccessors);
					if(caching){
						cacheCollection[parent.id] = parentDependencies;
					}
					inheritedRelations.push.apply(inheritedRelations, parentDependencies);
				}
			}

		}, task.id, this);

		//	for (var i = 0; i < inheritedRelations.length; i++) {
		//		successors.push(inheritedRelations[i]);
		//	}
		}

		return inheritedRelations;
	};


	gantt._getDirectSuccessors = function (task) {
		return this._getDirectDependencies(task, true);
	};

	gantt._getInheritedSuccessors = function (task) {
		return this._getInheritedDependencies(task, true);
	};

	gantt._getDirectPredecessors = function (task) {
		return this._getDirectDependencies(task, false);
	};

	gantt._getInheritedPredecessors = function (task) {
		return this._getInheritedDependencies(task, false);
	};

	gantt._getSuccessors = function (task, skipInherited) {
		var successors = this._getDirectSuccessors(task);
		if(skipInherited){
			return successors;
		}else{
			return successors.concat(this._getInheritedSuccessors(task));
		}
	};

	gantt._getPredecessors = function (task, skipInherited) {
		var key = task.id + skipInherited;
		var result;

		if(caching && getPredecessorsCache[key]){
			return getPredecessorsCache[key];
		}

		var predecessors = this._getDirectPredecessors(task);
		if(skipInherited){
			result = predecessors;
		}else{
			result = predecessors.concat(this._getInheritedPredecessors(task));
		}
		if(caching){
			getPredecessorsCache[key] = result;
		}
		return result;
	};


	gantt._convertToFinishToStartLink = function (id, link, sourceTask, targetTask, sourceParent, targetParent) {
		// convert finish-to-finish, start-to-finish and start-to-start to finish-to-start link and provide some additional properties
		var res = {
			target: id,
			link: gantt.config.links.finish_to_start,
			id: link.id,
			lag: link.lag || 0,
			source: link.source,
			preferredStart: null,
			sourceParent: sourceParent,
			targetParent: targetParent
		};

		var additionalLag = 0;
		switch (link.type) {
			case gantt.config.links.start_to_start:
				additionalLag = -sourceTask.duration;
				break;
			case gantt.config.links.finish_to_finish:
				additionalLag = -targetTask.duration;
				break;
			case gantt.config.links.start_to_finish:
				additionalLag = -sourceTask.duration - targetTask.duration;
				break;
			default:
				additionalLag = 0;
		}

		res.lag += additionalLag;
		return res;
	};
};

/***/ }),
/* 2 */
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(3);


/***/ }),
/* 3 */
/***/ (function(module, exports, __webpack_require__) {

__webpack_require__(1)(gantt);

gantt.config.auto_scheduling = false;
gantt.config.auto_scheduling_descendant_links = false;
gantt.config.auto_scheduling_initial = true;
gantt.config.auto_scheduling_strict = false;
gantt.config.auto_scheduling_move_projects = true;

(function(){

var helpers = __webpack_require__(4);


gantt._autoSchedulingGraph = {
	getVertices: function(relations){
		var ids = {};
		var rel;
		for(var i = 0, len = relations.length; i < len; i++){
			rel = relations[i];
			ids[rel.target] = rel.target;
			ids[rel.source] = rel.source;
		}

		var vertices = [];
		var id;
		for(var i in ids){
			id = ids[i];
			vertices.push(id);
		}

		return vertices;
	},
	topologicalSort: function(edges){
		var vertices = this.getVertices(edges);
		var hash = {};

		for(var i = 0, len = vertices.length; i < len; i ++){
			hash[vertices[i]] = {id: vertices[i], $source:[], $target:[], $incoming: 0};
		}

		for(var i = 0, len = edges.length; i < len; i++){
			var successor = hash[edges[i].target];
			successor.$target.push(i);
			successor.$incoming = successor.$target.length;
			hash[edges[i].source].$source.push(i);

		}

		// topological sort, Kahn's algorithm
		var S = vertices.filter(function(v){ return !hash[v].$incoming; });

		var L = [];

		while(S.length){
			var n = S.pop();

			L.push(n);

			var node = hash[n];

			for(var i = 0; i < node.$source.length; i++){
				var m = hash[edges[node.$source[i]].target];
				m.$incoming--;
				if(!m.$incoming){
					S.push(m.id);
				}

			}
		}

		return L;

	},
	_groupEdgesBySource: function(edges){
		var res = {};
		var edge;
		for(var i = 0, len = edges.length; i < len; i++){
			edge = edges[i];
			if(!res[edge.source]){
				res[edge.source] = [];
			}
			res[edge.source].push(edge);
		}
		return res;
	},
	tarjanStronglyConnectedComponents: function(vertices, edges){
		//https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
		var verticesHash = {};

		var index = 0;
		var S = [];
		var connectedEdges = [];
		var connectedComponents = [];

		var edgesFromTasks = this._groupEdgesBySource(edges);

		var vertexId;
		for(var v = 0, len = vertices.length; v < len; v++){
			vertexId = vertices[v];
			var vertex = getVertex(vertexId);
			if(vertex.index === undefined){
				strongConnect(vertexId);
			}
		}

		function strongConnect(vertexId, previousLink){
			var v = getVertex(vertexId);
			v.index = index;
			v.lowLink = index;
			index++;

			if(previousLink){
				connectedEdges.push(previousLink);
			}

			S.push(v);
			v.onStack = true;

			// Consider successors of v

			var edge;
			var successors = edgesFromTasks[vertexId];
			for(var e = 0; successors  && e < successors.length; e++){
				edge = successors[e];
				if(edge.source != vertexId) continue;

				var v = getVertex(edge.source);
				var w = getVertex(edge.target);
				if(w.index === undefined){
					// Successor w has not yet been visited; recurse on it
					strongConnect(edge.target, edge);
					v.lowLink = Math.min(v.lowLink, w.lowLink);
				}else if(w.onStack){
					// Successor w is in stack S and hence in the current SCC
					v.lowLink = Math.min(v.lowLink, w.index);
					connectedEdges.push(edge);
				}

			}

			// If v is a root node, pop the stack and generate an SCC
			if(v.lowLink == v.index){
				var connectedComponent = {tasks: [], links:[]};
				var w;
				do{
					var rel = connectedEdges.pop();
					w = S.pop();
					w.onStack = false;
					connectedComponent.tasks.push(w.id);
					if(rel){
						connectedComponent.links.push(rel.id);
					}
				}while(w.id != v.id);
				connectedComponents.push(connectedComponent);
			}

		}

		return connectedComponents;

		function getVertex(id){
			if(!verticesHash[id]){
				verticesHash[id] = {id: id, onStack:false, index: undefined, lowLink: undefined};
			}

			return verticesHash[id];
		}

	}

};

gantt._autoSchedulingPath = {
	getKey: function(rel){
		return rel.lag +"_"+ rel.link +"_"+ rel.source +"_"+ rel.target;
	},
	getVirtualRoot: function(){
		return gantt.mixin(
			gantt.getSubtaskDates(),
			{
				id: gantt.config.root_id,
				type: gantt.config.types.project,
				$source: [],
				$target: [],
				$virtual: true
			}
		);
	},

	filterDuplicates: function(relations){
		var keys = {};
		for(var i = 0; i < relations.length; i++){
			var key = this.getKey(relations[i]);
			if(keys[key]){
				relations.splice(i, 1);
				i--;
			}else{
				keys[key] = true;
			}
		}
		return relations;
	},

	getLinkedTasks: function(id, includePredecessors){
		var startIds = [id];

		//TODO: format links cache
		var clearCache = false;
		if(!gantt._isLinksCacheEnabled()) {
			gantt._startLinksCache();
			clearCache = true;
		}
		var relations = [];
		var visited = {};
		for(var i = 0; i < startIds.length; i++){
			this._getLinkedTasks(startIds[i], visited, includePredecessors);
		}

		for(var i in visited){
			relations.push(visited[i]);
		}
		if(clearCache)
			gantt._endLinksCache();
		return relations;
	},

	_getLinkedTasks: function(rootTask, visitedTasks, includePredecessors, isChild) {
		var from = rootTask === undefined ? gantt.config.root_id : rootTask;
		var visited = visitedTasks || {};

		var rootObj = gantt.isTaskExists(from) ? gantt.getTask(from) : this.getVirtualRoot();

		var successors = gantt._getSuccessors(rootObj, isChild);

		var predecessors = [];
		if (includePredecessors) {
			predecessors = gantt._getPredecessors(rootObj, isChild);
		}
		var relations = [];
		var linkKey;

		for(var i = 0; i < successors.length; i++){
			linkKey = this.getKey(successors[i]);
			if(visited[linkKey]) {
				continue;
			}else{
				visited[linkKey] = successors[i];
				relations.push(successors[i]);
			}
		}
		for(var i = 0; i < predecessors.length; i++){
			linkKey = this.getKey(predecessors[i]);
			if(visited[linkKey]) {
				continue;
			}else{
				visited[linkKey] = predecessors[i];
				relations.push(predecessors[i]);
			}
		}

		for(var i=0; i < relations.length; i++){
			var isSameParent = relations[i].sourceParent == relations[i].targetParent;
			this._getLinkedTasks(relations[i].target, visited, true, isSameParent);
		}

		if(gantt.hasChild(rootObj.id)){
			var children = gantt.getChildren(rootObj.id);
			for(var i=0; i < children.length; i++){
				this._getLinkedTasks(children[i], visited, true, true);
			}
		}

		return relations;
	},

	findLoops: function(relations){

		var cycles = [];

		helpers.forEach(relations, function(rel){
			if(rel.target == rel.source)
				cycles.push([rel.target, rel.source]);
		});

		var graph =  gantt._autoSchedulingGraph;
		var vertices = graph.getVertices(relations);

		var connectedComponents = graph.tarjanStronglyConnectedComponents(vertices, relations);

		helpers.forEach(connectedComponents, function(component){
			if(component.tasks.length > 1){
				cycles.push(component);//{ tasks: [task ids], links: [links ids]}
			}
		});

		return cycles;
		//{task:id, link:link.type, lag: link.lag || 0, source: link.source}
	}
};

gantt._autoSchedulingDateResolver = {
	isFirstSmaller: function(small, big, task){
		if(small.valueOf() < big.valueOf() && gantt._hasDuration(small, big, task))
			return true;
		return false;
	},

	isSmallerOrDefault: function(smallDate, bigDate, task){
		return !!(!smallDate || this.isFirstSmaller(smallDate, bigDate, task));
	},

	resolveRelationDate: function(taskId, relations, getEndDate){
		var minStart = null;
		var linkId = null;

		var defaultStart = null;
		var task;
		for(var i = 0; i < relations.length; i++){
			var relation = relations[i];
			taskId = relation.target;

			defaultStart = relation.preferredStart;
			task = gantt.getTask(taskId);
			var constraintDate = this.getConstraintDate(relation, getEndDate, task);


			if(this.isSmallerOrDefault(defaultStart, constraintDate, task) && this.isSmallerOrDefault(minStart, constraintDate, task)){
				minStart = constraintDate;
				linkId = relation.id;
			}

		}

		if(minStart){
			minStart = gantt.getClosestWorkTime({date:minStart, dir:"future", task:gantt.getTask(taskId)});
		}

		return {
			link: linkId,
			task: taskId,
			start_date: minStart
		};
	},
	getConstraintDate: function(relation, getEndDate, task){
		var predecessorEnd = getEndDate(relation.source);
		var successor = task;

		var successorStart = gantt.getClosestWorkTime({date:predecessorEnd, dir:"future", task:successor});

		if(predecessorEnd && relation.lag && relation.lag*1 == relation.lag){
			successorStart = gantt.calculateEndDate({start_date: predecessorEnd, duration: relation.lag*1, task: successor});
		}

		return successorStart;
	}
};

gantt._autoSchedulingPlanner = {
	generatePlan: function(relations){

		var graph = gantt._autoSchedulingGraph;
		var orderedIds = graph.topologicalSort(relations);
		var predecessorRelations = {},
			plansHash = {};

		var id;
		for(var i = 0, len = orderedIds.length; i < len; i++){
			id = orderedIds[i];
			var task = gantt.getTask(id);
			if(task.auto_scheduling === false){
				continue;
			}
			predecessorRelations[id] = [];
			plansHash[id] = null;
		}

		function getPredecessorEndDate(id){
			var plan = plansHash[id];
			var task = gantt.getTask(id);
			var res;

			if(!(plan && (plan.start_date || plan.end_date))){
				res = task.end_date;
			}else if(plan.end_date){
				res = plan.end_date;
			}else {
				res = gantt.calculateEndDate({start_date: plan.start_date, duration: task.duration, task: task});
			}

			return res;
		}

		var rel;
		for(var i = 0, len = relations.length; i < len; i++){
			rel = relations[i];
			if(predecessorRelations[rel.target]) {
				predecessorRelations[rel.target].push(rel);
			}
		}

		var dateResolver = gantt._autoSchedulingDateResolver;

		var result = [];
		for(var i = 0; i < orderedIds.length; i++){
			var currentId = orderedIds[i];

			var plan = dateResolver.resolveRelationDate(currentId, predecessorRelations[currentId] || [], getPredecessorEndDate);


			if(plan.start_date && gantt.isLinkExists(plan.link)){
				var link = gantt.getLink(plan.link);
				var task = gantt.getTask(currentId);
				var predecessor = gantt.getTask(link.source);

				if (task.start_date.valueOf() !== plan.start_date.valueOf() && gantt.callEvent("onBeforeTaskAutoSchedule", [task, plan.start_date, link, predecessor]) === false) {
					continue;
				}
			}

			plansHash[currentId] = plan;
			if(plan.start_date){
				result.push(plan);
			}
		}

		return result;

	},

	applyProjectPlan: function(projectPlan){
		var plan, task, link, predecessor;

		var updateTasks = [];
		for(var i = 0; i <  projectPlan.length; i++){
			link = null;
			predecessor = null;
			plan = projectPlan[i];

			if(!plan.task) continue;

			task = gantt.getTask(plan.task);
			if(plan.link){
				link = gantt.getLink(plan.link);
				predecessor = gantt.getTask(link.source);
			}

			var newDate = null;
			if(plan.start_date && (task.start_date.valueOf() != plan.start_date.valueOf())){
				newDate = plan.start_date;
			}

			if(!newDate) continue;
			
			task.start_date = newDate;
			task.end_date = gantt.calculateEndDate(task);

			updateTasks.push(task.id);
			gantt.callEvent("onAfterTaskAutoSchedule", [task, newDate, link, predecessor]);

		}
		return updateTasks;
	}
};

gantt._autoSchedulingPreferredDates = function(startTask, relations){
	for(var i = 0; i < relations.length; i++){
		var rel = relations[i];
		var task = gantt.getTask(rel.target);

		if(!gantt.config.auto_scheduling_strict || rel.target == startTask){
			rel.preferredStart = new Date(task.start_date);
		}
	}
};

gantt._autoSchedule = function(id, relations, updateCallback){
	if (gantt.callEvent("onBeforeAutoSchedule", [id]) === false) {
		return;
	}
	gantt._autoscheduling_in_progress = true;

	var path = gantt._autoSchedulingPath;

	var updatedTasks = [];

	var cycles = path.findLoops(relations);
	if(cycles.length){
		gantt.callEvent("onAutoScheduleCircularLink", [cycles]);
	}else{

		var planner = gantt._autoSchedulingPlanner;
		gantt._autoSchedulingPreferredDates(id, relations);

		var plan = planner.generatePlan(relations);
		updatedTasks = planner.applyProjectPlan(plan);

		if(updateCallback){
			updateCallback(updatedTasks);
		}
	}

	gantt._autoscheduling_in_progress = false;
	gantt.callEvent("onAfterAutoSchedule", [id, updatedTasks]);

	return updatedTasks;
};

gantt.autoSchedule = function(id, inclusive){

	if(inclusive === undefined){
		inclusive = true;
	}else{
		inclusive = !!inclusive;
	}

	var relations =  gantt._autoSchedulingPath.getLinkedTasks(id, inclusive);

	var totalRelations = relations.length;
	var startAutoSchedule = Date.now();
	gantt._autoSchedule(id, relations, gantt._finalizeAutoSchedulingChanges);
	var durationAutoSchedule = Date.now() - startAutoSchedule;
};

gantt._finalizeAutoSchedulingChanges = function(updatedTasks){
	function resetTime(task){
		if(batchUpdate)
			return;

		var start = task.start_date.valueOf(),
			end = task.end_date.valueOf();

		gantt.resetProjectDates(task);
		if(task.start_date.valueOf() != start || task.end_date.valueOf() != end){
			batchUpdate = true;
			return;
		}
		var children = gantt.getChildren(task.id);
		for(var i = 0; !batchUpdate && i < children.length; i++){
			resetTime(gantt.getTask(children[i]));
		}
	}

	var batchUpdate = false;
	// call batchUpdate (full repaint) only if we update multiple tasks,
	if(updatedTasks.length == 1){
		gantt.eachParent(resetTime, updatedTasks[0]);
	}else if(updatedTasks.length){
		batchUpdate = true;
	}

	function payload(){
		for(var i = 0; i < updatedTasks.length; i++){
			gantt.updateTask(updatedTasks[i]);
		}
	}
	if(batchUpdate){
		gantt.batchUpdate(payload);
	}else{
		payload();
	}

};

gantt.isCircularLink = function(link){
	return !!gantt._getConnectedGroup(link);

};

gantt._getConnectedGroup = function(link){
	var manager = gantt._autoSchedulingPath;

	var allRelations = manager.getLinkedTasks();
	if(!gantt.isLinkExists(link.id)){
		allRelations = allRelations.concat(gantt._formatLink(link));
	}

	var cycles = manager.findLoops(allRelations);

	var found = false;
	for(var i = 0; (i < cycles.length) && !found; i++){
		var links = cycles[i].links;
		for(var j = 0; j < links.length; j++){
			if(links[j] == link.id){
				return cycles[i];
			}
		}
	}

	return null;
};

gantt.findCycles = function(){
	var manager = gantt._autoSchedulingPath;

	var allRelations = manager.getLinkedTasks();
	return manager.findLoops(allRelations);
};

gantt._attachAutoSchedulingHandlers = function(){

	gantt._autoScheduleAfterLinkChange = function (linkId, link) {
		if (gantt.config.auto_scheduling && !this._autoscheduling_in_progress) {
			gantt.autoSchedule(link.source);
		}
	};

	gantt.attachEvent("onAfterLinkUpdate", gantt._autoScheduleAfterLinkChange);
	gantt.attachEvent("onAfterLinkAdd", gantt._autoScheduleAfterLinkChange);

	gantt.attachEvent("onAfterLinkDelete", function(id, link){
		if (this.config.auto_scheduling && !this._autoscheduling_in_progress && this.isTaskExists(link.target)) {
			// after link deleted - auto schedule target for other relations that may be left
			var target = this.getTask(link.target);
			var predecessors = this._getPredecessors(target);
			if(predecessors.length){
				this.autoSchedule(predecessors[0].source, false);
			}
		}
	});

	gantt.attachEvent("onParse", function(){
		if (gantt.config.auto_scheduling && gantt.config.auto_scheduling_initial) {
			gantt.autoSchedule();
		}
	});

	gantt._preventCircularLink = function(id, link){
		if(gantt.isCircularLink(link)){
			gantt.callEvent("onCircularLinkError", [link, gantt._getConnectedGroup(link)]);
			return false;
		}else{
			return true;
		}
	};

	gantt._preventDescendantLink = function(id, link){
		var source = gantt.getTask(link.source),
			target = gantt.getTask(link.target);

		if(!gantt.config.auto_scheduling_descendant_links){
			if((gantt.isChildOf(source.id, target.id) && gantt.isSummaryTask(target)) || (gantt.isChildOf(target.id, source.id) && gantt.isSummaryTask(source))){
				return false;
			}
		}
		return true;
	};

	gantt.attachEvent("onBeforeLinkAdd", gantt._preventCircularLink);
	gantt.attachEvent("onBeforeLinkAdd", gantt._preventDescendantLink);
	gantt.attachEvent("onBeforeLinkUpdate", gantt._preventCircularLink);
	gantt.attachEvent("onBeforeLinkUpdate", gantt._preventDescendantLink);

	gantt._datesNotEqual = function(dateA, dateB, taskA, taskB){
		if(dateA.valueOf() > dateB.valueOf()){
			return this._hasDuration({start_date: dateB, end_date: dateA, task: taskB});
		}else{
			return this._hasDuration({start_date: dateA, end_date: dateB, task: taskA});
		}
	};
	gantt._notEqualTaskDates = function(task1, task2){
		if (this._datesNotEqual(task1.start_date, task2.start_date, task1, task2) ||
			((this._datesNotEqual(task1.end_date, task2.end_date, task1, task2) ||
				task1.duration != task2.duration) && task1.type != gantt.config.types.milestone)) {
			return true;
		}
	};

	var relations;
	var movedTask;
	gantt.attachEvent("onBeforeTaskDrag", function(id, mode, task){
		if(gantt.config.auto_scheduling && gantt.config.auto_scheduling_move_projects){
			// collect relations before drag and drop  in order to have original positions of subtasks within project since they are used as lag when moving dependent project
			relations = gantt._autoSchedulingPath.getLinkedTasks(id, true);
			movedTask = id;
		}
		return true;
	});

	function resetToStartLinksLags(taskId, relations){
		var skipped = false;
		for(var i = 0; i < relations.length; i++){
			var originalLink = gantt.getLink(relations[i].id);
			if(originalLink.type == gantt.config.links.start_to_start || originalLink.type == gantt.config.links.start_to_finish){
				relations.splice(i, 1);
				i--;
				skipped = true;
			}
		}

		if(skipped){
			var presentLinks = {};
			for(var i = 0; i < relations.length; i++){
				presentLinks[relations[i].id] = true;
			}

			var updatedLinks = gantt._autoSchedulingPath.getLinkedTasks(taskId, true);
			for(var i = 0; i < updatedLinks.length; i++){
				if(!presentLinks[updatedLinks[i].id]){
					relations.push(updatedLinks[i]);
				}
			}
		}
	}

	gantt._autoScheduleAfterDND = function(taskId, task){
		if (gantt.config.auto_scheduling && !this._autoscheduling_in_progress) {
			var newTask = this.getTask(taskId);
			if (gantt._notEqualTaskDates(task, newTask)){
				if(gantt.config.auto_scheduling_move_projects && movedTask == taskId){

					if(gantt.calculateDuration(task) != gantt.calculateDuration(newTask)){
						// task duration is used as lag when converting start_to_start and start_to_finish into finish to start links
						// recalculate these links if task duration has changed
						resetToStartLinksLags(taskId, relations);
					}


					gantt._autoSchedule(taskId, relations, gantt._finalizeAutoSchedulingChanges);
				}else{
					gantt.autoSchedule(newTask.id);
				}


			}
		}
		relations = null;
		movedTask = null;
		return true;
	};

	gantt._lightBoxChangesHandler = function (taskId, task) {
		if (gantt.config.auto_scheduling && !this._autoscheduling_in_progress) {
			var oldTask = this.getTask(taskId);
			if (gantt._notEqualTaskDates(task, oldTask)) {
				gantt._autoschedule_lightbox_id = taskId;
			}
		}
		return true;
	};
	gantt._lightBoxSaveHandler = function (taskId, task) {

		if (gantt.config.auto_scheduling && !this._autoscheduling_in_progress) {
			if (gantt._autoschedule_lightbox_id && gantt._autoschedule_lightbox_id == taskId) {
				gantt._autoschedule_lightbox_id = null;
				gantt.autoSchedule(task.id);
			}
		}
		return true;
	};




	gantt.attachEvent("onBeforeTaskChanged", function(id, mode, task){ return gantt._autoScheduleAfterDND(id, task); });
	gantt.attachEvent("onLightboxSave", gantt._lightBoxChangesHandler);
	gantt.attachEvent("onAfterTaskUpdate", gantt._lightBoxSaveHandler);


};


gantt.attachEvent("onGanttReady", function(){
	gantt._attachAutoSchedulingHandlers();
	// attach handlers only when initialized for the first time
	gantt._attachAutoSchedulingHandlers = function(){};
});

})();

/***/ }),
/* 4 */
/***/ (function(module, exports) {

var units = {
	"second": 1,
	"minute": 60,
	"hour": 60 * 60,
	"day": 60 * 60 * 24,
	"week": 60 * 60 * 24 * 7,
	"month": 60 * 60 * 24 * 30,
	"quarter": 60 * 60 * 24 * 30 * 3,
	"year": 60 * 60 * 24 * 365
};
function getSecondsInUnit(unit){
	return units[unit] || units.hour;
}

function forEach(arr, callback){
	if(arr.forEach){
		arr.forEach(callback);
	}else{
		var workArray = arr.slice();
		for(var i = 0; i < workArray.length; i++){
			callback(workArray[i], i);
		}
	}
}

function arrayMap(arr, callback){
	if(arr.map){
		return arr.map(callback);
	}else{
		var workArray = arr.slice();
		var resArray = [];

		for(var i = 0; i < workArray.length; i++){
			resArray.push(callback(workArray[i], i));
		}

		return resArray;
	}

}

module.exports = {
	getSecondsInUnit: getSecondsInUnit,
	forEach: forEach,
	arrayMap: arrayMap
};

/***/ })
/******/ ]);
});