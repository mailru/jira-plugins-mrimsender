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
/******/ 	return __webpack_require__(__webpack_require__.s = 5);
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
/* 2 */,
/* 3 */,
/* 4 */,
/* 5 */
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(6);


/***/ }),
/* 6 */
/***/ (function(module, exports, __webpack_require__) {

__webpack_require__(1)(gantt);

gantt.config.highlight_critical_path = false;
gantt._criticalPathHandler = function(){
	if(gantt.config.highlight_critical_path)
		gantt.render();
};
gantt.attachEvent("onAfterLinkAdd", gantt._criticalPathHandler);
gantt.attachEvent("onAfterLinkUpdate", gantt._criticalPathHandler);
gantt.attachEvent("onAfterLinkDelete", gantt._criticalPathHandler);
gantt.attachEvent("onAfterTaskAdd", gantt._criticalPathHandler);
gantt.attachEvent("onAfterTaskUpdate", gantt._criticalPathHandler);
gantt.attachEvent("onAfterTaskDelete", gantt._criticalPathHandler);


gantt._isCriticalTask = function(task, chain){
	if(!task || !task.id) return;
	var path = chain || {};

	if(this._isProjectEnd(task)){
		return true;
	}else{

		var clearCache = false;
		if(!gantt._isLinksCacheEnabled()) {
			gantt._startLinksCache();
			clearCache = true;
		}

		path[task.id] = true;
		var successors = this._getDependencies(task);
		for(var i=0; i < successors.length; i++){
			var next = this.getTask(successors[i].target);
			if(this._getSlack(task, next, successors[i]) <= 0 && (!path[next.id] && this._isCriticalTask(next, path)))
				return true;
		}

		if(clearCache)
			gantt._endLinksCache();
	}

	return false;
};

gantt.isCriticalTask = function (task) {
	gantt.assert(!!(task && task.id !== undefined), "Invalid argument for gantt.isCriticalTask");
	return this._isCriticalTask(task, {});
};

gantt.isCriticalLink = function (link) {
	return this.isCriticalTask(gantt.getTask(link.source));
};

gantt.getSlack = function(task1, task2){
	var relations = [];
	var common = {};
	for(var i=0; i < task1.$source.length; i++){
		common[task1.$source[i]] = true;
	}
	for(var i=0; i < task2.$target.length; i++){
		if(common[task2.$target[i]])
			relations.push(task2.$target[i]);
	}

	var slacks = [];
	for(var i=0; i < relations.length; i++){
		var link = this.getLink(relations[i]);
		slacks.push(this._getSlack(task1, task2, this._convertToFinishToStartLink(link.id, link, task1, task2, task1.parent, task2.parent)));
	}

	return Math.min.apply(Math, slacks);
};

gantt._getSlack = function (task, next_task, relation) {
	// relation - link expressed as finish-to-start (gantt._convertToFinishToStartLink)
	var types = this.config.types;

	var from = null;
	if(this.getTaskType(task.type) == types.milestone){
		from = task.start_date;
	}else{
		from = task.end_date;
	}

	var to = next_task.start_date;

	var duration = 0;
	if(+from > +to){
		duration = -this.calculateDuration({start_date: to, end_date: from, task: task});
	}else{
		duration = this.calculateDuration({start_date: from, end_date: to, task: task});
	}

	var lag = relation.lag;
	if(lag && lag*1 == lag){
		duration -= lag;
	}

	return duration;
};

gantt._getProjectEnd = function () {
	var tasks = gantt.getTaskByTime();
	tasks = tasks.sort(function (a, b) { return +a.end_date > +b.end_date ? 1 : -1; });
	return tasks.length ? tasks[tasks.length - 1].end_date : null;
};

gantt._isProjectEnd = function (task) {
	return !(this._hasDuration({start_date:task.end_date, end_date: this._getProjectEnd(), task:task}));
};

gantt._getSummaryPredecessors = function(task){
	var predecessors = [];

	// all constraints that are applied to summary parents must be applied to the task
	this.eachParent(function(parent){
		if(this.isSummaryTask(parent))
			predecessors = predecessors.concat(gantt._getDependencies(parent));
	}, task);

	return predecessors;
};


gantt._getDependencies = function(task){
	var successors = this._getSuccessors(task).concat(
		this._getSummaryPredecessors(task)
	);
	return successors;
};


/***/ })
/******/ ]);
});