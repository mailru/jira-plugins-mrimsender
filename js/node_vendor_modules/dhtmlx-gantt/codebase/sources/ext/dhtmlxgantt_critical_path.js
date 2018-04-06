/*!
 * @license
 * 
 * dhtmlxGantt v.5.1.2 Professional
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
		var role = getTarget ? "target" : "source";
		gantt.assert(task, "Link " + role + " not found. Task id=" + taskId + ", link id=" + link.id);
		return task;
	};
	gantt._get_link_target = function (link) {
		return gantt._get_linked_task(link, true);
	};

	gantt._get_link_source = function (link) {
		return gantt._get_linked_task(link, false);
	};


	gantt._formatLink = function (link) {
		var relations = [];
		var target = this._get_link_target(link);
		var source = this._get_link_source(link);

		if (!(source && target)) {
			return relations;
		}

		if ((gantt.isChildOf(source.id, target.id) && gantt.isSummaryTask(target)) || (gantt.isChildOf(target.id, source.id) && gantt.isSummaryTask(source))) {
			return relations;

		}


		// there are three kinds of connections at this point
		// task -> task - regular link
		// task -> project - transform it into set of regular links (task -> [each subtask]), use offset beetween subtask and project dates as lag, in order not to change mutual positions of subtasks inside a project
		// project -> task - transform it into ([each subtask] -> task) links
		// project -> project - transform it into ([each subtask of p1] -> [each subtask of p2]) links

		var from = this._getImplicitLinks(link, source, function (c) {
			return 0;
		});

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

		for (var i = 0; i < from.length; i++) {
			var fromTask = from[i];
			for (var j = 0; j < to.length; j++) {
				var toTask = to[j];

				var lag = fromTask.lag * 1 + toTask.lag * 1;

				var subtaskLink = {
					id: link.id,
					type: link.type,
					source: fromTask.task,
					target: toTask.task,
					lag: (link.lag * 1 || 0) + lag
				};

				relations.push(gantt._convertToFinishToStartLink(toTask.task, subtaskLink, source, target));
			}
		}

		return relations;
	};

gantt._isAutoSchedulable = function(task){
	return task.auto_scheduling !== false;
};

gantt._getImplicitLinks = function(link, parent, selectOffset){
	var relations = [];
	if(this.isSummaryTask(parent)){
		this.eachTask(function(c){
			if(!this.isSummaryTask(c))
				relations.push({task: c.id, lag: selectOffset(c)});
		}, parent.id);
	}else{
		relations.push({task:parent.id, lag: 0});
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
		var successors = [];
	var stop = false;
	var inheritedRelations = [];
	if(this.isTaskExists(task.id)){
		this.eachParent(function(parent){
			if(stop)
				return;

			if(this.isSummaryTask(parent)){
				if(!this._isAutoSchedulable(parent)){
					stop = true;
				}else{
					inheritedRelations.push.apply(inheritedRelations, this._getDirectDependencies(parent, selectSuccessors));
				}
			}
		}, task.id, this);

			for (var i = 0; i < inheritedRelations.length; i++) {

				var relProperty = selectSuccessors ? inheritedRelations[i].source : inheritedRelations[i].target;

				if (relProperty == task.id) {
					successors.push(inheritedRelations[i]);
				}
			}
		}

		return successors;
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


	gantt._getSuccessors = function (task) {
		return this._getDirectSuccessors(task).concat(this._getInheritedSuccessors(task));
	};

	gantt._getPredecessors = function (task) {
		return this._getDirectPredecessors(task).concat(this._getInheritedPredecessors(task));
	};


	gantt._convertToFinishToStartLink = function (id, link, sourceTask, targetTask) {
		// convert finish-to-finish, start-to-finish and start-to-start to finish-to-start link and provide some additional properties
		var res = {
			target: id,
			link: gantt.config.links.finish_to_start,
			id: link.id,
			lag: link.lag || 0,
			source: link.source,
			preferredStart: null
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
		path[task.id] = true;
		var successors = this._getDependencies(task);
		for(var i=0; i < successors.length; i++){
			var next = this.getTask(successors[i].target);
			if(this._getSlack(task, next, successors[i]) <= 0 && (!path[next.id] && this._isCriticalTask(next, path)))
				return true;
		}
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
		slacks.push(this._getSlack(task1, task2, this._convertToFinishToStartLink(link.id, link, task1, task2)));
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