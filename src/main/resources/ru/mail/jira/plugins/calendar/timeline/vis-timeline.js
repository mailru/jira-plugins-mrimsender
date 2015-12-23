(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.vis = f()}})(function(){var define,module,exports;return (function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
'use strict';

exports.DataSet = require('./lib/DataSet');
exports.Timeline = require('./lib/timeline/Timeline');
exports.TimeStep = require('./lib/timeline/TimeStep');

},{"./lib/DataSet":2,"./lib/timeline/TimeStep":17,"./lib/timeline/Timeline":18}],2:[function(require,module,exports){
'use strict';

var util = require('./util');
var Queue = require('./Queue');

/**
 * DataSet
 *
 * Usage:
 *     var dataSet = new DataSet({
 *         fieldId: '_id',
 *         type: {
 *             // ...
 *         }
 *     });
 *
 *     dataSet.add(item);
 *     dataSet.add(data);
 *     dataSet.update(item);
 *     dataSet.update(data);
 *     dataSet.remove(id);
 *     dataSet.remove(ids);
 *     var data = dataSet.get();
 *     var data = dataSet.get(id);
 *     var data = dataSet.get(ids);
 *     var data = dataSet.get(ids, options, data);
 *     dataSet.clear();
 *
 * A data set can:
 * - add/remove/update data
 * - gives triggers upon changes in the data
 * - can  import/export data in various data formats
 *
 * @param {Array} [data]    Optional array with initial data
 * @param {Object} [options]   Available options:
 *                             {String} fieldId Field name of the id in the
 *                                              items, 'id' by default.
 *                             {Object.<String, String} type
 *                                              A map with field names as key,
 *                                              and the field type as value.
 *                             {Object} queue   Queue changes to the DataSet,
 *                                              flush them all at once.
 *                                              Queue options:
 *                                              - {number} delay  Delay in ms, null by default
 *                                              - {number} max    Maximum number of entries in the queue, Infinity by default
 * @constructor DataSet
 */
// TODO: add a DataSet constructor DataSet(data, options)
function DataSet(data, options) {
  // correctly read optional arguments
  if (data && !Array.isArray(data)) {
    options = data;
    data = null;
  }

  this._options = options || {};
  this._data = {}; // map with data indexed by id
  this.length = 0; // number of items in the DataSet
  this._fieldId = this._options.fieldId || 'id'; // name of the field containing id
  this._type = {}; // internal field types (NOTE: this can differ from this._options.type)

  // all variants of a Date are internally stored as Date, so we can convert
  // from everything to everything (also from ISODate to Number for example)
  if (this._options.type) {
    var fields = Object.keys(this._options.type);
    for (var i = 0, len = fields.length; i < len; i++) {
      var field = fields[i];
      var value = this._options.type[field];
      if (value == 'Date' || value == 'ISODate' || value == 'ASPDate') {
        this._type[field] = 'Date';
      } else {
        this._type[field] = value;
      }
    }
  }

  // TODO: deprecated since version 1.1.1 (or 2.0.0?)
  if (this._options.convert) {
    throw new Error('Option "convert" is deprecated. Use "type" instead.');
  }

  this._subscribers = {}; // event subscribers

  // add initial data when provided
  if (data) {
    this.add(data);
  }

  this.setOptions(options);
}

/**
 * @param {Object} [options]   Available options:
 *                             {Object} queue   Queue changes to the DataSet,
 *                                              flush them all at once.
 *                                              Queue options:
 *                                              - {number} delay  Delay in ms, null by default
 *                                              - {number} max    Maximum number of entries in the queue, Infinity by default
 * @param options
 */
DataSet.prototype.setOptions = function (options) {
  if (options && options.queue !== undefined) {
    if (options.queue === false) {
      // delete queue if loaded
      if (this._queue) {
        this._queue.destroy();
        delete this._queue;
      }
    } else {
      // create queue and update its options
      if (!this._queue) {
        this._queue = Queue.extend(this, {
          replace: ['add', 'update', 'remove']
        });
      }

      if (typeof options.queue === 'object') {
        this._queue.setOptions(options.queue);
      }
    }
  }
};

/**
 * Subscribe to an event, add an event listener
 * @param {String} event        Event name. Available events: 'put', 'update',
 *                              'remove'
 * @param {function} callback   Callback method. Called with three parameters:
 *                                  {String} event
 *                                  {Object | null} params
 *                                  {String | Number} senderId
 */
DataSet.prototype.on = function (event, callback) {
  var subscribers = this._subscribers[event];
  if (!subscribers) {
    subscribers = [];
    this._subscribers[event] = subscribers;
  }

  subscribers.push({
    callback: callback
  });
};

// TODO: remove this deprecated function some day (replaced with `on` since version 0.5, deprecated since v4.0)
DataSet.prototype.subscribe = function () {
  throw new Error('DataSet.subscribe is deprecated. Use DataSet.on instead.');
};

/**
 * Unsubscribe from an event, remove an event listener
 * @param {String} event
 * @param {function} callback
 */
DataSet.prototype.off = function (event, callback) {
  var subscribers = this._subscribers[event];
  if (subscribers) {
    this._subscribers[event] = subscribers.filter(function (listener) {
      return listener.callback != callback;
    });
  }
};

// TODO: remove this deprecated function some day (replaced with `on` since version 0.5, deprecated since v4.0)
DataSet.prototype.unsubscribe = function () {
  throw new Error('DataSet.unsubscribe is deprecated. Use DataSet.off instead.');
};

/**
 * Trigger an event
 * @param {String} event
 * @param {Object | null} params
 * @param {String} [senderId]       Optional id of the sender.
 * @private
 */
DataSet.prototype._trigger = function (event, params, senderId) {
  if (event == '*') {
    throw new Error('Cannot trigger event *');
  }

  var subscribers = [];
  if (event in this._subscribers) {
    subscribers = subscribers.concat(this._subscribers[event]);
  }
  if ('*' in this._subscribers) {
    subscribers = subscribers.concat(this._subscribers['*']);
  }

  for (var i = 0, len = subscribers.length; i < len; i++) {
    var subscriber = subscribers[i];
    if (subscriber.callback) {
      subscriber.callback(event, params, senderId || null);
    }
  }
};

/**
 * Add data.
 * Adding an item will fail when there already is an item with the same id.
 * @param {Object | Array} data
 * @param {String} [senderId] Optional sender id
 * @return {Array} addedIds      Array with the ids of the added items
 */
DataSet.prototype.add = function (data, senderId) {
  var addedIds = [],
      id,
      me = this;

  if (Array.isArray(data)) {
    // Array
    for (var i = 0, len = data.length; i < len; i++) {
      id = me._addItem(data[i]);
      addedIds.push(id);
    }
  } else if (data instanceof Object) {
    // Single item
    id = me._addItem(data);
    addedIds.push(id);
  } else {
    throw new Error('Unknown dataType');
  }

  if (addedIds.length) {
    this._trigger('add', { items: addedIds }, senderId);
  }

  return addedIds;
};

/**
 * Update existing items. When an item does not exist, it will be created
 * @param {Object | Array} data
 * @param {String} [senderId] Optional sender id
 * @return {Array} updatedIds     The ids of the added or updated items
 */
DataSet.prototype.update = function (data, senderId) {
  var addedIds = [];
  var updatedIds = [];
  var oldData = [];
  var updatedData = [];
  var me = this;
  var fieldId = me._fieldId;

  var addOrUpdate = function addOrUpdate(item) {
    var id = item[fieldId];
    if (me._data[id]) {
      var oldItem = util.extend({}, me._data[id]);
      // update item
      id = me._updateItem(item);
      updatedIds.push(id);
      updatedData.push(item);
      oldData.push(oldItem);
    } else {
      // add new item
      id = me._addItem(item);
      addedIds.push(id);
    }
  };

  if (Array.isArray(data)) {
    // Array
    for (var i = 0, len = data.length; i < len; i++) {
      if (data[i] instanceof Object) {
        addOrUpdate(data[i]);
      } else {
        console.warn("Ignoring input item, which is not an object at index" + i);
      }
    }
  } else if (data instanceof Object) {
    // Single item
    addOrUpdate(data);
  } else {
    throw new Error('Unknown dataType');
  }

  if (addedIds.length) {
    this._trigger('add', { items: addedIds }, senderId);
  }
  if (updatedIds.length) {
    var props = { items: updatedIds, oldData: oldData, data: updatedData };
    // TODO: remove deprecated property 'data' some day
    //Object.defineProperty(props, 'data', {
    //  'get': (function() {
    //    console.warn('Property data is deprecated. Use DataSet.get(ids) to retrieve the new data, use the oldData property on this object to get the old data');
    //    return updatedData;
    //  }).bind(this)
    //});
    this._trigger('update', props, senderId);
  }

  return addedIds.concat(updatedIds);
};

/**
 * Get a data item or multiple items.
 *
 * Usage:
 *
 *     get()
 *     get(options: Object)
 *
 *     get(id: Number | String)
 *     get(id: Number | String, options: Object)
 *
 *     get(ids: Number[] | String[])
 *     get(ids: Number[] | String[], options: Object)
 *
 * Where:
 *
 * {Number | String} id         The id of an item
 * {Number[] | String{}} ids    An array with ids of items
 * {Object} options             An Object with options. Available options:
 * {String} [returnType]        Type of data to be returned.
 *                              Can be 'Array' (default) or 'Object'.
 * {Object.<String, String>} [type]
 * {String[]} [fields]          field names to be returned
 * {function} [filter]          filter items
 * {String | function} [order]  Order the items by a field name or custom sort function.
 * @throws Error
 */
DataSet.prototype.get = function (args) {
  var me = this;

  // parse the arguments
  var id, ids, options;
  var firstType = util.getType(arguments[0]);
  if (firstType == 'String' || firstType == 'Number') {
    // get(id [, options])
    id = arguments[0];
    options = arguments[1];
  } else if (firstType == 'Array') {
    // get(ids [, options])
    ids = arguments[0];
    options = arguments[1];
  } else {
    // get([, options])
    options = arguments[0];
  }

  // determine the return type
  var returnType;
  if (options && options.returnType) {
    var allowedValues = ['Array', 'Object'];
    returnType = allowedValues.indexOf(options.returnType) == -1 ? 'Array' : options.returnType;
  } else {
    returnType = 'Array';
  }

  // build options
  var type = options && options.type || this._options.type;
  var filter = options && options.filter;
  var items = [],
      item,
      itemIds,
      itemId,
      i,
      len;

  // convert items
  if (id != undefined) {
    // return a single item
    item = me._getItem(id, type);
    if (item && filter && !filter(item)) {
      item = null;
    }
  } else if (ids != undefined) {
    // return a subset of items
    for (i = 0, len = ids.length; i < len; i++) {
      item = me._getItem(ids[i], type);
      if (!filter || filter(item)) {
        items.push(item);
      }
    }
  } else {
    // return all items
    itemIds = Object.keys(this._data);
    for (i = 0, len = itemIds.length; i < len; i++) {
      itemId = itemIds[i];
      item = me._getItem(itemId, type);
      if (!filter || filter(item)) {
        items.push(item);
      }
    }
  }

  // order the results
  if (options && options.order && id == undefined) {
    this._sort(items, options.order);
  }

  // filter fields of the items
  if (options && options.fields) {
    var fields = options.fields;
    if (id != undefined) {
      item = this._filterFields(item, fields);
    } else {
      for (i = 0, len = items.length; i < len; i++) {
        items[i] = this._filterFields(items[i], fields);
      }
    }
  }

  // return the results
  if (returnType == 'Object') {
    var result = {},
        resultant;
    for (i = 0, len = items.length; i < len; i++) {
      resultant = items[i];
      result[resultant.id] = resultant;
    }
    return result;
  } else {
    if (id != undefined) {
      // a single item
      return item;
    } else {
      // just return our array
      return items;
    }
  }
};

/**
 * Get ids of all items or from a filtered set of items.
 * @param {Object} [options]    An Object with options. Available options:
 *                              {function} [filter] filter items
 *                              {String | function} [order] Order the items by
 *                                  a field name or custom sort function.
 * @return {Array} ids
 */
DataSet.prototype.getIds = function (options) {
  var data = this._data,
      filter = options && options.filter,
      order = options && options.order,
      type = options && options.type || this._options.type,
      itemIds = Object.keys(data),
      i,
      len,
      id,
      item,
      items,
      ids = [];

  if (filter) {
    // get filtered items
    if (order) {
      // create ordered list
      items = [];
      for (i = 0, len = itemIds.length; i < len; i++) {
        id = itemIds[i];
        item = this._getItem(id, type);
        if (filter(item)) {
          items.push(item);
        }
      }

      this._sort(items, order);

      for (i = 0, len = items.length; i < len; i++) {
        ids.push(items[i][this._fieldId]);
      }
    } else {
      // create unordered list
      for (i = 0, len = itemIds.length; i < len; i++) {
        id = itemIds[i];
        item = this._getItem(id, type);
        if (filter(item)) {
          ids.push(item[this._fieldId]);
        }
      }
    }
  } else {
    // get all items
    if (order) {
      // create an ordered list
      items = [];
      for (i = 0, len = itemIds.length; i < len; i++) {
        id = itemIds[i];
        items.push(data[id]);
      }

      this._sort(items, order);

      for (i = 0, len = items.length; i < len; i++) {
        ids.push(items[i][this._fieldId]);
      }
    } else {
      // create unordered list
      for (i = 0, len = itemIds.length; i < len; i++) {
        id = itemIds[i];
        item = data[id];
        ids.push(item[this._fieldId]);
      }
    }
  }

  return ids;
};

/**
 * Returns the DataSet itself. Is overwritten for example by the DataView,
 * which returns the DataSet it is connected to instead.
 */
DataSet.prototype.getDataSet = function () {
  return this;
};

/**
 * Execute a callback function for every item in the dataset.
 * @param {function} callback
 * @param {Object} [options]    Available options:
 *                              {Object.<String, String>} [type]
 *                              {String[]} [fields] filter fields
 *                              {function} [filter] filter items
 *                              {String | function} [order] Order the items by
 *                                  a field name or custom sort function.
 */
DataSet.prototype.forEach = function (callback, options) {
  var filter = options && options.filter,
      type = options && options.type || this._options.type,
      data = this._data,
      itemIds = Object.keys(data),
      i,
      len,
      item,
      id;

  if (options && options.order) {
    // execute forEach on ordered list
    var items = this.get(options);

    for (i = 0, len = items.length; i < len; i++) {
      item = items[i];
      id = item[this._fieldId];
      callback(item, id);
    }
  } else {
    // unordered
    for (i = 0, len = itemIds.length; i < len; i++) {
      id = itemIds[i];
      item = this._getItem(id, type);
      if (!filter || filter(item)) {
        callback(item, id);
      }
    }
  }
};

/**
 * Map every item in the dataset.
 * @param {function} callback
 * @param {Object} [options]    Available options:
 *                              {Object.<String, String>} [type]
 *                              {String[]} [fields] filter fields
 *                              {function} [filter] filter items
 *                              {String | function} [order] Order the items by
 *                                  a field name or custom sort function.
 * @return {Object[]} mappedItems
 */
DataSet.prototype.map = function (callback, options) {
  var filter = options && options.filter,
      type = options && options.type || this._options.type,
      mappedItems = [],
      data = this._data,
      itemIds = Object.keys(data),
      i,
      len,
      id,
      item;

  // convert and filter items
  for (i = 0, len = itemIds.length; i < len; i++) {
    id = itemIds[i];
    item = this._getItem(id, type);
    if (!filter || filter(item)) {
      mappedItems.push(callback(item, id));
    }
  }

  // order items
  if (options && options.order) {
    this._sort(mappedItems, options.order);
  }

  return mappedItems;
};

/**
 * Filter the fields of an item
 * @param {Object | null} item
 * @param {String[]} fields     Field names
 * @return {Object | null} filteredItem or null if no item is provided
 * @private
 */
DataSet.prototype._filterFields = function (item, fields) {
  if (!item) {
    // item is null
    return item;
  }

  var filteredItem = {},
      itemFields = Object.keys(item),
      len = itemFields.length,
      i,
      field;

  if (Array.isArray(fields)) {
    for (i = 0; i < len; i++) {
      field = itemFields[i];
      if (fields.indexOf(field) != -1) {
        filteredItem[field] = item[field];
      }
    }
  } else {
    for (i = 0; i < len; i++) {
      field = itemFields[i];
      if (fields.hasOwnProperty(field)) {
        filteredItem[fields[field]] = item[field];
      }
    }
  }

  return filteredItem;
};

/**
 * Sort the provided array with items
 * @param {Object[]} items
 * @param {String | function} order      A field name or custom sort function.
 * @private
 */
DataSet.prototype._sort = function (items, order) {
  if (util.isString(order)) {
    // order by provided field name
    var name = order; // field name
    items.sort(function (a, b) {
      var av = a[name];
      var bv = b[name];
      return av > bv ? 1 : av < bv ? -1 : 0;
    });
  } else if (typeof order === 'function') {
    // order by sort function
    items.sort(order);
  }
  // TODO: extend order by an Object {field:String, direction:String}
  //       where direction can be 'asc' or 'desc'
  else {
      throw new TypeError('Order must be a function or a string');
    }
};

/**
 * Remove an object by pointer or by id
 * @param {String | Number | Object | Array} id Object or id, or an array with
 *                                              objects or ids to be removed
 * @param {String} [senderId] Optional sender id
 * @return {Array} removedIds
 */
DataSet.prototype.remove = function (id, senderId) {
  var removedIds = [],
      i,
      len,
      removedId;

  if (Array.isArray(id)) {
    for (i = 0, len = id.length; i < len; i++) {
      removedId = this._remove(id[i]);
      if (removedId != null) {
        removedIds.push(removedId);
      }
    }
  } else {
    removedId = this._remove(id);
    if (removedId != null) {
      removedIds.push(removedId);
    }
  }

  if (removedIds.length) {
    this._trigger('remove', { items: removedIds }, senderId);
  }

  return removedIds;
};

/**
 * Remove an item by its id
 * @param {Number | String | Object} id   id or item
 * @returns {Number | String | null} id
 * @private
 */
DataSet.prototype._remove = function (id) {
  if (util.isNumber(id) || util.isString(id)) {
    if (this._data[id]) {
      delete this._data[id];
      this.length--;
      return id;
    }
  } else if (id instanceof Object) {
    var itemId = id[this._fieldId];
    if (itemId !== undefined && this._data[itemId]) {
      delete this._data[itemId];
      this.length--;
      return itemId;
    }
  }
  return null;
};

/**
 * Clear the data
 * @param {String} [senderId] Optional sender id
 * @return {Array} removedIds    The ids of all removed items
 */
DataSet.prototype.clear = function (senderId) {
  var ids = Object.keys(this._data);

  this._data = {};
  this.length = 0;

  this._trigger('remove', { items: ids }, senderId);

  return ids;
};

/**
 * Find the item with maximum value of a specified field
 * @param {String} field
 * @return {Object | null} item  Item containing max value, or null if no items
 */
DataSet.prototype.max = function (field) {
  var data = this._data,
      itemIds = Object.keys(data),
      max = null,
      maxField = null,
      i,
      len;

  for (i = 0, len = itemIds.length; i < len; i++) {
    var id = itemIds[i];
    var item = data[id];
    var itemField = item[field];
    if (itemField != null && (!max || itemField > maxField)) {
      max = item;
      maxField = itemField;
    }
  }

  return max;
};

/**
 * Find the item with minimum value of a specified field
 * @param {String} field
 * @return {Object | null} item  Item containing max value, or null if no items
 */
DataSet.prototype.min = function (field) {
  var data = this._data,
      itemIds = Object.keys(data),
      min = null,
      minField = null,
      i,
      len;

  for (i = 0, len = itemIds.length; i < len; i++) {
    var id = itemIds[i];
    var item = data[id];
    var itemField = item[field];
    if (itemField != null && (!min || itemField < minField)) {
      min = item;
      minField = itemField;
    }
  }

  return min;
};

/**
 * Find all distinct values of a specified field
 * @param {String} field
 * @return {Array} values  Array containing all distinct values. If data items
 *                         do not contain the specified field are ignored.
 *                         The returned array is unordered.
 */
DataSet.prototype.distinct = function (field) {
  var data = this._data;
  var itemIds = Object.keys(data);
  var values = [];
  var fieldType = this._options.type && this._options.type[field] || null;
  var count = 0;
  var i, j, len;

  for (i = 0, len = itemIds.length; i < len; i++) {
    var id = itemIds[i];
    var item = data[id];
    var value = item[field];
    var exists = false;
    for (j = 0; j < count; j++) {
      if (values[j] == value) {
        exists = true;
        break;
      }
    }
    if (!exists && value !== undefined) {
      values[count] = value;
      count++;
    }
  }

  if (fieldType) {
    for (i = 0, len = values.length; i < len; i++) {
      values[i] = util.convert(values[i], fieldType);
    }
  }

  return values;
};

/**
 * Add a single item. Will fail when an item with the same id already exists.
 * @param {Object} item
 * @return {String} id
 * @private
 */
DataSet.prototype._addItem = function (item) {
  var id = item[this._fieldId];

  if (id != undefined) {
    // check whether this id is already taken
    if (this._data[id]) {
      // item already exists
      throw new Error('Cannot add item: item with id ' + id + ' already exists');
    }
  } else {
    // generate an id
    id = util.randomUUID();
    item[this._fieldId] = id;
  }

  var d = {},
      fields = Object.keys(item),
      i,
      len;
  for (i = 0, len = fields.length; i < len; i++) {
    var field = fields[i];
    var fieldType = this._type[field]; // type may be undefined
    d[field] = util.convert(item[field], fieldType);
  }
  this._data[id] = d;
  this.length++;

  return id;
};

/**
 * Get an item. Fields can be converted to a specific type
 * @param {String} id
 * @param {Object.<String, String>} [types]  field types to convert
 * @return {Object | null} item
 * @private
 */
DataSet.prototype._getItem = function (id, types) {
  var field, value, i, len;

  // get the item from the dataset
  var raw = this._data[id];
  if (!raw) {
    return null;
  }

  // convert the items field types
  var converted = {},
      fields = Object.keys(raw);

  if (types) {
    for (i = 0, len = fields.length; i < len; i++) {
      field = fields[i];
      value = raw[field];
      converted[field] = util.convert(value, types[field]);
    }
  } else {
    // no field types specified, no converting needed
    for (i = 0, len = fields.length; i < len; i++) {
      field = fields[i];
      value = raw[field];
      converted[field] = value;
    }
  }
  return converted;
};

/**
 * Update a single item: merge with existing item.
 * Will fail when the item has no id, or when there does not exist an item
 * with the same id.
 * @param {Object} item
 * @return {String} id
 * @private
 */
DataSet.prototype._updateItem = function (item) {
  var id = item[this._fieldId];
  if (id == undefined) {
    throw new Error('Cannot update item: item has no id (item: ' + JSON.stringify(item) + ')');
  }
  var d = this._data[id];
  if (!d) {
    // item doesn't exist
    throw new Error('Cannot update item: no item with id ' + id + ' found');
  }

  // merge with current item
  var fields = Object.keys(item);
  for (var i = 0, len = fields.length; i < len; i++) {
    var field = fields[i];
    var fieldType = this._type[field]; // type may be undefined
    d[field] = util.convert(item[field], fieldType);
  }

  return id;
};

module.exports = DataSet;

},{"./Queue":4,"./util":33}],3:[function(require,module,exports){
'use strict';

var util = require('./util');
var DataSet = require('./DataSet');

/**
 * DataView
 *
 * a dataview offers a filtered view on a dataset or an other dataview.
 *
 * @param {DataSet | DataView} data
 * @param {Object} [options]   Available options: see method get
 *
 * @constructor DataView
 */
function DataView(data, options) {
  this._data = null;
  this._ids = {}; // ids of the items currently in memory (just contains a boolean true)
  this.length = 0; // number of items in the DataView
  this._options = options || {};
  this._fieldId = 'id'; // name of the field containing id
  this._subscribers = {}; // event subscribers

  var me = this;
  this.listener = function () {
    me._onEvent.apply(me, arguments);
  };

  this.setData(data);
}

// TODO: implement a function .config() to dynamically update things like configured filter
// and trigger changes accordingly

/**
 * Set a data source for the view
 * @param {DataSet | DataView} data
 */
DataView.prototype.setData = function (data) {
  var ids, id, i, len;

  if (this._data) {
    // unsubscribe from current dataset
    if (this._data.off) {
      this._data.off('*', this.listener);
    }

    // trigger a remove of all items in memory
    ids = Object.keys(this._ids);
    this._ids = {};
    this.length = 0;
    this._trigger('remove', { items: ids });
  }

  this._data = data;

  if (this._data) {
    // update fieldId
    this._fieldId = this._options.fieldId || this._data && this._data.options && this._data.options.fieldId || 'id';

    // trigger an add of all added items
    ids = this._data.getIds({ filter: this._options && this._options.filter });
    for (i = 0, len = ids.length; i < len; i++) {
      id = ids[i];
      this._ids[id] = true;
    }
    this.length = ids.length;
    this._trigger('add', { items: ids });

    // subscribe to new dataset
    if (this._data.on) {
      this._data.on('*', this.listener);
    }
  }
};

/**
 * Refresh the DataView. Useful when the DataView has a filter function
 * containing a variable parameter.
 */
DataView.prototype.refresh = function () {
  var id, i, len;
  var ids = this._data.getIds({ filter: this._options && this._options.filter });
  var oldIds = Object.keys(this._ids);
  var newIds = {};
  var added = [];
  var removed = [];

  // check for additions
  for (i = 0, len = ids.length; i < len; i++) {
    id = ids[i];
    newIds[id] = true;
    if (!this._ids[id]) {
      added.push(id);
      this._ids[id] = true;
    }
  }

  // check for removals
  for (i = 0, len = oldIds.length; i < len; i++) {
    id = oldIds[i];
    if (!newIds[id]) {
      removed.push(id);
      delete this._ids[id];
    }
  }

  this.length += added.length - removed.length;

  // trigger events
  if (added.length) {
    this._trigger('add', { items: added });
  }
  if (removed.length) {
    this._trigger('remove', { items: removed });
  }
};

/**
 * Get data from the data view
 *
 * Usage:
 *
 *     get()
 *     get(options: Object)
 *     get(options: Object, data: Array | DataTable)
 *
 *     get(id: Number)
 *     get(id: Number, options: Object)
 *     get(id: Number, options: Object, data: Array | DataTable)
 *
 *     get(ids: Number[])
 *     get(ids: Number[], options: Object)
 *     get(ids: Number[], options: Object, data: Array | DataTable)
 *
 * Where:
 *
 * {Number | String} id         The id of an item
 * {Number[] | String{}} ids    An array with ids of items
 * {Object} options             An Object with options. Available options:
 *                              {String} [type] Type of data to be returned. Can
 *                                              be 'DataTable' or 'Array' (default)
 *                              {Object.<String, String>} [convert]
 *                              {String[]} [fields] field names to be returned
 *                              {function} [filter] filter items
 *                              {String | function} [order] Order the items by
 *                                  a field name or custom sort function.
 * {Array | DataTable} [data]   If provided, items will be appended to this
 *                              array or table. Required in case of Google
 *                              DataTable.
 * @param args
 */
DataView.prototype.get = function (args) {
  var me = this;

  // parse the arguments
  var ids, options, data;
  var firstType = util.getType(arguments[0]);
  if (firstType == 'String' || firstType == 'Number' || firstType == 'Array') {
    // get(id(s) [, options] [, data])
    ids = arguments[0]; // can be a single id or an array with ids
    options = arguments[1];
    data = arguments[2];
  } else {
    // get([, options] [, data])
    options = arguments[0];
    data = arguments[1];
  }

  // extend the options with the default options and provided options
  var viewOptions = util.extend({}, this._options, options);

  // create a combined filter method when needed
  if (this._options.filter && options && options.filter) {
    viewOptions.filter = function (item) {
      return me._options.filter(item) && options.filter(item);
    };
  }

  // build up the call to the linked data set
  var getArguments = [];
  if (ids != undefined) {
    getArguments.push(ids);
  }
  getArguments.push(viewOptions);
  getArguments.push(data);

  return this._data && this._data.get.apply(this._data, getArguments);
};

/**
 * Get ids of all items or from a filtered set of items.
 * @param {Object} [options]    An Object with options. Available options:
 *                              {function} [filter] filter items
 *                              {String | function} [order] Order the items by
 *                                  a field name or custom sort function.
 * @return {Array} ids
 */
DataView.prototype.getIds = function (options) {
  var ids;

  if (this._data) {
    var defaultFilter = this._options.filter;
    var filter;

    if (options && options.filter) {
      if (defaultFilter) {
        filter = function (item) {
          return defaultFilter(item) && options.filter(item);
        };
      } else {
        filter = options.filter;
      }
    } else {
      filter = defaultFilter;
    }

    ids = this._data.getIds({
      filter: filter,
      order: options && options.order
    });
  } else {
    ids = [];
  }

  return ids;
};

/**
 * Map every item in the dataset.
 * @param {function} callback
 * @param {Object} [options]    Available options:
 *                              {Object.<String, String>} [type]
 *                              {String[]} [fields] filter fields
 *                              {function} [filter] filter items
 *                              {String | function} [order] Order the items by
 *                                  a field name or custom sort function.
 * @return {Object[]} mappedItems
 */
DataView.prototype.map = function (callback, options) {
  var mappedItems = [];
  if (this._data) {
    var defaultFilter = this._options.filter;
    var filter;

    if (options && options.filter) {
      if (defaultFilter) {
        filter = function (item) {
          return defaultFilter(item) && options.filter(item);
        };
      } else {
        filter = options.filter;
      }
    } else {
      filter = defaultFilter;
    }

    mappedItems = this._data.map(callback, {
      filter: filter,
      order: options && options.order
    });
  } else {
    mappedItems = [];
  }

  return mappedItems;
};

/**
 * Get the DataSet to which this DataView is connected. In case there is a chain
 * of multiple DataViews, the root DataSet of this chain is returned.
 * @return {DataSet} dataSet
 */
DataView.prototype.getDataSet = function () {
  var dataSet = this;
  while (dataSet instanceof DataView) {
    dataSet = dataSet._data;
  }
  return dataSet || null;
};

/**
 * Event listener. Will propagate all events from the connected data set to
 * the subscribers of the DataView, but will filter the items and only trigger
 * when there are changes in the filtered data set.
 * @param {String} event
 * @param {Object | null} params
 * @param {String} senderId
 * @private
 */
DataView.prototype._onEvent = function (event, params, senderId) {
  var i, len, id, item;
  var ids = params && params.items;
  var data = this._data;
  var updatedData = [];
  var added = [];
  var updated = [];
  var removed = [];

  if (ids && data) {
    switch (event) {
      case 'add':
        // filter the ids of the added items
        for (i = 0, len = ids.length; i < len; i++) {
          id = ids[i];
          item = this.get(id);
          if (item) {
            this._ids[id] = true;
            added.push(id);
          }
        }

        break;

      case 'update':
        // determine the event from the views viewpoint: an updated
        // item can be added, updated, or removed from this view.
        for (i = 0, len = ids.length; i < len; i++) {
          id = ids[i];
          item = this.get(id);

          if (item) {
            if (this._ids[id]) {
              updated.push(id);
              updatedData.push(params.data[i]);
            } else {
              this._ids[id] = true;
              added.push(id);
            }
          } else {
            if (this._ids[id]) {
              delete this._ids[id];
              removed.push(id);
            } else {
              // nothing interesting for me :-(
            }
          }
        }

        break;

      case 'remove':
        // filter the ids of the removed items
        for (i = 0, len = ids.length; i < len; i++) {
          id = ids[i];
          if (this._ids[id]) {
            delete this._ids[id];
            removed.push(id);
          }
        }

        break;
    }

    this.length += added.length - removed.length;

    if (added.length) {
      this._trigger('add', { items: added }, senderId);
    }
    if (updated.length) {
      this._trigger('update', { items: updated, data: updatedData }, senderId);
    }
    if (removed.length) {
      this._trigger('remove', { items: removed }, senderId);
    }
  }
};

// copy subscription functionality from DataSet
DataView.prototype.on = DataSet.prototype.on;
DataView.prototype.off = DataSet.prototype.off;
DataView.prototype._trigger = DataSet.prototype._trigger;

// TODO: make these functions deprecated (replaced with `on` and `off` since version 0.5)
DataView.prototype.subscribe = DataView.prototype.on;
DataView.prototype.unsubscribe = DataView.prototype.off;

module.exports = DataView;

},{"./DataSet":2,"./util":33}],4:[function(require,module,exports){
/**
 * A queue
 * @param {Object} options
 *            Available options:
 *            - delay: number    When provided, the queue will be flushed
 *                               automatically after an inactivity of this delay
 *                               in milliseconds.
 *                               Default value is null.
 *            - max: number      When the queue exceeds the given maximum number
 *                               of entries, the queue is flushed automatically.
 *                               Default value of max is Infinity.
 * @constructor
 */
'use strict';

function Queue(options) {
  // options
  this.delay = null;
  this.max = Infinity;

  // properties
  this._queue = [];
  this._timeout = null;
  this._extended = null;

  this.setOptions(options);
}

/**
 * Update the configuration of the queue
 * @param {Object} options
 *            Available options:
 *            - delay: number    When provided, the queue will be flushed
 *                               automatically after an inactivity of this delay
 *                               in milliseconds.
 *                               Default value is null.
 *            - max: number      When the queue exceeds the given maximum number
 *                               of entries, the queue is flushed automatically.
 *                               Default value of max is Infinity.
 * @param options
 */
Queue.prototype.setOptions = function (options) {
  if (options && typeof options.delay !== 'undefined') {
    this.delay = options.delay;
  }
  if (options && typeof options.max !== 'undefined') {
    this.max = options.max;
  }

  this._flushIfNeeded();
};

/**
 * Extend an object with queuing functionality.
 * The object will be extended with a function flush, and the methods provided
 * in options.replace will be replaced with queued ones.
 * @param {Object} object
 * @param {Object} options
 *            Available options:
 *            - replace: Array.<string>
 *                               A list with method names of the methods
 *                               on the object to be replaced with queued ones.
 *            - delay: number    When provided, the queue will be flushed
 *                               automatically after an inactivity of this delay
 *                               in milliseconds.
 *                               Default value is null.
 *            - max: number      When the queue exceeds the given maximum number
 *                               of entries, the queue is flushed automatically.
 *                               Default value of max is Infinity.
 * @return {Queue} Returns the created queue
 */
Queue.extend = function (object, options) {
  var queue = new Queue(options);

  if (object.flush !== undefined) {
    throw new Error('Target object already has a property flush');
  }
  object.flush = function () {
    queue.flush();
  };

  var methods = [{
    name: 'flush',
    original: undefined
  }];

  if (options && options.replace) {
    for (var i = 0; i < options.replace.length; i++) {
      var name = options.replace[i];
      methods.push({
        name: name,
        original: object[name]
      });
      queue.replace(object, name);
    }
  }

  queue._extended = {
    object: object,
    methods: methods
  };

  return queue;
};

/**
 * Destroy the queue. The queue will first flush all queued actions, and in
 * case it has extended an object, will restore the original object.
 */
Queue.prototype.destroy = function () {
  this.flush();

  if (this._extended) {
    var object = this._extended.object;
    var methods = this._extended.methods;
    for (var i = 0; i < methods.length; i++) {
      var method = methods[i];
      if (method.original) {
        object[method.name] = method.original;
      } else {
        delete object[method.name];
      }
    }
    this._extended = null;
  }
};

/**
 * Replace a method on an object with a queued version
 * @param {Object} object   Object having the method
 * @param {string} method   The method name
 */
Queue.prototype.replace = function (object, method) {
  var me = this;
  var original = object[method];
  if (!original) {
    throw new Error('Method ' + method + ' undefined');
  }

  object[method] = function () {
    // create an Array with the arguments
    var args = [];
    for (var i = 0; i < arguments.length; i++) {
      args[i] = arguments[i];
    }

    // add this call to the queue
    me.queue({
      args: args,
      fn: original,
      context: this
    });
  };
};

/**
 * Queue a call
 * @param {function | {fn: function, args: Array} | {fn: function, args: Array, context: Object}} entry
 */
Queue.prototype.queue = function (entry) {
  if (typeof entry === 'function') {
    this._queue.push({ fn: entry });
  } else {
    this._queue.push(entry);
  }

  this._flushIfNeeded();
};

/**
 * Check whether the queue needs to be flushed
 * @private
 */
Queue.prototype._flushIfNeeded = function () {
  // flush when the maximum is exceeded.
  if (this._queue.length > this.max) {
    this.flush();
  }

  // flush after a period of inactivity when a delay is configured
  clearTimeout(this._timeout);
  if (this.queue.length > 0 && typeof this.delay === 'number') {
    var me = this;
    this._timeout = setTimeout(function () {
      me.flush();
    }, this.delay);
  }
};

/**
 * Flush all queued calls
 */
Queue.prototype.flush = function () {
  while (this._queue.length > 0) {
    var entry = this._queue.shift();
    entry.fn.apply(entry.context || entry.fn, entry.args || []);
  }
};

module.exports = Queue;

},{}],5:[function(require,module,exports){
'use strict';

var Hammer = require('./module/hammer');

/**
 * Register a touch event, taking place before a gesture
 * @param {Hammer} hammer       A hammer instance
 * @param {function} callback   Callback, called as callback(event)
 */
exports.onTouch = function (hammer, callback) {
  callback.inputHandler = function (event) {
    if (event.isFirst && !isTouching) {
      callback(event);

      isTouching = true;
      setTimeout(function () {
        isTouching = false;
      }, 0);
    }
  };

  hammer.on('hammer.input', callback.inputHandler);
};

// isTouching is true while a touch action is being emitted
// this is a hack to prevent `touch` from being fired twice
var isTouching = false;

/**
 * Register a release event, taking place after a gesture
 * @param {Hammer} hammer       A hammer instance
 * @param {function} callback   Callback, called as callback(event)
 */
exports.onRelease = function (hammer, callback) {
  callback.inputHandler = function (event) {
    if (event.isFinal && !isReleasing) {
      callback(event);

      isReleasing = true;
      setTimeout(function () {
        isReleasing = false;
      }, 0);
    }
  };

  return hammer.on('hammer.input', callback.inputHandler);
};

// isReleasing is true while a release action is being emitted
// this is a hack to prevent `release` from being fired twice
var isReleasing = false;

/**
 * Unregister a touch event, taking place before a gesture
 * @param {Hammer} hammer       A hammer instance
 * @param {function} callback   Callback, called as callback(event)
 */
exports.offTouch = function (hammer, callback) {
  hammer.off('hammer.input', callback.inputHandler);
};

/**
 * Unregister a release event, taking place before a gesture
 * @param {Hammer} hammer       A hammer instance
 * @param {function} callback   Callback, called as callback(event)
 */
exports.offRelease = exports.offTouch;

},{"./module/hammer":6}],6:[function(require,module,exports){
// Only load hammer.js when in a browser environment
// (loading hammer.js in a node.js environment gives errors)
'use strict';

if (typeof window !== 'undefined') {
  var propagating = require('propagating-hammerjs');
  var Hammer = window['Hammer'] || require('hammerjs');
  module.exports = propagating(Hammer, {
    preventDefault: 'mouse'
  });
} else {
  module.exports = function () {
    throw Error('hammer.js is only available in a browser, not in node.js.');
  };
}

},{"hammerjs":35,"propagating-hammerjs":37}],7:[function(require,module,exports){
// first check if moment.js is already loaded in the browser window, if so,
// use this instance. Else, load via commonjs.
'use strict';

module.exports = typeof window !== 'undefined' && window['moment'] || require('moment');

},{"moment":"moment"}],8:[function(require,module,exports){
(function (global){
'use strict';

var _rng;

var globalVar = typeof window !== 'undefined' ? window : typeof global !== 'undefined' ? global : null;

if (globalVar && globalVar.crypto && crypto.getRandomValues) {
  // WHATWG crypto-based RNG - http://wiki.whatwg.org/wiki/Crypto
  // Moderately fast, high quality
  var _rnds8 = new Uint8Array(16);
  _rng = function whatwgRNG() {
    crypto.getRandomValues(_rnds8);
    return _rnds8;
  };
}

if (!_rng) {
  // Math.random()-based (RNG)
  //
  // If all else fails, use Math.random().  It's fast, but is of unspecified
  // quality.
  var _rnds = new Array(16);
  _rng = function () {
    for (var i = 0, r; i < 16; i++) {
      if ((i & 0x03) === 0) r = Math.random() * 0x100000000;
      _rnds[i] = r >>> ((i & 0x03) << 3) & 0xff;
    }

    return _rnds;
  };
}

//     uuid.js
//
//     Copyright (c) 2010-2012 Robert Kieffer
//     MIT License - http://opensource.org/licenses/mit-license.php

// Unique ID creation requires a high quality random # generator.  We feature
// detect to determine the best RNG source, normalizing to a function that
// returns 128-bits of randomness, since that's what's usually required

//var _rng = require('./rng');

// Maps for number <-> hex string conversion
var _byteToHex = [];
var _hexToByte = {};
for (var i = 0; i < 256; i++) {
  _byteToHex[i] = (i + 0x100).toString(16).substr(1);
  _hexToByte[_byteToHex[i]] = i;
}

// **`parse()` - Parse a UUID into it's component bytes**
function parse(s, buf, offset) {
  var i = buf && offset || 0,
      ii = 0;

  buf = buf || [];
  s.toLowerCase().replace(/[0-9a-f]{2}/g, function (oct) {
    if (ii < 16) {
      // Don't overflow!
      buf[i + ii++] = _hexToByte[oct];
    }
  });

  // Zero out remaining bytes if string was short
  while (ii < 16) {
    buf[i + ii++] = 0;
  }

  return buf;
}

// **`unparse()` - Convert UUID byte array (ala parse()) into a string**
function unparse(buf, offset) {
  var i = offset || 0,
      bth = _byteToHex;
  return bth[buf[i++]] + bth[buf[i++]] + bth[buf[i++]] + bth[buf[i++]] + '-' + bth[buf[i++]] + bth[buf[i++]] + '-' + bth[buf[i++]] + bth[buf[i++]] + '-' + bth[buf[i++]] + bth[buf[i++]] + '-' + bth[buf[i++]] + bth[buf[i++]] + bth[buf[i++]] + bth[buf[i++]] + bth[buf[i++]] + bth[buf[i++]];
}

// **`v1()` - Generate time-based UUID**
//
// Inspired by https://github.com/LiosK/UUID.js
// and http://docs.python.org/library/uuid.html

// random #'s we need to init node and clockseq
var _seedBytes = _rng();

// Per 4.5, create and 48-bit node id, (47 random bits + multicast bit = 1)
var _nodeId = [_seedBytes[0] | 0x01, _seedBytes[1], _seedBytes[2], _seedBytes[3], _seedBytes[4], _seedBytes[5]];

// Per 4.2.2, randomize (14 bit) clockseq
var _clockseq = (_seedBytes[6] << 8 | _seedBytes[7]) & 0x3fff;

// Previous uuid creation time
var _lastMSecs = 0,
    _lastNSecs = 0;

// See https://github.com/broofa/node-uuid for API details
function v1(options, buf, offset) {
  var i = buf && offset || 0;
  var b = buf || [];

  options = options || {};

  var clockseq = options.clockseq !== undefined ? options.clockseq : _clockseq;

  // UUID timestamps are 100 nano-second units since the Gregorian epoch,
  // (1582-10-15 00:00).  JSNumbers aren't precise enough for this, so
  // time is handled internally as 'msecs' (integer milliseconds) and 'nsecs'
  // (100-nanoseconds offset from msecs) since unix epoch, 1970-01-01 00:00.
  var msecs = options.msecs !== undefined ? options.msecs : new Date().getTime();

  // Per 4.2.1.2, use count of uuid's generated during the current clock
  // cycle to simulate higher resolution clock
  var nsecs = options.nsecs !== undefined ? options.nsecs : _lastNSecs + 1;

  // Time since last uuid creation (in msecs)
  var dt = msecs - _lastMSecs + (nsecs - _lastNSecs) / 10000;

  // Per 4.2.1.2, Bump clockseq on clock regression
  if (dt < 0 && options.clockseq === undefined) {
    clockseq = clockseq + 1 & 0x3fff;
  }

  // Reset nsecs if clock regresses (new clockseq) or we've moved onto a new
  // time interval
  if ((dt < 0 || msecs > _lastMSecs) && options.nsecs === undefined) {
    nsecs = 0;
  }

  // Per 4.2.1.2 Throw error if too many uuids are requested
  if (nsecs >= 10000) {
    throw new Error('uuid.v1(): Can\'t create more than 10M uuids/sec');
  }

  _lastMSecs = msecs;
  _lastNSecs = nsecs;
  _clockseq = clockseq;

  // Per 4.1.4 - Convert from unix epoch to Gregorian epoch
  msecs += 12219292800000;

  // `time_low`
  var tl = ((msecs & 0xfffffff) * 10000 + nsecs) % 0x100000000;
  b[i++] = tl >>> 24 & 0xff;
  b[i++] = tl >>> 16 & 0xff;
  b[i++] = tl >>> 8 & 0xff;
  b[i++] = tl & 0xff;

  // `time_mid`
  var tmh = msecs / 0x100000000 * 10000 & 0xfffffff;
  b[i++] = tmh >>> 8 & 0xff;
  b[i++] = tmh & 0xff;

  // `time_high_and_version`
  b[i++] = tmh >>> 24 & 0xf | 0x10; // include version
  b[i++] = tmh >>> 16 & 0xff;

  // `clock_seq_hi_and_reserved` (Per 4.2.2 - include variant)
  b[i++] = clockseq >>> 8 | 0x80;

  // `clock_seq_low`
  b[i++] = clockseq & 0xff;

  // `node`
  var node = options.node || _nodeId;
  for (var n = 0; n < 6; n++) {
    b[i + n] = node[n];
  }

  return buf ? buf : unparse(b);
}

// **`v4()` - Generate random UUID**

// See https://github.com/broofa/node-uuid for API details
function v4(options, buf, offset) {
  // Deprecated - 'format' argument, as supported in v1.2
  var i = buf && offset || 0;

  if (typeof options == 'string') {
    buf = options == 'binary' ? new Array(16) : null;
    options = null;
  }
  options = options || {};

  var rnds = options.random || (options.rng || _rng)();

  // Per 4.4, set bits for version and `clock_seq_hi_and_reserved`
  rnds[6] = rnds[6] & 0x0f | 0x40;
  rnds[8] = rnds[8] & 0x3f | 0x80;

  // Copy bytes to buffer, if provided
  if (buf) {
    for (var ii = 0; ii < 16; ii++) {
      buf[i + ii] = rnds[ii];
    }
  }

  return buf || unparse(rnds);
}

// Export public API
var uuid = v4;
uuid.v1 = v1;
uuid.v4 = v4;
uuid.parse = parse;
uuid.unparse = unparse;

module.exports = uuid;

}).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{}],9:[function(require,module,exports){
'use strict';

var keycharm = require('keycharm');
var Emitter = require('emitter-component');
var Hammer = require('../module/hammer');
var util = require('../util');

/**
 * Turn an element into an clickToUse element.
 * When not active, the element has a transparent overlay. When the overlay is
 * clicked, the mode is changed to active.
 * When active, the element is displayed with a blue border around it, and
 * the interactive contents of the element can be used. When clicked outside
 * the element, the elements mode is changed to inactive.
 * @param {Element} container
 * @constructor
 */
function Activator(container) {
  this.active = false;

  this.dom = {
    container: container
  };

  this.dom.overlay = document.createElement('div');
  this.dom.overlay.className = 'vis-overlay';

  this.dom.container.appendChild(this.dom.overlay);

  this.hammer = Hammer(this.dom.overlay);
  this.hammer.on('tap', this._onTapOverlay.bind(this));

  // block all touch events (except tap)
  var me = this;
  var events = ['tap', 'doubletap', 'press', 'pinch', 'pan', 'panstart', 'panmove', 'panend'];
  events.forEach(function (event) {
    me.hammer.on(event, function (event) {
      event.stopPropagation();
    });
  });

  // attach a click event to the window, in order to deactivate when clicking outside the timeline
  if (document && document.body) {
    this.onClick = function (event) {
      if (!_hasParent(event.target, container)) {
        me.deactivate();
      }
    };
    document.body.addEventListener('click', this.onClick);
  }

  if (this.keycharm !== undefined) {
    this.keycharm.destroy();
  }
  this.keycharm = keycharm();

  // keycharm listener only bounded when active)
  this.escListener = this.deactivate.bind(this);
}

// turn into an event emitter
Emitter(Activator.prototype);

// The currently active activator
Activator.current = null;

/**
 * Destroy the activator. Cleans up all created DOM and event listeners
 */
Activator.prototype.destroy = function () {
  this.deactivate();

  // remove dom
  this.dom.overlay.parentNode.removeChild(this.dom.overlay);

  // remove global event listener
  if (this.onClick) {
    document.body.removeEventListener('click', this.onClick);
  }

  // cleanup hammer instances
  this.hammer.destroy();
  this.hammer = null;
  // FIXME: cleaning up hammer instances doesn't work (Timeline not removed from memory)
};

/**
 * Activate the element
 * Overlay is hidden, element is decorated with a blue shadow border
 */
Activator.prototype.activate = function () {
  // we allow only one active activator at a time
  if (Activator.current) {
    Activator.current.deactivate();
  }
  Activator.current = this;

  this.active = true;
  this.dom.overlay.style.display = 'none';
  util.addClassName(this.dom.container, 'vis-active');

  this.emit('change');
  this.emit('activate');

  // ugly hack: bind ESC after emitting the events, as the Network rebinds all
  // keyboard events on a 'change' event
  this.keycharm.bind('esc', this.escListener);
};

/**
 * Deactivate the element
 * Overlay is displayed on top of the element
 */
Activator.prototype.deactivate = function () {
  this.active = false;
  this.dom.overlay.style.display = '';
  util.removeClassName(this.dom.container, 'vis-active');
  this.keycharm.unbind('esc', this.escListener);

  this.emit('change');
  this.emit('deactivate');
};

/**
 * Handle a tap event: activate the container
 * @param event
 * @private
 */
Activator.prototype._onTapOverlay = function (event) {
  // activate the container
  this.activate();
  event.stopPropagation();
};

/**
 * Test whether the element has the requested parent element somewhere in
 * its chain of parent nodes.
 * @param {HTMLElement} element
 * @param {HTMLElement} parent
 * @returns {boolean} Returns true when the parent is found somewhere in the
 *                    chain of parent nodes.
 * @private
 */
function _hasParent(element, parent) {
  while (element) {
    if (element === parent) {
      return true;
    }
    element = element.parentNode;
  }
  return false;
}

module.exports = Activator;

},{"../module/hammer":6,"../util":33,"emitter-component":34,"keycharm":36}],10:[function(require,module,exports){
'use strict';

Object.defineProperty(exports, '__esModule', {
  value: true
});

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

var Hammer = require('../module/hammer');
var hammerUtil = require('../hammerUtil');
var util = require('../util');

var ColorPicker = (function () {
  function ColorPicker() {
    var pixelRatio = arguments.length <= 0 || arguments[0] === undefined ? 1 : arguments[0];

    _classCallCheck(this, ColorPicker);

    this.pixelRatio = pixelRatio;
    this.generated = false;
    this.centerCoordinates = { x: 289 / 2, y: 289 / 2 };
    this.r = 289 * 0.49;
    this.color = { r: 255, g: 255, b: 255, a: 1.0 };
    this.hueCircle = undefined;
    this.initialColor = { r: 255, g: 255, b: 255, a: 1.0 };
    this.previousColor = undefined;
    this.applied = false;

    // bound by
    this.updateCallback = function () {};

    // create all DOM elements
    this._create();
  }

  /**
   * this inserts the colorPicker into a div from the DOM
   * @param container
   */

  _createClass(ColorPicker, [{
    key: 'insertTo',
    value: function insertTo(container) {
      if (this.hammer !== undefined) {
        this.hammer.destroy();
        this.hammer = undefined;
      }
      this.container = container;
      this.container.appendChild(this.frame);
      this._bindHammer();

      this._setSize();
    }

    /**
     * the callback is executed on apply and save. Bind it to the application
     * @param callback
     */
  }, {
    key: 'setCallback',
    value: function setCallback(callback) {
      if (typeof callback === 'function') {
        this.updateCallback = callback;
      } else {
        throw new Error("Function attempted to set as colorPicker callback is not a function.");
      }
    }
  }, {
    key: '_isColorString',
    value: function _isColorString(color) {
      var htmlColors = { black: '#000000', navy: '#000080', darkblue: '#00008B', mediumblue: '#0000CD', blue: '#0000FF', darkgreen: '#006400', green: '#008000', teal: '#008080', darkcyan: '#008B8B', deepskyblue: '#00BFFF', darkturquoise: '#00CED1', mediumspringgreen: '#00FA9A', lime: '#00FF00', springgreen: '#00FF7F', aqua: '#00FFFF', cyan: '#00FFFF', midnightblue: '#191970', dodgerblue: '#1E90FF', lightseagreen: '#20B2AA', forestgreen: '#228B22', seagreen: '#2E8B57', darkslategray: '#2F4F4F', limegreen: '#32CD32', mediumseagreen: '#3CB371', turquoise: '#40E0D0', royalblue: '#4169E1', steelblue: '#4682B4', darkslateblue: '#483D8B', mediumturquoise: '#48D1CC', indigo: '#4B0082', darkolivegreen: '#556B2F', cadetblue: '#5F9EA0', cornflowerblue: '#6495ED', mediumaquamarine: '#66CDAA', dimgray: '#696969', slateblue: '#6A5ACD', olivedrab: '#6B8E23', slategray: '#708090', lightslategray: '#778899', mediumslateblue: '#7B68EE', lawngreen: '#7CFC00', chartreuse: '#7FFF00', aquamarine: '#7FFFD4', maroon: '#800000', purple: '#800080', olive: '#808000', gray: '#808080', skyblue: '#87CEEB', lightskyblue: '#87CEFA', blueviolet: '#8A2BE2', darkred: '#8B0000', darkmagenta: '#8B008B', saddlebrown: '#8B4513', darkseagreen: '#8FBC8F', lightgreen: '#90EE90', mediumpurple: '#9370D8', darkviolet: '#9400D3', palegreen: '#98FB98', darkorchid: '#9932CC', yellowgreen: '#9ACD32', sienna: '#A0522D', brown: '#A52A2A', darkgray: '#A9A9A9', lightblue: '#ADD8E6', greenyellow: '#ADFF2F', paleturquoise: '#AFEEEE', lightsteelblue: '#B0C4DE', powderblue: '#B0E0E6', firebrick: '#B22222', darkgoldenrod: '#B8860B', mediumorchid: '#BA55D3', rosybrown: '#BC8F8F', darkkhaki: '#BDB76B', silver: '#C0C0C0', mediumvioletred: '#C71585', indianred: '#CD5C5C', peru: '#CD853F', chocolate: '#D2691E', tan: '#D2B48C', lightgrey: '#D3D3D3', palevioletred: '#D87093', thistle: '#D8BFD8', orchid: '#DA70D6', goldenrod: '#DAA520', crimson: '#DC143C', gainsboro: '#DCDCDC', plum: '#DDA0DD', burlywood: '#DEB887', lightcyan: '#E0FFFF', lavender: '#E6E6FA', darksalmon: '#E9967A', violet: '#EE82EE', palegoldenrod: '#EEE8AA', lightcoral: '#F08080', khaki: '#F0E68C', aliceblue: '#F0F8FF', honeydew: '#F0FFF0', azure: '#F0FFFF', sandybrown: '#F4A460', wheat: '#F5DEB3', beige: '#F5F5DC', whitesmoke: '#F5F5F5', mintcream: '#F5FFFA', ghostwhite: '#F8F8FF', salmon: '#FA8072', antiquewhite: '#FAEBD7', linen: '#FAF0E6', lightgoldenrodyellow: '#FAFAD2', oldlace: '#FDF5E6', red: '#FF0000', fuchsia: '#FF00FF', magenta: '#FF00FF', deeppink: '#FF1493', orangered: '#FF4500', tomato: '#FF6347', hotpink: '#FF69B4', coral: '#FF7F50', darkorange: '#FF8C00', lightsalmon: '#FFA07A', orange: '#FFA500', lightpink: '#FFB6C1', pink: '#FFC0CB', gold: '#FFD700', peachpuff: '#FFDAB9', navajowhite: '#FFDEAD', moccasin: '#FFE4B5', bisque: '#FFE4C4', mistyrose: '#FFE4E1', blanchedalmond: '#FFEBCD', papayawhip: '#FFEFD5', lavenderblush: '#FFF0F5', seashell: '#FFF5EE', cornsilk: '#FFF8DC', lemonchiffon: '#FFFACD', floralwhite: '#FFFAF0', snow: '#FFFAFA', yellow: '#FFFF00', lightyellow: '#FFFFE0', ivory: '#FFFFF0', white: '#FFFFFF' };
      if (typeof color === 'string') {
        return htmlColors[color];
      }
    }

    /**
     * Set the color of the colorPicker
     * Supported formats:
     * 'red'                   --> HTML color string
     * '#ffffff'               --> hex string
     * 'rbg(255,255,255)'      --> rgb string
     * 'rgba(255,255,255,1.0)' --> rgba string
     * {r:255,g:255,b:255}     --> rgb object
     * {r:255,g:255,b:255,a:1.0} --> rgba object
     * @param color
     * @param setInitial
     */
  }, {
    key: 'setColor',
    value: function setColor(color) {
      var setInitial = arguments.length <= 1 || arguments[1] === undefined ? true : arguments[1];

      if (color === 'none') {
        return;
      }

      var rgba = undefined;

      // if a html color shorthand is used, convert to hex
      var htmlColor = this._isColorString(color);
      if (htmlColor !== undefined) {
        color = htmlColor;
      }

      // check format
      if (util.isString(color) === true) {
        if (util.isValidRGB(color) === true) {
          var rgbaArray = color.substr(4).substr(0, color.length - 5).split(',');
          rgba = { r: rgbaArray[0], g: rgbaArray[1], b: rgbaArray[2], a: 1.0 };
        } else if (util.isValidRGBA(color) === true) {
          var rgbaArray = color.substr(5).substr(0, color.length - 6).split(',');
          rgba = { r: rgbaArray[0], g: rgbaArray[1], b: rgbaArray[2], a: rgbaArray[3] };
        } else if (util.isValidHex(color) === true) {
          var rgbObj = util.hexToRGB(color);
          rgba = { r: rgbObj.r, g: rgbObj.g, b: rgbObj.b, a: 1.0 };
        }
      } else {
        if (color instanceof Object) {
          if (color.r !== undefined && color.g !== undefined && color.b !== undefined) {
            var alpha = color.a !== undefined ? color.a : '1.0';
            rgba = { r: color.r, g: color.g, b: color.b, a: alpha };
          }
        }
      }

      // set color
      if (rgba === undefined) {
        throw new Error("Unknown color passed to the colorPicker. Supported are strings: rgb, hex, rgba. Object: rgb ({r:r,g:g,b:b,[a:a]}). Supplied: " + JSON.stringify(color));
      } else {
        this._setColor(rgba, setInitial);
      }
    }

    /**
     * this shows the color picker at a location. The hue circle is constructed once and stored.
     * @param x
     * @param y
     */
  }, {
    key: 'show',
    value: function show(x, y) {
      this.applied = false;
      this.frame.style.display = 'block';
      this.frame.style.top = y + 'px';
      this.frame.style.left = x + 'px';
      this._generateHueCircle();
    }

    // ------------------------------------------ PRIVATE ----------------------------- //

    /**
     * Hide the picker. Is called by the cancel button.
     * Optional boolean to store the previous color for easy access later on.
     * @param storePrevious
     * @private
     */
  }, {
    key: '_hide',
    value: function _hide() {
      var storePrevious = arguments.length <= 0 || arguments[0] === undefined ? true : arguments[0];

      // store the previous color for next time;
      if (storePrevious === true) {
        this.previousColor = util.extend({}, this.color);
      }

      if (this.applied === true) {
        this.updateCallback(this.initialColor);
      }

      this.frame.style.display = 'none';
    }

    /**
     * bound to the save button. Saves and hides.
     * @private
     */
  }, {
    key: '_save',
    value: function _save() {
      this.updateCallback(this.color);
      this.applied = false;
      this._hide();
    }

    /**
     * Bound to apply button. Saves but does not close. Is undone by the cancel button.
     * @private
     */
  }, {
    key: '_apply',
    value: function _apply() {
      this.applied = true;
      this.updateCallback(this.color);
      this._updatePicker(this.color);
    }

    /**
     * load the color from the previous session.
     * @private
     */
  }, {
    key: '_loadLast',
    value: function _loadLast() {
      if (this.previousColor !== undefined) {
        this.setColor(this.previousColor, false);
      } else {
        alert("There is no last color to load...");
      }
    }

    /**
     * set the color, place the picker
     * @param rgba
     * @param setInitial
     * @private
     */
  }, {
    key: '_setColor',
    value: function _setColor(rgba) {
      var setInitial = arguments.length <= 1 || arguments[1] === undefined ? true : arguments[1];

      // store the initial color
      if (setInitial === true) {
        this.initialColor = util.extend({}, rgba);
      }

      this.color = rgba;
      var hsv = util.RGBToHSV(rgba.r, rgba.g, rgba.b);

      var angleConvert = 2 * Math.PI;
      var radius = this.r * hsv.s;
      var x = this.centerCoordinates.x + radius * Math.sin(angleConvert * hsv.h);
      var y = this.centerCoordinates.y + radius * Math.cos(angleConvert * hsv.h);

      this.colorPickerSelector.style.left = x - 0.5 * this.colorPickerSelector.clientWidth + 'px';
      this.colorPickerSelector.style.top = y - 0.5 * this.colorPickerSelector.clientHeight + 'px';

      this._updatePicker(rgba);
    }

    /**
     * bound to opacity control
     * @param value
     * @private
     */
  }, {
    key: '_setOpacity',
    value: function _setOpacity(value) {
      this.color.a = value / 100;
      this._updatePicker(this.color);
    }

    /**
     * bound to brightness control
     * @param value
     * @private
     */
  }, {
    key: '_setBrightness',
    value: function _setBrightness(value) {
      var hsv = util.RGBToHSV(this.color.r, this.color.g, this.color.b);
      hsv.v = value / 100;
      var rgba = util.HSVToRGB(hsv.h, hsv.s, hsv.v);
      rgba['a'] = this.color.a;
      this.color = rgba;
      this._updatePicker();
    }

    /**
     * update the colorpicker. A black circle overlays the hue circle to mimic the brightness decreasing.
     * @param rgba
     * @private
     */
  }, {
    key: '_updatePicker',
    value: function _updatePicker() {
      var rgba = arguments.length <= 0 || arguments[0] === undefined ? this.color : arguments[0];

      var hsv = util.RGBToHSV(rgba.r, rgba.g, rgba.b);
      var ctx = this.colorPickerCanvas.getContext('2d');
      if (this.pixelRation === undefined) {
        this.pixelRatio = (window.devicePixelRatio || 1) / (ctx.webkitBackingStorePixelRatio || ctx.mozBackingStorePixelRatio || ctx.msBackingStorePixelRatio || ctx.oBackingStorePixelRatio || ctx.backingStorePixelRatio || 1);
      }
      ctx.setTransform(this.pixelRatio, 0, 0, this.pixelRatio, 0, 0);

      // clear the canvas
      var w = this.colorPickerCanvas.clientWidth;
      var h = this.colorPickerCanvas.clientHeight;
      ctx.clearRect(0, 0, w, h);

      ctx.putImageData(this.hueCircle, 0, 0);
      ctx.fillStyle = 'rgba(0,0,0,' + (1 - hsv.v) + ')';
      ctx.circle(this.centerCoordinates.x, this.centerCoordinates.y, this.r);
      ctx.fill();

      this.brightnessRange.value = 100 * hsv.v;
      this.opacityRange.value = 100 * rgba.a;

      this.initialColorDiv.style.backgroundColor = 'rgba(' + this.initialColor.r + ',' + this.initialColor.g + ',' + this.initialColor.b + ',' + this.initialColor.a + ')';
      this.newColorDiv.style.backgroundColor = 'rgba(' + this.color.r + ',' + this.color.g + ',' + this.color.b + ',' + this.color.a + ')';
    }

    /**
     * used by create to set the size of the canvas.
     * @private
     */
  }, {
    key: '_setSize',
    value: function _setSize() {
      this.colorPickerCanvas.style.width = '100%';
      this.colorPickerCanvas.style.height = '100%';

      this.colorPickerCanvas.width = 289 * this.pixelRatio;
      this.colorPickerCanvas.height = 289 * this.pixelRatio;
    }

    /**
     * create all dom elements
     * TODO: cleanup, lots of similar dom elements
     * @private
     */
  }, {
    key: '_create',
    value: function _create() {
      this.frame = document.createElement('div');
      this.frame.className = 'vis-color-picker';

      this.colorPickerDiv = document.createElement('div');
      this.colorPickerSelector = document.createElement('div');
      this.colorPickerSelector.className = 'vis-selector';
      this.colorPickerDiv.appendChild(this.colorPickerSelector);

      this.colorPickerCanvas = document.createElement('canvas');
      this.colorPickerDiv.appendChild(this.colorPickerCanvas);

      if (!this.colorPickerCanvas.getContext) {
        var noCanvas = document.createElement('DIV');
        noCanvas.style.color = 'red';
        noCanvas.style.fontWeight = 'bold';
        noCanvas.style.padding = '10px';
        noCanvas.innerHTML = 'Error: your browser does not support HTML canvas';
        this.colorPickerCanvas.appendChild(noCanvas);
      } else {
        var ctx = this.colorPickerCanvas.getContext("2d");
        this.pixelRatio = (window.devicePixelRatio || 1) / (ctx.webkitBackingStorePixelRatio || ctx.mozBackingStorePixelRatio || ctx.msBackingStorePixelRatio || ctx.oBackingStorePixelRatio || ctx.backingStorePixelRatio || 1);

        this.colorPickerCanvas.getContext("2d").setTransform(this.pixelRatio, 0, 0, this.pixelRatio, 0, 0);
      }

      this.colorPickerDiv.className = 'vis-color';

      this.opacityDiv = document.createElement('div');
      this.opacityDiv.className = 'vis-opacity';

      this.brightnessDiv = document.createElement('div');
      this.brightnessDiv.className = 'vis-brightness';

      this.arrowDiv = document.createElement('div');
      this.arrowDiv.className = 'vis-arrow';

      this.opacityRange = document.createElement('input');
      try {
        this.opacityRange.type = 'range'; // Not supported on IE9
        this.opacityRange.min = '0';
        this.opacityRange.max = '100';
      } catch (err) {}
      this.opacityRange.value = '100';
      this.opacityRange.className = 'vis-range';

      this.brightnessRange = document.createElement('input');
      try {
        this.brightnessRange.type = 'range'; // Not supported on IE9
        this.brightnessRange.min = '0';
        this.brightnessRange.max = '100';
      } catch (err) {}
      this.brightnessRange.value = '100';
      this.brightnessRange.className = 'vis-range';

      this.opacityDiv.appendChild(this.opacityRange);
      this.brightnessDiv.appendChild(this.brightnessRange);

      var me = this;
      this.opacityRange.onchange = function () {
        me._setOpacity(this.value);
      };
      this.opacityRange.oninput = function () {
        me._setOpacity(this.value);
      };
      this.brightnessRange.onchange = function () {
        me._setBrightness(this.value);
      };
      this.brightnessRange.oninput = function () {
        me._setBrightness(this.value);
      };

      this.brightnessLabel = document.createElement("div");
      this.brightnessLabel.className = "vis-label vis-brightness";
      this.brightnessLabel.innerHTML = 'brightness:';

      this.opacityLabel = document.createElement("div");
      this.opacityLabel.className = "vis-label vis-opacity";
      this.opacityLabel.innerHTML = 'opacity:';

      this.newColorDiv = document.createElement("div");
      this.newColorDiv.className = "vis-new-color";
      this.newColorDiv.innerHTML = 'new';

      this.initialColorDiv = document.createElement("div");
      this.initialColorDiv.className = "vis-initial-color";
      this.initialColorDiv.innerHTML = 'initial';

      this.cancelButton = document.createElement("div");
      this.cancelButton.className = "vis-button vis-cancel";
      this.cancelButton.innerHTML = 'cancel';
      this.cancelButton.onclick = this._hide.bind(this, false);

      this.applyButton = document.createElement("div");
      this.applyButton.className = "vis-button vis-apply";
      this.applyButton.innerHTML = 'apply';
      this.applyButton.onclick = this._apply.bind(this);

      this.saveButton = document.createElement("div");
      this.saveButton.className = "vis-button vis-save";
      this.saveButton.innerHTML = 'save';
      this.saveButton.onclick = this._save.bind(this);

      this.loadButton = document.createElement("div");
      this.loadButton.className = "vis-button vis-load";
      this.loadButton.innerHTML = 'load last';
      this.loadButton.onclick = this._loadLast.bind(this);

      this.frame.appendChild(this.colorPickerDiv);
      this.frame.appendChild(this.arrowDiv);
      this.frame.appendChild(this.brightnessLabel);
      this.frame.appendChild(this.brightnessDiv);
      this.frame.appendChild(this.opacityLabel);
      this.frame.appendChild(this.opacityDiv);
      this.frame.appendChild(this.newColorDiv);
      this.frame.appendChild(this.initialColorDiv);

      this.frame.appendChild(this.cancelButton);
      this.frame.appendChild(this.applyButton);
      this.frame.appendChild(this.saveButton);
      this.frame.appendChild(this.loadButton);
    }

    /**
     * bind hammer to the color picker
     * @private
     */
  }, {
    key: '_bindHammer',
    value: function _bindHammer() {
      var _this = this;

      this.drag = {};
      this.pinch = {};
      this.hammer = new Hammer(this.colorPickerCanvas);
      this.hammer.get('pinch').set({ enable: true });

      hammerUtil.onTouch(this.hammer, function (event) {
        _this._moveSelector(event);
      });
      this.hammer.on('tap', function (event) {
        _this._moveSelector(event);
      });
      this.hammer.on('panstart', function (event) {
        _this._moveSelector(event);
      });
      this.hammer.on('panmove', function (event) {
        _this._moveSelector(event);
      });
      this.hammer.on('panend', function (event) {
        _this._moveSelector(event);
      });
    }

    /**
     * generate the hue circle. This is relatively heavy (200ms) and is done only once on the first time it is shown.
     * @private
     */
  }, {
    key: '_generateHueCircle',
    value: function _generateHueCircle() {
      if (this.generated === false) {
        var ctx = this.colorPickerCanvas.getContext('2d');
        if (this.pixelRation === undefined) {
          this.pixelRatio = (window.devicePixelRatio || 1) / (ctx.webkitBackingStorePixelRatio || ctx.mozBackingStorePixelRatio || ctx.msBackingStorePixelRatio || ctx.oBackingStorePixelRatio || ctx.backingStorePixelRatio || 1);
        }
        ctx.setTransform(this.pixelRatio, 0, 0, this.pixelRatio, 0, 0);

        // clear the canvas
        var w = this.colorPickerCanvas.clientWidth;
        var h = this.colorPickerCanvas.clientHeight;
        ctx.clearRect(0, 0, w, h);

        // draw hue circle
        var x = undefined,
            y = undefined,
            hue = undefined,
            sat = undefined;
        this.centerCoordinates = { x: w * 0.5, y: h * 0.5 };
        this.r = 0.49 * w;
        var angleConvert = 2 * Math.PI / 360;
        var hfac = 1 / 360;
        var sfac = 1 / this.r;
        var rgb = undefined;
        for (hue = 0; hue < 360; hue++) {
          for (sat = 0; sat < this.r; sat++) {
            x = this.centerCoordinates.x + sat * Math.sin(angleConvert * hue);
            y = this.centerCoordinates.y + sat * Math.cos(angleConvert * hue);
            rgb = util.HSVToRGB(hue * hfac, sat * sfac, 1);
            ctx.fillStyle = 'rgb(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
            ctx.fillRect(x - 0.5, y - 0.5, 2, 2);
          }
        }
        ctx.strokeStyle = 'rgba(0,0,0,1)';
        ctx.circle(this.centerCoordinates.x, this.centerCoordinates.y, this.r);
        ctx.stroke();

        this.hueCircle = ctx.getImageData(0, 0, w, h);
      }
      this.generated = true;
    }

    /**
     * move the selector. This is called by hammer functions.
     *
     * @param event
     * @private
     */
  }, {
    key: '_moveSelector',
    value: function _moveSelector(event) {
      var rect = this.colorPickerDiv.getBoundingClientRect();
      var left = event.center.x - rect.left;
      var top = event.center.y - rect.top;

      var centerY = 0.5 * this.colorPickerDiv.clientHeight;
      var centerX = 0.5 * this.colorPickerDiv.clientWidth;

      var x = left - centerX;
      var y = top - centerY;

      var angle = Math.atan2(x, y);
      var radius = 0.98 * Math.min(Math.sqrt(x * x + y * y), centerX);

      var newTop = Math.cos(angle) * radius + centerY;
      var newLeft = Math.sin(angle) * radius + centerX;

      this.colorPickerSelector.style.top = newTop - 0.5 * this.colorPickerSelector.clientHeight + 'px';
      this.colorPickerSelector.style.left = newLeft - 0.5 * this.colorPickerSelector.clientWidth + 'px';

      // set color
      var h = angle / (2 * Math.PI);
      h = h < 0 ? h + 1 : h;
      var s = radius / this.r;
      var hsv = util.RGBToHSV(this.color.r, this.color.g, this.color.b);
      hsv.h = h;
      hsv.s = s;
      var rgba = util.HSVToRGB(hsv.h, hsv.s, hsv.v);
      rgba['a'] = this.color.a;
      this.color = rgba;

      // update previews
      this.initialColorDiv.style.backgroundColor = 'rgba(' + this.initialColor.r + ',' + this.initialColor.g + ',' + this.initialColor.b + ',' + this.initialColor.a + ')';
      this.newColorDiv.style.backgroundColor = 'rgba(' + this.color.r + ',' + this.color.g + ',' + this.color.b + ',' + this.color.a + ')';
    }
  }]);

  return ColorPicker;
})();

exports['default'] = ColorPicker;
module.exports = exports['default'];

},{"../hammerUtil":5,"../module/hammer":6,"../util":33}],11:[function(require,module,exports){
'use strict';

Object.defineProperty(exports, '__esModule', {
  value: true
});

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

var _ColorPicker = require('./ColorPicker');

var _ColorPicker2 = _interopRequireDefault(_ColorPicker);

/**
 * The way this works is for all properties of this.possible options, you can supply the property name in any form to list the options.
 * Boolean options are recognised as Boolean
 * Number options should be written as array: [default value, min value, max value, stepsize]
 * Colors should be written as array: ['color', '#ffffff']
 * Strings with should be written as array: [option1, option2, option3, ..]
 *
 * The options are matched with their counterparts in each of the modules and the values used in the configuration are
 *
 * @param parentModule        | the location where parentModule.setOptions() can be called
 * @param defaultContainer    | the default container of the module
 * @param configureOptions    | the fully configured and predefined options set found in allOptions.js
 * @param pixelRatio          | canvas pixel ratio
 */
var util = require('../util');

var Configurator = (function () {
  function Configurator(parentModule, defaultContainer, configureOptions) {
    var pixelRatio = arguments.length <= 3 || arguments[3] === undefined ? 1 : arguments[3];

    _classCallCheck(this, Configurator);

    this.parent = parentModule;
    this.changedOptions = [];
    this.container = defaultContainer;
    this.allowCreation = false;

    this.options = {};
    this.initialized = false;
    this.popupCounter = 0;
    this.defaultOptions = {
      enabled: false,
      filter: true,
      container: undefined,
      showButton: true
    };
    util.extend(this.options, this.defaultOptions);

    this.configureOptions = configureOptions;
    this.moduleOptions = {};
    this.domElements = [];
    this.popupDiv = {};
    this.popupLimit = 5;
    this.popupHistory = {};
    this.colorPicker = new _ColorPicker2['default'](pixelRatio);
    this.wrapper = undefined;
  }

  /**
   * refresh all options.
   * Because all modules parse their options by themselves, we just use their options. We copy them here.
   *
   * @param options
   */

  _createClass(Configurator, [{
    key: 'setOptions',
    value: function setOptions(options) {
      if (options !== undefined) {
        // reset the popup history because the indices may have been changed.
        this.popupHistory = {};
        this._removePopup();

        var enabled = true;
        if (typeof options === 'string') {
          this.options.filter = options;
        } else if (options instanceof Array) {
          this.options.filter = options.join();
        } else if (typeof options === 'object') {
          if (options.container !== undefined) {
            this.options.container = options.container;
          }
          if (options.filter !== undefined) {
            this.options.filter = options.filter;
          }
          if (options.showButton !== undefined) {
            this.options.showButton = options.showButton;
          }
          if (options.enabled !== undefined) {
            enabled = options.enabled;
          }
        } else if (typeof options === 'boolean') {
          this.options.filter = true;
          enabled = options;
        } else if (typeof options === 'function') {
          this.options.filter = options;
          enabled = true;
        }
        if (this.options.filter === false) {
          enabled = false;
        }

        this.options.enabled = enabled;
      }
      this._clean();
    }
  }, {
    key: 'setModuleOptions',
    value: function setModuleOptions(moduleOptions) {
      this.moduleOptions = moduleOptions;
      if (this.options.enabled === true) {
        this._clean();
        if (this.options.container !== undefined) {
          this.container = this.options.container;
        }
        this._create();
      }
    }

    /**
     * Create all DOM elements
     * @private
     */
  }, {
    key: '_create',
    value: function _create() {
      var _this = this;

      this._clean();
      this.changedOptions = [];

      var filter = this.options.filter;
      var counter = 0;
      var show = false;
      for (var option in this.configureOptions) {
        if (this.configureOptions.hasOwnProperty(option)) {
          this.allowCreation = false;
          show = false;
          if (typeof filter === 'function') {
            show = filter(option, []);
            show = show || this._handleObject(this.configureOptions[option], [option], true);
          } else if (filter === true || filter.indexOf(option) !== -1) {
            show = true;
          }

          if (show !== false) {
            this.allowCreation = true;

            // linebreak between categories
            if (counter > 0) {
              this._makeItem([]);
            }
            // a header for the category
            this._makeHeader(option);

            // get the suboptions
            this._handleObject(this.configureOptions[option], [option]);
          }
          counter++;
        }
      }

      if (this.options.showButton === true) {
        (function () {
          var generateButton = document.createElement('div');
          generateButton.className = 'vis-configuration vis-config-button';
          generateButton.innerHTML = 'generate options';
          generateButton.onclick = function () {
            _this._printOptions();
          };
          generateButton.onmouseover = function () {
            generateButton.className = 'vis-configuration vis-config-button hover';
          };
          generateButton.onmouseout = function () {
            generateButton.className = 'vis-configuration vis-config-button';
          };

          _this.optionsContainer = document.createElement('div');
          _this.optionsContainer.className = 'vis-configuration vis-config-option-container';

          _this.domElements.push(_this.optionsContainer);
          _this.domElements.push(generateButton);
        })();
      }

      this._push();
      this.colorPicker.insertTo(this.container);
    }

    /**
     * draw all DOM elements on the screen
     * @private
     */
  }, {
    key: '_push',
    value: function _push() {
      this.wrapper = document.createElement('div');
      this.wrapper.className = 'vis-configuration-wrapper';
      this.container.appendChild(this.wrapper);
      for (var i = 0; i < this.domElements.length; i++) {
        this.wrapper.appendChild(this.domElements[i]);
      }

      this._showPopupIfNeeded();
    }

    /**
     * delete all DOM elements
     * @private
     */
  }, {
    key: '_clean',
    value: function _clean() {
      for (var i = 0; i < this.domElements.length; i++) {
        this.wrapper.removeChild(this.domElements[i]);
      }

      if (this.wrapper !== undefined) {
        this.container.removeChild(this.wrapper);
        this.wrapper = undefined;
      }
      this.domElements = [];

      this._removePopup();
    }

    /**
     * get the value from the actualOptions if it exists
     * @param {array} path    | where to look for the actual option
     * @returns {*}
     * @private
     */
  }, {
    key: '_getValue',
    value: function _getValue(path) {
      var base = this.moduleOptions;
      for (var i = 0; i < path.length; i++) {
        if (base[path[i]] !== undefined) {
          base = base[path[i]];
        } else {
          base = undefined;
          break;
        }
      }
      return base;
    }

    /**
     * all option elements are wrapped in an item
     * @param path
     * @param domElements
     * @private
     */
  }, {
    key: '_makeItem',
    value: function _makeItem(path) {
      var _arguments = arguments,
          _this2 = this;

      if (this.allowCreation === true) {
        var _len, domElements, _key;

        var _ret2 = (function () {
          var item = document.createElement('div');
          item.className = 'vis-configuration vis-config-item vis-config-s' + path.length;

          for (_len = _arguments.length, domElements = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
            domElements[_key - 1] = _arguments[_key];
          }

          domElements.forEach(function (element) {
            item.appendChild(element);
          });
          _this2.domElements.push(item);
          return {
            v: _this2.domElements.length
          };
        })();

        if (typeof _ret2 === 'object') return _ret2.v;
      }
      return 0;
    }

    /**
     * header for major subjects
     * @param name
     * @private
     */
  }, {
    key: '_makeHeader',
    value: function _makeHeader(name) {
      var div = document.createElement('div');
      div.className = 'vis-configuration vis-config-header';
      div.innerHTML = name;
      this._makeItem([], div);
    }

    /**
     * make a label, if it is an object label, it gets different styling.
     * @param name
     * @param path
     * @param objectLabel
     * @returns {HTMLElement}
     * @private
     */
  }, {
    key: '_makeLabel',
    value: function _makeLabel(name, path) {
      var objectLabel = arguments.length <= 2 || arguments[2] === undefined ? false : arguments[2];

      var div = document.createElement('div');
      div.className = 'vis-configuration vis-config-label vis-config-s' + path.length;
      if (objectLabel === true) {
        div.innerHTML = '<i><b>' + name + ':</b></i>';
      } else {
        div.innerHTML = name + ':';
      }
      return div;
    }

    /**
     * make a dropdown list for multiple possible string optoins
     * @param arr
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_makeDropdown',
    value: function _makeDropdown(arr, value, path) {
      var select = document.createElement('select');
      select.className = 'vis-configuration vis-config-select';
      var selectedValue = 0;
      if (value !== undefined) {
        if (arr.indexOf(value) !== -1) {
          selectedValue = arr.indexOf(value);
        }
      }

      for (var i = 0; i < arr.length; i++) {
        var option = document.createElement('option');
        option.value = arr[i];
        if (i === selectedValue) {
          option.selected = 'selected';
        }
        option.innerHTML = arr[i];
        select.appendChild(option);
      }

      var me = this;
      select.onchange = function () {
        me._update(this.value, path);
      };

      var label = this._makeLabel(path[path.length - 1], path);
      this._makeItem(path, label, select);
    }

    /**
     * make a range object for numeric options
     * @param arr
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_makeRange',
    value: function _makeRange(arr, value, path) {
      var defaultValue = arr[0];
      var min = arr[1];
      var max = arr[2];
      var step = arr[3];
      var range = document.createElement('input');
      range.className = 'vis-configuration vis-config-range';
      try {
        range.type = 'range'; // not supported on IE9
        range.min = min;
        range.max = max;
      } catch (err) {}
      range.step = step;

      // set up the popup settings in case they are needed.
      var popupString = '';
      var popupValue = 0;

      if (value !== undefined) {
        var factor = 1.20;
        if (value < 0 && value * factor < min) {
          range.min = Math.ceil(value * factor);
          popupValue = range.min;
          popupString = 'range increased';
        } else if (value / factor < min) {
          range.min = Math.ceil(value / factor);
          popupValue = range.min;
          popupString = 'range increased';
        }
        if (value * factor > max && max !== 1) {
          range.max = Math.ceil(value * factor);
          popupValue = range.max;
          popupString = 'range increased';
        }
        range.value = value;
      } else {
        range.value = defaultValue;
      }

      var input = document.createElement('input');
      input.className = 'vis-configuration vis-config-rangeinput';
      input.value = range.value;

      var me = this;
      range.onchange = function () {
        input.value = this.value;me._update(Number(this.value), path);
      };
      range.oninput = function () {
        input.value = this.value;
      };

      var label = this._makeLabel(path[path.length - 1], path);
      var itemIndex = this._makeItem(path, label, range, input);

      // if a popup is needed AND it has not been shown for this value, show it.
      if (popupString !== '' && this.popupHistory[itemIndex] !== popupValue) {
        this.popupHistory[itemIndex] = popupValue;
        this._setupPopup(popupString, itemIndex);
      }
    }

    /**
     * prepare the popup
     * @param string
     * @param index
     * @private
     */
  }, {
    key: '_setupPopup',
    value: function _setupPopup(string, index) {
      var _this3 = this;

      if (this.initialized === true && this.allowCreation === true && this.popupCounter < this.popupLimit) {
        var div = document.createElement("div");
        div.id = "vis-configuration-popup";
        div.className = "vis-configuration-popup";
        div.innerHTML = string;
        div.onclick = function () {
          _this3._removePopup();
        };
        this.popupCounter += 1;
        this.popupDiv = { html: div, index: index };
      }
    }

    /**
     * remove the popup from the dom
     * @private
     */
  }, {
    key: '_removePopup',
    value: function _removePopup() {
      if (this.popupDiv.html !== undefined) {
        this.popupDiv.html.parentNode.removeChild(this.popupDiv.html);
        clearTimeout(this.popupDiv.hideTimeout);
        clearTimeout(this.popupDiv.deleteTimeout);
        this.popupDiv = {};
      }
    }

    /**
     * Show the popup if it is needed.
     * @private
     */
  }, {
    key: '_showPopupIfNeeded',
    value: function _showPopupIfNeeded() {
      var _this4 = this;

      if (this.popupDiv.html !== undefined) {
        var correspondingElement = this.domElements[this.popupDiv.index];
        var rect = correspondingElement.getBoundingClientRect();
        this.popupDiv.html.style.left = rect.left + "px";
        this.popupDiv.html.style.top = rect.top - 30 + "px"; // 30 is the height;
        document.body.appendChild(this.popupDiv.html);
        this.popupDiv.hideTimeout = setTimeout(function () {
          _this4.popupDiv.html.style.opacity = 0;
        }, 1500);
        this.popupDiv.deleteTimeout = setTimeout(function () {
          _this4._removePopup();
        }, 1800);
      }
    }

    /**
     * make a checkbox for boolean options.
     * @param defaultValue
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_makeCheckbox',
    value: function _makeCheckbox(defaultValue, value, path) {
      var checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.className = 'vis-configuration vis-config-checkbox';
      checkbox.checked = defaultValue;
      if (value !== undefined) {
        checkbox.checked = value;
        if (value !== defaultValue) {
          if (typeof defaultValue === 'object') {
            if (value !== defaultValue.enabled) {
              this.changedOptions.push({ path: path, value: value });
            }
          } else {
            this.changedOptions.push({ path: path, value: value });
          }
        }
      }

      var me = this;
      checkbox.onchange = function () {
        me._update(this.checked, path);
      };

      var label = this._makeLabel(path[path.length - 1], path);
      this._makeItem(path, label, checkbox);
    }

    /**
     * make a text input field for string options.
     * @param defaultValue
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_makeTextInput',
    value: function _makeTextInput(defaultValue, value, path) {
      var checkbox = document.createElement('input');
      checkbox.type = 'text';
      checkbox.className = 'vis-configuration vis-config-text';
      checkbox.value = value;
      if (value !== defaultValue) {
        this.changedOptions.push({ path: path, value: value });
      }

      var me = this;
      checkbox.onchange = function () {
        me._update(this.value, path);
      };

      var label = this._makeLabel(path[path.length - 1], path);
      this._makeItem(path, label, checkbox);
    }

    /**
     * make a color field with a color picker for color fields
     * @param arr
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_makeColorField',
    value: function _makeColorField(arr, value, path) {
      var _this5 = this;

      var defaultColor = arr[1];
      var div = document.createElement('div');
      value = value === undefined ? defaultColor : value;

      if (value !== 'none') {
        div.className = 'vis-configuration vis-config-colorBlock';
        div.style.backgroundColor = value;
      } else {
        div.className = 'vis-configuration vis-config-colorBlock none';
      }

      value = value === undefined ? defaultColor : value;
      div.onclick = function () {
        _this5._showColorPicker(value, div, path);
      };

      var label = this._makeLabel(path[path.length - 1], path);
      this._makeItem(path, label, div);
    }

    /**
     * used by the color buttons to call the color picker.
     * @param event
     * @param value
     * @param div
     * @param path
     * @private
     */
  }, {
    key: '_showColorPicker',
    value: function _showColorPicker(value, div, path) {
      var _this6 = this;

      var rect = div.getBoundingClientRect();
      var bodyRect = document.body.getBoundingClientRect();
      var pickerX = rect.left + rect.width + 5;
      var pickerY = rect.top - bodyRect.top + rect.height + 2;
      this.colorPicker.show(pickerX, pickerY);
      this.colorPicker.setColor(value);
      this.colorPicker.setCallback(function (color) {
        var colorString = 'rgba(' + color.r + ',' + color.g + ',' + color.b + ',' + color.a + ')';
        div.style.backgroundColor = colorString;
        _this6._update(colorString, path);
      });
    }

    /**
     * parse an object and draw the correct items
     * @param obj
     * @param path
     * @private
     */
  }, {
    key: '_handleObject',
    value: function _handleObject(obj) {
      var path = arguments.length <= 1 || arguments[1] === undefined ? [] : arguments[1];
      var checkOnly = arguments.length <= 2 || arguments[2] === undefined ? false : arguments[2];

      var show = false;
      var filter = this.options.filter;
      var visibleInSet = false;
      for (var subObj in obj) {
        if (obj.hasOwnProperty(subObj)) {
          show = true;
          var item = obj[subObj];
          var newPath = util.copyAndExtendArray(path, subObj);
          if (typeof filter === 'function') {
            show = filter(subObj, path);

            // if needed we must go deeper into the object.
            if (show === false) {
              if (!(item instanceof Array) && typeof item !== 'string' && typeof item !== 'boolean' && item instanceof Object) {
                this.allowCreation = false;
                show = this._handleObject(item, newPath, true);
                this.allowCreation = checkOnly === false;
              }
            }
          }

          if (show !== false) {
            visibleInSet = true;
            var value = this._getValue(newPath);

            if (item instanceof Array) {
              this._handleArray(item, value, newPath);
            } else if (typeof item === 'string') {
              this._makeTextInput(item, value, newPath);
            } else if (typeof item === 'boolean') {
              this._makeCheckbox(item, value, newPath);
            } else if (item instanceof Object) {
              // collapse the physics options that are not enabled
              var draw = true;
              if (path.indexOf('physics') !== -1) {
                if (this.moduleOptions.physics.solver !== subObj) {
                  draw = false;
                }
              }

              if (draw === true) {
                // initially collapse options with an disabled enabled option.
                if (item.enabled !== undefined) {
                  var enabledPath = util.copyAndExtendArray(newPath, 'enabled');
                  var enabledValue = this._getValue(enabledPath);
                  if (enabledValue === true) {
                    var label = this._makeLabel(subObj, newPath, true);
                    this._makeItem(newPath, label);
                    visibleInSet = this._handleObject(item, newPath) || visibleInSet;
                  } else {
                    this._makeCheckbox(item, enabledValue, newPath);
                  }
                } else {
                  var label = this._makeLabel(subObj, newPath, true);
                  this._makeItem(newPath, label);
                  visibleInSet = this._handleObject(item, newPath) || visibleInSet;
                }
              }
            } else {
              console.error('dont know how to handle', item, subObj, newPath);
            }
          }
        }
      }
      return visibleInSet;
    }

    /**
     * handle the array type of option
     * @param optionName
     * @param arr
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_handleArray',
    value: function _handleArray(arr, value, path) {
      if (typeof arr[0] === 'string' && arr[0] === 'color') {
        this._makeColorField(arr, value, path);
        if (arr[1] !== value) {
          this.changedOptions.push({ path: path, value: value });
        }
      } else if (typeof arr[0] === 'string') {
        this._makeDropdown(arr, value, path);
        if (arr[0] !== value) {
          this.changedOptions.push({ path: path, value: value });
        }
      } else if (typeof arr[0] === 'number') {
        this._makeRange(arr, value, path);
        if (arr[0] !== value) {
          this.changedOptions.push({ path: path, value: Number(value) });
        }
      }
    }

    /**
     * called to update the network with the new settings.
     * @param value
     * @param path
     * @private
     */
  }, {
    key: '_update',
    value: function _update(value, path) {
      var options = this._constructOptions(value, path);

      if (this.parent.body && this.parent.body.emitter && this.parent.body.emitter.emit) {
        this.parent.body.emitter.emit("configChange", options);
      }
      this.initialized = true;
      this.parent.setOptions(options);
    }
  }, {
    key: '_constructOptions',
    value: function _constructOptions(value, path) {
      var optionsObj = arguments.length <= 2 || arguments[2] === undefined ? {} : arguments[2];

      var pointer = optionsObj;

      // when dropdown boxes can be string or boolean, we typecast it into correct types
      value = value === 'true' ? true : value;
      value = value === 'false' ? false : value;

      for (var i = 0; i < path.length; i++) {
        if (path[i] !== 'global') {
          if (pointer[path[i]] === undefined) {
            pointer[path[i]] = {};
          }
          if (i !== path.length - 1) {
            pointer = pointer[path[i]];
          } else {
            pointer[path[i]] = value;
          }
        }
      }
      return optionsObj;
    }
  }, {
    key: '_printOptions',
    value: function _printOptions() {
      var options = this.getOptions();
      this.optionsContainer.innerHTML = '<pre>var options = ' + JSON.stringify(options, null, 2) + '</pre>';
    }
  }, {
    key: 'getOptions',
    value: function getOptions() {
      var options = {};
      for (var i = 0; i < this.changedOptions.length; i++) {
        this._constructOptions(this.changedOptions[i].value, this.changedOptions[i].path, options);
      }
      return options;
    }
  }]);

  return Configurator;
})();

exports['default'] = Configurator;
module.exports = exports['default'];

},{"../util":33,"./ColorPicker":10}],12:[function(require,module,exports){
'use strict';

Object.defineProperty(exports, '__esModule', {
  value: true
});

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

var util = require('../util');

var errorFound = false;
var allOptions = undefined;
var printStyle = 'background: #FFeeee; color: #dd0000';
/**
 *  Used to validate options.
 */

var Validator = (function () {
  function Validator() {
    _classCallCheck(this, Validator);
  }

  /**
   * Main function to be called
   * @param options
   * @param subObject
   * @returns {boolean}
   */

  _createClass(Validator, null, [{
    key: 'validate',
    value: function validate(options, referenceOptions, subObject) {
      errorFound = false;
      allOptions = referenceOptions;
      var usedOptions = referenceOptions;
      if (subObject !== undefined) {
        usedOptions = referenceOptions[subObject];
      }
      Validator.parse(options, usedOptions, []);
      return errorFound;
    }

    /**
     * Will traverse an object recursively and check every value
     * @param options
     * @param referenceOptions
     * @param path
     */
  }, {
    key: 'parse',
    value: function parse(options, referenceOptions, path) {
      for (var option in options) {
        if (options.hasOwnProperty(option)) {
          Validator.check(option, options, referenceOptions, path);
        }
      }
    }

    /**
     * Check every value. If the value is an object, call the parse function on that object.
     * @param option
     * @param options
     * @param referenceOptions
     * @param path
     */
  }, {
    key: 'check',
    value: function check(option, options, referenceOptions, path) {
      if (referenceOptions[option] === undefined && referenceOptions.__any__ === undefined) {
        Validator.getSuggestion(option, referenceOptions, path);
      } else if (referenceOptions[option] === undefined && referenceOptions.__any__ !== undefined) {
        // __any__ is a wildcard. Any value is accepted and will be further analysed by reference.
        if (Validator.getType(options[option]) === 'object' && referenceOptions['__any__'].__type__ !== undefined) {
          // if the any subgroup is not a predefined object int he configurator we do not look deeper into the object.
          Validator.checkFields(option, options, referenceOptions, '__any__', referenceOptions['__any__'].__type__, path);
        } else {
          Validator.checkFields(option, options, referenceOptions, '__any__', referenceOptions['__any__'], path);
        }
      } else {
        // Since all options in the reference are objects, we can check whether they are supposed to be object to look for the __type__ field.
        if (referenceOptions[option].__type__ !== undefined) {
          // if this should be an object, we check if the correct type has been supplied to account for shorthand options.
          Validator.checkFields(option, options, referenceOptions, option, referenceOptions[option].__type__, path);
        } else {
          Validator.checkFields(option, options, referenceOptions, option, referenceOptions[option], path);
        }
      }
    }

    /**
     *
     * @param {String}  option     | the option property
     * @param {Object}  options    | The supplied options object
     * @param {Object}  referenceOptions    | The reference options containing all options and their allowed formats
     * @param {String}  referenceOption     | Usually this is the same as option, except when handling an __any__ tag.
     * @param {String}  refOptionType       | This is the type object from the reference options
     * @param {Array}   path      | where in the object is the option
     */
  }, {
    key: 'checkFields',
    value: function checkFields(option, options, referenceOptions, referenceOption, refOptionObj, path) {
      var optionType = Validator.getType(options[option]);
      var refOptionType = refOptionObj[optionType];
      if (refOptionType !== undefined) {
        // if the type is correct, we check if it is supposed to be one of a few select values
        if (Validator.getType(refOptionType) === 'array') {
          if (refOptionType.indexOf(options[option]) === -1) {
            console.log('%cInvalid option detected in "' + option + '".' + ' Allowed values are:' + Validator.print(refOptionType) + ' not "' + options[option] + '". ' + Validator.printLocation(path, option), printStyle);
            errorFound = true;
          } else if (optionType === 'object' && referenceOption !== "__any__") {
            path = util.copyAndExtendArray(path, option);
            Validator.parse(options[option], referenceOptions[referenceOption], path);
          }
        } else if (optionType === 'object' && referenceOption !== "__any__") {
          path = util.copyAndExtendArray(path, option);
          Validator.parse(options[option], referenceOptions[referenceOption], path);
        }
      } else if (refOptionObj['any'] === undefined) {
        // type of the field is incorrect and the field cannot be any
        console.log('%cInvalid type received for "' + option + '". Expected: ' + Validator.print(Object.keys(refOptionObj)) + '. Received [' + optionType + '] "' + options[option] + '"' + Validator.printLocation(path, option), printStyle);
        errorFound = true;
      }
    }
  }, {
    key: 'getType',
    value: function getType(object) {
      var type = typeof object;

      if (type === 'object') {
        if (object === null) {
          return 'null';
        }
        if (object instanceof Boolean) {
          return 'boolean';
        }
        if (object instanceof Number) {
          return 'number';
        }
        if (object instanceof String) {
          return 'string';
        }
        if (Array.isArray(object)) {
          return 'array';
        }
        if (object instanceof Date) {
          return 'date';
        }
        if (object.nodeType !== undefined) {
          return 'dom';
        }
        if (object._isAMomentObject === true) {
          return 'moment';
        }
        return 'object';
      } else if (type === 'number') {
        return 'number';
      } else if (type === 'boolean') {
        return 'boolean';
      } else if (type === 'string') {
        return 'string';
      } else if (type === undefined) {
        return 'undefined';
      }
      return type;
    }
  }, {
    key: 'getSuggestion',
    value: function getSuggestion(option, options, path) {
      var localSearch = Validator.findInOptions(option, options, path, false);
      var globalSearch = Validator.findInOptions(option, allOptions, [], true);

      var localSearchThreshold = 8;
      var globalSearchThreshold = 4;

      if (localSearch.indexMatch !== undefined) {
        console.log('%cUnknown option detected: "' + option + '" in ' + Validator.printLocation(localSearch.path, option, '') + 'Perhaps it was incomplete? Did you mean: "' + localSearch.indexMatch + '"?\n\n', printStyle);
      } else if (globalSearch.distance <= globalSearchThreshold && localSearch.distance > globalSearch.distance) {
        console.log('%cUnknown option detected: "' + option + '" in ' + Validator.printLocation(localSearch.path, option, '') + 'Perhaps it was misplaced? Matching option found at: ' + Validator.printLocation(globalSearch.path, globalSearch.closestMatch, ''), printStyle);
      } else if (localSearch.distance <= localSearchThreshold) {
        console.log('%cUnknown option detected: "' + option + '". Did you mean "' + localSearch.closestMatch + '"?' + Validator.printLocation(localSearch.path, option), printStyle);
      } else {
        console.log('%cUnknown option detected: "' + option + '". Did you mean one of these: ' + Validator.print(Object.keys(options)) + Validator.printLocation(path, option), printStyle);
      }

      errorFound = true;
    }

    /**
     * traverse the options in search for a match.
     * @param option
     * @param options
     * @param path
     * @param recursive
     * @returns {{closestMatch: string, path: Array, distance: number}}
     */
  }, {
    key: 'findInOptions',
    value: function findInOptions(option, options, path) {
      var recursive = arguments.length <= 3 || arguments[3] === undefined ? false : arguments[3];

      var min = 1e9;
      var closestMatch = '';
      var closestMatchPath = [];
      var lowerCaseOption = option.toLowerCase();
      var indexMatch = undefined;
      for (var op in options) {
        var distance = undefined;
        if (options[op].__type__ !== undefined && recursive === true) {
          var result = Validator.findInOptions(option, options[op], util.copyAndExtendArray(path, op));
          if (min > result.distance) {
            closestMatch = result.closestMatch;
            closestMatchPath = result.path;
            min = result.distance;
            indexMatch = result.indexMatch;
          }
        } else {
          if (op.toLowerCase().indexOf(lowerCaseOption) !== -1) {
            indexMatch = op;
          }
          distance = Validator.levenshteinDistance(option, op);
          if (min > distance) {
            closestMatch = op;
            closestMatchPath = util.copyArray(path);
            min = distance;
          }
        }
      }
      return { closestMatch: closestMatch, path: closestMatchPath, distance: min, indexMatch: indexMatch };
    }
  }, {
    key: 'printLocation',
    value: function printLocation(path, option) {
      var prefix = arguments.length <= 2 || arguments[2] === undefined ? 'Problem value found at: \n' : arguments[2];

      var str = '\n\n' + prefix + 'options = {\n';
      for (var i = 0; i < path.length; i++) {
        for (var j = 0; j < i + 1; j++) {
          str += '  ';
        }
        str += path[i] + ': {\n';
      }
      for (var j = 0; j < path.length + 1; j++) {
        str += '  ';
      }
      str += option + '\n';
      for (var i = 0; i < path.length + 1; i++) {
        for (var j = 0; j < path.length - i; j++) {
          str += '  ';
        }
        str += '}\n';
      }
      return str + '\n\n';
    }
  }, {
    key: 'print',
    value: function print(options) {
      return JSON.stringify(options).replace(/(\")|(\[)|(\])|(,"__type__")/g, "").replace(/(\,)/g, ', ');
    }

    // Compute the edit distance between the two given strings
    // http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#JavaScript
    /*
     Copyright (c) 2011 Andrei Mackenzie
      Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
      The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
     */
  }, {
    key: 'levenshteinDistance',
    value: function levenshteinDistance(a, b) {
      if (a.length === 0) return b.length;
      if (b.length === 0) return a.length;

      var matrix = [];

      // increment along the first column of each row
      var i;
      for (i = 0; i <= b.length; i++) {
        matrix[i] = [i];
      }

      // increment each column in the first row
      var j;
      for (j = 0; j <= a.length; j++) {
        matrix[0][j] = j;
      }

      // Fill in the rest of the matrix
      for (i = 1; i <= b.length; i++) {
        for (j = 1; j <= a.length; j++) {
          if (b.charAt(i - 1) == a.charAt(j - 1)) {
            matrix[i][j] = matrix[i - 1][j - 1];
          } else {
            matrix[i][j] = Math.min(matrix[i - 1][j - 1] + 1, // substitution
            Math.min(matrix[i][j - 1] + 1, // insertion
            matrix[i - 1][j] + 1)); // deletion
          }
        }
      }

      return matrix[b.length][a.length];
    }
  }]);

  return Validator;
})();

exports['default'] = Validator;
exports.printStyle = printStyle;

},{"../util":33}],13:[function(require,module,exports){
'use strict';

var Emitter = require('emitter-component');
var Hammer = require('../module/hammer');
var hammerUtil = require('../hammerUtil');
var util = require('../util');
var DataSet = require('../DataSet');
var DataView = require('../DataView');
var Range = require('./Range');
var ItemSet = require('./component/ItemSet');
var TimeAxis = require('./component/TimeAxis');
var Activator = require('../shared/Activator');
var DateUtil = require('./DateUtil');
var CustomTime = require('./component/CustomTime');

/**
 * Create a timeline visualization
 * @constructor
 */
function Core() {}

// turn Core into an event emitter
Emitter(Core.prototype);

/**
 * Create the main DOM for the Core: a root panel containing left, right,
 * top, bottom, content, and background panel.
 * @param {Element} container  The container element where the Core will
 *                             be attached.
 * @protected
 */
Core.prototype._create = function (container) {
  this.dom = {};

  this.dom.container = container;

  this.dom.root = document.createElement('div');
  this.dom.background = document.createElement('div');
  this.dom.backgroundVertical = document.createElement('div');
  this.dom.backgroundHorizontal = document.createElement('div');
  this.dom.centerContainer = document.createElement('div');
  this.dom.leftContainer = document.createElement('div');
  this.dom.rightContainer = document.createElement('div');
  this.dom.center = document.createElement('div');
  this.dom.left = document.createElement('div');
  this.dom.right = document.createElement('div');
  this.dom.top = document.createElement('div');
  this.dom.bottom = document.createElement('div');
  this.dom.shadowTop = document.createElement('div');
  this.dom.shadowBottom = document.createElement('div');
  this.dom.shadowTopLeft = document.createElement('div');
  this.dom.shadowBottomLeft = document.createElement('div');
  this.dom.shadowTopRight = document.createElement('div');
  this.dom.shadowBottomRight = document.createElement('div');

  this.dom.root.className = 'vis-timeline';
  this.dom.background.className = 'vis-panel vis-background';
  this.dom.backgroundVertical.className = 'vis-panel vis-background vis-vertical';
  this.dom.backgroundHorizontal.className = 'vis-panel vis-background vis-horizontal';
  this.dom.centerContainer.className = 'vis-panel vis-center';
  this.dom.leftContainer.className = 'vis-panel vis-left';
  this.dom.rightContainer.className = 'vis-panel vis-right';
  this.dom.top.className = 'vis-panel vis-top';
  this.dom.bottom.className = 'vis-panel vis-bottom';
  this.dom.left.className = 'vis-content';
  this.dom.center.className = 'vis-content';
  this.dom.right.className = 'vis-content';
  this.dom.shadowTop.className = 'vis-shadow vis-top';
  this.dom.shadowBottom.className = 'vis-shadow vis-bottom';
  this.dom.shadowTopLeft.className = 'vis-shadow vis-top';
  this.dom.shadowBottomLeft.className = 'vis-shadow vis-bottom';
  this.dom.shadowTopRight.className = 'vis-shadow vis-top';
  this.dom.shadowBottomRight.className = 'vis-shadow vis-bottom';

  this.dom.root.appendChild(this.dom.background);
  this.dom.root.appendChild(this.dom.backgroundVertical);
  this.dom.root.appendChild(this.dom.backgroundHorizontal);
  this.dom.root.appendChild(this.dom.centerContainer);
  this.dom.root.appendChild(this.dom.leftContainer);
  this.dom.root.appendChild(this.dom.rightContainer);
  this.dom.root.appendChild(this.dom.top);
  this.dom.root.appendChild(this.dom.bottom);

  this.dom.centerContainer.appendChild(this.dom.center);
  this.dom.leftContainer.appendChild(this.dom.left);
  this.dom.rightContainer.appendChild(this.dom.right);

  this.dom.centerContainer.appendChild(this.dom.shadowTop);
  this.dom.centerContainer.appendChild(this.dom.shadowBottom);
  this.dom.leftContainer.appendChild(this.dom.shadowTopLeft);
  this.dom.leftContainer.appendChild(this.dom.shadowBottomLeft);
  this.dom.rightContainer.appendChild(this.dom.shadowTopRight);
  this.dom.rightContainer.appendChild(this.dom.shadowBottomRight);

  this.on('rangechange', (function () {
    this._redraw(); // this allows overriding the _redraw method
  }).bind(this));
  this.on('touch', this._onTouch.bind(this));
  this.on('pan', this._onDrag.bind(this));

  var me = this;
  this.on('change', function (properties) {
    if (properties && properties.queue == true) {
      // redraw once on next tick
      if (!me._redrawTimer) {
        me._redrawTimer = setTimeout(function () {
          me._redrawTimer = null;
          me._redraw();
        }, 0);
      }
    } else {
      // redraw immediately
      me._redraw();
    }
  });

  // create event listeners for all interesting events, these events will be
  // emitted via emitter
  this.hammer = new Hammer(this.dom.root);
  this.hammer.get('pinch').set({ enable: true });
  this.hammer.get('pan').set({ threshold: 5, direction: 30 }); // 30 is ALL_DIRECTIONS in hammer.
  this.listeners = {};

  var events = ['tap', 'doubletap', 'press', 'pinch', 'pan', 'panstart', 'panmove', 'panend'
  // TODO: cleanup
  //'touch', 'pinch',
  //'tap', 'doubletap', 'hold',
  //'dragstart', 'drag', 'dragend',
  //'mousewheel', 'DOMMouseScroll' // DOMMouseScroll is needed for Firefox
  ];
  events.forEach(function (type) {
    var listener = function listener(event) {
      if (me.isActive()) {
        me.emit(type, event);
      }
    };
    me.hammer.on(type, listener);
    me.listeners[type] = listener;
  });

  // emulate a touch event (emitted before the start of a pan, pinch, tap, or press)
  hammerUtil.onTouch(this.hammer, (function (event) {
    me.emit('touch', event);
  }).bind(this));

  // emulate a release event (emitted after a pan, pinch, tap, or press)
  hammerUtil.onRelease(this.hammer, (function (event) {
    me.emit('release', event);
  }).bind(this));

  function onMouseWheel(event) {
    if (me.isActive()) {
      me.emit('mousewheel', event);
    }
  }
  this.dom.root.addEventListener('mousewheel', onMouseWheel);
  this.dom.root.addEventListener('DOMMouseScroll', onMouseWheel);

  // size properties of each of the panels
  this.props = {
    root: {},
    background: {},
    centerContainer: {},
    leftContainer: {},
    rightContainer: {},
    center: {},
    left: {},
    right: {},
    top: {},
    bottom: {},
    border: {},
    scrollTop: 0,
    scrollTopMin: 0
  };

  this.customTimes = [];

  // store state information needed for touch events
  this.touch = {};

  this.redrawCount = 0;

  // attach the root panel to the provided container
  if (!container) throw new Error('No container provided');
  container.appendChild(this.dom.root);
};

/**
 * Set options. Options will be passed to all components loaded in the Timeline.
 * @param {Object} [options]
 *                           {String} orientation
 *                              Vertical orientation for the Timeline,
 *                              can be 'bottom' (default) or 'top'.
 *                           {String | Number} width
 *                              Width for the timeline, a number in pixels or
 *                              a css string like '1000px' or '75%'. '100%' by default.
 *                           {String | Number} height
 *                              Fixed height for the Timeline, a number in pixels or
 *                              a css string like '400px' or '75%'. If undefined,
 *                              The Timeline will automatically size such that
 *                              its contents fit.
 *                           {String | Number} minHeight
 *                              Minimum height for the Timeline, a number in pixels or
 *                              a css string like '400px' or '75%'.
 *                           {String | Number} maxHeight
 *                              Maximum height for the Timeline, a number in pixels or
 *                              a css string like '400px' or '75%'.
 *                           {Number | Date | String} start
 *                              Start date for the visible window
 *                           {Number | Date | String} end
 *                              End date for the visible window
 */
Core.prototype.setOptions = function (options) {
  if (options) {
    // copy the known options
    var fields = ['width', 'height', 'minHeight', 'maxHeight', 'autoResize', 'start', 'end', 'clickToUse', 'dataAttributes', 'hiddenDates', 'locale', 'locales', 'moment', 'throttleRedraw'];
    util.selectiveExtend(fields, this.options, options);

    if ('orientation' in options) {
      if (typeof options.orientation === 'string') {
        this.options.orientation = {
          item: options.orientation,
          axis: options.orientation
        };
      } else if (typeof options.orientation === 'object') {
        if ('item' in options.orientation) {
          this.options.orientation.item = options.orientation.item;
        }
        if ('axis' in options.orientation) {
          this.options.orientation.axis = options.orientation.axis;
        }
      }
    }

    if (this.options.orientation.axis === 'both') {
      if (!this.timeAxis2) {
        var timeAxis2 = this.timeAxis2 = new TimeAxis(this.body);
        timeAxis2.setOptions = function (options) {
          var _options = options ? util.extend({}, options) : {};
          _options.orientation = 'top'; // override the orientation option, always top
          TimeAxis.prototype.setOptions.call(timeAxis2, _options);
        };
        this.components.push(timeAxis2);
      }
    } else {
      if (this.timeAxis2) {
        var index = this.components.indexOf(this.timeAxis2);
        if (index !== -1) {
          this.components.splice(index, 1);
        }
        this.timeAxis2.destroy();
        this.timeAxis2 = null;
      }
    }

    // if the graph2d's drawPoints is a function delegate the callback to the onRender property
    if (typeof options.drawPoints == 'function') {
      options.drawPoints = {
        onRender: options.drawPoints
      };
    }

    if ('hiddenDates' in this.options) {
      DateUtil.convertHiddenOptions(this.options.moment, this.body, this.options.hiddenDates);
    }

    if ('clickToUse' in options) {
      if (options.clickToUse) {
        if (!this.activator) {
          this.activator = new Activator(this.dom.root);
        }
      } else {
        if (this.activator) {
          this.activator.destroy();
          delete this.activator;
        }
      }
    }

    if ('showCustomTime' in options) {
      throw new Error('Option `showCustomTime` is deprecated. Create a custom time bar via timeline.addCustomTime(time [, id])');
    }

    // enable/disable autoResize
    this._initAutoResize();
  }

  // propagate options to all components
  this.components.forEach(function (component) {
    return component.setOptions(options);
  });

  // enable/disable configure
  if ('configure' in options) {
    if (!this.configurator) {
      this.configurator = this._createConfigurator();
    }

    this.configurator.setOptions(options.configure);

    // collect the settings of all components, and pass them to the configuration system
    var appliedOptions = util.deepExtend({}, this.options);
    this.components.forEach(function (component) {
      util.deepExtend(appliedOptions, component.options);
    });
    this.configurator.setModuleOptions({ global: appliedOptions });
  }

  // override redraw with a throttled version
  if (!this._origRedraw) {
    this._origRedraw = this._redraw.bind(this);
  }
  this._redraw = util.throttle(this._origRedraw, this.options.throttleRedraw);

  // redraw everything
  this._redraw();
};

/**
 * Returns true when the Timeline is active.
 * @returns {boolean}
 */
Core.prototype.isActive = function () {
  return !this.activator || this.activator.active;
};

/**
 * Destroy the Core, clean up all DOM elements and event listeners.
 */
Core.prototype.destroy = function () {
  // unbind datasets
  this.setItems(null);
  this.setGroups(null);

  // remove all event listeners
  this.off();

  // stop checking for changed size
  this._stopAutoResize();

  // remove from DOM
  if (this.dom.root.parentNode) {
    this.dom.root.parentNode.removeChild(this.dom.root);
  }
  this.dom = null;

  // remove Activator
  if (this.activator) {
    this.activator.destroy();
    delete this.activator;
  }

  // cleanup hammer touch events
  for (var event in this.listeners) {
    if (this.listeners.hasOwnProperty(event)) {
      delete this.listeners[event];
    }
  }
  this.listeners = null;
  this.hammer = null;

  // give all components the opportunity to cleanup
  this.components.forEach(function (component) {
    return component.destroy();
  });

  this.body = null;
};

/**
 * Set a custom time bar
 * @param {Date} time
 * @param {number} [id=undefined] Optional id of the custom time bar to be adjusted.
 */
Core.prototype.setCustomTime = function (time, id) {
  var customTimes = this.customTimes.filter(function (component) {
    return id === component.options.id;
  });

  if (customTimes.length === 0) {
    throw new Error('No custom time bar found with id ' + JSON.stringify(id));
  }

  if (customTimes.length > 0) {
    customTimes[0].setCustomTime(time);
  }
};

/**
 * Retrieve the current custom time.
 * @param {number} [id=undefined]    Id of the custom time bar.
 * @return {Date | undefined} customTime
 */
Core.prototype.getCustomTime = function (id) {
  var customTimes = this.customTimes.filter(function (component) {
    return component.options.id === id;
  });

  if (customTimes.length === 0) {
    throw new Error('No custom time bar found with id ' + JSON.stringify(id));
  }
  return customTimes[0].getCustomTime();
};

/**
 * Set a custom title for the custom time bar.
 * @param {String} [title] Custom title
 * @param {number} [id=undefined]    Id of the custom time bar.
 */
Core.prototype.setCustomTimeTitle = function (title, id) {
  var customTimes = this.customTimes.filter(function (component) {
    return component.options.id === id;
  });

  if (customTimes.length === 0) {
    throw new Error('No custom time bar found with id ' + JSON.stringify(id));
  }
  if (customTimes.length > 0) {
    return customTimes[0].setCustomTitle(title);
  }
};

/**
 * Retrieve meta information from an event.
 * Should be overridden by classes extending Core
 * @param {Event} event
 * @return {Object} An object with related information.
 */
Core.prototype.getEventProperties = function (event) {
  return { event: event };
};

/**
 * Add custom vertical bar
 * @param {Date | String | Number} [time]  A Date, unix timestamp, or
 *                                         ISO date string. Time point where
 *                                         the new bar should be placed.
 *                                         If not provided, `new Date()` will
 *                                         be used.
 * @param {Number | String} [id=undefined] Id of the new bar. Optional
 * @return {Number | String}               Returns the id of the new bar
 */
Core.prototype.addCustomTime = function (time, id) {
  var timestamp = time !== undefined ? util.convert(time, 'Date').valueOf() : new Date();

  var exists = this.customTimes.some(function (customTime) {
    return customTime.options.id === id;
  });
  if (exists) {
    throw new Error('A custom time with id ' + JSON.stringify(id) + ' already exists');
  }

  var customTime = new CustomTime(this.body, util.extend({}, this.options, {
    time: timestamp,
    id: id
  }));

  this.customTimes.push(customTime);
  this.components.push(customTime);
  this._redraw();

  return id;
};

/**
 * Remove previously added custom bar
 * @param {int} id ID of the custom bar to be removed
 * @return {boolean} True if the bar exists and is removed, false otherwise
 */
Core.prototype.removeCustomTime = function (id) {
  var customTimes = this.customTimes.filter(function (bar) {
    return bar.options.id === id;
  });

  if (customTimes.length === 0) {
    throw new Error('No custom time bar found with id ' + JSON.stringify(id));
  }

  customTimes.forEach((function (customTime) {
    this.customTimes.splice(this.customTimes.indexOf(customTime), 1);
    this.components.splice(this.components.indexOf(customTime), 1);
    customTime.destroy();
  }).bind(this));
};

/**
 * Get the id's of the currently visible items.
 * @returns {Array} The ids of the visible items
 */
Core.prototype.getVisibleItems = function () {
  return this.itemSet && this.itemSet.getVisibleItems() || [];
};

/**
 * Set Core window such that it fits all items
 * @param {Object} [options]  Available options:
 *                                `animation: boolean | {duration: number, easingFunction: string}`
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 */
Core.prototype.fit = function (options) {
  var range = this.getDataRange();

  // skip range set if there is no min and max date
  if (range.min === null && range.max === null) {
    return;
  }

  // apply a margin of 1% left and right of the data
  var interval = range.max - range.min;
  var min = new Date(range.min.valueOf() - interval * 0.01);
  var max = new Date(range.max.valueOf() + interval * 0.01);

  var animation = options && options.animation !== undefined ? options.animation : true;
  this.range.setRange(min, max, animation);
};

/**
 * Calculate the data range of the items start and end dates
 * @returns {{min: Date | null, max: Date | null}}
 * @protected
 */
Core.prototype.getDataRange = function () {
  // must be implemented by Timeline and Graph2d
  throw new Error('Cannot invoke abstract method getDataRange');
};

/**
 * Set the visible window. Both parameters are optional, you can change only
 * start or only end. Syntax:
 *
 *     TimeLine.setWindow(start, end)
 *     TimeLine.setWindow(start, end, options)
 *     TimeLine.setWindow(range)
 *
 * Where start and end can be a Date, number, or string, and range is an
 * object with properties start and end.
 *
 * @param {Date | Number | String | Object} [start] Start date of visible window
 * @param {Date | Number | String} [end]            End date of visible window
 * @param {Object} [options]  Available options:
 *                                `animation: boolean | {duration: number, easingFunction: string}`
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 */
Core.prototype.setWindow = function (start, end, options) {
  var animation;
  if (arguments.length == 1) {
    var range = arguments[0];
    animation = range.animation !== undefined ? range.animation : true;
    this.range.setRange(range.start, range.end, animation);
  } else {
    animation = options && options.animation !== undefined ? options.animation : true;
    this.range.setRange(start, end, animation);
  }
};

/**
 * Move the window such that given time is centered on screen.
 * @param {Date | Number | String} time
 * @param {Object} [options]  Available options:
 *                                `animation: boolean | {duration: number, easingFunction: string}`
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 */
Core.prototype.moveTo = function (time, options) {
  var interval = this.range.end - this.range.start;
  var t = util.convert(time, 'Date').valueOf();

  var start = t - interval / 2;
  var end = t + interval / 2;
  var animation = options && options.animation !== undefined ? options.animation : true;

  this.range.setRange(start, end, animation);
};

/**
 * Get the visible window
 * @return {{start: Date, end: Date}}   Visible range
 */
Core.prototype.getWindow = function () {
  var range = this.range.getRange();
  return {
    start: new Date(range.start),
    end: new Date(range.end)
  };
};

/**
 * Force a redraw. Can be overridden by implementations of Core
 *
 * Note: this function will be overridden on construction with a trottled version
 */
Core.prototype.redraw = function () {
  this._redraw();
};

/**
 * Redraw for internal use. Redraws all components. See also the public
 * method redraw.
 * @protected
 */
Core.prototype._redraw = function () {
  var resized = false;
  var options = this.options;
  var props = this.props;
  var dom = this.dom;

  if (!dom) return; // when destroyed

  DateUtil.updateHiddenDates(this.options.moment, this.body, this.options.hiddenDates);

  // update class names
  if (options.orientation == 'top') {
    util.addClassName(dom.root, 'vis-top');
    util.removeClassName(dom.root, 'vis-bottom');
  } else {
    util.removeClassName(dom.root, 'vis-top');
    util.addClassName(dom.root, 'vis-bottom');
  }

  // update root width and height options
  dom.root.style.maxHeight = util.option.asSize(options.maxHeight, '');
  dom.root.style.minHeight = util.option.asSize(options.minHeight, '');
  dom.root.style.width = util.option.asSize(options.width, '');

  // calculate border widths
  props.border.left = (dom.centerContainer.offsetWidth - dom.centerContainer.clientWidth) / 2;
  props.border.right = props.border.left;
  props.border.top = (dom.centerContainer.offsetHeight - dom.centerContainer.clientHeight) / 2;
  props.border.bottom = props.border.top;
  var borderRootHeight = dom.root.offsetHeight - dom.root.clientHeight;
  var borderRootWidth = dom.root.offsetWidth - dom.root.clientWidth;

  // workaround for a bug in IE: the clientWidth of an element with
  // a height:0px and overflow:hidden is not calculated and always has value 0
  if (dom.centerContainer.clientHeight === 0) {
    props.border.left = props.border.top;
    props.border.right = props.border.left;
  }
  if (dom.root.clientHeight === 0) {
    borderRootWidth = borderRootHeight;
  }

  // calculate the heights. If any of the side panels is empty, we set the height to
  // minus the border width, such that the border will be invisible
  props.center.height = dom.center.offsetHeight;
  props.left.height = dom.left.offsetHeight;
  props.right.height = dom.right.offsetHeight;
  props.top.height = dom.top.clientHeight || -props.border.top;
  props.bottom.height = dom.bottom.clientHeight || -props.border.bottom;

  // TODO: compensate borders when any of the panels is empty.

  // apply auto height
  // TODO: only calculate autoHeight when needed (else we cause an extra reflow/repaint of the DOM)
  var contentHeight = Math.max(props.left.height, props.center.height, props.right.height);
  var autoHeight = props.top.height + contentHeight + props.bottom.height + borderRootHeight + props.border.top + props.border.bottom;
  dom.root.style.height = util.option.asSize(options.height, autoHeight + 'px');

  // calculate heights of the content panels
  props.root.height = dom.root.offsetHeight;
  props.background.height = props.root.height - borderRootHeight;
  var containerHeight = props.root.height - props.top.height - props.bottom.height - borderRootHeight;
  props.centerContainer.height = containerHeight;
  props.leftContainer.height = containerHeight;
  props.rightContainer.height = props.leftContainer.height;

  // calculate the widths of the panels
  props.root.width = dom.root.offsetWidth;
  props.background.width = props.root.width - borderRootWidth;
  props.left.width = dom.leftContainer.clientWidth || -props.border.left;
  props.leftContainer.width = props.left.width;
  props.right.width = dom.rightContainer.clientWidth || -props.border.right;
  props.rightContainer.width = props.right.width;
  var centerWidth = props.root.width - props.left.width - props.right.width - borderRootWidth;
  props.center.width = centerWidth;
  props.centerContainer.width = centerWidth;
  props.top.width = centerWidth;
  props.bottom.width = centerWidth;

  // resize the panels
  dom.background.style.height = props.background.height + 'px';
  dom.backgroundVertical.style.height = props.background.height + 'px';
  dom.backgroundHorizontal.style.height = props.centerContainer.height + 'px';
  dom.centerContainer.style.height = props.centerContainer.height + 'px';
  dom.leftContainer.style.height = props.leftContainer.height + 'px';
  dom.rightContainer.style.height = props.rightContainer.height + 'px';

  dom.background.style.width = props.background.width + 'px';
  dom.backgroundVertical.style.width = props.centerContainer.width + 'px';
  dom.backgroundHorizontal.style.width = props.background.width + 'px';
  dom.centerContainer.style.width = props.center.width + 'px';
  dom.top.style.width = props.top.width + 'px';
  dom.bottom.style.width = props.bottom.width + 'px';

  // reposition the panels
  dom.background.style.left = '0';
  dom.background.style.top = '0';
  dom.backgroundVertical.style.left = props.left.width + props.border.left + 'px';
  dom.backgroundVertical.style.top = '0';
  dom.backgroundHorizontal.style.left = '0';
  dom.backgroundHorizontal.style.top = props.top.height + 'px';
  dom.centerContainer.style.left = props.left.width + 'px';
  dom.centerContainer.style.top = props.top.height + 'px';
  dom.leftContainer.style.left = '0';
  dom.leftContainer.style.top = props.top.height + 'px';
  dom.rightContainer.style.left = props.left.width + props.center.width + 'px';
  dom.rightContainer.style.top = props.top.height + 'px';
  dom.top.style.left = props.left.width + 'px';
  dom.top.style.top = '0';
  dom.bottom.style.left = props.left.width + 'px';
  dom.bottom.style.top = props.top.height + props.centerContainer.height + 'px';

  // update the scrollTop, feasible range for the offset can be changed
  // when the height of the Core or of the contents of the center changed
  this._updateScrollTop();

  // reposition the scrollable contents
  var offset = this.props.scrollTop;
  if (options.orientation.item != 'top') {
    offset += Math.max(this.props.centerContainer.height - this.props.center.height - this.props.border.top - this.props.border.bottom, 0);
  }
  dom.center.style.left = '0';
  dom.center.style.top = offset + 'px';
  dom.left.style.left = '0';
  dom.left.style.top = offset + 'px';
  dom.right.style.left = '0';
  dom.right.style.top = offset + 'px';

  // show shadows when vertical scrolling is available
  var visibilityTop = this.props.scrollTop == 0 ? 'hidden' : '';
  var visibilityBottom = this.props.scrollTop == this.props.scrollTopMin ? 'hidden' : '';
  dom.shadowTop.style.visibility = visibilityTop;
  dom.shadowBottom.style.visibility = visibilityBottom;
  dom.shadowTopLeft.style.visibility = visibilityTop;
  dom.shadowBottomLeft.style.visibility = visibilityBottom;
  dom.shadowTopRight.style.visibility = visibilityTop;
  dom.shadowBottomRight.style.visibility = visibilityBottom;

  // redraw all components
  this.components.forEach(function (component) {
    resized = component.redraw() || resized;
  });
  if (resized) {
    // keep repainting until all sizes are settled
    var MAX_REDRAWS = 3; // maximum number of consecutive redraws
    if (this.redrawCount < MAX_REDRAWS) {
      this.redrawCount++;
      this._redraw();
    } else {
      console.log('WARNING: infinite loop in redraw?');
    }
    this.redrawCount = 0;
  }
};

// TODO: deprecated since version 1.1.0, remove some day
Core.prototype.repaint = function () {
  throw new Error('Function repaint is deprecated. Use redraw instead.');
};

/**
 * Set a current time. This can be used for example to ensure that a client's
 * time is synchronized with a shared server time.
 * Only applicable when option `showCurrentTime` is true.
 * @param {Date | String | Number} time     A Date, unix timestamp, or
 *                                          ISO date string.
 */
Core.prototype.setCurrentTime = function (time) {
  if (!this.currentTime) {
    throw new Error('Option showCurrentTime must be true');
  }

  this.currentTime.setCurrentTime(time);
};

/**
 * Get the current time.
 * Only applicable when option `showCurrentTime` is true.
 * @return {Date} Returns the current time.
 */
Core.prototype.getCurrentTime = function () {
  if (!this.currentTime) {
    throw new Error('Option showCurrentTime must be true');
  }

  return this.currentTime.getCurrentTime();
};

/**
 * Convert a position on screen (pixels) to a datetime
 * @param {int}     x    Position on the screen in pixels
 * @return {Date}   time The datetime the corresponds with given position x
 * @protected
 */
// TODO: move this function to Range
Core.prototype._toTime = function (x) {
  return DateUtil.toTime(this, x, this.props.center.width);
};

/**
 * Convert a position on the global screen (pixels) to a datetime
 * @param {int}     x    Position on the screen in pixels
 * @return {Date}   time The datetime the corresponds with given position x
 * @protected
 */
// TODO: move this function to Range
Core.prototype._toGlobalTime = function (x) {
  return DateUtil.toTime(this, x, this.props.root.width);
  //var conversion = this.range.conversion(this.props.root.width);
  //return new Date(x / conversion.scale + conversion.offset);
};

/**
 * Convert a datetime (Date object) into a position on the screen
 * @param {Date}   time A date
 * @return {int}   x    The position on the screen in pixels which corresponds
 *                      with the given date.
 * @protected
 */
// TODO: move this function to Range
Core.prototype._toScreen = function (time) {
  return DateUtil.toScreen(this, time, this.props.center.width);
};

/**
 * Convert a datetime (Date object) into a position on the root
 * This is used to get the pixel density estimate for the screen, not the center panel
 * @param {Date}   time A date
 * @return {int}   x    The position on root in pixels which corresponds
 *                      with the given date.
 * @protected
 */
// TODO: move this function to Range
Core.prototype._toGlobalScreen = function (time) {
  return DateUtil.toScreen(this, time, this.props.root.width);
  //var conversion = this.range.conversion(this.props.root.width);
  //return (time.valueOf() - conversion.offset) * conversion.scale;
};

/**
 * Initialize watching when option autoResize is true
 * @private
 */
Core.prototype._initAutoResize = function () {
  if (this.options.autoResize == true) {
    this._startAutoResize();
  } else {
    this._stopAutoResize();
  }
};

/**
 * Watch for changes in the size of the container. On resize, the Panel will
 * automatically redraw itself.
 * @private
 */
Core.prototype._startAutoResize = function () {
  var me = this;

  this._stopAutoResize();

  this._onResize = function () {
    if (me.options.autoResize != true) {
      // stop watching when the option autoResize is changed to false
      me._stopAutoResize();
      return;
    }

    if (me.dom.root) {
      // check whether the frame is resized
      // Note: we compare offsetWidth here, not clientWidth. For some reason,
      // IE does not restore the clientWidth from 0 to the actual width after
      // changing the timeline's container display style from none to visible
      if (me.dom.root.offsetWidth != me.props.lastWidth || me.dom.root.offsetHeight != me.props.lastHeight) {
        me.props.lastWidth = me.dom.root.offsetWidth;
        me.props.lastHeight = me.dom.root.offsetHeight;

        me.emit('change');
      }
    }
  };

  // add event listener to window resize
  util.addEventListener(window, 'resize', this._onResize);

  this.watchTimer = setInterval(this._onResize, 1000);
};

/**
 * Stop watching for a resize of the frame.
 * @private
 */
Core.prototype._stopAutoResize = function () {
  if (this.watchTimer) {
    clearInterval(this.watchTimer);
    this.watchTimer = undefined;
  }

  // remove event listener on window.resize
  if (this._onResize) {
    util.removeEventListener(window, 'resize', this._onResize);
    this._onResize = null;
  }
};

/**
 * Start moving the timeline vertically
 * @param {Event} event
 * @private
 */
Core.prototype._onTouch = function (event) {
  this.touch.allowDragging = true;
  this.touch.initialScrollTop = this.props.scrollTop;
};

/**
 * Start moving the timeline vertically
 * @param {Event} event
 * @private
 */
Core.prototype._onPinch = function (event) {
  this.touch.allowDragging = false;
};

/**
 * Move the timeline vertically
 * @param {Event} event
 * @private
 */
Core.prototype._onDrag = function (event) {
  // refuse to drag when we where pinching to prevent the timeline make a jump
  // when releasing the fingers in opposite order from the touch screen
  if (!this.touch.allowDragging) return;

  var delta = event.deltaY;

  var oldScrollTop = this._getScrollTop();
  var newScrollTop = this._setScrollTop(this.touch.initialScrollTop + delta);

  if (newScrollTop != oldScrollTop) {
    this._redraw(); // TODO: this causes two redraws when dragging, the other is triggered by rangechange already
    this.emit("verticalDrag");
  }
};

/**
 * Apply a scrollTop
 * @param {Number} scrollTop
 * @returns {Number} scrollTop  Returns the applied scrollTop
 * @private
 */
Core.prototype._setScrollTop = function (scrollTop) {
  this.props.scrollTop = scrollTop;
  this._updateScrollTop();
  return this.props.scrollTop;
};

/**
 * Update the current scrollTop when the height of  the containers has been changed
 * @returns {Number} scrollTop  Returns the applied scrollTop
 * @private
 */
Core.prototype._updateScrollTop = function () {
  // recalculate the scrollTopMin
  var scrollTopMin = Math.min(this.props.centerContainer.height - this.props.center.height, 0); // is negative or zero
  if (scrollTopMin != this.props.scrollTopMin) {
    // in case of bottom orientation, change the scrollTop such that the contents
    // do not move relative to the time axis at the bottom
    if (this.options.orientation.item != 'top') {
      this.props.scrollTop += scrollTopMin - this.props.scrollTopMin;
    }
    this.props.scrollTopMin = scrollTopMin;
  }

  // limit the scrollTop to the feasible scroll range
  if (this.props.scrollTop > 0) this.props.scrollTop = 0;
  if (this.props.scrollTop < scrollTopMin) this.props.scrollTop = scrollTopMin;

  return this.props.scrollTop;
};

/**
 * Get the current scrollTop
 * @returns {number} scrollTop
 * @private
 */
Core.prototype._getScrollTop = function () {
  return this.props.scrollTop;
};

/**
 * Load a configurator
 * @return {Object}
 * @private
 */
Core.prototype._createConfigurator = function () {
  throw new Error('Cannot invoke abstract method _createConfigurator');
};

module.exports = Core;

},{"../DataSet":2,"../DataView":3,"../hammerUtil":5,"../module/hammer":6,"../shared/Activator":9,"../util":33,"./DateUtil":14,"./Range":15,"./component/CustomTime":22,"./component/ItemSet":24,"./component/TimeAxis":25,"emitter-component":34}],14:[function(require,module,exports){

/**
 * used in Core to convert the options into a volatile variable
 * 
 * @param {function} moment
 * @param {Object} body
 * @param {Array | Object} hiddenDates
 */
"use strict";

exports.convertHiddenOptions = function (moment, body, hiddenDates) {
  if (hiddenDates && !Array.isArray(hiddenDates)) {
    return exports.convertHiddenOptions(moment, body, [hiddenDates]);
  }

  body.hiddenDates = [];
  if (hiddenDates) {
    if (Array.isArray(hiddenDates) == true) {
      for (var i = 0; i < hiddenDates.length; i++) {
        if (hiddenDates[i].repeat === undefined) {
          var dateItem = {};
          dateItem.start = moment(hiddenDates[i].start).toDate().valueOf();
          dateItem.end = moment(hiddenDates[i].end).toDate().valueOf();
          body.hiddenDates.push(dateItem);
        }
      }
      body.hiddenDates.sort(function (a, b) {
        return a.start - b.start;
      }); // sort by start time
    }
  }
};

/**
 * create new entrees for the repeating hidden dates
 * @param {function} moment
 * @param {Object} body
 * @param {Array | Object} hiddenDates
 */
exports.updateHiddenDates = function (moment, body, hiddenDates) {
  if (hiddenDates && !Array.isArray(hiddenDates)) {
    return exports.updateHiddenDates(moment, body, [hiddenDates]);
  }

  if (hiddenDates && body.domProps.centerContainer.width !== undefined) {
    exports.convertHiddenOptions(moment, body, hiddenDates);

    var start = moment(body.range.start);
    var end = moment(body.range.end);

    var totalRange = body.range.end - body.range.start;
    var pixelTime = totalRange / body.domProps.centerContainer.width;

    for (var i = 0; i < hiddenDates.length; i++) {
      if (hiddenDates[i].repeat !== undefined) {
        var startDate = moment(hiddenDates[i].start);
        var endDate = moment(hiddenDates[i].end);

        if (startDate._d == "Invalid Date") {
          throw new Error("Supplied start date is not valid: " + hiddenDates[i].start);
        }
        if (endDate._d == "Invalid Date") {
          throw new Error("Supplied end date is not valid: " + hiddenDates[i].end);
        }

        var duration = endDate - startDate;
        if (duration >= 4 * pixelTime) {

          var offset = 0;
          var runUntil = end.clone();
          switch (hiddenDates[i].repeat) {
            case "daily":
              // case of time
              if (startDate.day() != endDate.day()) {
                offset = 1;
              }
              startDate.dayOfYear(start.dayOfYear());
              startDate.year(start.year());
              startDate.subtract(7, 'days');

              endDate.dayOfYear(start.dayOfYear());
              endDate.year(start.year());
              endDate.subtract(7 - offset, 'days');

              runUntil.add(1, 'weeks');
              break;
            case "weekly":
              var dayOffset = endDate.diff(startDate, 'days');
              var day = startDate.day();

              // set the start date to the range.start
              startDate.date(start.date());
              startDate.month(start.month());
              startDate.year(start.year());
              endDate = startDate.clone();

              // force
              startDate.day(day);
              endDate.day(day);
              endDate.add(dayOffset, 'days');

              startDate.subtract(1, 'weeks');
              endDate.subtract(1, 'weeks');

              runUntil.add(1, 'weeks');
              break;
            case "monthly":
              if (startDate.month() != endDate.month()) {
                offset = 1;
              }
              startDate.month(start.month());
              startDate.year(start.year());
              startDate.subtract(1, 'months');

              endDate.month(start.month());
              endDate.year(start.year());
              endDate.subtract(1, 'months');
              endDate.add(offset, 'months');

              runUntil.add(1, 'months');
              break;
            case "yearly":
              if (startDate.year() != endDate.year()) {
                offset = 1;
              }
              startDate.year(start.year());
              startDate.subtract(1, 'years');
              endDate.year(start.year());
              endDate.subtract(1, 'years');
              endDate.add(offset, 'years');

              runUntil.add(1, 'years');
              break;
            default:
              console.log("Wrong repeat format, allowed are: daily, weekly, monthly, yearly. Given:", hiddenDates[i].repeat);
              return;
          }
          while (startDate < runUntil) {
            body.hiddenDates.push({ start: startDate.valueOf(), end: endDate.valueOf() });
            switch (hiddenDates[i].repeat) {
              case "daily":
                startDate.add(1, 'days');
                endDate.add(1, 'days');
                break;
              case "weekly":
                startDate.add(1, 'weeks');
                endDate.add(1, 'weeks');
                break;
              case "monthly":
                startDate.add(1, 'months');
                endDate.add(1, 'months');
                break;
              case "yearly":
                startDate.add(1, 'y');
                endDate.add(1, 'y');
                break;
              default:
                console.log("Wrong repeat format, allowed are: daily, weekly, monthly, yearly. Given:", hiddenDates[i].repeat);
                return;
            }
          }
          body.hiddenDates.push({ start: startDate.valueOf(), end: endDate.valueOf() });
        }
      }
    }
    // remove duplicates, merge where possible
    exports.removeDuplicates(body);
    // ensure the new positions are not on hidden dates
    var startHidden = exports.isHidden(body.range.start, body.hiddenDates);
    var endHidden = exports.isHidden(body.range.end, body.hiddenDates);
    var rangeStart = body.range.start;
    var rangeEnd = body.range.end;
    if (startHidden.hidden == true) {
      rangeStart = body.range.startToFront == true ? startHidden.startDate - 1 : startHidden.endDate + 1;
    }
    if (endHidden.hidden == true) {
      rangeEnd = body.range.endToFront == true ? endHidden.startDate - 1 : endHidden.endDate + 1;
    }
    if (startHidden.hidden == true || endHidden.hidden == true) {
      body.range._applyRange(rangeStart, rangeEnd);
    }
  }
};

/**
 * remove duplicates from the hidden dates list. Duplicates are evil. They mess everything up.
 * Scales with N^2
 * @param body
 */
exports.removeDuplicates = function (body) {
  var hiddenDates = body.hiddenDates;
  var safeDates = [];
  for (var i = 0; i < hiddenDates.length; i++) {
    for (var j = 0; j < hiddenDates.length; j++) {
      if (i != j && hiddenDates[j].remove != true && hiddenDates[i].remove != true) {
        // j inside i
        if (hiddenDates[j].start >= hiddenDates[i].start && hiddenDates[j].end <= hiddenDates[i].end) {
          hiddenDates[j].remove = true;
        }
        // j start inside i
        else if (hiddenDates[j].start >= hiddenDates[i].start && hiddenDates[j].start <= hiddenDates[i].end) {
            hiddenDates[i].end = hiddenDates[j].end;
            hiddenDates[j].remove = true;
          }
          // j end inside i
          else if (hiddenDates[j].end >= hiddenDates[i].start && hiddenDates[j].end <= hiddenDates[i].end) {
              hiddenDates[i].start = hiddenDates[j].start;
              hiddenDates[j].remove = true;
            }
      }
    }
  }

  for (var i = 0; i < hiddenDates.length; i++) {
    if (hiddenDates[i].remove !== true) {
      safeDates.push(hiddenDates[i]);
    }
  }

  body.hiddenDates = safeDates;
  body.hiddenDates.sort(function (a, b) {
    return a.start - b.start;
  }); // sort by start time
};

exports.printDates = function (dates) {
  for (var i = 0; i < dates.length; i++) {
    console.log(i, new Date(dates[i].start), new Date(dates[i].end), dates[i].start, dates[i].end, dates[i].remove);
  }
};

/**
 * Used in TimeStep to avoid the hidden times.
 * @param {function} moment
 * @param {TimeStep} timeStep
 * @param previousTime
 */
exports.stepOverHiddenDates = function (moment, timeStep, previousTime) {
  var stepInHidden = false;
  var currentValue = timeStep.current.valueOf();
  for (var i = 0; i < timeStep.hiddenDates.length; i++) {
    var startDate = timeStep.hiddenDates[i].start;
    var endDate = timeStep.hiddenDates[i].end;
    if (currentValue >= startDate && currentValue < endDate) {
      stepInHidden = true;
      break;
    }
  }

  if (stepInHidden == true && currentValue < timeStep._end.valueOf() && currentValue != previousTime) {
    var prevValue = moment(previousTime);
    var newValue = moment(endDate);
    //check if the next step should be major
    if (prevValue.year() != newValue.year()) {
      timeStep.switchedYear = true;
    } else if (prevValue.month() != newValue.month()) {
      timeStep.switchedMonth = true;
    } else if (prevValue.dayOfYear() != newValue.dayOfYear()) {
      timeStep.switchedDay = true;
    }

    timeStep.current = newValue;
  }
};

///**
// * Used in TimeStep to avoid the hidden times.
// * @param timeStep
// * @param previousTime
// */
//exports.checkFirstStep = function(timeStep) {
//  var stepInHidden = false;
//  var currentValue = timeStep.current.valueOf();
//  for (var i = 0; i < timeStep.hiddenDates.length; i++) {
//    var startDate = timeStep.hiddenDates[i].start;
//    var endDate = timeStep.hiddenDates[i].end;
//    if (currentValue >= startDate && currentValue < endDate) {
//      stepInHidden = true;
//      break;
//    }
//  }
//
//  if (stepInHidden == true && currentValue <= timeStep._end.valueOf()) {
//    var newValue = moment(endDate);
//    timeStep.current = newValue.toDate();
//  }
//};

/**
 * replaces the Core toScreen methods
 * @param Core
 * @param time
 * @param width
 * @returns {number}
 */
exports.toScreen = function (Core, time, width) {
  if (Core.body.hiddenDates.length == 0) {
    var conversion = Core.range.conversion(width);
    return (time.valueOf() - conversion.offset) * conversion.scale;
  } else {
    var hidden = exports.isHidden(time, Core.body.hiddenDates);
    if (hidden.hidden == true) {
      time = hidden.startDate;
    }

    var duration = exports.getHiddenDurationBetween(Core.body.hiddenDates, Core.range.start, Core.range.end);
    time = exports.correctTimeForHidden(Core.options.moment, Core.body.hiddenDates, Core.range, time);

    var conversion = Core.range.conversion(width, duration);
    return (time.valueOf() - conversion.offset) * conversion.scale;
  }
};

/**
 * Replaces the core toTime methods
 * @param body
 * @param range
 * @param x
 * @param width
 * @returns {Date}
 */
exports.toTime = function (Core, x, width) {
  if (Core.body.hiddenDates.length == 0) {
    var conversion = Core.range.conversion(width);
    return new Date(x / conversion.scale + conversion.offset);
  } else {
    var hiddenDuration = exports.getHiddenDurationBetween(Core.body.hiddenDates, Core.range.start, Core.range.end);
    var totalDuration = Core.range.end - Core.range.start - hiddenDuration;
    var partialDuration = totalDuration * x / width;
    var accumulatedHiddenDuration = exports.getAccumulatedHiddenDuration(Core.body.hiddenDates, Core.range, partialDuration);

    var newTime = new Date(accumulatedHiddenDuration + partialDuration + Core.range.start);
    return newTime;
  }
};

/**
 * Support function
 *
 * @param hiddenDates
 * @param range
 * @returns {number}
 */
exports.getHiddenDurationBetween = function (hiddenDates, start, end) {
  var duration = 0;
  for (var i = 0; i < hiddenDates.length; i++) {
    var startDate = hiddenDates[i].start;
    var endDate = hiddenDates[i].end;
    // if time after the cutout, and the
    if (startDate >= start && endDate < end) {
      duration += endDate - startDate;
    }
  }
  return duration;
};

/**
 * Support function
 * @param moment
 * @param hiddenDates
 * @param range
 * @param time
 * @returns {{duration: number, time: *, offset: number}}
 */
exports.correctTimeForHidden = function (moment, hiddenDates, range, time) {
  time = moment(time).toDate().valueOf();
  time -= exports.getHiddenDurationBefore(moment, hiddenDates, range, time);
  return time;
};

exports.getHiddenDurationBefore = function (moment, hiddenDates, range, time) {
  var timeOffset = 0;
  time = moment(time).toDate().valueOf();

  for (var i = 0; i < hiddenDates.length; i++) {
    var startDate = hiddenDates[i].start;
    var endDate = hiddenDates[i].end;
    // if time after the cutout, and the
    if (startDate >= range.start && endDate < range.end) {
      if (time >= endDate) {
        timeOffset += endDate - startDate;
      }
    }
  }
  return timeOffset;
};

/**
 * sum the duration from start to finish, including the hidden duration,
 * until the required amount has been reached, return the accumulated hidden duration
 * @param hiddenDates
 * @param range
 * @param time
 * @returns {{duration: number, time: *, offset: number}}
 */
exports.getAccumulatedHiddenDuration = function (hiddenDates, range, requiredDuration) {
  var hiddenDuration = 0;
  var duration = 0;
  var previousPoint = range.start;
  //exports.printDates(hiddenDates)
  for (var i = 0; i < hiddenDates.length; i++) {
    var startDate = hiddenDates[i].start;
    var endDate = hiddenDates[i].end;
    // if time after the cutout, and the
    if (startDate >= range.start && endDate < range.end) {
      duration += startDate - previousPoint;
      previousPoint = endDate;
      if (duration >= requiredDuration) {
        break;
      } else {
        hiddenDuration += endDate - startDate;
      }
    }
  }

  return hiddenDuration;
};

/**
 * used to step over to either side of a hidden block. Correction is disabled on tablets, might be set to true
 * @param hiddenDates
 * @param time
 * @param direction
 * @param correctionEnabled
 * @returns {*}
 */
exports.snapAwayFromHidden = function (hiddenDates, time, direction, correctionEnabled) {
  var isHidden = exports.isHidden(time, hiddenDates);
  if (isHidden.hidden == true) {
    if (direction < 0) {
      if (correctionEnabled == true) {
        return isHidden.startDate - (isHidden.endDate - time) - 1;
      } else {
        return isHidden.startDate - 1;
      }
    } else {
      if (correctionEnabled == true) {
        return isHidden.endDate + (time - isHidden.startDate) + 1;
      } else {
        return isHidden.endDate + 1;
      }
    }
  } else {
    return time;
  }
};

/**
 * Check if a time is hidden
 *
 * @param time
 * @param hiddenDates
 * @returns {{hidden: boolean, startDate: Window.start, endDate: *}}
 */
exports.isHidden = function (time, hiddenDates) {
  for (var i = 0; i < hiddenDates.length; i++) {
    var startDate = hiddenDates[i].start;
    var endDate = hiddenDates[i].end;

    if (time >= startDate && time < endDate) {
      // if the start is entering a hidden zone
      return { hidden: true, startDate: startDate, endDate: endDate };
      break;
    }
  }
  return { hidden: false, startDate: startDate, endDate: endDate };
};

},{}],15:[function(require,module,exports){
'use strict';

var util = require('../util');
var hammerUtil = require('../hammerUtil');
var moment = require('../module/moment');
var Component = require('./component/Component');
var DateUtil = require('./DateUtil');

/**
 * @constructor Range
 * A Range controls a numeric range with a start and end value.
 * The Range adjusts the range based on mouse events or programmatic changes,
 * and triggers events when the range is changing or has been changed.
 * @param {{dom: Object, domProps: Object, emitter: Emitter}} body
 * @param {Object} [options]    See description at Range.setOptions
 */
function Range(body, options) {
  var now = moment().hours(0).minutes(0).seconds(0).milliseconds(0);
  this.start = now.clone().add(-3, 'days').valueOf(); // Number
  this.end = now.clone().add(4, 'days').valueOf(); // Number

  this.body = body;
  this.deltaDifference = 0;
  this.scaleOffset = 0;
  this.startToFront = false;
  this.endToFront = true;

  // default options
  this.defaultOptions = {
    start: null,
    end: null,
    moment: moment,
    direction: 'horizontal', // 'horizontal' or 'vertical'
    moveable: true,
    zoomable: true,
    min: null,
    max: null,
    zoomMin: 10, // milliseconds
    zoomMax: 1000 * 60 * 60 * 24 * 365 * 10000 // milliseconds
  };
  this.options = util.extend({}, this.defaultOptions);

  this.props = {
    touch: {}
  };
  this.animationTimer = null;

  // drag listeners for dragging
  this.body.emitter.on('panstart', this._onDragStart.bind(this));
  this.body.emitter.on('panmove', this._onDrag.bind(this));
  this.body.emitter.on('panend', this._onDragEnd.bind(this));

  // mouse wheel for zooming
  this.body.emitter.on('mousewheel', this._onMouseWheel.bind(this));

  // pinch to zoom
  this.body.emitter.on('touch', this._onTouch.bind(this));
  this.body.emitter.on('pinch', this._onPinch.bind(this));

  this.setOptions(options);
}

Range.prototype = new Component();

/**
 * Set options for the range controller
 * @param {Object} options      Available options:
 *                              {Number | Date | String} start  Start date for the range
 *                              {Number | Date | String} end    End date for the range
 *                              {Number} min    Minimum value for start
 *                              {Number} max    Maximum value for end
 *                              {Number} zoomMin    Set a minimum value for
 *                                                  (end - start).
 *                              {Number} zoomMax    Set a maximum value for
 *                                                  (end - start).
 *                              {Boolean} moveable Enable moving of the range
 *                                                 by dragging. True by default
 *                              {Boolean} zoomable Enable zooming of the range
 *                                                 by pinching/scrolling. True by default
 */
Range.prototype.setOptions = function (options) {
  if (options) {
    // copy the options that we know
    var fields = ['direction', 'min', 'max', 'zoomMin', 'zoomMax', 'moveable', 'zoomable', 'moment', 'activate', 'hiddenDates', 'zoomKey'];
    util.selectiveExtend(fields, this.options, options);

    if ('start' in options || 'end' in options) {
      // apply a new range. both start and end are optional
      this.setRange(options.start, options.end);
    }
  }
};

/**
 * Test whether direction has a valid value
 * @param {String} direction    'horizontal' or 'vertical'
 */
function validateDirection(direction) {
  if (direction != 'horizontal' && direction != 'vertical') {
    throw new TypeError('Unknown direction "' + direction + '". ' + 'Choose "horizontal" or "vertical".');
  }
}

/**
 * Set a new start and end range
 * @param {Date | Number | String} [start]
 * @param {Date | Number | String} [end]
 * @param {boolean | {duration: number, easingFunction: string}} [animation=false]
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 * @param {Boolean} [byUser=false]
 *
 */
Range.prototype.setRange = function (start, end, animation, byUser) {
  if (byUser !== true) {
    byUser = false;
  }
  var finalStart = start != undefined ? util.convert(start, 'Date').valueOf() : null;
  var finalEnd = end != undefined ? util.convert(end, 'Date').valueOf() : null;
  this._cancelAnimation();

  if (animation) {
    // true or an Object
    var me = this;
    var initStart = this.start;
    var initEnd = this.end;
    var duration = typeof animation === 'object' && 'duration' in animation ? animation.duration : 500;
    var easingName = typeof animation === 'object' && 'easingFunction' in animation ? animation.easingFunction : 'easeInOutQuad';
    var easingFunction = util.easingFunctions[easingName];
    if (!easingFunction) {
      throw new Error('Unknown easing function ' + JSON.stringify(easingName) + '. ' + 'Choose from: ' + Object.keys(util.easingFunctions).join(', '));
    }

    var initTime = new Date().valueOf();
    var anyChanged = false;

    var next = function next() {
      if (!me.props.touch.dragging) {
        var now = new Date().valueOf();
        var time = now - initTime;
        var ease = easingFunction(time / duration);
        var done = time > duration;
        var s = done || finalStart === null ? finalStart : initStart + (finalStart - initStart) * ease;
        var e = done || finalEnd === null ? finalEnd : initEnd + (finalEnd - initEnd) * ease;

        changed = me._applyRange(s, e);
        DateUtil.updateHiddenDates(me.options.moment, me.body, me.options.hiddenDates);
        anyChanged = anyChanged || changed;
        if (changed) {
          me.body.emitter.emit('rangechange', { start: new Date(me.start), end: new Date(me.end), byUser: byUser });
        }

        if (done) {
          if (anyChanged) {
            me.body.emitter.emit('rangechanged', { start: new Date(me.start), end: new Date(me.end), byUser: byUser });
          }
        } else {
          // animate with as high as possible frame rate, leave 20 ms in between
          // each to prevent the browser from blocking
          me.animationTimer = setTimeout(next, 20);
        }
      }
    };

    return next();
  } else {
    var changed = this._applyRange(finalStart, finalEnd);
    DateUtil.updateHiddenDates(this.options.moment, this.body, this.options.hiddenDates);
    if (changed) {
      var params = { start: new Date(this.start), end: new Date(this.end), byUser: byUser };
      this.body.emitter.emit('rangechange', params);
      this.body.emitter.emit('rangechanged', params);
    }
  }
};

/**
 * Stop an animation
 * @private
 */
Range.prototype._cancelAnimation = function () {
  if (this.animationTimer) {
    clearTimeout(this.animationTimer);
    this.animationTimer = null;
  }
};

/**
 * Set a new start and end range. This method is the same as setRange, but
 * does not trigger a range change and range changed event, and it returns
 * true when the range is changed
 * @param {Number} [start]
 * @param {Number} [end]
 * @return {Boolean} changed
 * @private
 */
Range.prototype._applyRange = function (start, end) {
  var newStart = start != null ? util.convert(start, 'Date').valueOf() : this.start,
      newEnd = end != null ? util.convert(end, 'Date').valueOf() : this.end,
      max = this.options.max != null ? util.convert(this.options.max, 'Date').valueOf() : null,
      min = this.options.min != null ? util.convert(this.options.min, 'Date').valueOf() : null,
      diff;

  // check for valid number
  if (isNaN(newStart) || newStart === null) {
    throw new Error('Invalid start "' + start + '"');
  }
  if (isNaN(newEnd) || newEnd === null) {
    throw new Error('Invalid end "' + end + '"');
  }

  // prevent start < end
  if (newEnd < newStart) {
    newEnd = newStart;
  }

  // prevent start < min
  if (min !== null) {
    if (newStart < min) {
      diff = min - newStart;
      newStart += diff;
      newEnd += diff;

      // prevent end > max
      if (max != null) {
        if (newEnd > max) {
          newEnd = max;
        }
      }
    }
  }

  // prevent end > max
  if (max !== null) {
    if (newEnd > max) {
      diff = newEnd - max;
      newStart -= diff;
      newEnd -= diff;

      // prevent start < min
      if (min != null) {
        if (newStart < min) {
          newStart = min;
        }
      }
    }
  }

  // prevent (end-start) < zoomMin
  if (this.options.zoomMin !== null) {
    var zoomMin = parseFloat(this.options.zoomMin);
    if (zoomMin < 0) {
      zoomMin = 0;
    }
    if (newEnd - newStart < zoomMin) {
      if (this.end - this.start === zoomMin && newStart > this.start && newEnd < this.end) {
        // ignore this action, we are already zoomed to the minimum
        newStart = this.start;
        newEnd = this.end;
      } else {
        // zoom to the minimum
        diff = zoomMin - (newEnd - newStart);
        newStart -= diff / 2;
        newEnd += diff / 2;
      }
    }
  }

  // prevent (end-start) > zoomMax
  if (this.options.zoomMax !== null) {
    var zoomMax = parseFloat(this.options.zoomMax);
    if (zoomMax < 0) {
      zoomMax = 0;
    }

    if (newEnd - newStart > zoomMax) {
      if (this.end - this.start === zoomMax && newStart < this.start && newEnd > this.end) {
        // ignore this action, we are already zoomed to the maximum
        newStart = this.start;
        newEnd = this.end;
      } else {
        // zoom to the maximum
        diff = newEnd - newStart - zoomMax;
        newStart += diff / 2;
        newEnd -= diff / 2;
      }
    }
  }

  var changed = this.start != newStart || this.end != newEnd;

  // if the new range does NOT overlap with the old range, emit checkRangedItems to avoid not showing ranged items (ranged meaning has end time, not necessarily of type Range)
  if (!(newStart >= this.start && newStart <= this.end || newEnd >= this.start && newEnd <= this.end) && !(this.start >= newStart && this.start <= newEnd || this.end >= newStart && this.end <= newEnd)) {
    this.body.emitter.emit('checkRangedItems');
  }

  this.start = newStart;
  this.end = newEnd;
  return changed;
};

/**
 * Retrieve the current range.
 * @return {Object} An object with start and end properties
 */
Range.prototype.getRange = function () {
  return {
    start: this.start,
    end: this.end
  };
};

/**
 * Calculate the conversion offset and scale for current range, based on
 * the provided width
 * @param {Number} width
 * @returns {{offset: number, scale: number}} conversion
 */
Range.prototype.conversion = function (width, totalHidden) {
  return Range.conversion(this.start, this.end, width, totalHidden);
};

/**
 * Static method to calculate the conversion offset and scale for a range,
 * based on the provided start, end, and width
 * @param {Number} start
 * @param {Number} end
 * @param {Number} width
 * @returns {{offset: number, scale: number}} conversion
 */
Range.conversion = function (start, end, width, totalHidden) {
  if (totalHidden === undefined) {
    totalHidden = 0;
  }
  if (width != 0 && end - start != 0) {
    return {
      offset: start,
      scale: width / (end - start - totalHidden)
    };
  } else {
    return {
      offset: 0,
      scale: 1
    };
  }
};

/**
 * Start dragging horizontally or vertically
 * @param {Event} event
 * @private
 */
Range.prototype._onDragStart = function (event) {
  this.deltaDifference = 0;
  this.previousDelta = 0;

  // only allow dragging when configured as movable
  if (!this.options.moveable) return;

  // only start dragging when the mouse is inside the current range
  if (!this._isInsideRange(event)) return;

  // refuse to drag when we where pinching to prevent the timeline make a jump
  // when releasing the fingers in opposite order from the touch screen
  if (!this.props.touch.allowDragging) return;

  this.props.touch.start = this.start;
  this.props.touch.end = this.end;
  this.props.touch.dragging = true;

  if (this.body.dom.root) {
    this.body.dom.root.style.cursor = 'move';
  }
};

/**
 * Perform dragging operation
 * @param {Event} event
 * @private
 */
Range.prototype._onDrag = function (event) {
  if (!this.props.touch.dragging) return;

  // only allow dragging when configured as movable
  if (!this.options.moveable) return;

  // TODO: this may be redundant in hammerjs2
  // refuse to drag when we where pinching to prevent the timeline make a jump
  // when releasing the fingers in opposite order from the touch screen
  if (!this.props.touch.allowDragging) return;

  var direction = this.options.direction;
  validateDirection(direction);
  var delta = direction == 'horizontal' ? event.deltaX : event.deltaY;
  delta -= this.deltaDifference;
  var interval = this.props.touch.end - this.props.touch.start;

  // normalize dragging speed if cutout is in between.
  var duration = DateUtil.getHiddenDurationBetween(this.body.hiddenDates, this.start, this.end);
  interval -= duration;

  var width = direction == 'horizontal' ? this.body.domProps.center.width : this.body.domProps.center.height;
  var diffRange = -delta / width * interval;
  var newStart = this.props.touch.start + diffRange;
  var newEnd = this.props.touch.end + diffRange;

  // snapping times away from hidden zones
  var safeStart = DateUtil.snapAwayFromHidden(this.body.hiddenDates, newStart, this.previousDelta - delta, true);
  var safeEnd = DateUtil.snapAwayFromHidden(this.body.hiddenDates, newEnd, this.previousDelta - delta, true);
  if (safeStart != newStart || safeEnd != newEnd) {
    this.deltaDifference += delta;
    this.props.touch.start = safeStart;
    this.props.touch.end = safeEnd;
    this._onDrag(event);
    return;
  }

  this.previousDelta = delta;
  this._applyRange(newStart, newEnd);

  // fire a rangechange event
  this.body.emitter.emit('rangechange', {
    start: new Date(this.start),
    end: new Date(this.end),
    byUser: true
  });
};

/**
 * Stop dragging operation
 * @param {event} event
 * @private
 */
Range.prototype._onDragEnd = function (event) {
  if (!this.props.touch.dragging) return;

  // only allow dragging when configured as movable
  if (!this.options.moveable) return;

  // TODO: this may be redundant in hammerjs2
  // refuse to drag when we where pinching to prevent the timeline make a jump
  // when releasing the fingers in opposite order from the touch screen
  if (!this.props.touch.allowDragging) return;

  this.props.touch.dragging = false;
  if (this.body.dom.root) {
    this.body.dom.root.style.cursor = 'auto';
  }

  // fire a rangechanged event
  this.body.emitter.emit('rangechanged', {
    start: new Date(this.start),
    end: new Date(this.end),
    byUser: true
  });
};

/**
 * Event handler for mouse wheel event, used to zoom
 * Code from http://adomas.org/javascript-mouse-wheel/
 * @param {Event} event
 * @private
 */
Range.prototype._onMouseWheel = function (event) {
  // only allow zooming when configured as zoomable and moveable
  if (!(this.options.zoomable && this.options.moveable)) return;

  // only zoom when the mouse is inside the current range
  if (!this._isInsideRange(event)) return;

  // only zoom when the according key is pressed and the zoomKey option is set
  if (this.options.zoomKey && !event[this.options.zoomKey]) return;

  // retrieve delta
  var delta = 0;
  if (event.wheelDelta) {
    /* IE/Opera. */
    delta = event.wheelDelta / 120;
  } else if (event.detail) {
    /* Mozilla case. */
    // In Mozilla, sign of delta is different than in IE.
    // Also, delta is multiple of 3.
    delta = -event.detail / 3;
  }

  // If delta is nonzero, handle it.
  // Basically, delta is now positive if wheel was scrolled up,
  // and negative, if wheel was scrolled down.
  if (delta) {
    // perform the zoom action. Delta is normally 1 or -1

    // adjust a negative delta such that zooming in with delta 0.1
    // equals zooming out with a delta -0.1
    var scale;
    if (delta < 0) {
      scale = 1 - delta / 5;
    } else {
      scale = 1 / (1 + delta / 5);
    }

    // calculate center, the date to zoom around
    var pointer = getPointer({ x: event.clientX, y: event.clientY }, this.body.dom.center);
    var pointerDate = this._pointerToDate(pointer);

    this.zoom(scale, pointerDate, delta);
  }

  // Prevent default actions caused by mouse wheel
  // (else the page and timeline both zoom and scroll)
  event.preventDefault();
};

/**
 * Start of a touch gesture
 * @private
 */
Range.prototype._onTouch = function (event) {
  this.props.touch.start = this.start;
  this.props.touch.end = this.end;
  this.props.touch.allowDragging = true;
  this.props.touch.center = null;
  this.scaleOffset = 0;
  this.deltaDifference = 0;
};

/**
 * Handle pinch event
 * @param {Event} event
 * @private
 */
Range.prototype._onPinch = function (event) {
  // only allow zooming when configured as zoomable and moveable
  if (!(this.options.zoomable && this.options.moveable)) return;

  this.props.touch.allowDragging = false;

  if (!this.props.touch.center) {
    this.props.touch.center = getPointer(event.center, this.body.dom.center);
  }

  var scale = 1 / (event.scale + this.scaleOffset);
  var centerDate = this._pointerToDate(this.props.touch.center);

  var hiddenDuration = DateUtil.getHiddenDurationBetween(this.body.hiddenDates, this.start, this.end);
  var hiddenDurationBefore = DateUtil.getHiddenDurationBefore(this.options.moment, this.body.hiddenDates, this, centerDate);
  var hiddenDurationAfter = hiddenDuration - hiddenDurationBefore;

  // calculate new start and end
  var newStart = centerDate - hiddenDurationBefore + (this.props.touch.start - (centerDate - hiddenDurationBefore)) * scale;
  var newEnd = centerDate + hiddenDurationAfter + (this.props.touch.end - (centerDate + hiddenDurationAfter)) * scale;

  // snapping times away from hidden zones
  this.startToFront = 1 - scale <= 0; // used to do the right auto correction with periodic hidden times
  this.endToFront = scale - 1 <= 0; // used to do the right auto correction with periodic hidden times

  var safeStart = DateUtil.snapAwayFromHidden(this.body.hiddenDates, newStart, 1 - scale, true);
  var safeEnd = DateUtil.snapAwayFromHidden(this.body.hiddenDates, newEnd, scale - 1, true);
  if (safeStart != newStart || safeEnd != newEnd) {
    this.props.touch.start = safeStart;
    this.props.touch.end = safeEnd;
    this.scaleOffset = 1 - event.scale;
    newStart = safeStart;
    newEnd = safeEnd;
  }

  this.setRange(newStart, newEnd, false, true);

  this.startToFront = false; // revert to default
  this.endToFront = true; // revert to default
};

/**
 * Test whether the mouse from a mouse event is inside the visible window,
 * between the current start and end date
 * @param {Object} event
 * @return {boolean} Returns true when inside the visible window
 * @private
 */
Range.prototype._isInsideRange = function (event) {
  // calculate the time where the mouse is, check whether inside
  // and no scroll action should happen.
  var clientX = event.center ? event.center.x : event.clientX;
  var x = clientX - util.getAbsoluteLeft(this.body.dom.centerContainer);
  var time = this.body.util.toTime(x);

  return time >= this.start && time <= this.end;
};

/**
 * Helper function to calculate the center date for zooming
 * @param {{x: Number, y: Number}} pointer
 * @return {number} date
 * @private
 */
Range.prototype._pointerToDate = function (pointer) {
  var conversion;
  var direction = this.options.direction;

  validateDirection(direction);

  if (direction == 'horizontal') {
    return this.body.util.toTime(pointer.x).valueOf();
  } else {
    var height = this.body.domProps.center.height;
    conversion = this.conversion(height);
    return pointer.y / conversion.scale + conversion.offset;
  }
};

/**
 * Get the pointer location relative to the location of the dom element
 * @param {{x: Number, y: Number}} touch
 * @param {Element} element   HTML DOM element
 * @return {{x: Number, y: Number}} pointer
 * @private
 */
function getPointer(touch, element) {
  return {
    x: touch.x - util.getAbsoluteLeft(element),
    y: touch.y - util.getAbsoluteTop(element)
  };
}

/**
 * Zoom the range the given scale in or out. Start and end date will
 * be adjusted, and the timeline will be redrawn. You can optionally give a
 * date around which to zoom.
 * For example, try scale = 0.9 or 1.1
 * @param {Number} scale      Scaling factor. Values above 1 will zoom out,
 *                            values below 1 will zoom in.
 * @param {Number} [center]   Value representing a date around which will
 *                            be zoomed.
 */
Range.prototype.zoom = function (scale, center, delta) {
  // if centerDate is not provided, take it half between start Date and end Date
  if (center == null) {
    center = (this.start + this.end) / 2;
  }

  var hiddenDuration = DateUtil.getHiddenDurationBetween(this.body.hiddenDates, this.start, this.end);
  var hiddenDurationBefore = DateUtil.getHiddenDurationBefore(this.options.moment, this.body.hiddenDates, this, center);
  var hiddenDurationAfter = hiddenDuration - hiddenDurationBefore;

  // calculate new start and end
  var newStart = center - hiddenDurationBefore + (this.start - (center - hiddenDurationBefore)) * scale;
  var newEnd = center + hiddenDurationAfter + (this.end - (center + hiddenDurationAfter)) * scale;

  // snapping times away from hidden zones
  this.startToFront = delta > 0 ? false : true; // used to do the right autocorrection with periodic hidden times
  this.endToFront = -delta > 0 ? false : true; // used to do the right autocorrection with periodic hidden times
  var safeStart = DateUtil.snapAwayFromHidden(this.body.hiddenDates, newStart, delta, true);
  var safeEnd = DateUtil.snapAwayFromHidden(this.body.hiddenDates, newEnd, -delta, true);
  if (safeStart != newStart || safeEnd != newEnd) {
    newStart = safeStart;
    newEnd = safeEnd;
  }

  this.setRange(newStart, newEnd, false, true);

  this.startToFront = false; // revert to default
  this.endToFront = true; // revert to default
};

/**
 * Move the range with a given delta to the left or right. Start and end
 * value will be adjusted. For example, try delta = 0.1 or -0.1
 * @param {Number}  delta     Moving amount. Positive value will move right,
 *                            negative value will move left
 */
Range.prototype.move = function (delta) {
  // zoom start Date and end Date relative to the centerDate
  var diff = this.end - this.start;

  // apply new values
  var newStart = this.start + diff * delta;
  var newEnd = this.end + diff * delta;

  // TODO: reckon with min and max range

  this.start = newStart;
  this.end = newEnd;
};

/**
 * Move the range to a new center point
 * @param {Number} moveTo      New center point of the range
 */
Range.prototype.moveTo = function (moveTo) {
  var center = (this.start + this.end) / 2;

  var diff = center - moveTo;

  // calculate new start and end
  var newStart = this.start - diff;
  var newEnd = this.end - diff;

  this.setRange(newStart, newEnd);
};

module.exports = Range;

},{"../hammerUtil":5,"../module/moment":7,"../util":33,"./DateUtil":14,"./component/Component":20}],16:[function(require,module,exports){
// Utility functions for ordering and stacking of items
'use strict';

var EPSILON = 0.001; // used when checking collisions, to prevent round-off errors

/**
 * Order items by their start data
 * @param {Item[]} items
 */
exports.orderByStart = function (items) {
  items.sort(function (a, b) {
    return a.data.start - b.data.start;
  });
};

/**
 * Order items by their end date. If they have no end date, their start date
 * is used.
 * @param {Item[]} items
 */
exports.orderByEnd = function (items) {
  items.sort(function (a, b) {
    var aTime = 'end' in a.data ? a.data.end : a.data.start,
        bTime = 'end' in b.data ? b.data.end : b.data.start;

    return aTime - bTime;
  });
};

/**
 * Adjust vertical positions of the items such that they don't overlap each
 * other.
 * @param {Item[]} items
 *            All visible items
 * @param {{item: {horizontal: number, vertical: number}, axis: number}} margin
 *            Margins between items and between items and the axis.
 * @param {boolean} [force=false]
 *            If true, all items will be repositioned. If false (default), only
 *            items having a top===null will be re-stacked
 */
exports.stack = function (items, margin, force) {
  var i, iMax;

  if (force) {
    // reset top position of all items
    for (i = 0, iMax = items.length; i < iMax; i++) {
      items[i].top = null;
    }
  }

  // calculate new, non-overlapping positions
  for (i = 0, iMax = items.length; i < iMax; i++) {
    var item = items[i];
    if (item.stack && item.top === null) {
      // initialize top position
      item.top = margin.axis;

      do {
        // TODO: optimize checking for overlap. when there is a gap without items,
        //       you only need to check for items from the next item on, not from zero
        var collidingItem = null;
        for (var j = 0, jj = items.length; j < jj; j++) {
          var other = items[j];
          if (other.top !== null && other !== item && other.stack && exports.collision(item, other, margin.item)) {
            collidingItem = other;
            break;
          }
        }

        if (collidingItem != null) {
          // There is a collision. Reposition the items above the colliding element
          item.top = collidingItem.top + collidingItem.height + margin.item.vertical;
        }
      } while (collidingItem);
    }
  }
};

/**
 * Adjust vertical positions of the items without stacking them
 * @param {Item[]} items
 *            All visible items
 * @param {{item: {horizontal: number, vertical: number}, axis: number}} margin
 *            Margins between items and between items and the axis.
 */
exports.nostack = function (items, margin, subgroups) {
  var i, iMax, newTop;

  // reset top position of all items
  for (i = 0, iMax = items.length; i < iMax; i++) {
    if (items[i].data.subgroup !== undefined) {
      newTop = margin.axis;
      for (var subgroup in subgroups) {
        if (subgroups.hasOwnProperty(subgroup)) {
          if (subgroups[subgroup].visible == true && subgroups[subgroup].index < subgroups[items[i].data.subgroup].index) {
            newTop += subgroups[subgroup].height + margin.item.vertical;
          }
        }
      }
      items[i].top = newTop;
    } else {
      items[i].top = margin.axis;
    }
  }
};

/**
 * Test if the two provided items collide
 * The items must have parameters left, width, top, and height.
 * @param {Item} a          The first item
 * @param {Item} b          The second item
 * @param {{horizontal: number, vertical: number}} margin
 *                          An object containing a horizontal and vertical
 *                          minimum required margin.
 * @return {boolean}        true if a and b collide, else false
 */
exports.collision = function (a, b, margin) {
  return a.left - margin.horizontal + EPSILON < b.left + b.width && a.left + a.width + margin.horizontal - EPSILON > b.left && a.top - margin.vertical + EPSILON < b.top + b.height && a.top + a.height + margin.vertical - EPSILON > b.top;
};

},{}],17:[function(require,module,exports){
'use strict';

var moment = require('../module/moment');
var DateUtil = require('./DateUtil');
var util = require('../util');

/**
 * @constructor  TimeStep
 * The class TimeStep is an iterator for dates. You provide a start date and an
 * end date. The class itself determines the best scale (step size) based on the
 * provided start Date, end Date, and minimumStep.
 *
 * If minimumStep is provided, the step size is chosen as close as possible
 * to the minimumStep but larger than minimumStep. If minimumStep is not
 * provided, the scale is set to 1 DAY.
 * The minimumStep should correspond with the onscreen size of about 6 characters
 *
 * Alternatively, you can set a scale by hand.
 * After creation, you can initialize the class by executing first(). Then you
 * can iterate from the start date to the end date via next(). You can check if
 * the end date is reached with the function hasNext(). After each step, you can
 * retrieve the current date via getCurrent().
 * The TimeStep has scales ranging from milliseconds, seconds, minutes, hours,
 * days, to years.
 *
 * Version: 1.2
 *
 * @param {Date} [start]         The start date, for example new Date(2010, 9, 21)
 *                               or new Date(2010, 9, 21, 23, 45, 00)
 * @param {Date} [end]           The end date
 * @param {Number} [minimumStep] Optional. Minimum step size in milliseconds
 */
function TimeStep(start, end, minimumStep, hiddenDates) {
  this.moment = moment;

  // variables
  this.current = this.moment();
  this._start = this.moment();
  this._end = this.moment();

  this.autoScale = true;
  this.scale = 'day';
  this.step = 1;

  // initialize the range
  this.setRange(start, end, minimumStep);

  // hidden Dates options
  this.switchedDay = false;
  this.switchedMonth = false;
  this.switchedYear = false;
  if (Array.isArray(hiddenDates)) {
    this.hiddenDates = hiddenDates;
  } else if (hiddenDates != undefined) {
    this.hiddenDates = [hiddenDates];
  } else {
    this.hiddenDates = [];
  }

  this.format = TimeStep.FORMAT; // default formatting
}

// Time formatting
TimeStep.FORMAT = {
  minorLabels: {
    millisecond: 'SSS',
    second: 's',
    minute: 'HH:mm',
    hour: 'HH:mm',
    weekday: 'ddd D',
    day: 'D',
    month: 'MMM',
    year: 'YYYY'
  },
  majorLabels: {
    millisecond: 'HH:mm:ss',
    second: 'D MMMM HH:mm',
    minute: 'ddd D MMMM',
    hour: 'ddd D MMMM',
    weekday: 'MMMM YYYY',
    day: 'MMMM YYYY',
    month: 'YYYY',
    year: ''
  }
};

/**
 * Set custom constructor function for moment. Can be used to set dates
 * to UTC or to set a utcOffset.
 * @param {function} moment
 */
TimeStep.prototype.setMoment = function (moment) {
  this.moment = moment;

  // update the date properties, can have a new utcOffset
  this.current = this.moment(this.current);
  this._start = this.moment(this._start);
  this._end = this.moment(this._end);
};

/**
 * Set custom formatting for the minor an major labels of the TimeStep.
 * Both `minorLabels` and `majorLabels` are an Object with properties:
 * 'millisecond', 'second', 'minute', 'hour', 'weekday', 'day', 'month', 'year'.
 * @param {{minorLabels: Object, majorLabels: Object}} format
 */
TimeStep.prototype.setFormat = function (format) {
  var defaultFormat = util.deepExtend({}, TimeStep.FORMAT);
  this.format = util.deepExtend(defaultFormat, format);
};

/**
 * Set a new range
 * If minimumStep is provided, the step size is chosen as close as possible
 * to the minimumStep but larger than minimumStep. If minimumStep is not
 * provided, the scale is set to 1 DAY.
 * The minimumStep should correspond with the onscreen size of about 6 characters
 * @param {Date} [start]      The start date and time.
 * @param {Date} [end]        The end date and time.
 * @param {int} [minimumStep] Optional. Minimum step size in milliseconds
 */
TimeStep.prototype.setRange = function (start, end, minimumStep) {
  if (!(start instanceof Date) || !(end instanceof Date)) {
    throw "No legal start or end date in method setRange";
  }

  this._start = start != undefined ? this.moment(start.valueOf()) : new Date();
  this._end = end != undefined ? this.moment(end.valueOf()) : new Date();

  if (this.autoScale) {
    this.setMinimumStep(minimumStep);
  }
};

/**
 * Set the range iterator to the start date.
 */
TimeStep.prototype.start = function () {
  this.current = this._start.clone();
  this.roundToMinor();
};

/**
 * Round the current date to the first minor date value
 * This must be executed once when the current date is set to start Date
 */
TimeStep.prototype.roundToMinor = function () {
  // round to floor
  // IMPORTANT: we have no breaks in this switch! (this is no bug)
  // noinspection FallThroughInSwitchStatementJS
  switch (this.scale) {
    case 'year':
      this.current.year(this.step * Math.floor(this.current.year() / this.step));
      this.current.month(0);
    case 'month':
      this.current.date(1);
    case 'day': // intentional fall through
    case 'weekday':
      this.current.hours(0);
    case 'hour':
      this.current.minutes(0);
    case 'minute':
      this.current.seconds(0);
    case 'second':
      this.current.milliseconds(0);
    //case 'millisecond': // nothing to do for milliseconds
  }

  if (this.step != 1) {
    // round down to the first minor value that is a multiple of the current step size
    switch (this.scale) {
      case 'millisecond':
        this.current.subtract(this.current.milliseconds() % this.step, 'milliseconds');break;
      case 'second':
        this.current.subtract(this.current.seconds() % this.step, 'seconds');break;
      case 'minute':
        this.current.subtract(this.current.minutes() % this.step, 'minutes');break;
      case 'hour':
        this.current.subtract(this.current.hours() % this.step, 'hours');break;
      case 'weekday': // intentional fall through
      case 'day':
        this.current.subtract((this.current.date() - 1) % this.step, 'day');break;
      case 'month':
        this.current.subtract(this.current.month() % this.step, 'month');break;
      case 'year':
        this.current.subtract(this.current.year() % this.step, 'year');break;
      default:
        break;
    }
  }
};

/**
 * Check if the there is a next step
 * @return {boolean}  true if the current date has not passed the end date
 */
TimeStep.prototype.hasNext = function () {
  return this.current.valueOf() <= this._end.valueOf();
};

/**
 * Do the next step
 */
TimeStep.prototype.next = function () {
  var prev = this.current.valueOf();

  // Two cases, needed to prevent issues with switching daylight savings
  // (end of March and end of October)
  if (this.current.month() < 6) {
    switch (this.scale) {
      case 'millisecond':
        this.current.add(this.step, 'millisecond');break;
      case 'second':
        this.current.add(this.step, 'second');break;
      case 'minute':
        this.current.add(this.step, 'minute');break;
      case 'hour':
        this.current.add(this.step, 'hour');
        // in case of skipping an hour for daylight savings, adjust the hour again (else you get: 0h 5h 9h ... instead of 0h 4h 8h ...)
        // TODO: is this still needed now we use the function of moment.js?
        this.current.subtract(this.current.hours() % this.step, 'hour');
        break;
      case 'weekday': // intentional fall through
      case 'day':
        this.current.add(this.step, 'day');break;
      case 'month':
        this.current.add(this.step, 'month');break;
      case 'year':
        this.current.add(this.step, 'year');break;
      default:
        break;
    }
  } else {
    switch (this.scale) {
      case 'millisecond':
        this.current.add(this.step, 'millisecond');break;
      case 'second':
        this.current.add(this.step, 'second');break;
      case 'minute':
        this.current.add(this.step, 'minute');break;
      case 'hour':
        this.current.add(this.step, 'hour');break;
      case 'weekday': // intentional fall through
      case 'day':
        this.current.add(this.step, 'day');break;
      case 'month':
        this.current.add(this.step, 'month');break;
      case 'year':
        this.current.add(this.step, 'year');break;
      default:
        break;
    }
  }

  if (this.step != 1) {
    // round down to the correct major value
    switch (this.scale) {
      case 'millisecond':
        if (this.current.milliseconds() < this.step) this.current.milliseconds(0);break;
      case 'second':
        if (this.current.seconds() < this.step) this.current.seconds(0);break;
      case 'minute':
        if (this.current.minutes() < this.step) this.current.minutes(0);break;
      case 'hour':
        if (this.current.hours() < this.step) this.current.hours(0);break;
      case 'weekday': // intentional fall through
      case 'day':
        if (this.current.date() < this.step + 1) this.current.date(1);break;
      case 'month':
        if (this.current.month() < this.step) this.current.month(0);break;
      case 'year':
        break; // nothing to do for year
      default:
        break;
    }
  }

  // safety mechanism: if current time is still unchanged, move to the end
  if (this.current.valueOf() == prev) {
    this.current = this._end.clone();
  }

  DateUtil.stepOverHiddenDates(this.moment, this, prev);
};

/**
 * Get the current datetime
 * @return {Moment}  current The current date
 */
TimeStep.prototype.getCurrent = function () {
  return this.current;
};

/**
 * Set a custom scale. Autoscaling will be disabled.
 * For example setScale('minute', 5) will result
 * in minor steps of 5 minutes, and major steps of an hour.
 *
 * @param {{scale: string, step: number}} params
 *                               An object containing two properties:
 *                               - A string 'scale'. Choose from 'millisecond', 'second',
 *                                 'minute', 'hour', 'weekday', 'day', 'month', 'year'.
 *                               - A number 'step'. A step size, by default 1.
 *                                 Choose for example 1, 2, 5, or 10.
 */
TimeStep.prototype.setScale = function (params) {
  if (params && typeof params.scale == 'string') {
    this.scale = params.scale;
    this.step = params.step > 0 ? params.step : 1;
    this.autoScale = false;
  }
};

/**
 * Enable or disable autoscaling
 * @param {boolean} enable  If true, autoascaling is set true
 */
TimeStep.prototype.setAutoScale = function (enable) {
  this.autoScale = enable;
};

/**
 * Automatically determine the scale that bests fits the provided minimum step
 * @param {Number} [minimumStep]  The minimum step size in milliseconds
 */
TimeStep.prototype.setMinimumStep = function (minimumStep) {
  if (minimumStep == undefined) {
    return;
  }

  //var b = asc + ds;

  var stepYear = 1000 * 60 * 60 * 24 * 30 * 12;
  var stepMonth = 1000 * 60 * 60 * 24 * 30;
  var stepDay = 1000 * 60 * 60 * 24;
  var stepHour = 1000 * 60 * 60;
  var stepMinute = 1000 * 60;
  var stepSecond = 1000;
  var stepMillisecond = 1;

  // find the smallest step that is larger than the provided minimumStep
  if (stepYear * 1000 > minimumStep) {
    this.scale = 'year';this.step = 1000;
  }
  if (stepYear * 500 > minimumStep) {
    this.scale = 'year';this.step = 500;
  }
  if (stepYear * 100 > minimumStep) {
    this.scale = 'year';this.step = 100;
  }
  if (stepYear * 50 > minimumStep) {
    this.scale = 'year';this.step = 50;
  }
  if (stepYear * 10 > minimumStep) {
    this.scale = 'year';this.step = 10;
  }
  if (stepYear * 5 > minimumStep) {
    this.scale = 'year';this.step = 5;
  }
  if (stepYear > minimumStep) {
    this.scale = 'year';this.step = 1;
  }
  if (stepMonth * 3 > minimumStep) {
    this.scale = 'month';this.step = 3;
  }
  if (stepMonth > minimumStep) {
    this.scale = 'month';this.step = 1;
  }
  if (stepDay * 5 > minimumStep) {
    this.scale = 'day';this.step = 5;
  }
  if (stepDay * 2 > minimumStep) {
    this.scale = 'day';this.step = 2;
  }
  if (stepDay > minimumStep) {
    this.scale = 'day';this.step = 1;
  }
  if (stepDay / 2 > minimumStep) {
    this.scale = 'weekday';this.step = 1;
  }
  if (stepHour * 4 > minimumStep) {
    this.scale = 'hour';this.step = 4;
  }
  if (stepHour > minimumStep) {
    this.scale = 'hour';this.step = 1;
  }
  if (stepMinute * 15 > minimumStep) {
    this.scale = 'minute';this.step = 15;
  }
  if (stepMinute * 10 > minimumStep) {
    this.scale = 'minute';this.step = 10;
  }
  if (stepMinute * 5 > minimumStep) {
    this.scale = 'minute';this.step = 5;
  }
  if (stepMinute > minimumStep) {
    this.scale = 'minute';this.step = 1;
  }
  if (stepSecond * 15 > minimumStep) {
    this.scale = 'second';this.step = 15;
  }
  if (stepSecond * 10 > minimumStep) {
    this.scale = 'second';this.step = 10;
  }
  if (stepSecond * 5 > minimumStep) {
    this.scale = 'second';this.step = 5;
  }
  if (stepSecond > minimumStep) {
    this.scale = 'second';this.step = 1;
  }
  if (stepMillisecond * 200 > minimumStep) {
    this.scale = 'millisecond';this.step = 200;
  }
  if (stepMillisecond * 100 > minimumStep) {
    this.scale = 'millisecond';this.step = 100;
  }
  if (stepMillisecond * 50 > minimumStep) {
    this.scale = 'millisecond';this.step = 50;
  }
  if (stepMillisecond * 10 > minimumStep) {
    this.scale = 'millisecond';this.step = 10;
  }
  if (stepMillisecond * 5 > minimumStep) {
    this.scale = 'millisecond';this.step = 5;
  }
  if (stepMillisecond > minimumStep) {
    this.scale = 'millisecond';this.step = 1;
  }
};

/**
 * Snap a date to a rounded value.
 * The snap intervals are dependent on the current scale and step.
 * Static function
 * @param {Date} date    the date to be snapped.
 * @param {string} scale Current scale, can be 'millisecond', 'second',
 *                       'minute', 'hour', 'weekday, 'day', 'month', 'year'.
 * @param {number} step  Current step (1, 2, 4, 5, ...
 * @return {Date} snappedDate
 */
TimeStep.snap = function (date, scale, step) {
  var clone = moment(date);

  if (scale == 'year') {
    var year = clone.year() + Math.round(clone.month() / 12);
    clone.year(Math.round(year / step) * step);
    clone.month(0);
    clone.date(0);
    clone.hours(0);
    clone.minutes(0);
    clone.seconds(0);
    clone.milliseconds(0);
  } else if (scale == 'month') {
    if (clone.date() > 15) {
      clone.date(1);
      clone.add(1, 'month');
      // important: first set Date to 1, after that change the month.
    } else {
        clone.date(1);
      }

    clone.hours(0);
    clone.minutes(0);
    clone.seconds(0);
    clone.milliseconds(0);
  } else if (scale == 'day') {
    //noinspection FallthroughInSwitchStatementJS
    switch (step) {
      case 5:
      case 2:
        clone.hours(Math.round(clone.hours() / 24) * 24);break;
      default:
        clone.hours(Math.round(clone.hours() / 12) * 12);break;
    }
    clone.minutes(0);
    clone.seconds(0);
    clone.milliseconds(0);
  } else if (scale == 'weekday') {
    //noinspection FallthroughInSwitchStatementJS
    switch (step) {
      case 5:
      case 2:
        clone.hours(Math.round(clone.hours() / 12) * 12);break;
      default:
        clone.hours(Math.round(clone.hours() / 6) * 6);break;
    }
    clone.minutes(0);
    clone.seconds(0);
    clone.milliseconds(0);
  } else if (scale == 'hour') {
    switch (step) {
      case 4:
        clone.minutes(Math.round(clone.minutes() / 60) * 60);break;
      default:
        clone.minutes(Math.round(clone.minutes() / 30) * 30);break;
    }
    clone.seconds(0);
    clone.milliseconds(0);
  } else if (scale == 'minute') {
    //noinspection FallthroughInSwitchStatementJS
    switch (step) {
      case 15:
      case 10:
        clone.minutes(Math.round(clone.minutes() / 5) * 5);
        clone.seconds(0);
        break;
      case 5:
        clone.seconds(Math.round(clone.seconds() / 60) * 60);break;
      default:
        clone.seconds(Math.round(clone.seconds() / 30) * 30);break;
    }
    clone.milliseconds(0);
  } else if (scale == 'second') {
    //noinspection FallthroughInSwitchStatementJS
    switch (step) {
      case 15:
      case 10:
        clone.seconds(Math.round(clone.seconds() / 5) * 5);
        clone.milliseconds(0);
        break;
      case 5:
        clone.milliseconds(Math.round(clone.milliseconds() / 1000) * 1000);break;
      default:
        clone.milliseconds(Math.round(clone.milliseconds() / 500) * 500);break;
    }
  } else if (scale == 'millisecond') {
    var _step = step > 5 ? step / 2 : 1;
    clone.milliseconds(Math.round(clone.milliseconds() / _step) * _step);
  }

  return clone;
};

/**
 * Check if the current value is a major value (for example when the step
 * is DAY, a major value is each first day of the MONTH)
 * @return {boolean} true if current date is major, else false.
 */
TimeStep.prototype.isMajor = function () {
  if (this.switchedYear == true) {
    this.switchedYear = false;
    switch (this.scale) {
      case 'year':
      case 'month':
      case 'weekday':
      case 'day':
      case 'hour':
      case 'minute':
      case 'second':
      case 'millisecond':
        return true;
      default:
        return false;
    }
  } else if (this.switchedMonth == true) {
    this.switchedMonth = false;
    switch (this.scale) {
      case 'weekday':
      case 'day':
      case 'hour':
      case 'minute':
      case 'second':
      case 'millisecond':
        return true;
      default:
        return false;
    }
  } else if (this.switchedDay == true) {
    this.switchedDay = false;
    switch (this.scale) {
      case 'millisecond':
      case 'second':
      case 'minute':
      case 'hour':
        return true;
      default:
        return false;
    }
  }

  var date = this.moment(this.current);
  switch (this.scale) {
    case 'millisecond':
      return date.milliseconds() == 0;
    case 'second':
      return date.seconds() == 0;
    case 'minute':
      return date.hours() == 0 && date.minutes() == 0;
    case 'hour':
      return date.hours() == 0;
    case 'weekday': // intentional fall through
    case 'day':
      return date.date() == 1;
    case 'month':
      return date.month() == 0;
    case 'year':
      return false;
    default:
      return false;
  }
};

/**
 * Returns formatted text for the minor axislabel, depending on the current
 * date and the scale. For example when scale is MINUTE, the current time is
 * formatted as "hh:mm".
 * @param {Date} [date] custom date. if not provided, current date is taken
 */
TimeStep.prototype.getLabelMinor = function (date) {
  if (date == undefined) {
    date = this.current;
  }

  var format = this.format.minorLabels[this.scale];
  return format && format.length > 0 ? this.moment(date).format(format) : '';
};

/**
 * Returns formatted text for the major axis label, depending on the current
 * date and the scale. For example when scale is MINUTE, the major scale is
 * hours, and the hour will be formatted as "hh".
 * @param {Date} [date] custom date. if not provided, current date is taken
 */
TimeStep.prototype.getLabelMajor = function (date) {
  if (date == undefined) {
    date = this.current;
  }

  var format = this.format.majorLabels[this.scale];
  return format && format.length > 0 ? this.moment(date).format(format) : '';
};

TimeStep.prototype.getClassName = function () {
  var _moment = this.moment;
  var m = this.moment(this.current);
  var current = m.locale ? m.locale('en') : m.lang('en'); // old versions of moment have .lang() function
  var step = this.step;

  function even(value) {
    return value / step % 2 == 0 ? ' vis-even' : ' vis-odd';
  }

  function today(date) {
    if (date.isSame(new Date(), 'day')) {
      return ' vis-today';
    }
    if (date.isSame(_moment().add(1, 'day'), 'day')) {
      return ' vis-tomorrow';
    }
    if (date.isSame(_moment().add(-1, 'day'), 'day')) {
      return ' vis-yesterday';
    }
    return '';
  }

  function currentWeek(date) {
    return date.isSame(new Date(), 'week') ? ' vis-current-week' : '';
  }

  function currentMonth(date) {
    return date.isSame(new Date(), 'month') ? ' vis-current-month' : '';
  }

  function currentYear(date) {
    return date.isSame(new Date(), 'year') ? ' vis-current-year' : '';
  }

  switch (this.scale) {
    case 'millisecond':
      return even(current.milliseconds()).trim();

    case 'second':
      return even(current.seconds()).trim();

    case 'minute':
      return even(current.minutes()).trim();

    case 'hour':
      var hours = current.hours();
      if (this.step == 4) {
        hours = hours + '-h' + (hours + 4);
      }
      return 'vis-h' + hours + today(current) + even(current.hours());

    case 'weekday':
      return 'vis-' + current.format('dddd').toLowerCase() + today(current) + currentWeek(current) + even(current.date());

    case 'day':
      var day = current.date();
      var month = current.format('MMMM').toLowerCase();
      return 'vis-day' + day + ' vis-' + month + currentMonth(current) + even(day - 1);

    case 'month':
      return 'vis-' + current.format('MMMM').toLowerCase() + currentMonth(current) + even(current.month());

    case 'year':
      var year = current.year();
      return 'vis-year' + year + currentYear(current) + even(year);

    default:
      return '';
  }
};

module.exports = TimeStep;

},{"../module/moment":7,"../util":33,"./DateUtil":14}],18:[function(require,module,exports){
'use strict';

var Emitter = require('emitter-component');
var Hammer = require('../module/hammer');
var moment = require('../module/moment');
var util = require('../util');
var DataSet = require('../DataSet');
var DataView = require('../DataView');
var Range = require('./Range');
var Core = require('./Core');
var TimeAxis = require('./component/TimeAxis');
var CurrentTime = require('./component/CurrentTime');
var CustomTime = require('./component/CustomTime');
var ItemSet = require('./component/ItemSet');

var Configurator = require('../shared/Configurator');
var Validator = require('../shared/Validator')['default'];
var printStyle = require('../shared/Validator').printStyle;
var allOptions = require('./optionsTimeline').allOptions;
var configureOptions = require('./optionsTimeline').configureOptions;

/**
 * Create a timeline visualization
 * @param {HTMLElement} container
 * @param {vis.DataSet | vis.DataView | Array} [items]
 * @param {vis.DataSet | vis.DataView | Array} [groups]
 * @param {Object} [options]  See Timeline.setOptions for the available options.
 * @constructor
 * @extends Core
 */
function Timeline(container, items, groups, options) {
  if (!(this instanceof Timeline)) {
    throw new SyntaxError('Constructor must be called with the new operator');
  }

  // if the third element is options, the forth is groups (optionally);
  if (!(Array.isArray(groups) || groups instanceof DataSet || groups instanceof DataView) && groups instanceof Object) {
    var forthArgument = options;
    options = groups;
    groups = forthArgument;
  }

  var me = this;
  this.defaultOptions = {
    start: null,
    end: null,

    autoResize: true,
    throttleRedraw: 0, // ms

    orientation: {
      axis: 'bottom', // axis orientation: 'bottom', 'top', or 'both'
      item: 'bottom' // not relevant
    },

    moment: moment,

    width: null,
    height: null,
    maxHeight: null,
    minHeight: null
  };
  this.options = util.deepExtend({}, this.defaultOptions);

  // Create the DOM, props, and emitter
  this._create(container);

  // all components listed here will be repainted automatically
  this.components = [];

  this.body = {
    dom: this.dom,
    domProps: this.props,
    emitter: {
      on: this.on.bind(this),
      off: this.off.bind(this),
      emit: this.emit.bind(this)
    },
    hiddenDates: [],
    util: {
      getScale: function getScale() {
        return me.timeAxis.step.scale;
      },
      getStep: function getStep() {
        return me.timeAxis.step.step;
      },

      toScreen: me._toScreen.bind(me),
      toGlobalScreen: me._toGlobalScreen.bind(me), // this refers to the root.width
      toTime: me._toTime.bind(me),
      toGlobalTime: me._toGlobalTime.bind(me)
    }
  };

  // range
  this.range = new Range(this.body);
  this.components.push(this.range);
  this.body.range = this.range;

  // time axis
  this.timeAxis = new TimeAxis(this.body);
  this.timeAxis2 = null; // used in case of orientation option 'both'
  this.components.push(this.timeAxis);

  // current time bar
  this.currentTime = new CurrentTime(this.body);
  this.components.push(this.currentTime);

  // item set
  this.itemSet = new ItemSet(this.body);
  this.components.push(this.itemSet);

  this.itemsData = null; // DataSet
  this.groupsData = null; // DataSet

  this.on('tap', function (event) {
    me.emit('click', me.getEventProperties(event));
  });
  this.on('doubletap', function (event) {
    me.emit('doubleClick', me.getEventProperties(event));
  });
  this.dom.root.oncontextmenu = function (event) {
    me.emit('contextmenu', me.getEventProperties(event));
  };

  // apply options
  if (options) {
    this.setOptions(options);
  }

  // IMPORTANT: THIS HAPPENS BEFORE SET ITEMS!
  if (groups) {
    this.setGroups(groups);
  }

  // create itemset
  if (items) {
    this.setItems(items);
  } else {
    this._redraw();
  }
}

// Extend the functionality from Core
Timeline.prototype = new Core();

/**
 * Load a configurator
 * @return {Object}
 * @private
 */
Timeline.prototype._createConfigurator = function () {
  return new Configurator(this, this.dom.container, configureOptions);
};

/**
 * Force a redraw. The size of all items will be recalculated.
 * Can be useful to manually redraw when option autoResize=false and the window
 * has been resized, or when the items CSS has been changed.
 *
 * Note: this function will be overridden on construction with a trottled version
 */
Timeline.prototype.redraw = function () {
  this.itemSet && this.itemSet.markDirty({ refreshItems: true });
  this._redraw();
};

Timeline.prototype.setOptions = function (options) {
  // validate options
  var errorFound = Validator.validate(options, allOptions);
  if (errorFound === true) {
    console.log('%cErrors have been found in the supplied options object.', printStyle);
  }

  Core.prototype.setOptions.call(this, options);

  if ('type' in options) {
    if (options.type !== this.options.type) {
      this.options.type = options.type;

      // force recreation of all items
      var itemsData = this.itemsData;
      if (itemsData) {
        var selection = this.getSelection();
        this.setItems(null); // remove all
        this.setItems(itemsData); // add all
        this.setSelection(selection); // restore selection
      }
    }
  }
};

/**
 * Set items
 * @param {vis.DataSet | Array | null} items
 */
Timeline.prototype.setItems = function (items) {
  var initialLoad = this.itemsData == null;

  // convert to type DataSet when needed
  var newDataSet;
  if (!items) {
    newDataSet = null;
  } else if (items instanceof DataSet || items instanceof DataView) {
    newDataSet = items;
  } else {
    // turn an array into a dataset
    newDataSet = new DataSet(items, {
      type: {
        start: 'Date',
        end: 'Date'
      }
    });
  }

  // set items
  this.itemsData = newDataSet;
  this.itemSet && this.itemSet.setItems(newDataSet);

  if (initialLoad) {
    if (this.options.start != undefined || this.options.end != undefined) {
      if (this.options.start == undefined || this.options.end == undefined) {
        var range = this.getItemRange();
      }

      var start = this.options.start != undefined ? this.options.start : range.min;
      var end = this.options.end != undefined ? this.options.end : range.max;

      this.setWindow(start, end, { animation: false });
    } else {
      this.fit({ animation: false });
    }
  }
};

/**
 * Set groups
 * @param {vis.DataSet | Array} groups
 */
Timeline.prototype.setGroups = function (groups) {
  // convert to type DataSet when needed
  var newDataSet;
  if (!groups) {
    newDataSet = null;
  } else if (groups instanceof DataSet || groups instanceof DataView) {
    newDataSet = groups;
  } else {
    // turn an array into a dataset
    newDataSet = new DataSet(groups);
  }

  this.groupsData = newDataSet;
  this.itemSet.setGroups(newDataSet);
};

/**
 * Set both items and groups in one go
 * @param {{items: Array | vis.DataSet, groups: Array | vis.DataSet}} data
 */
Timeline.prototype.setData = function (data) {
  if (data && data.groups) {
    this.setGroups(data.groups);
  }

  if (data && data.items) {
    this.setItems(data.items);
  }
};

/**
 * Set selected items by their id. Replaces the current selection
 * Unknown id's are silently ignored.
 * @param {string[] | string} [ids]  An array with zero or more id's of the items to be
 *                                selected. If ids is an empty array, all items will be
 *                                unselected.
 * @param {Object} [options]      Available options:
 *                                `focus: boolean`
 *                                    If true, focus will be set to the selected item(s)
 *                                `animation: boolean | {duration: number, easingFunction: string}`
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 *                                    Only applicable when option focus is true.
 */
Timeline.prototype.setSelection = function (ids, options) {
  this.itemSet && this.itemSet.setSelection(ids);

  if (options && options.focus) {
    this.focus(ids, options);
  }
};

/**
 * Get the selected items by their id
 * @return {Array} ids  The ids of the selected items
 */
Timeline.prototype.getSelection = function () {
  return this.itemSet && this.itemSet.getSelection() || [];
};

/**
 * Adjust the visible window such that the selected item (or multiple items)
 * are centered on screen.
 * @param {String | String[]} id     An item id or array with item ids
 * @param {Object} [options]      Available options:
 *                                `animation: boolean | {duration: number, easingFunction: string}`
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 */
Timeline.prototype.focus = function (id, options) {
  if (!this.itemsData || id == undefined) return;

  var ids = Array.isArray(id) ? id : [id];

  // get the specified item(s)
  var itemsData = this.itemsData.getDataSet().get(ids, {
    type: {
      start: 'Date',
      end: 'Date'
    }
  });

  // calculate minimum start and maximum end of specified items
  var start = null;
  var end = null;
  itemsData.forEach(function (itemData) {
    var s = itemData.start.valueOf();
    var e = 'end' in itemData ? itemData.end.valueOf() : itemData.start.valueOf();

    if (start === null || s < start) {
      start = s;
    }

    if (end === null || e > end) {
      end = e;
    }
  });

  if (start !== null && end !== null) {
    // calculate the new middle and interval for the window
    var middle = (start + end) / 2;
    var interval = Math.max(this.range.end - this.range.start, (end - start) * 1.1);

    var animation = options && options.animation !== undefined ? options.animation : true;
    this.range.setRange(middle - interval / 2, middle + interval / 2, animation);
  }
};

/**
 * Set Timeline window such that it fits all items
 * @param {Object} [options]  Available options:
 *                                `animation: boolean | {duration: number, easingFunction: string}`
 *                                    If true (default), the range is animated
 *                                    smoothly to the new window. An object can be
 *                                    provided to specify duration and easing function.
 *                                    Default duration is 500 ms, and default easing
 *                                    function is 'easeInOutQuad'.
 */
Timeline.prototype.fit = function (options) {
  var animation = options && options.animation !== undefined ? options.animation : true;
  var range = this.getItemRange();
  this.range.setRange(range.min, range.max, animation);
};

/**
 * Determine the range of the items, taking into account their actual width
 * and a margin of 10 pixels on both sides.
 * @return {{min: Date | null, max: Date | null}}
 */
Timeline.prototype.getItemRange = function () {
  var _this = this;

  // get a rough approximation for the range based on the items start and end dates
  var range = this.getDataRange();
  var min = range.min.valueOf();
  var max = range.max.valueOf();
  var minItem = null;
  var maxItem = null;

  if (min != null && max != null) {
    var interval;
    var factor;
    var lhs;
    var rhs;
    var delta;

    (function () {
      var getStart = function getStart(item) {
        return util.convert(item.data.start, 'Date').valueOf();
      };

      var getEnd = function getEnd(item) {
        var end = item.data.end != undefined ? item.data.end : item.data.start;
        return util.convert(end, 'Date').valueOf();
      }

      // calculate the date of the left side and right side of the items given
      ;

      interval = max - min;
      // ms
      if (interval <= 0) {
        interval = 10;
      }
      factor = interval / _this.props.center.width;
      util.forEach(_this.itemSet.items, (function (item) {
        item.show();

        var start = getStart(item);
        var end = getEnd(item);

        var left = start - (item.getWidthLeft() + 10) * factor;
        var right = end + (item.getWidthRight() + 10) * factor;

        if (left < min) {
          min = left;
          minItem = item;
        }
        if (right > max) {
          max = right;
          maxItem = item;
        }
      }).bind(_this));

      if (minItem && maxItem) {
        lhs = minItem.getWidthLeft() + 10;
        rhs = maxItem.getWidthRight() + 10;
        delta = _this.props.center.width - lhs - rhs;
        // px

        if (delta > 0) {
          min = getStart(minItem) - lhs * interval / delta; // ms
          max = getEnd(maxItem) + rhs * interval / delta; // ms
        }
      }
    })();
  }

  return {
    min: min != null ? new Date(min) : null,
    max: max != null ? new Date(max) : null
  };
};

/**
 * Calculate the data range of the items start and end dates
 * @returns {{min: Date | null, max: Date | null}}
 */
Timeline.prototype.getDataRange = function () {
  var min = null;
  var max = null;

  var dataset = this.itemsData && this.itemsData.getDataSet();
  if (dataset) {
    dataset.forEach(function (item) {
      var start = util.convert(item.start, 'Date').valueOf();
      var end = util.convert(item.end != undefined ? item.end : item.start, 'Date').valueOf();
      if (min === null || start < min) {
        min = start;
      }
      if (max === null || end > max) {
        max = end;
      }
    });
  }

  return {
    min: min != null ? new Date(min) : null,
    max: max != null ? new Date(max) : null
  };
};

/**
 * Generate Timeline related information from an event
 * @param {Event} event
 * @return {Object} An object with related information, like on which area
 *                  The event happened, whether clicked on an item, etc.
 */
Timeline.prototype.getEventProperties = function (event) {
  var clientX = event.center ? event.center.x : event.clientX;
  var clientY = event.center ? event.center.y : event.clientY;
  var x = clientX - util.getAbsoluteLeft(this.dom.centerContainer);
  var y = clientY - util.getAbsoluteTop(this.dom.centerContainer);

  var item = this.itemSet.itemFromTarget(event);
  var group = this.itemSet.groupFromTarget(event);
  var customTime = CustomTime.customTimeFromTarget(event);

  var snap = this.itemSet.options.snap || null;
  var scale = this.body.util.getScale();
  var step = this.body.util.getStep();
  var time = this._toTime(x);
  var snappedTime = snap ? snap(time, scale, step) : time;

  var element = util.getTarget(event);
  var what = null;
  if (item != null) {
    what = 'item';
  } else if (customTime != null) {
    what = 'custom-time';
  } else if (util.hasParent(element, this.timeAxis.dom.foreground)) {
    what = 'axis';
  } else if (this.timeAxis2 && util.hasParent(element, this.timeAxis2.dom.foreground)) {
    what = 'axis';
  } else if (util.hasParent(element, this.itemSet.dom.labelSet)) {
    what = 'group-label';
  } else if (util.hasParent(element, this.currentTime.bar)) {
    what = 'current-time';
  } else if (util.hasParent(element, this.dom.center)) {
    what = 'background';
  }

  return {
    event: event,
    item: item ? item.id : null,
    group: group ? group.groupId : null,
    what: what,
    pageX: event.srcEvent ? event.srcEvent.pageX : event.pageX,
    pageY: event.srcEvent ? event.srcEvent.pageY : event.pageY,
    x: x,
    y: y,
    time: time,
    snappedTime: snappedTime
  };
};

module.exports = Timeline;

},{"../DataSet":2,"../DataView":3,"../module/hammer":6,"../module/moment":7,"../shared/Configurator":11,"../shared/Validator":12,"../util":33,"./Core":13,"./Range":15,"./component/CurrentTime":21,"./component/CustomTime":22,"./component/ItemSet":24,"./component/TimeAxis":25,"./optionsTimeline":32,"emitter-component":34}],19:[function(require,module,exports){
'use strict';

var util = require('../../util');
var Group = require('./Group');

/**
 * @constructor BackgroundGroup
 * @param {Number | String} groupId
 * @param {Object} data
 * @param {ItemSet} itemSet
 */
function BackgroundGroup(groupId, data, itemSet) {
  Group.call(this, groupId, data, itemSet);

  this.width = 0;
  this.height = 0;
  this.top = 0;
  this.left = 0;
}

BackgroundGroup.prototype = Object.create(Group.prototype);

/**
 * Repaint this group
 * @param {{start: number, end: number}} range
 * @param {{item: {horizontal: number, vertical: number}, axis: number}} margin
 * @param {boolean} [restack=false]  Force restacking of all items
 * @return {boolean} Returns true if the group is resized
 */
BackgroundGroup.prototype.redraw = function (range, margin, restack) {
  var resized = false;

  this.visibleItems = this._updateVisibleItems(this.orderedItems, this.visibleItems, range);

  // calculate actual size
  this.width = this.dom.background.offsetWidth;

  // apply new height (just always zero for BackgroundGroup
  this.dom.background.style.height = '0';

  // update vertical position of items after they are re-stacked and the height of the group is calculated
  for (var i = 0, ii = this.visibleItems.length; i < ii; i++) {
    var item = this.visibleItems[i];
    item.repositionY(margin);
  }

  return resized;
};

/**
 * Show this group: attach to the DOM
 */
BackgroundGroup.prototype.show = function () {
  if (!this.dom.background.parentNode) {
    this.itemSet.dom.background.appendChild(this.dom.background);
  }
};

module.exports = BackgroundGroup;

},{"../../util":33,"./Group":23}],20:[function(require,module,exports){
/**
 * Prototype for visual components
 * @param {{dom: Object, domProps: Object, emitter: Emitter, range: Range}} [body]
 * @param {Object} [options]
 */
"use strict";

function Component(body, options) {
  this.options = null;
  this.props = null;
}

/**
 * Set options for the component. The new options will be merged into the
 * current options.
 * @param {Object} options
 */
Component.prototype.setOptions = function (options) {
  if (options) {
    util.extend(this.options, options);
  }
};

/**
 * Repaint the component
 * @return {boolean} Returns true if the component is resized
 */
Component.prototype.redraw = function () {
  // should be implemented by the component
  return false;
};

/**
 * Destroy the component. Cleanup DOM and event listeners
 */
Component.prototype.destroy = function () {
  // should be implemented by the component
};

/**
 * Test whether the component is resized since the last time _isResized() was
 * called.
 * @return {Boolean} Returns true if the component is resized
 * @protected
 */
Component.prototype._isResized = function () {
  var resized = this.props._previousWidth !== this.props.width || this.props._previousHeight !== this.props.height;

  this.props._previousWidth = this.props.width;
  this.props._previousHeight = this.props.height;

  return resized;
};

module.exports = Component;

},{}],21:[function(require,module,exports){
'use strict';

var util = require('../../util');
var Component = require('./Component');
var moment = require('../../module/moment');
var locales = require('../locales');

/**
 * A current time bar
 * @param {{range: Range, dom: Object, domProps: Object}} body
 * @param {Object} [options]        Available parameters:
 *                                  {Boolean} [showCurrentTime]
 * @constructor CurrentTime
 * @extends Component
 */
function CurrentTime(body, options) {
  this.body = body;

  // default options
  this.defaultOptions = {
    showCurrentTime: true,

    moment: moment,
    locales: locales,
    locale: 'en'
  };
  this.options = util.extend({}, this.defaultOptions);
  this.offset = 0;

  this._create();

  this.setOptions(options);
}

CurrentTime.prototype = new Component();

/**
 * Create the HTML DOM for the current time bar
 * @private
 */
CurrentTime.prototype._create = function () {
  var bar = document.createElement('div');
  bar.className = 'vis-current-time';
  bar.style.position = 'absolute';
  bar.style.top = '0px';
  bar.style.height = '100%';

  this.bar = bar;
};

/**
 * Destroy the CurrentTime bar
 */
CurrentTime.prototype.destroy = function () {
  this.options.showCurrentTime = false;
  this.redraw(); // will remove the bar from the DOM and stop refreshing

  this.body = null;
};

/**
 * Set options for the component. Options will be merged in current options.
 * @param {Object} options  Available parameters:
 *                          {boolean} [showCurrentTime]
 */
CurrentTime.prototype.setOptions = function (options) {
  if (options) {
    // copy all options that we know
    util.selectiveExtend(['showCurrentTime', 'moment', 'locale', 'locales'], this.options, options);
  }
};

/**
 * Repaint the component
 * @return {boolean} Returns true if the component is resized
 */
CurrentTime.prototype.redraw = function () {
  if (this.options.showCurrentTime) {
    var parent = this.body.dom.backgroundVertical;
    if (this.bar.parentNode != parent) {
      // attach to the dom
      if (this.bar.parentNode) {
        this.bar.parentNode.removeChild(this.bar);
      }
      parent.appendChild(this.bar);

      this.start();
    }

    var now = this.options.moment(new Date().valueOf() + this.offset);
    var x = this.body.util.toScreen(now);

    var locale = this.options.locales[this.options.locale];
    if (!locale) {
      if (!this.warned) {
        console.log('WARNING: options.locales[\'' + this.options.locale + '\'] not found. See http://visjs.org/docs/timeline/#Localization');
        this.warned = true;
      }
      locale = this.options.locales['en']; // fall back on english when not available
    }
    var title = locale.current + ' ' + locale.time + ': ' + now.format('dddd, MMMM Do YYYY, H:mm:ss');
    title = title.charAt(0).toUpperCase() + title.substring(1);

    this.bar.style.left = x + 'px';
    this.bar.title = title;
  } else {
    // remove the line from the DOM
    if (this.bar.parentNode) {
      this.bar.parentNode.removeChild(this.bar);
    }
    this.stop();
  }

  return false;
};

/**
 * Start auto refreshing the current time bar
 */
CurrentTime.prototype.start = function () {
  var me = this;

  function update() {
    me.stop();

    // determine interval to refresh
    var scale = me.body.range.conversion(me.body.domProps.center.width).scale;
    var interval = 1 / scale / 10;
    if (interval < 30) interval = 30;
    if (interval > 1000) interval = 1000;

    me.redraw();

    // start a renderTimer to adjust for the new time
    me.currentTimeTimer = setTimeout(update, interval);
  }

  update();
};

/**
 * Stop auto refreshing the current time bar
 */
CurrentTime.prototype.stop = function () {
  if (this.currentTimeTimer !== undefined) {
    clearTimeout(this.currentTimeTimer);
    delete this.currentTimeTimer;
  }
};

/**
 * Set a current time. This can be used for example to ensure that a client's
 * time is synchronized with a shared server time.
 * @param {Date | String | Number} time     A Date, unix timestamp, or
 *                                          ISO date string.
 */
CurrentTime.prototype.setCurrentTime = function (time) {
  var t = util.convert(time, 'Date').valueOf();
  var now = new Date().valueOf();
  this.offset = t - now;
  this.redraw();
};

/**
 * Get the current time.
 * @return {Date} Returns the current time.
 */
CurrentTime.prototype.getCurrentTime = function () {
  return new Date(new Date().valueOf() + this.offset);
};

module.exports = CurrentTime;

},{"../../module/moment":7,"../../util":33,"../locales":31,"./Component":20}],22:[function(require,module,exports){
'use strict';

var Hammer = require('../../module/hammer');
var util = require('../../util');
var Component = require('./Component');
var moment = require('../../module/moment');
var locales = require('../locales');

/**
 * A custom time bar
 * @param {{range: Range, dom: Object}} body
 * @param {Object} [options]        Available parameters:
 *                                  {number | string} id
 *                                  {string} locales
 *                                  {string} locale
 * @constructor CustomTime
 * @extends Component
 */

function CustomTime(body, options) {
  this.body = body;

  // default options
  this.defaultOptions = {
    moment: moment,
    locales: locales,
    locale: 'en',
    id: undefined,
    title: undefined
  };
  this.options = util.extend({}, this.defaultOptions);

  if (options && options.time) {
    this.customTime = options.time;
  } else {
    this.customTime = new Date();
  }

  this.eventParams = {}; // stores state parameters while dragging the bar

  this.setOptions(options);

  // create the DOM
  this._create();
}

CustomTime.prototype = new Component();

/**
 * Set options for the component. Options will be merged in current options.
 * @param {Object} options  Available parameters:
 *                                  {number | string} id
 *                                  {string} locales
 *                                  {string} locale
 */
CustomTime.prototype.setOptions = function (options) {
  if (options) {
    // copy all options that we know
    util.selectiveExtend(['moment', 'locale', 'locales', 'id'], this.options, options);
  }
};

/**
 * Create the DOM for the custom time
 * @private
 */
CustomTime.prototype._create = function () {
  var bar = document.createElement('div');
  bar['custom-time'] = this;
  bar.className = 'vis-custom-time ' + (this.options.id || '');
  bar.style.position = 'absolute';
  bar.style.top = '0px';
  bar.style.height = '100%';
  this.bar = bar;

  var drag = document.createElement('div');
  drag.style.position = 'relative';
  drag.style.top = '0px';
  drag.style.left = '-10px';
  drag.style.height = '100%';
  drag.style.width = '20px';
  bar.appendChild(drag);

  // attach event listeners
  this.hammer = new Hammer(drag);
  this.hammer.on('panstart', this._onDragStart.bind(this));
  this.hammer.on('panmove', this._onDrag.bind(this));
  this.hammer.on('panend', this._onDragEnd.bind(this));
  this.hammer.get('pan').set({ threshold: 5, direction: 30 }); // 30 is ALL_DIRECTIONS in hammer.
};

/**
 * Destroy the CustomTime bar
 */
CustomTime.prototype.destroy = function () {
  this.hide();

  this.hammer.destroy();
  this.hammer = null;

  this.body = null;
};

/**
 * Repaint the component
 * @return {boolean} Returns true if the component is resized
 */
CustomTime.prototype.redraw = function () {
  var parent = this.body.dom.backgroundVertical;
  if (this.bar.parentNode != parent) {
    // attach to the dom
    if (this.bar.parentNode) {
      this.bar.parentNode.removeChild(this.bar);
    }
    parent.appendChild(this.bar);
  }

  var x = this.body.util.toScreen(this.customTime);

  var locale = this.options.locales[this.options.locale];
  if (!locale) {
    if (!this.warned) {
      console.log('WARNING: options.locales[\'' + this.options.locale + '\'] not found. See http://visjs.org/docs/timeline.html#Localization');
      this.warned = true;
    }
    locale = this.options.locales['en']; // fall back on english when not available
  }

  var title = this.options.title;
  // To hide the title completely use empty string ''.
  if (title === undefined) {
    title = locale.time + ': ' + this.options.moment(this.customTime).format('dddd, MMMM Do YYYY, H:mm:ss');
    title = title.charAt(0).toUpperCase() + title.substring(1);
  }

  this.bar.style.left = x + 'px';
  this.bar.title = title;

  return false;
};

/**
 * Remove the CustomTime from the DOM
 */
CustomTime.prototype.hide = function () {
  // remove the line from the DOM
  if (this.bar.parentNode) {
    this.bar.parentNode.removeChild(this.bar);
  }
};

/**
 * Set custom time.
 * @param {Date | number | string} time
 */
CustomTime.prototype.setCustomTime = function (time) {
  this.customTime = util.convert(time, 'Date');
  this.redraw();
};

/**
 * Retrieve the current custom time.
 * @return {Date} customTime
 */
CustomTime.prototype.getCustomTime = function () {
  return new Date(this.customTime.valueOf());
};

/**
  * Set custom title.
  * @param {Date | number | string} title
  */
CustomTime.prototype.setCustomTitle = function (title) {
  this.options.title = title;
};

/**
 * Start moving horizontally
 * @param {Event} event
 * @private
 */
CustomTime.prototype._onDragStart = function (event) {
  this.eventParams.dragging = true;
  this.eventParams.customTime = this.customTime;

  event.stopPropagation();
};

/**
 * Perform moving operating.
 * @param {Event} event
 * @private
 */
CustomTime.prototype._onDrag = function (event) {
  if (!this.eventParams.dragging) return;

  var x = this.body.util.toScreen(this.eventParams.customTime) + event.deltaX;
  var time = this.body.util.toTime(x);

  this.setCustomTime(time);

  // fire a timechange event
  this.body.emitter.emit('timechange', {
    id: this.options.id,
    time: new Date(this.customTime.valueOf())
  });

  event.stopPropagation();
};

/**
 * Stop moving operating.
 * @param {Event} event
 * @private
 */
CustomTime.prototype._onDragEnd = function (event) {
  if (!this.eventParams.dragging) return;

  // fire a timechanged event
  this.body.emitter.emit('timechanged', {
    id: this.options.id,
    time: new Date(this.customTime.valueOf())
  });

  event.stopPropagation();
};

/**
 * Find a custom time from an event target:
 * searches for the attribute 'custom-time' in the event target's element tree
 * @param {Event} event
 * @return {CustomTime | null} customTime
 */
CustomTime.customTimeFromTarget = function (event) {
  var target = event.target;
  while (target) {
    if (target.hasOwnProperty('custom-time')) {
      return target['custom-time'];
    }
    target = target.parentNode;
  }

  return null;
};

module.exports = CustomTime;

},{"../../module/hammer":6,"../../module/moment":7,"../../util":33,"../locales":31,"./Component":20}],23:[function(require,module,exports){
'use strict';

var util = require('../../util');
var stack = require('../Stack');
var RangeItem = require('./item/RangeItem');

/**
 * @constructor Group
 * @param {Number | String} groupId
 * @param {Object} data
 * @param {ItemSet} itemSet
 */
function Group(groupId, data, itemSet) {
  this.groupId = groupId;
  this.subgroups = {};
  this.subgroupIndex = 0;
  this.subgroupOrderer = data && data.subgroupOrder;
  this.itemSet = itemSet;

  this.dom = {};
  this.props = {
    label: {
      width: 0,
      height: 0
    }
  };
  this.className = null;

  this.items = {}; // items filtered by groupId of this group
  this.visibleItems = []; // items currently visible in window
  this.orderedItems = {
    byStart: [],
    byEnd: []
  };
  this.checkRangedItems = false; // needed to refresh the ranged items if the window is programatically changed with NO overlap.
  var me = this;
  this.itemSet.body.emitter.on("checkRangedItems", function () {
    me.checkRangedItems = true;
  });

  this._create();

  this.setData(data);
}

/**
 * Create DOM elements for the group
 * @private
 */
Group.prototype._create = function () {
  var label = document.createElement('div');
  if (this.itemSet.options.groupEditable.order) {
    label.className = 'vis-label draggable';
  } else {
    label.className = 'vis-label';
  }
  this.dom.label = label;

  var inner = document.createElement('div');
  inner.className = 'vis-inner';
  label.appendChild(inner);
  this.dom.inner = inner;

  var foreground = document.createElement('div');
  foreground.className = 'vis-group';
  foreground['timeline-group'] = this;
  this.dom.foreground = foreground;

  this.dom.background = document.createElement('div');
  this.dom.background.className = 'vis-group';

  this.dom.axis = document.createElement('div');
  this.dom.axis.className = 'vis-group';

  // create a hidden marker to detect when the Timelines container is attached
  // to the DOM, or the style of a parent of the Timeline is changed from
  // display:none is changed to visible.
  this.dom.marker = document.createElement('div');
  this.dom.marker.style.visibility = 'hidden';
  this.dom.marker.innerHTML = '?';
  this.dom.background.appendChild(this.dom.marker);
};

/**
 * Set the group data for this group
 * @param {Object} data   Group data, can contain properties content and className
 */
Group.prototype.setData = function (data) {
  // update contents
  var content;
  if (this.itemSet.options && this.itemSet.options.groupTemplate) {
    content = this.itemSet.options.groupTemplate(data);
  } else {
    content = data && data.content;
  }

  if (content instanceof Element) {
    this.dom.inner.appendChild(content);
    while (this.dom.inner.firstChild) {
      this.dom.inner.removeChild(this.dom.inner.firstChild);
    }
    this.dom.inner.appendChild(content);
  } else if (content !== undefined && content !== null) {
    this.dom.inner.innerHTML = content;
  } else {
    this.dom.inner.innerHTML = this.groupId || ''; // groupId can be null
  }

  // update title
  this.dom.label.title = data && data.title || '';

  if (!this.dom.inner.firstChild) {
    util.addClassName(this.dom.inner, 'vis-hidden');
  } else {
    util.removeClassName(this.dom.inner, 'vis-hidden');
  }

  // update className
  var className = data && data.className || null;
  if (className != this.className) {
    if (this.className) {
      util.removeClassName(this.dom.label, this.className);
      util.removeClassName(this.dom.foreground, this.className);
      util.removeClassName(this.dom.background, this.className);
      util.removeClassName(this.dom.axis, this.className);
    }
    util.addClassName(this.dom.label, className);
    util.addClassName(this.dom.foreground, className);
    util.addClassName(this.dom.background, className);
    util.addClassName(this.dom.axis, className);
    this.className = className;
  }

  // update style
  if (this.style) {
    util.removeCssText(this.dom.label, this.style);
    this.style = null;
  }
  if (data && data.style) {
    util.addCssText(this.dom.label, data.style);
    this.style = data.style;
  }
};

/**
 * Get the width of the group label
 * @return {number} width
 */
Group.prototype.getLabelWidth = function () {
  return this.props.label.width;
};

/**
 * Repaint this group
 * @param {{start: number, end: number}} range
 * @param {{item: {horizontal: number, vertical: number}, axis: number}} margin
 * @param {boolean} [restack=false]  Force restacking of all items
 * @return {boolean} Returns true if the group is resized
 */
Group.prototype.redraw = function (range, margin, restack) {
  var resized = false;

  // force recalculation of the height of the items when the marker height changed
  // (due to the Timeline being attached to the DOM or changed from display:none to visible)
  var markerHeight = this.dom.marker.clientHeight;
  if (markerHeight != this.lastMarkerHeight) {
    this.lastMarkerHeight = markerHeight;

    util.forEach(this.items, function (item) {
      item.dirty = true;
      if (item.displayed) item.redraw();
    });

    restack = true;
  }

  // recalculate the height of the subgroups
  this._calculateSubGroupHeights();

  // reposition visible items vertically
  if (typeof this.itemSet.options.order === 'function') {
    // a custom order function

    if (restack) {
      // brute force restack of all items

      // show all items
      var me = this;
      var limitSize = false;
      util.forEach(this.items, function (item) {
        if (!item.displayed) {
          item.redraw();
          me.visibleItems.push(item);
        }
        item.repositionX(limitSize);
      });

      // order all items and force a restacking
      var customOrderedItems = this.orderedItems.byStart.slice().sort(function (a, b) {
        return me.itemSet.options.order(a.data, b.data);
      });
      stack.stack(customOrderedItems, margin, true /* restack=true */);
    }

    this.visibleItems = this._updateVisibleItems(this.orderedItems, this.visibleItems, range);
  } else {
    // no custom order function, lazy stacking
    this.visibleItems = this._updateVisibleItems(this.orderedItems, this.visibleItems, range);

    if (this.itemSet.options.stack) {
      // TODO: ugly way to access options...
      stack.stack(this.visibleItems, margin, restack);
    } else {
      // no stacking
      stack.nostack(this.visibleItems, margin, this.subgroups);
    }
  }

  // recalculate the height of the group
  var height = this._calculateHeight(margin);

  // calculate actual size and position
  var foreground = this.dom.foreground;
  this.top = foreground.offsetTop;
  this.left = foreground.offsetLeft;
  this.width = foreground.offsetWidth;
  resized = util.updateProperty(this, 'height', height) || resized;

  // recalculate size of label
  resized = util.updateProperty(this.props.label, 'width', this.dom.inner.clientWidth) || resized;
  resized = util.updateProperty(this.props.label, 'height', this.dom.inner.clientHeight) || resized;

  // apply new height
  this.dom.background.style.height = height + 'px';
  this.dom.foreground.style.height = height + 'px';
  this.dom.label.style.height = height + 'px';

  // update vertical position of items after they are re-stacked and the height of the group is calculated
  for (var i = 0, ii = this.visibleItems.length; i < ii; i++) {
    var item = this.visibleItems[i];
    item.repositionY(margin);
  }

  return resized;
};

/**
 * recalculate the height of the subgroups
 * @private
 */
Group.prototype._calculateSubGroupHeights = function () {
  if (Object.keys(this.subgroups).length > 0) {
    var me = this;

    this.resetSubgroups();

    util.forEach(this.visibleItems, function (item) {
      if (item.data.subgroup !== undefined) {
        me.subgroups[item.data.subgroup].height = Math.max(me.subgroups[item.data.subgroup].height, item.height);
        me.subgroups[item.data.subgroup].visible = true;
      }
    });
  }
};

/**
 * recalculate the height of the group
 * @param {{item: {horizontal: number, vertical: number}, axis: number}} margin
 * @returns {number} Returns the height
 * @private
 */
Group.prototype._calculateHeight = function (margin) {
  // recalculate the height of the group
  var height;
  var visibleItems = this.visibleItems;
  if (visibleItems.length > 0) {
    var min = visibleItems[0].top;
    var max = visibleItems[0].top + visibleItems[0].height;
    util.forEach(visibleItems, function (item) {
      min = Math.min(min, item.top);
      max = Math.max(max, item.top + item.height);
    });
    if (min > margin.axis) {
      // there is an empty gap between the lowest item and the axis
      var offset = min - margin.axis;
      max -= offset;
      util.forEach(visibleItems, function (item) {
        item.top -= offset;
      });
    }
    height = max + margin.item.vertical / 2;
  } else {
    height = 0;
  }
  height = Math.max(height, this.props.label.height);

  return height;
};

/**
 * Show this group: attach to the DOM
 */
Group.prototype.show = function () {
  if (!this.dom.label.parentNode) {
    this.itemSet.dom.labelSet.appendChild(this.dom.label);
  }

  if (!this.dom.foreground.parentNode) {
    this.itemSet.dom.foreground.appendChild(this.dom.foreground);
  }

  if (!this.dom.background.parentNode) {
    this.itemSet.dom.background.appendChild(this.dom.background);
  }

  if (!this.dom.axis.parentNode) {
    this.itemSet.dom.axis.appendChild(this.dom.axis);
  }
};

/**
 * Hide this group: remove from the DOM
 */
Group.prototype.hide = function () {
  var label = this.dom.label;
  if (label.parentNode) {
    label.parentNode.removeChild(label);
  }

  var foreground = this.dom.foreground;
  if (foreground.parentNode) {
    foreground.parentNode.removeChild(foreground);
  }

  var background = this.dom.background;
  if (background.parentNode) {
    background.parentNode.removeChild(background);
  }

  var axis = this.dom.axis;
  if (axis.parentNode) {
    axis.parentNode.removeChild(axis);
  }
};

/**
 * Add an item to the group
 * @param {Item} item
 */
Group.prototype.add = function (item) {
  this.items[item.id] = item;
  item.setParent(this);

  // add to
  if (item.data.subgroup !== undefined) {
    if (this.subgroups[item.data.subgroup] === undefined) {
      this.subgroups[item.data.subgroup] = { height: 0, visible: false, index: this.subgroupIndex, items: [] };
      this.subgroupIndex++;
    }
    this.subgroups[item.data.subgroup].items.push(item);
  }
  this.orderSubgroups();

  if (this.visibleItems.indexOf(item) == -1) {
    var range = this.itemSet.body.range; // TODO: not nice accessing the range like this
    this._checkIfVisible(item, this.visibleItems, range);
  }
};

Group.prototype.orderSubgroups = function () {
  if (this.subgroupOrderer !== undefined) {
    var sortArray = [];
    if (typeof this.subgroupOrderer == 'string') {
      for (var subgroup in this.subgroups) {
        sortArray.push({ subgroup: subgroup, sortField: this.subgroups[subgroup].items[0].data[this.subgroupOrderer] });
      }
      sortArray.sort(function (a, b) {
        return a.sortField - b.sortField;
      });
    } else if (typeof this.subgroupOrderer == 'function') {
      for (var subgroup in this.subgroups) {
        sortArray.push(this.subgroups[subgroup].items[0].data);
      }
      sortArray.sort(this.subgroupOrderer);
    }

    if (sortArray.length > 0) {
      for (var i = 0; i < sortArray.length; i++) {
        this.subgroups[sortArray[i].subgroup].index = i;
      }
    }
  }
};

Group.prototype.resetSubgroups = function () {
  for (var subgroup in this.subgroups) {
    if (this.subgroups.hasOwnProperty(subgroup)) {
      this.subgroups[subgroup].visible = false;
    }
  }
};

/**
 * Remove an item from the group
 * @param {Item} item
 */
Group.prototype.remove = function (item) {
  delete this.items[item.id];
  item.setParent(null);

  // remove from visible items
  var index = this.visibleItems.indexOf(item);
  if (index != -1) this.visibleItems.splice(index, 1);

  if (item.data.subgroup !== undefined) {
    var subgroup = this.subgroups[item.data.subgroup];
    if (subgroup) {
      var itemIndex = subgroup.items.indexOf(item);
      subgroup.items.splice(itemIndex, 1);
      if (!subgroup.items.length) {
        delete this.subgroups[item.data.subgroup];
        this.subgroupIndex--;
      }
      this.orderSubgroups();
    }
  }
};

/**
 * Remove an item from the corresponding DataSet
 * @param {Item} item
 */
Group.prototype.removeFromDataSet = function (item) {
  this.itemSet.removeItem(item.id);
};

/**
 * Reorder the items
 */
Group.prototype.order = function () {
  var array = util.toArray(this.items);
  var startArray = [];
  var endArray = [];

  for (var i = 0; i < array.length; i++) {
    if (array[i].data.end !== undefined) {
      endArray.push(array[i]);
    }
    startArray.push(array[i]);
  }
  this.orderedItems = {
    byStart: startArray,
    byEnd: endArray
  };

  stack.orderByStart(this.orderedItems.byStart);
  stack.orderByEnd(this.orderedItems.byEnd);
};

/**
 * Update the visible items
 * @param {{byStart: Item[], byEnd: Item[]}} orderedItems   All items ordered by start date and by end date
 * @param {Item[]} visibleItems                             The previously visible items.
 * @param {{start: number, end: number}} range              Visible range
 * @return {Item[]} visibleItems                            The new visible items.
 * @private
 */
Group.prototype._updateVisibleItems = function (orderedItems, oldVisibleItems, range) {
  var visibleItems = [];
  var visibleItemsLookup = {}; // we keep this to quickly look up if an item already exists in the list without using indexOf on visibleItems
  var interval = (range.end - range.start) / 4;
  var lowerBound = range.start - interval;
  var upperBound = range.end + interval;
  var item, i;

  // this function is used to do the binary search.
  var searchFunction = function searchFunction(value) {
    if (value < lowerBound) {
      return -1;
    } else if (value <= upperBound) {
      return 0;
    } else {
      return 1;
    }
  };

  // first check if the items that were in view previously are still in view.
  // IMPORTANT: this handles the case for the items with startdate before the window and enddate after the window!
  // also cleans up invisible items.
  if (oldVisibleItems.length > 0) {
    for (i = 0; i < oldVisibleItems.length; i++) {
      this._checkIfVisibleWithReference(oldVisibleItems[i], visibleItems, visibleItemsLookup, range);
    }
  }

  // we do a binary search for the items that have only start values.
  var initialPosByStart = util.binarySearchCustom(orderedItems.byStart, searchFunction, 'data', 'start');

  // trace the visible items from the inital start pos both ways until an invisible item is found, we only look at the start values.
  this._traceVisible(initialPosByStart, orderedItems.byStart, visibleItems, visibleItemsLookup, function (item) {
    return item.data.start < lowerBound || item.data.start > upperBound;
  });

  // if the window has changed programmatically without overlapping the old window, the ranged items with start < lowerBound and end > upperbound are not shown.
  // We therefore have to brute force check all items in the byEnd list
  if (this.checkRangedItems == true) {
    this.checkRangedItems = false;
    for (i = 0; i < orderedItems.byEnd.length; i++) {
      this._checkIfVisibleWithReference(orderedItems.byEnd[i], visibleItems, visibleItemsLookup, range);
    }
  } else {
    // we do a binary search for the items that have defined end times.
    var initialPosByEnd = util.binarySearchCustom(orderedItems.byEnd, searchFunction, 'data', 'end');

    // trace the visible items from the inital start pos both ways until an invisible item is found, we only look at the end values.
    this._traceVisible(initialPosByEnd, orderedItems.byEnd, visibleItems, visibleItemsLookup, function (item) {
      return item.data.end < lowerBound || item.data.end > upperBound;
    });
  }

  // finally, we reposition all the visible items.
  for (i = 0; i < visibleItems.length; i++) {
    item = visibleItems[i];
    if (!item.displayed) item.show();
    // reposition item horizontally
    item.repositionX();
  }

  // debug
  //console.log("new line")
  //if (this.groupId == null) {
  //  for (i = 0; i < orderedItems.byStart.length; i++) {
  //    item = orderedItems.byStart[i].data;
  //    console.log('start',i,initialPosByStart, item.start.valueOf(), item.content, item.start >= lowerBound && item.start <= upperBound,i == initialPosByStart ? "<------------------- HEREEEE" : "")
  //  }
  //  for (i = 0; i < orderedItems.byEnd.length; i++) {
  //    item = orderedItems.byEnd[i].data;
  //    console.log('rangeEnd',i,initialPosByEnd, item.end.valueOf(), item.content, item.end >= range.start && item.end <= range.end,i == initialPosByEnd ? "<------------------- HEREEEE" : "")
  //  }
  //}

  return visibleItems;
};

Group.prototype._traceVisible = function (initialPos, items, visibleItems, visibleItemsLookup, breakCondition) {
  var item;
  var i;

  if (initialPos != -1) {
    for (i = initialPos; i >= 0; i--) {
      item = items[i];
      if (breakCondition(item)) {
        break;
      } else {
        if (visibleItemsLookup[item.id] === undefined) {
          visibleItemsLookup[item.id] = true;
          visibleItems.push(item);
        }
      }
    }

    for (i = initialPos + 1; i < items.length; i++) {
      item = items[i];
      if (breakCondition(item)) {
        break;
      } else {
        if (visibleItemsLookup[item.id] === undefined) {
          visibleItemsLookup[item.id] = true;
          visibleItems.push(item);
        }
      }
    }
  }
};

/**
 * this function is very similar to the _checkIfInvisible() but it does not
 * return booleans, hides the item if it should not be seen and always adds to
 * the visibleItems.
 * this one is for brute forcing and hiding.
 *
 * @param {Item} item
 * @param {Array} visibleItems
 * @param {{start:number, end:number}} range
 * @private
 */
Group.prototype._checkIfVisible = function (item, visibleItems, range) {
  if (item.isVisible(range)) {
    if (!item.displayed) item.show();
    // reposition item horizontally
    item.repositionX();
    visibleItems.push(item);
  } else {
    if (item.displayed) item.hide();
  }
};

/**
 * this function is very similar to the _checkIfInvisible() but it does not
 * return booleans, hides the item if it should not be seen and always adds to
 * the visibleItems.
 * this one is for brute forcing and hiding.
 *
 * @param {Item} item
 * @param {Array} visibleItems
 * @param {{start:number, end:number}} range
 * @private
 */
Group.prototype._checkIfVisibleWithReference = function (item, visibleItems, visibleItemsLookup, range) {
  if (item.isVisible(range)) {
    if (visibleItemsLookup[item.id] === undefined) {
      visibleItemsLookup[item.id] = true;
      visibleItems.push(item);
    }
  } else {
    if (item.displayed) item.hide();
  }
};

module.exports = Group;

},{"../../util":33,"../Stack":16,"./item/RangeItem":30}],24:[function(require,module,exports){
'use strict';

var Hammer = require('../../module/hammer');
var util = require('../../util');
var DataSet = require('../../DataSet');
var DataView = require('../../DataView');
var TimeStep = require('../TimeStep');
var Component = require('./Component');
var Group = require('./Group');
var BackgroundGroup = require('./BackgroundGroup');
var BoxItem = require('./item/BoxItem');
var PointItem = require('./item/PointItem');
var RangeItem = require('./item/RangeItem');
var BackgroundItem = require('./item/BackgroundItem');

var UNGROUPED = '__ungrouped__'; // reserved group id for ungrouped items
var BACKGROUND = '__background__'; // reserved group id for background items without group

/**
 * An ItemSet holds a set of items and ranges which can be displayed in a
 * range. The width is determined by the parent of the ItemSet, and the height
 * is determined by the size of the items.
 * @param {{dom: Object, domProps: Object, emitter: Emitter, range: Range}} body
 * @param {Object} [options]      See ItemSet.setOptions for the available options.
 * @constructor ItemSet
 * @extends Component
 */
function ItemSet(body, options) {
  this.body = body;

  this.defaultOptions = {
    type: null, // 'box', 'point', 'range', 'background'
    orientation: {
      item: 'bottom' // item orientation: 'top' or 'bottom'
    },
    align: 'auto', // alignment of box items
    stack: true,
    groupOrderSwap: function groupOrderSwap(fromGroup, toGroup, groups) {
      var targetOrder = toGroup.order;
      toGroup.order = fromGroup.order;
      fromGroup.order = targetOrder;
    },
    groupOrder: 'order',

    selectable: true,
    multiselect: false,
    itemsAlwaysDraggable: false,

    editable: {
      updateTime: false,
      updateGroup: false,
      add: false,
      remove: false
    },

    groupEditable: {
      order: false,
      add: false,
      remove: false
    },

    snap: TimeStep.snap,

    onAdd: function onAdd(item, callback) {
      callback(item);
    },
    onUpdate: function onUpdate(item, callback) {
      callback(item);
    },
    onMove: function onMove(item, callback) {
      callback(item);
    },
    onRemove: function onRemove(item, callback) {
      callback(item);
    },
    onMoving: function onMoving(item, callback) {
      callback(item);
    },
    onAddGroup: function onAddGroup(item, callback) {
      callback(item);
    },
    onMoveGroup: function onMoveGroup(item, callback) {
      callback(item);
    },
    onRemoveGroup: function onRemoveGroup(item, callback) {
      callback(item);
    },

    margin: {
      item: {
        horizontal: 10,
        vertical: 10
      },
      axis: 20
    }
  };

  // options is shared by this ItemSet and all its items
  this.options = util.extend({}, this.defaultOptions);

  // options for getting items from the DataSet with the correct type
  this.itemOptions = {
    type: { start: 'Date', end: 'Date' }
  };

  this.conversion = {
    toScreen: body.util.toScreen,
    toTime: body.util.toTime
  };
  this.dom = {};
  this.props = {};
  this.hammer = null;

  var me = this;
  this.itemsData = null; // DataSet
  this.groupsData = null; // DataSet

  // listeners for the DataSet of the items
  this.itemListeners = {
    'add': function add(event, params, senderId) {
      me._onAdd(params.items);
    },
    'update': function update(event, params, senderId) {
      me._onUpdate(params.items);
    },
    'remove': function remove(event, params, senderId) {
      me._onRemove(params.items);
    }
  };

  // listeners for the DataSet of the groups
  this.groupListeners = {
    'add': function add(event, params, senderId) {
      me._onAddGroups(params.items);
    },
    'update': function update(event, params, senderId) {
      me._onUpdateGroups(params.items);
    },
    'remove': function remove(event, params, senderId) {
      me._onRemoveGroups(params.items);
    }
  };

  this.items = {}; // object with an Item for every data item
  this.groups = {}; // Group object for every group
  this.groupIds = [];

  this.selection = []; // list with the ids of all selected nodes
  this.stackDirty = true; // if true, all items will be restacked on next redraw

  this.touchParams = {}; // stores properties while dragging
  this.groupTouchParams = {};
  // create the HTML DOM

  this._create();

  this.setOptions(options);
}

ItemSet.prototype = new Component();

// available item types will be registered here
ItemSet.types = {
  background: BackgroundItem,
  box: BoxItem,
  range: RangeItem,
  point: PointItem
};

/**
 * Create the HTML DOM for the ItemSet
 */
ItemSet.prototype._create = function () {
  var frame = document.createElement('div');
  frame.className = 'vis-itemset';
  frame['timeline-itemset'] = this;
  this.dom.frame = frame;

  // create background panel
  var background = document.createElement('div');
  background.className = 'vis-background';
  frame.appendChild(background);
  this.dom.background = background;

  // create foreground panel
  var foreground = document.createElement('div');
  foreground.className = 'vis-foreground';
  frame.appendChild(foreground);
  this.dom.foreground = foreground;

  // create axis panel
  var axis = document.createElement('div');
  axis.className = 'vis-axis';
  this.dom.axis = axis;

  // create labelset
  var labelSet = document.createElement('div');
  labelSet.className = 'vis-labelset';
  this.dom.labelSet = labelSet;

  // create ungrouped Group
  this._updateUngrouped();

  // create background Group
  var backgroundGroup = new BackgroundGroup(BACKGROUND, null, this);
  backgroundGroup.show();
  this.groups[BACKGROUND] = backgroundGroup;

  // attach event listeners
  // Note: we bind to the centerContainer for the case where the height
  //       of the center container is larger than of the ItemSet, so we
  //       can click in the empty area to create a new item or deselect an item.
  this.hammer = new Hammer(this.body.dom.centerContainer);

  // drag items when selected
  this.hammer.on('hammer.input', (function (event) {
    if (event.isFirst) {
      this._onTouch(event);
    }
  }).bind(this));
  this.hammer.on('panstart', this._onDragStart.bind(this));
  this.hammer.on('panmove', this._onDrag.bind(this));
  this.hammer.on('panend', this._onDragEnd.bind(this));
  this.hammer.get('pan').set({ threshold: 5, direction: 30 }); // 30 is ALL_DIRECTIONS in hammer.

  // single select (or unselect) when tapping an item
  this.hammer.on('tap', this._onSelectItem.bind(this));

  // multi select when holding mouse/touch, or on ctrl+click
  this.hammer.on('press', this._onMultiSelectItem.bind(this));

  // add item on doubletap
  this.hammer.on('doubletap', this._onAddItem.bind(this));

  this.groupHammer = new Hammer(this.body.dom.leftContainer);
  this.groupHammer.on('panstart', this._onGroupDragStart.bind(this));
  this.groupHammer.on('panmove', this._onGroupDrag.bind(this));
  this.groupHammer.on('panend', this._onGroupDragEnd.bind(this));
  this.groupHammer.get('pan').set({ threshold: 5, direction: 30 });

  // attach to the DOM
  this.show();
};

/**
 * Set options for the ItemSet. Existing options will be extended/overwritten.
 * @param {Object} [options] The following options are available:
 *                           {String} type
 *                              Default type for the items. Choose from 'box'
 *                              (default), 'point', 'range', or 'background'.
 *                              The default style can be overwritten by
 *                              individual items.
 *                           {String} align
 *                              Alignment for the items, only applicable for
 *                              BoxItem. Choose 'center' (default), 'left', or
 *                              'right'.
 *                           {String} orientation.item
 *                              Orientation of the item set. Choose 'top' or
 *                              'bottom' (default).
 *                           {Function} groupOrder
 *                              A sorting function for ordering groups
 *                           {Boolean} stack
 *                              If true (default), items will be stacked on
 *                              top of each other.
 *                           {Number} margin.axis
 *                              Margin between the axis and the items in pixels.
 *                              Default is 20.
 *                           {Number} margin.item.horizontal
 *                              Horizontal margin between items in pixels.
 *                              Default is 10.
 *                           {Number} margin.item.vertical
 *                              Vertical Margin between items in pixels.
 *                              Default is 10.
 *                           {Number} margin.item
 *                              Margin between items in pixels in both horizontal
 *                              and vertical direction. Default is 10.
 *                           {Number} margin
 *                              Set margin for both axis and items in pixels.
 *                           {Boolean} selectable
 *                              If true (default), items can be selected.
 *                           {Boolean} multiselect
 *                              If true, multiple items can be selected.
 *                              False by default.
 *                           {Boolean} editable
 *                              Set all editable options to true or false
 *                           {Boolean} editable.updateTime
 *                              Allow dragging an item to an other moment in time
 *                           {Boolean} editable.updateGroup
 *                              Allow dragging an item to an other group
 *                           {Boolean} editable.add
 *                              Allow creating new items on double tap
 *                           {Boolean} editable.remove
 *                              Allow removing items by clicking the delete button
 *                              top right of a selected item.
 *                           {Function(item: Item, callback: Function)} onAdd
 *                              Callback function triggered when an item is about to be added:
 *                              when the user double taps an empty space in the Timeline.
 *                           {Function(item: Item, callback: Function)} onUpdate
 *                              Callback function fired when an item is about to be updated.
 *                              This function typically has to show a dialog where the user
 *                              change the item. If not implemented, nothing happens.
 *                           {Function(item: Item, callback: Function)} onMove
 *                              Fired when an item has been moved. If not implemented,
 *                              the move action will be accepted.
 *                           {Function(item: Item, callback: Function)} onRemove
 *                              Fired when an item is about to be deleted.
 *                              If not implemented, the item will be always removed.
 */
ItemSet.prototype.setOptions = function (options) {
  if (options) {
    // copy all options that we know
    var fields = ['type', 'align', 'order', 'stack', 'selectable', 'multiselect', 'itemsAlwaysDraggable', 'multiselectPerGroup', 'groupOrder', 'dataAttributes', 'template', 'groupTemplate', 'hide', 'snap', 'groupOrderSwap'];
    util.selectiveExtend(fields, this.options, options);

    if ('orientation' in options) {
      if (typeof options.orientation === 'string') {
        this.options.orientation.item = options.orientation === 'top' ? 'top' : 'bottom';
      } else if (typeof options.orientation === 'object' && 'item' in options.orientation) {
        this.options.orientation.item = options.orientation.item;
      }
    }

    if ('margin' in options) {
      if (typeof options.margin === 'number') {
        this.options.margin.axis = options.margin;
        this.options.margin.item.horizontal = options.margin;
        this.options.margin.item.vertical = options.margin;
      } else if (typeof options.margin === 'object') {
        util.selectiveExtend(['axis'], this.options.margin, options.margin);
        if ('item' in options.margin) {
          if (typeof options.margin.item === 'number') {
            this.options.margin.item.horizontal = options.margin.item;
            this.options.margin.item.vertical = options.margin.item;
          } else if (typeof options.margin.item === 'object') {
            util.selectiveExtend(['horizontal', 'vertical'], this.options.margin.item, options.margin.item);
          }
        }
      }
    }

    if ('editable' in options) {
      if (typeof options.editable === 'boolean') {
        this.options.editable.updateTime = options.editable;
        this.options.editable.updateGroup = options.editable;
        this.options.editable.add = options.editable;
        this.options.editable.remove = options.editable;
      } else if (typeof options.editable === 'object') {
        util.selectiveExtend(['updateTime', 'updateGroup', 'add', 'remove'], this.options.editable, options.editable);
      }
    }

    if ('groupEditable' in options) {
      if (typeof options.groupEditable === 'boolean') {
        this.options.groupEditable.order = options.groupEditable;
        this.options.groupEditable.add = options.groupEditable;
        this.options.groupEditable.remove = options.groupEditable;
      } else if (typeof options.groupEditable === 'object') {
        util.selectiveExtend(['order', 'add', 'remove'], this.options.groupEditable, options.groupEditable);
      }
    }

    // callback functions
    var addCallback = (function (name) {
      var fn = options[name];
      if (fn) {
        if (!(fn instanceof Function)) {
          throw new Error('option ' + name + ' must be a function ' + name + '(item, callback)');
        }
        this.options[name] = fn;
      }
    }).bind(this);
    ['onAdd', 'onUpdate', 'onRemove', 'onMove', 'onMoving', 'onAddGroup', 'onMoveGroup', 'onRemoveGroup'].forEach(addCallback);

    // force the itemSet to refresh: options like orientation and margins may be changed
    this.markDirty();
  }
};

/**
 * Mark the ItemSet dirty so it will refresh everything with next redraw.
 * Optionally, all items can be marked as dirty and be refreshed.
 * @param {{refreshItems: boolean}} [options]
 */
ItemSet.prototype.markDirty = function (options) {
  this.groupIds = [];
  this.stackDirty = true;

  if (options && options.refreshItems) {
    util.forEach(this.items, function (item) {
      item.dirty = true;
      if (item.displayed) item.redraw();
    });
  }
};

/**
 * Destroy the ItemSet
 */
ItemSet.prototype.destroy = function () {
  this.hide();
  this.setItems(null);
  this.setGroups(null);

  this.hammer = null;

  this.body = null;
  this.conversion = null;
};

/**
 * Hide the component from the DOM
 */
ItemSet.prototype.hide = function () {
  // remove the frame containing the items
  if (this.dom.frame.parentNode) {
    this.dom.frame.parentNode.removeChild(this.dom.frame);
  }

  // remove the axis with dots
  if (this.dom.axis.parentNode) {
    this.dom.axis.parentNode.removeChild(this.dom.axis);
  }

  // remove the labelset containing all group labels
  if (this.dom.labelSet.parentNode) {
    this.dom.labelSet.parentNode.removeChild(this.dom.labelSet);
  }
};

/**
 * Show the component in the DOM (when not already visible).
 * @return {Boolean} changed
 */
ItemSet.prototype.show = function () {
  // show frame containing the items
  if (!this.dom.frame.parentNode) {
    this.body.dom.center.appendChild(this.dom.frame);
  }

  // show axis with dots
  if (!this.dom.axis.parentNode) {
    this.body.dom.backgroundVertical.appendChild(this.dom.axis);
  }

  // show labelset containing labels
  if (!this.dom.labelSet.parentNode) {
    this.body.dom.left.appendChild(this.dom.labelSet);
  }
};

/**
 * Set selected items by their id. Replaces the current selection
 * Unknown id's are silently ignored.
 * @param {string[] | string} [ids] An array with zero or more id's of the items to be
 *                                  selected, or a single item id. If ids is undefined
 *                                  or an empty array, all items will be unselected.
 */
ItemSet.prototype.setSelection = function (ids) {
  var i, ii, id, item;

  if (ids == undefined) ids = [];
  if (!Array.isArray(ids)) ids = [ids];

  // unselect currently selected items
  for (i = 0, ii = this.selection.length; i < ii; i++) {
    id = this.selection[i];
    item = this.items[id];
    if (item) item.unselect();
  }

  // select items
  this.selection = [];
  for (i = 0, ii = ids.length; i < ii; i++) {
    id = ids[i];
    item = this.items[id];
    if (item) {
      this.selection.push(id);
      item.select();
    }
  }
};

/**
 * Get the selected items by their id
 * @return {Array} ids  The ids of the selected items
 */
ItemSet.prototype.getSelection = function () {
  return this.selection.concat([]);
};

/**
 * Get the id's of the currently visible items.
 * @returns {Array} The ids of the visible items
 */
ItemSet.prototype.getVisibleItems = function () {
  var range = this.body.range.getRange();
  var left = this.body.util.toScreen(range.start);
  var right = this.body.util.toScreen(range.end);

  var ids = [];
  for (var groupId in this.groups) {
    if (this.groups.hasOwnProperty(groupId)) {
      var group = this.groups[groupId];
      var rawVisibleItems = group.visibleItems;

      // filter the "raw" set with visibleItems into a set which is really
      // visible by pixels
      for (var i = 0; i < rawVisibleItems.length; i++) {
        var item = rawVisibleItems[i];
        // TODO: also check whether visible vertically
        if (item.left < right && item.left + item.width > left) {
          ids.push(item.id);
        }
      }
    }
  }

  return ids;
};

/**
 * Deselect a selected item
 * @param {String | Number} id
 * @private
 */
ItemSet.prototype._deselect = function (id) {
  var selection = this.selection;
  for (var i = 0, ii = selection.length; i < ii; i++) {
    if (selection[i] == id) {
      // non-strict comparison!
      selection.splice(i, 1);
      break;
    }
  }
};

/**
 * Repaint the component
 * @return {boolean} Returns true if the component is resized
 */
ItemSet.prototype.redraw = function () {
  var margin = this.options.margin,
      range = this.body.range,
      asSize = util.option.asSize,
      options = this.options,
      orientation = options.orientation.item,
      resized = false,
      frame = this.dom.frame;

  // recalculate absolute position (before redrawing groups)
  this.props.top = this.body.domProps.top.height + this.body.domProps.border.top;
  this.props.left = this.body.domProps.left.width + this.body.domProps.border.left;

  // update class name
  frame.className = 'vis-itemset';

  // reorder the groups (if needed)
  resized = this._orderGroups() || resized;

  // check whether zoomed (in that case we need to re-stack everything)
  // TODO: would be nicer to get this as a trigger from Range
  var visibleInterval = range.end - range.start;
  var zoomed = visibleInterval != this.lastVisibleInterval || this.props.width != this.props.lastWidth;
  if (zoomed) this.stackDirty = true;
  this.lastVisibleInterval = visibleInterval;
  this.props.lastWidth = this.props.width;

  var restack = this.stackDirty;
  var firstGroup = this._firstGroup();
  var firstMargin = {
    item: margin.item,
    axis: margin.axis
  };
  var nonFirstMargin = {
    item: margin.item,
    axis: margin.item.vertical / 2
  };
  var height = 0;
  var minHeight = margin.axis + margin.item.vertical;

  // redraw the background group
  this.groups[BACKGROUND].redraw(range, nonFirstMargin, restack);

  // redraw all regular groups
  util.forEach(this.groups, function (group) {
    var groupMargin = group == firstGroup ? firstMargin : nonFirstMargin;
    var groupResized = group.redraw(range, groupMargin, restack);
    resized = groupResized || resized;
    height += group.height;
  });
  height = Math.max(height, minHeight);
  this.stackDirty = false;

  // update frame height
  frame.style.height = asSize(height);

  // calculate actual size
  this.props.width = frame.offsetWidth;
  this.props.height = height;

  // reposition axis
  this.dom.axis.style.top = asSize(orientation == 'top' ? this.body.domProps.top.height + this.body.domProps.border.top : this.body.domProps.top.height + this.body.domProps.centerContainer.height);
  this.dom.axis.style.left = '0';

  // check if this component is resized
  resized = this._isResized() || resized;

  return resized;
};

/**
 * Get the first group, aligned with the axis
 * @return {Group | null} firstGroup
 * @private
 */
ItemSet.prototype._firstGroup = function () {
  var firstGroupIndex = this.options.orientation.item == 'top' ? 0 : this.groupIds.length - 1;
  var firstGroupId = this.groupIds[firstGroupIndex];
  var firstGroup = this.groups[firstGroupId] || this.groups[UNGROUPED];

  return firstGroup || null;
};

/**
 * Create or delete the group holding all ungrouped items. This group is used when
 * there are no groups specified.
 * @protected
 */
ItemSet.prototype._updateUngrouped = function () {
  var ungrouped = this.groups[UNGROUPED];
  var background = this.groups[BACKGROUND];
  var item, itemId;

  if (this.groupsData) {
    // remove the group holding all ungrouped items
    if (ungrouped) {
      ungrouped.hide();
      delete this.groups[UNGROUPED];

      for (itemId in this.items) {
        if (this.items.hasOwnProperty(itemId)) {
          item = this.items[itemId];
          item.parent && item.parent.remove(item);
          var groupId = this._getGroupId(item.data);
          var group = this.groups[groupId];
          group && group.add(item) || item.hide();
        }
      }
    }
  } else {
    // create a group holding all (unfiltered) items
    if (!ungrouped) {
      var id = null;
      var data = null;
      ungrouped = new Group(id, data, this);
      this.groups[UNGROUPED] = ungrouped;

      for (itemId in this.items) {
        if (this.items.hasOwnProperty(itemId)) {
          item = this.items[itemId];
          ungrouped.add(item);
        }
      }

      ungrouped.show();
    }
  }
};

/**
 * Get the element for the labelset
 * @return {HTMLElement} labelSet
 */
ItemSet.prototype.getLabelSet = function () {
  return this.dom.labelSet;
};

/**
 * Set items
 * @param {vis.DataSet | null} items
 */
ItemSet.prototype.setItems = function (items) {
  var me = this,
      ids,
      oldItemsData = this.itemsData;

  // replace the dataset
  if (!items) {
    this.itemsData = null;
  } else if (items instanceof DataSet || items instanceof DataView) {
    this.itemsData = items;
  } else {
    throw new TypeError('Data must be an instance of DataSet or DataView');
  }

  if (oldItemsData) {
    // unsubscribe from old dataset
    util.forEach(this.itemListeners, function (callback, event) {
      oldItemsData.off(event, callback);
    });

    // remove all drawn items
    ids = oldItemsData.getIds();
    this._onRemove(ids);
  }

  if (this.itemsData) {
    // subscribe to new dataset
    var id = this.id;
    util.forEach(this.itemListeners, function (callback, event) {
      me.itemsData.on(event, callback, id);
    });

    // add all new items
    ids = this.itemsData.getIds();
    this._onAdd(ids);

    // update the group holding all ungrouped items
    this._updateUngrouped();
  }
};

/**
 * Get the current items
 * @returns {vis.DataSet | null}
 */
ItemSet.prototype.getItems = function () {
  return this.itemsData;
};

/**
 * Set groups
 * @param {vis.DataSet} groups
 */
ItemSet.prototype.setGroups = function (groups) {
  var me = this,
      ids;

  // unsubscribe from current dataset
  if (this.groupsData) {
    util.forEach(this.groupListeners, function (callback, event) {
      me.groupsData.off(event, callback);
    });

    // remove all drawn groups
    ids = this.groupsData.getIds();
    this.groupsData = null;
    this._onRemoveGroups(ids); // note: this will cause a redraw
  }

  // replace the dataset
  if (!groups) {
    this.groupsData = null;
  } else if (groups instanceof DataSet || groups instanceof DataView) {
    this.groupsData = groups;
  } else {
    throw new TypeError('Data must be an instance of DataSet or DataView');
  }

  if (this.groupsData) {
    // subscribe to new dataset
    var id = this.id;
    util.forEach(this.groupListeners, function (callback, event) {
      me.groupsData.on(event, callback, id);
    });

    // draw all ms
    ids = this.groupsData.getIds();
    this._onAddGroups(ids);
  }

  // update the group holding all ungrouped items
  this._updateUngrouped();

  // update the order of all items in each group
  this._order();

  this.body.emitter.emit('change', { queue: true });
};

/**
 * Get the current groups
 * @returns {vis.DataSet | null} groups
 */
ItemSet.prototype.getGroups = function () {
  return this.groupsData;
};

/**
 * Remove an item by its id
 * @param {String | Number} id
 */
ItemSet.prototype.removeItem = function (id) {
  var item = this.itemsData.get(id),
      dataset = this.itemsData.getDataSet();

  if (item) {
    // confirm deletion
    this.options.onRemove(item, function (item) {
      if (item) {
        // remove by id here, it is possible that an item has no id defined
        // itself, so better not delete by the item itself
        dataset.remove(id);
      }
    });
  }
};

/**
 * Get the time of an item based on it's data and options.type
 * @param {Object} itemData
 * @returns {string} Returns the type
 * @private
 */
ItemSet.prototype._getType = function (itemData) {
  return itemData.type || this.options.type || (itemData.end ? 'range' : 'box');
};

/**
 * Get the group id for an item
 * @param {Object} itemData
 * @returns {string} Returns the groupId
 * @private
 */
ItemSet.prototype._getGroupId = function (itemData) {
  var type = this._getType(itemData);
  if (type == 'background' && itemData.group == undefined) {
    return BACKGROUND;
  } else {
    return this.groupsData ? itemData.group : UNGROUPED;
  }
};

/**
 * Handle updated items
 * @param {Number[]} ids
 * @protected
 */
ItemSet.prototype._onUpdate = function (ids) {
  var me = this;

  ids.forEach((function (id) {
    var itemData = me.itemsData.get(id, me.itemOptions);
    var item = me.items[id];
    var type = me._getType(itemData);

    var constructor = ItemSet.types[type];
    var selected;

    if (item) {
      // update item
      if (!constructor || !(item instanceof constructor)) {
        // item type has changed, delete the item and recreate it
        selected = item.selected; // preserve selection of this item
        me._removeItem(item);
        item = null;
      } else {
        me._updateItem(item, itemData);
      }
    }

    if (!item) {
      // create item
      if (constructor) {
        item = new constructor(itemData, me.conversion, me.options);
        item.id = id; // TODO: not so nice setting id afterwards
        me._addItem(item);
        if (selected) {
          this.selection.push(id);
          item.select();
        }
      } else if (type == 'rangeoverflow') {
        // TODO: deprecated since version 2.1.0 (or 3.0.0?). cleanup some day
        throw new TypeError('Item type "rangeoverflow" is deprecated. Use css styling instead: ' + '.vis-item.vis-range .vis-item-content {overflow: visible;}');
      } else {
        throw new TypeError('Unknown item type "' + type + '"');
      }
    }
  }).bind(this));

  this._order();
  this.stackDirty = true; // force re-stacking of all items next redraw
  this.body.emitter.emit('change', { queue: true });
};

/**
 * Handle added items
 * @param {Number[]} ids
 * @protected
 */
ItemSet.prototype._onAdd = ItemSet.prototype._onUpdate;

/**
 * Handle removed items
 * @param {Number[]} ids
 * @protected
 */
ItemSet.prototype._onRemove = function (ids) {
  var count = 0;
  var me = this;
  ids.forEach(function (id) {
    var item = me.items[id];
    if (item) {
      count++;
      me._removeItem(item);
    }
  });

  if (count) {
    // update order
    this._order();
    this.stackDirty = true; // force re-stacking of all items next redraw
    this.body.emitter.emit('change', { queue: true });
  }
};

/**
 * Update the order of item in all groups
 * @private
 */
ItemSet.prototype._order = function () {
  // reorder the items in all groups
  // TODO: optimization: only reorder groups affected by the changed items
  util.forEach(this.groups, function (group) {
    group.order();
  });
};

/**
 * Handle updated groups
 * @param {Number[]} ids
 * @private
 */
ItemSet.prototype._onUpdateGroups = function (ids) {
  this._onAddGroups(ids);
};

/**
 * Handle changed groups (added or updated)
 * @param {Number[]} ids
 * @private
 */
ItemSet.prototype._onAddGroups = function (ids) {
  var me = this;

  ids.forEach(function (id) {
    var groupData = me.groupsData.get(id);
    var group = me.groups[id];

    if (!group) {
      // check for reserved ids
      if (id == UNGROUPED || id == BACKGROUND) {
        throw new Error('Illegal group id. ' + id + ' is a reserved id.');
      }

      var groupOptions = Object.create(me.options);
      util.extend(groupOptions, {
        height: null
      });

      group = new Group(id, groupData, me);
      me.groups[id] = group;

      // add items with this groupId to the new group
      for (var itemId in me.items) {
        if (me.items.hasOwnProperty(itemId)) {
          var item = me.items[itemId];
          if (item.data.group == id) {
            group.add(item);
          }
        }
      }

      group.order();
      group.show();
    } else {
      // update group
      group.setData(groupData);
    }
  });

  this.body.emitter.emit('change', { queue: true });
};

/**
 * Handle removed groups
 * @param {Number[]} ids
 * @private
 */
ItemSet.prototype._onRemoveGroups = function (ids) {
  var groups = this.groups;
  ids.forEach(function (id) {
    var group = groups[id];

    if (group) {
      group.hide();
      delete groups[id];
    }
  });

  this.markDirty();

  this.body.emitter.emit('change', { queue: true });
};

/**
 * Reorder the groups if needed
 * @return {boolean} changed
 * @private
 */
ItemSet.prototype._orderGroups = function () {
  if (this.groupsData) {
    // reorder the groups
    var groupIds = this.groupsData.getIds({
      order: this.options.groupOrder
    });

    var changed = !util.equalArray(groupIds, this.groupIds);
    if (changed) {
      // hide all groups, removes them from the DOM
      var groups = this.groups;
      groupIds.forEach(function (groupId) {
        groups[groupId].hide();
      });

      // show the groups again, attach them to the DOM in correct order
      groupIds.forEach(function (groupId) {
        groups[groupId].show();
      });

      this.groupIds = groupIds;
    }

    return changed;
  } else {
    return false;
  }
};

/**
 * Add a new item
 * @param {Item} item
 * @private
 */
ItemSet.prototype._addItem = function (item) {
  this.items[item.id] = item;

  // add to group
  var groupId = this._getGroupId(item.data);
  var group = this.groups[groupId];
  if (group) group.add(item);
};

/**
 * Update an existing item
 * @param {Item} item
 * @param {Object} itemData
 * @private
 */
ItemSet.prototype._updateItem = function (item, itemData) {
  var oldGroupId = item.data.group;
  var oldSubGroupId = item.data.subgroup;

  // update the items data (will redraw the item when displayed)
  item.setData(itemData);

  // update group
  if (oldGroupId != item.data.group || oldSubGroupId != item.data.subgroup) {
    var oldGroup = this.groups[oldGroupId];
    if (oldGroup) oldGroup.remove(item);

    var groupId = this._getGroupId(item.data);
    var group = this.groups[groupId];
    if (group) group.add(item);
  }
};

/**
 * Delete an item from the ItemSet: remove it from the DOM, from the map
 * with items, and from the map with visible items, and from the selection
 * @param {Item} item
 * @private
 */
ItemSet.prototype._removeItem = function (item) {
  // remove from DOM
  item.hide();

  // remove from items
  delete this.items[item.id];

  // remove from selection
  var index = this.selection.indexOf(item.id);
  if (index != -1) this.selection.splice(index, 1);

  // remove from group
  item.parent && item.parent.remove(item);
};

/**
 * Create an array containing all items being a range (having an end date)
 * @param array
 * @returns {Array}
 * @private
 */
ItemSet.prototype._constructByEndArray = function (array) {
  var endArray = [];

  for (var i = 0; i < array.length; i++) {
    if (array[i] instanceof RangeItem) {
      endArray.push(array[i]);
    }
  }
  return endArray;
};

/**
 * Register the clicked item on touch, before dragStart is initiated.
 *
 * dragStart is initiated from a mousemove event, AFTER the mouse/touch is
 * already moving. Therefore, the mouse/touch can sometimes be above an other
 * DOM element than the item itself.
 *
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onTouch = function (event) {
  // store the touched item, used in _onDragStart
  this.touchParams.item = this.itemFromTarget(event);
  this.touchParams.dragLeftItem = event.target.dragLeftItem || false;
  this.touchParams.dragRightItem = event.target.dragRightItem || false;
  this.touchParams.itemProps = null;
};

/**
 * Given an group id, returns the index it has.
 *
 * @param {Number} groupID
 * @private
 */
ItemSet.prototype._getGroupIndex = function (groupId) {
  for (var i = 0; i < this.groupIds.length; i++) {
    if (groupId == this.groupIds[i]) return i;
  }
};

/**
 * Start dragging the selected events
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onDragStart = function (event) {
  var item = this.touchParams.item || null;
  var me = this;
  var props;

  if (item && (item.selected || this.options.itemsAlwaysDraggable)) {

    if (!this.options.editable.updateTime && !this.options.editable.updateGroup && !item.editable) {
      return;
    }

    // override options.editable
    if (item.editable === false) {
      return;
    }

    var dragLeftItem = this.touchParams.dragLeftItem;
    var dragRightItem = this.touchParams.dragRightItem;

    if (dragLeftItem) {
      props = {
        item: dragLeftItem,
        initialX: event.center.x,
        dragLeft: true,
        data: this._cloneItemData(item.data)
      };

      this.touchParams.itemProps = [props];
    } else if (dragRightItem) {
      props = {
        item: dragRightItem,
        initialX: event.center.x,
        dragRight: true,
        data: this._cloneItemData(item.data)
      };

      this.touchParams.itemProps = [props];
    } else {
      this.touchParams.selectedItem = item;

      var baseGroupIndex = this._getGroupIndex(item.data.group);

      var itemsToDrag = this.options.itemsAlwaysDraggable && !item.selected ? [item.id] : this.getSelection();

      this.touchParams.itemProps = itemsToDrag.map((function (id) {
        var item = me.items[id];
        var groupIndex = me._getGroupIndex(item.data.group);
        return {
          item: item,
          initialX: event.center.x,
          groupOffset: baseGroupIndex - groupIndex,
          data: this._cloneItemData(item.data)
        };
      }).bind(this));
    }

    event.stopPropagation();
  } else if (this.options.editable.add && (event.srcEvent.ctrlKey || event.srcEvent.metaKey)) {
    // create a new range item when dragging with ctrl key down
    this._onDragStartAddItem(event);
  }
};

/**
 * Start creating a new range item by dragging.
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onDragStartAddItem = function (event) {
  var snap = this.options.snap || null;
  var xAbs = util.getAbsoluteLeft(this.dom.frame);
  var x = event.center.x - xAbs - 10; // minus 10 to compensate for the drag starting as soon as you've moved 10px
  var time = this.body.util.toTime(x);
  var scale = this.body.util.getScale();
  var step = this.body.util.getStep();
  var start = snap ? snap(time, scale, step) : time;
  var end = start;

  var itemData = {
    type: 'range',
    start: start,
    end: end,
    content: 'new item'
  };

  var id = util.randomUUID();
  itemData[this.itemsData._fieldId] = id;

  var group = this.groupFromTarget(event);
  if (group) {
    itemData.group = group.groupId;
  }

  var newItem = new RangeItem(itemData, this.conversion, this.options);
  newItem.id = id; // TODO: not so nice setting id afterwards
  newItem.data = this._cloneItemData(itemData);
  this._addItem(newItem);

  var props = {
    item: newItem,
    dragRight: true,
    initialX: event.center.x,
    data: newItem.data
  };
  this.touchParams.itemProps = [props];

  event.stopPropagation();
};

/**
 * Drag selected items
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onDrag = function (event) {
  if (this.touchParams.itemProps) {
    event.stopPropagation();

    var me = this;
    var snap = this.options.snap || null;
    var xOffset = this.body.dom.root.offsetLeft + this.body.domProps.left.width;
    var scale = this.body.util.getScale();
    var step = this.body.util.getStep();

    //only calculate the new group for the item that's actually dragged
    var selectedItem = this.touchParams.selectedItem;
    var updateGroupAllowed = me.options.editable.updateGroup;
    var newGroupBase = null;
    if (updateGroupAllowed && selectedItem) {
      if (selectedItem.data.group != undefined) {
        // drag from one group to another
        var group = me.groupFromTarget(event);
        if (group) {
          //we know the offset for all items, so the new group for all items
          //will be relative to this one.
          newGroupBase = this._getGroupIndex(group.groupId);
        }
      }
    }

    // move
    this.touchParams.itemProps.forEach((function (props) {
      var current = me.body.util.toTime(event.center.x - xOffset);
      var initial = me.body.util.toTime(props.initialX - xOffset);
      var offset = current - initial; // ms

      var itemData = this._cloneItemData(props.item.data); // clone the data
      if (props.item.editable === false) {
        return;
      }

      var updateTimeAllowed = me.options.editable.updateTime || props.item.editable === true;

      if (updateTimeAllowed) {
        if (props.dragLeft) {
          // drag left side of a range item
          if (itemData.start != undefined) {
            var initialStart = util.convert(props.data.start, 'Date');
            var start = new Date(initialStart.valueOf() + offset);
            // TODO: pass a Moment instead of a Date to snap(). (Breaking change)
            itemData.start = snap ? snap(start, scale, step) : start;
          }
        } else if (props.dragRight) {
          // drag right side of a range item
          if (itemData.end != undefined) {
            var initialEnd = util.convert(props.data.end, 'Date');
            var end = new Date(initialEnd.valueOf() + offset);
            // TODO: pass a Moment instead of a Date to snap(). (Breaking change)
            itemData.end = snap ? snap(end, scale, step) : end;
          }
        } else {
          // drag both start and end
          if (itemData.start != undefined) {
            var initialStart = util.convert(props.data.start, 'Date').valueOf();
            var start = new Date(initialStart + offset);

            if (itemData.end != undefined) {
              var initialEnd = util.convert(props.data.end, 'Date');
              var duration = initialEnd.valueOf() - initialStart.valueOf();

              // TODO: pass a Moment instead of a Date to snap(). (Breaking change)
              itemData.start = snap ? snap(start, scale, step) : start;
              itemData.end = new Date(itemData.start.valueOf() + duration);
            } else {
              // TODO: pass a Moment instead of a Date to snap(). (Breaking change)
              itemData.start = snap ? snap(start, scale, step) : start;
            }
          }
        }
      }

      var updateGroupAllowed = me.options.editable.updateGroup || props.item.editable === true;

      if (updateGroupAllowed && !props.dragLeft && !props.dragRight && newGroupBase != null) {
        if (itemData.group != undefined) {
          var newOffset = newGroupBase - props.groupOffset;

          //make sure we stay in bounds
          newOffset = Math.max(0, newOffset);
          newOffset = Math.min(me.groupIds.length - 1, newOffset);

          itemData.group = me.groupIds[newOffset];
        }
      }

      // confirm moving the item
      itemData = this._cloneItemData(itemData); // convert start and end to the correct type
      me.options.onMoving(itemData, (function (itemData) {
        if (itemData) {
          props.item.setData(this._cloneItemData(itemData, 'Date'));
        }
      }).bind(this));
    }).bind(this));

    this.stackDirty = true; // force re-stacking of all items next redraw
    this.body.emitter.emit('change');
  }
};

/**
 * Move an item to another group
 * @param {Item} item
 * @param {String | Number} groupId
 * @private
 */
ItemSet.prototype._moveToGroup = function (item, groupId) {
  var group = this.groups[groupId];
  if (group && group.groupId != item.data.group) {
    var oldGroup = item.parent;
    oldGroup.remove(item);
    oldGroup.order();
    group.add(item);
    group.order();

    item.data.group = group.groupId;
  }
};

/**
 * End of dragging selected items
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onDragEnd = function (event) {
  if (this.touchParams.itemProps) {
    event.stopPropagation();

    var me = this;
    var dataset = this.itemsData.getDataSet();
    var itemProps = this.touchParams.itemProps;
    this.touchParams.itemProps = null;

    itemProps.forEach((function (props) {
      var id = props.item.id;
      var exists = me.itemsData.get(id, me.itemOptions) != null;

      if (!exists) {
        // add a new item
        me.options.onAdd(props.item.data, function (itemData) {
          me._removeItem(props.item); // remove temporary item
          if (itemData) {
            me.itemsData.getDataSet().add(itemData);
          }

          // force re-stacking of all items next redraw
          me.stackDirty = true;
          me.body.emitter.emit('change');
        });
      } else {
        // update existing item
        var itemData = this._cloneItemData(props.item.data); // convert start and end to the correct type
        me.options.onMove(itemData, function (itemData) {
          if (itemData) {
            // apply changes
            itemData[dataset._fieldId] = id; // ensure the item contains its id (can be undefined)
            dataset.update(itemData);
          } else {
            // restore original values
            props.item.setData(props.data);

            me.stackDirty = true; // force re-stacking of all items next redraw
            me.body.emitter.emit('change');
          }
        });
      }
    }).bind(this));
  }
};

ItemSet.prototype._onGroupDragStart = function (event) {
  if (this.options.groupEditable.order) {
    this.groupTouchParams.group = this.groupFromTarget(event);

    if (this.groupTouchParams.group) {
      event.stopPropagation();

      this.groupTouchParams.originalOrder = this.groupsData.getIds({
        order: this.options.groupOrder
      });
    }
  }
};

ItemSet.prototype._onGroupDrag = function (event) {
  if (this.options.groupEditable.order && this.groupTouchParams.group) {
    event.stopPropagation();

    // drag from one group to another
    var group = this.groupFromTarget(event);

    // try to avoid toggling when groups differ in height
    if (group && group.height != this.groupTouchParams.group.height) {
      var movingUp = group.top < this.groupTouchParams.group.top;
      var clientY = event.center ? event.center.y : event.clientY;
      var targetGroupTop = util.getAbsoluteTop(group.dom.foreground);
      var draggedGroupHeight = this.groupTouchParams.group.height;
      if (movingUp) {
        // skip swapping the groups when the dragged group is not below clientY afterwards
        if (targetGroupTop + draggedGroupHeight < clientY) {
          return;
        }
      } else {
        var targetGroupHeight = group.height;
        // skip swapping the groups when the dragged group is not below clientY afterwards
        if (targetGroupTop + targetGroupHeight - draggedGroupHeight > clientY) {
          return;
        }
      }
    }

    if (group && group != this.groupTouchParams.group) {
      var groupsData = this.groupsData;
      var targetGroup = groupsData.get(group.groupId);
      var draggedGroup = groupsData.get(this.groupTouchParams.group.groupId);

      // switch groups
      if (draggedGroup && targetGroup) {
        this.options.groupOrderSwap(draggedGroup, targetGroup, this.groupsData);
        this.groupsData.update(draggedGroup);
        this.groupsData.update(targetGroup);
      }

      // fetch current order of groups
      var newOrder = this.groupsData.getIds({
        order: this.options.groupOrder
      });

      // in case of changes since _onGroupDragStart
      if (!util.equalArray(newOrder, this.groupTouchParams.originalOrder)) {
        var groupsData = this.groupsData;
        var origOrder = this.groupTouchParams.originalOrder;
        var draggedId = this.groupTouchParams.group.groupId;
        var numGroups = Math.min(origOrder.length, newOrder.length);
        var curPos = 0;
        var newOffset = 0;
        var orgOffset = 0;
        while (curPos < numGroups) {
          // as long as the groups are where they should be step down along the groups order
          while (curPos + newOffset < numGroups && curPos + orgOffset < numGroups && newOrder[curPos + newOffset] == origOrder[curPos + orgOffset]) {
            curPos++;
          }

          // all ok
          if (curPos + newOffset >= numGroups) {
            break;
          }

          // not all ok
          // if dragged group was move upwards everything below should have an offset
          if (newOrder[curPos + newOffset] == draggedId) {
            newOffset = 1;
            continue;
          }
          // if dragged group was move downwards everything above should have an offset
          else if (origOrder[curPos + orgOffset] == draggedId) {
              orgOffset = 1;
              continue;
            }
            // found a group (apart from dragged group) that has the wrong position -> switch with the
            // group at the position where other one should be, fix index arrays and continue
            else {
                var slippedPosition = newOrder.indexOf(origOrder[curPos + orgOffset]);
                var switchGroup = groupsData.get(newOrder[curPos + newOffset]);
                var shouldBeGroup = groupsData.get(origOrder[curPos + orgOffset]);
                this.options.groupOrderSwap(switchGroup, shouldBeGroup, groupsData);
                groupsData.update(switchGroup);
                groupsData.update(shouldBeGroup);

                var switchGroupId = newOrder[curPos + newOffset];
                newOrder[curPos + newOffset] = origOrder[curPos + orgOffset];
                newOrder[slippedPosition] = switchGroupId;

                curPos++;
              }
        }
      }
    }
  }
};

ItemSet.prototype._onGroupDragEnd = function (event) {
  if (this.options.groupEditable.order && this.groupTouchParams.group) {
    event.stopPropagation();

    // update existing group
    var me = this;
    var id = me.groupTouchParams.group.groupId;
    var dataset = me.groupsData.getDataSet();
    var groupData = util.extend({}, dataset.get(id)); // clone the data
    me.options.onMoveGroup(groupData, function (groupData) {
      if (groupData) {
        // apply changes
        groupData[dataset._fieldId] = id; // ensure the group contains its id (can be undefined)
        dataset.update(groupData);
      } else {

        // fetch current order of groups
        var newOrder = dataset.getIds({
          order: me.options.groupOrder
        });

        // restore original order
        if (!util.equalArray(newOrder, me.groupTouchParams.originalOrder)) {
          var origOrder = me.groupTouchParams.originalOrder;
          var numGroups = Math.min(origOrder.length, newOrder.length);
          var curPos = 0;
          while (curPos < numGroups) {
            // as long as the groups are where they should be step down along the groups order
            while (curPos < numGroups && newOrder[curPos] == origOrder[curPos]) {
              curPos++;
            }

            // all ok
            if (curPos >= numGroups) {
              break;
            }

            // found a group that has the wrong position -> switch with the
            // group at the position where other one should be, fix index arrays and continue
            var slippedPosition = newOrder.indexOf(origOrder[curPos]);
            var switchGroup = dataset.get(newOrder[curPos]);
            var shouldBeGroup = dataset.get(origOrder[curPos]);
            me.options.groupOrderSwap(switchGroup, shouldBeGroup, dataset);
            groupsData.update(switchGroup);
            groupsData.update(shouldBeGroup);

            var switchGroupId = newOrder[curPos];
            newOrder[curPos] = origOrder[curPos];
            newOrder[slippedPosition] = switchGroupId;

            curPos++;
          }
        }
      }
    });

    me.body.emitter.emit('groupDragged', { groupId: id });
  }
};

/**
 * Handle selecting/deselecting an item when tapping it
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onSelectItem = function (event) {
  if (!this.options.selectable) return;

  var ctrlKey = event.srcEvent && (event.srcEvent.ctrlKey || event.srcEvent.metaKey);
  var shiftKey = event.srcEvent && event.srcEvent.shiftKey;
  if (ctrlKey || shiftKey) {
    this._onMultiSelectItem(event);
    return;
  }

  var oldSelection = this.getSelection();

  var item = this.itemFromTarget(event);
  var selection = item ? [item.id] : [];
  this.setSelection(selection);

  var newSelection = this.getSelection();

  // emit a select event,
  // except when old selection is empty and new selection is still empty
  if (newSelection.length > 0 || oldSelection.length > 0) {
    this.body.emitter.emit('select', {
      items: newSelection,
      event: event
    });
  }
};

/**
 * Handle creation and updates of an item on double tap
 * @param event
 * @private
 */
ItemSet.prototype._onAddItem = function (event) {
  if (!this.options.selectable) return;
  if (!this.options.editable.add) return;

  var me = this;
  var snap = this.options.snap || null;
  var item = this.itemFromTarget(event);

  event.stopPropagation();

  if (item) {
    // update item

    // execute async handler to update the item (or cancel it)
    var itemData = me.itemsData.get(item.id); // get a clone of the data from the dataset
    this.options.onUpdate(itemData, function (itemData) {
      if (itemData) {
        me.itemsData.getDataSet().update(itemData);
      }
    });
  } else {
    // add item
    var xAbs = util.getAbsoluteLeft(this.dom.frame);
    var x = event.center.x - xAbs;
    var start = this.body.util.toTime(x);
    var scale = this.body.util.getScale();
    var step = this.body.util.getStep();

    var newItemData = {
      start: snap ? snap(start, scale, step) : start,
      content: 'new item'
    };

    // when default type is a range, add a default end date to the new item
    if (this.options.type === 'range') {
      var end = this.body.util.toTime(x + this.props.width / 5);
      newItemData.end = snap ? snap(end, scale, step) : end;
    }

    newItemData[this.itemsData._fieldId] = util.randomUUID();

    var group = this.groupFromTarget(event);
    if (group) {
      newItemData.group = group.groupId;
    }

    // execute async handler to customize (or cancel) adding an item
    newItemData = this._cloneItemData(newItemData); // convert start and end to the correct type
    this.options.onAdd(newItemData, function (item) {
      if (item) {
        me.itemsData.getDataSet().add(item);
        // TODO: need to trigger a redraw?
      }
    });
  }
};

/**
 * Handle selecting/deselecting multiple items when holding an item
 * @param {Event} event
 * @private
 */
ItemSet.prototype._onMultiSelectItem = function (event) {
  if (!this.options.selectable) return;

  var item = this.itemFromTarget(event);

  if (item) {
    // multi select items (if allowed)

    var selection = this.options.multiselect ? this.getSelection() // take current selection
    : []; // deselect current selection

    var shiftKey = event.srcEvent && event.srcEvent.shiftKey || false;

    if (shiftKey && this.options.multiselect) {
      // select all items between the old selection and the tapped item
      var itemGroup = this.itemsData.get(item.id).group;

      // when filtering get the group of the last selected item
      var lastSelectedGroup = undefined;
      if (this.options.multiselectPerGroup) {
        if (selection.length > 0) {
          lastSelectedGroup = this.itemsData.get(selection[0]).group;
        }
      }

      // determine the selection range
      if (!this.options.multiselectPerGroup || lastSelectedGroup == undefined || lastSelectedGroup == itemGroup) {
        selection.push(item.id);
      }
      var range = ItemSet._getItemRange(this.itemsData.get(selection, this.itemOptions));

      if (!this.options.multiselectPerGroup || lastSelectedGroup == itemGroup) {
        // select all items within the selection range
        selection = [];
        for (var id in this.items) {
          if (this.items.hasOwnProperty(id)) {
            var _item = this.items[id];
            var start = _item.data.start;
            var end = _item.data.end !== undefined ? _item.data.end : start;

            if (start >= range.min && end <= range.max && (!this.options.multiselectPerGroup || lastSelectedGroup == this.itemsData.get(_item.id).group) && !(_item instanceof BackgroundItem)) {
              selection.push(_item.id); // do not use id but item.id, id itself is stringified
            }
          }
        }
      }
    } else {
        // add/remove this item from the current selection
        var index = selection.indexOf(item.id);
        if (index == -1) {
          // item is not yet selected -> select it
          selection.push(item.id);
        } else {
          // item is already selected -> deselect it
          selection.splice(index, 1);
        }
      }

    this.setSelection(selection);

    this.body.emitter.emit('select', {
      items: this.getSelection(),
      event: event
    });
  }
};

/**
 * Calculate the time range of a list of items
 * @param {Array.<Object>} itemsData
 * @return {{min: Date, max: Date}} Returns the range of the provided items
 * @private
 */
ItemSet._getItemRange = function (itemsData) {
  var max = null;
  var min = null;

  itemsData.forEach(function (data) {
    if (min == null || data.start < min) {
      min = data.start;
    }

    if (data.end != undefined) {
      if (max == null || data.end > max) {
        max = data.end;
      }
    } else {
      if (max == null || data.start > max) {
        max = data.start;
      }
    }
  });

  return {
    min: min,
    max: max
  };
};

/**
 * Find an item from an event target:
 * searches for the attribute 'timeline-item' in the event target's element tree
 * @param {Event} event
 * @return {Item | null} item
 */
ItemSet.prototype.itemFromTarget = function (event) {
  var target = event.target;
  while (target) {
    if (target.hasOwnProperty('timeline-item')) {
      return target['timeline-item'];
    }
    target = target.parentNode;
  }

  return null;
};

/**
 * Find the Group from an event target:
 * searches for the attribute 'timeline-group' in the event target's element tree
 * @param {Event} event
 * @return {Group | null} group
 */
ItemSet.prototype.groupFromTarget = function (event) {
  var clientY = event.center ? event.center.y : event.clientY;
  for (var i = 0; i < this.groupIds.length; i++) {
    var groupId = this.groupIds[i];
    var group = this.groups[groupId];
    var foreground = group.dom.foreground;
    var top = util.getAbsoluteTop(foreground);
    if (clientY > top && clientY < top + foreground.offsetHeight) {
      return group;
    }

    if (this.options.orientation.item === 'top') {
      if (i === this.groupIds.length - 1 && clientY > top) {
        return group;
      }
    } else {
      if (i === 0 && clientY < top + foreground.offset) {
        return group;
      }
    }
  }

  return null;
};

/**
 * Find the ItemSet from an event target:
 * searches for the attribute 'timeline-itemset' in the event target's element tree
 * @param {Event} event
 * @return {ItemSet | null} item
 */
ItemSet.itemSetFromTarget = function (event) {
  var target = event.target;
  while (target) {
    if (target.hasOwnProperty('timeline-itemset')) {
      return target['timeline-itemset'];
    }
    target = target.parentNode;
  }

  return null;
};

/**
 * Clone the data of an item, and "normalize" it: convert the start and end date
 * to the type (Date, Moment, ...) configured in the DataSet. If not configured,
 * start and end are converted to Date.
 * @param {Object} itemData, typically `item.data`
 * @param {string} [type]  Optional Date type. If not provided, the type from the DataSet is taken
 * @return {Object} The cloned object
 * @private
 */
ItemSet.prototype._cloneItemData = function (itemData, type) {
  var clone = util.extend({}, itemData);

  if (!type) {
    // convert start and end date to the type (Date, Moment, ...) configured in the DataSet
    type = this.itemsData.getDataSet()._options.type;
  }

  if (clone.start != undefined) {
    clone.start = util.convert(clone.start, type && type.start || 'Date');
  }
  if (clone.end != undefined) {
    clone.end = util.convert(clone.end, type && type.end || 'Date');
  }

  return clone;
};

module.exports = ItemSet;

},{"../../DataSet":2,"../../DataView":3,"../../module/hammer":6,"../../util":33,"../TimeStep":17,"./BackgroundGroup":19,"./Component":20,"./Group":23,"./item/BackgroundItem":26,"./item/BoxItem":27,"./item/PointItem":29,"./item/RangeItem":30}],25:[function(require,module,exports){
'use strict';

var util = require('../../util');
var Component = require('./Component');
var TimeStep = require('../TimeStep');
var DateUtil = require('../DateUtil');
var moment = require('../../module/moment');

/**
 * A horizontal time axis
 * @param {{dom: Object, domProps: Object, emitter: Emitter, range: Range}} body
 * @param {Object} [options]        See TimeAxis.setOptions for the available
 *                                  options.
 * @constructor TimeAxis
 * @extends Component
 */
function TimeAxis(body, options) {
  this.dom = {
    foreground: null,
    lines: [],
    majorTexts: [],
    minorTexts: [],
    redundant: {
      lines: [],
      majorTexts: [],
      minorTexts: []
    }
  };
  this.props = {
    range: {
      start: 0,
      end: 0,
      minimumStep: 0
    },
    lineTop: 0
  };

  this.defaultOptions = {
    orientation: {
      axis: 'bottom'
    }, // axis orientation: 'top' or 'bottom'
    showMinorLabels: true,
    showMajorLabels: true,
    maxMinorChars: 7,
    format: TimeStep.FORMAT,
    moment: moment,
    timeAxis: null
  };
  this.options = util.extend({}, this.defaultOptions);

  this.body = body;

  // create the HTML DOM
  this._create();

  this.setOptions(options);
}

TimeAxis.prototype = new Component();

/**
 * Set options for the TimeAxis.
 * Parameters will be merged in current options.
 * @param {Object} options  Available options:
 *                          {string} [orientation.axis]
 *                          {boolean} [showMinorLabels]
 *                          {boolean} [showMajorLabels]
 */
TimeAxis.prototype.setOptions = function (options) {
  if (options) {
    // copy all options that we know
    util.selectiveExtend(['showMinorLabels', 'showMajorLabels', 'maxMinorChars', 'hiddenDates', 'timeAxis', 'moment'], this.options, options);

    // deep copy the format options
    util.selectiveDeepExtend(['format'], this.options, options);

    if ('orientation' in options) {
      if (typeof options.orientation === 'string') {
        this.options.orientation.axis = options.orientation;
      } else if (typeof options.orientation === 'object' && 'axis' in options.orientation) {
        this.options.orientation.axis = options.orientation.axis;
      }
    }

    // apply locale to moment.js
    // TODO: not so nice, this is applied globally to moment.js
    if ('locale' in options) {
      if (typeof moment.locale === 'function') {
        // moment.js 2.8.1+
        moment.locale(options.locale);
      } else {
        moment.lang(options.locale);
      }
    }
  }
};

/**
 * Create the HTML DOM for the TimeAxis
 */
TimeAxis.prototype._create = function () {
  this.dom.foreground = document.createElement('div');
  this.dom.background = document.createElement('div');

  this.dom.foreground.className = 'vis-time-axis vis-foreground';
  this.dom.background.className = 'vis-time-axis vis-background';
};

/**
 * Destroy the TimeAxis
 */
TimeAxis.prototype.destroy = function () {
  // remove from DOM
  if (this.dom.foreground.parentNode) {
    this.dom.foreground.parentNode.removeChild(this.dom.foreground);
  }
  if (this.dom.background.parentNode) {
    this.dom.background.parentNode.removeChild(this.dom.background);
  }

  this.body = null;
};

/**
 * Repaint the component
 * @return {boolean} Returns true if the component is resized
 */
TimeAxis.prototype.redraw = function () {
  var props = this.props;
  var foreground = this.dom.foreground;
  var background = this.dom.background;

  // determine the correct parent DOM element (depending on option orientation)
  var parent = this.options.orientation.axis == 'top' ? this.body.dom.top : this.body.dom.bottom;
  var parentChanged = foreground.parentNode !== parent;

  // calculate character width and height
  this._calculateCharSize();

  // TODO: recalculate sizes only needed when parent is resized or options is changed
  var showMinorLabels = this.options.showMinorLabels && this.options.orientation.axis !== 'none';
  var showMajorLabels = this.options.showMajorLabels && this.options.orientation.axis !== 'none';

  // determine the width and height of the elemens for the axis
  props.minorLabelHeight = showMinorLabels ? props.minorCharHeight : 0;
  props.majorLabelHeight = showMajorLabels ? props.majorCharHeight : 0;
  props.height = props.minorLabelHeight + props.majorLabelHeight;
  props.width = foreground.offsetWidth;

  props.minorLineHeight = this.body.domProps.root.height - props.majorLabelHeight - (this.options.orientation.axis == 'top' ? this.body.domProps.bottom.height : this.body.domProps.top.height);
  props.minorLineWidth = 1; // TODO: really calculate width
  props.majorLineHeight = props.minorLineHeight + props.majorLabelHeight;
  props.majorLineWidth = 1; // TODO: really calculate width

  //  take foreground and background offline while updating (is almost twice as fast)
  var foregroundNextSibling = foreground.nextSibling;
  var backgroundNextSibling = background.nextSibling;
  foreground.parentNode && foreground.parentNode.removeChild(foreground);
  background.parentNode && background.parentNode.removeChild(background);

  foreground.style.height = this.props.height + 'px';

  this._repaintLabels();

  // put DOM online again (at the same place)
  if (foregroundNextSibling) {
    parent.insertBefore(foreground, foregroundNextSibling);
  } else {
    parent.appendChild(foreground);
  }
  if (backgroundNextSibling) {
    this.body.dom.backgroundVertical.insertBefore(background, backgroundNextSibling);
  } else {
    this.body.dom.backgroundVertical.appendChild(background);
  }

  return this._isResized() || parentChanged;
};

/**
 * Repaint major and minor text labels and vertical grid lines
 * @private
 */
TimeAxis.prototype._repaintLabels = function () {
  var orientation = this.options.orientation.axis;

  // calculate range and step (step such that we have space for 7 characters per label)
  var start = util.convert(this.body.range.start, 'Number');
  var end = util.convert(this.body.range.end, 'Number');
  var timeLabelsize = this.body.util.toTime((this.props.minorCharWidth || 10) * this.options.maxMinorChars).valueOf();
  var minimumStep = timeLabelsize - DateUtil.getHiddenDurationBefore(this.options.moment, this.body.hiddenDates, this.body.range, timeLabelsize);
  minimumStep -= this.body.util.toTime(0).valueOf();

  var step = new TimeStep(new Date(start), new Date(end), minimumStep, this.body.hiddenDates);
  step.setMoment(this.options.moment);
  if (this.options.format) {
    step.setFormat(this.options.format);
  }
  if (this.options.timeAxis) {
    step.setScale(this.options.timeAxis);
  }
  this.step = step;

  // Move all DOM elements to a "redundant" list, where they
  // can be picked for re-use, and clear the lists with lines and texts.
  // At the end of the function _repaintLabels, left over elements will be cleaned up
  var dom = this.dom;
  dom.redundant.lines = dom.lines;
  dom.redundant.majorTexts = dom.majorTexts;
  dom.redundant.minorTexts = dom.minorTexts;
  dom.lines = [];
  dom.majorTexts = [];
  dom.minorTexts = [];

  var current;
  var next;
  var x;
  var xNext;
  var isMajor, nextIsMajor;
  var width = 0,
      prevWidth;
  var line;
  var labelMinor;
  var xFirstMajorLabel = undefined;
  var count = 0;
  var MAX = 1000;
  var className;

  step.start();
  next = step.getCurrent();
  xNext = this.body.util.toScreen(next);
  while (step.hasNext() && count < MAX) {
    count++;

    isMajor = step.isMajor();
    className = step.getClassName();
    labelMinor = step.getLabelMinor();

    current = next;
    x = xNext;

    step.next();
    next = step.getCurrent();
    nextIsMajor = step.isMajor();
    xNext = this.body.util.toScreen(next);

    prevWidth = width;
    width = xNext - x;
    var showMinorGrid = width >= prevWidth * 0.4; // prevent displaying of the 31th of the month on a scale of 5 days

    if (this.options.showMinorLabels && showMinorGrid) {
      var label = this._repaintMinorText(x, labelMinor, orientation, className);
      label.style.width = width + 'px'; // set width to prevent overflow
    }

    if (isMajor && this.options.showMajorLabels) {
      if (x > 0) {
        if (xFirstMajorLabel == undefined) {
          xFirstMajorLabel = x;
        }
        label = this._repaintMajorText(x, step.getLabelMajor(), orientation, className);
      }
      line = this._repaintMajorLine(x, width, orientation, className);
    } else {
      // minor line
      if (showMinorGrid) {
        line = this._repaintMinorLine(x, width, orientation, className);
      } else {
        if (line) {
          // adjust the width of the previous grid
          line.style.width = parseInt(line.style.width) + width + 'px';
        }
      }
    }
  }

  if (count === MAX && !warnedForOverflow) {
    console.warn('Something is wrong with the Timeline scale. Limited drawing of grid lines to ' + MAX + ' lines.');
    warnedForOverflow = true;
  }

  // create a major label on the left when needed
  if (this.options.showMajorLabels) {
    var leftTime = this.body.util.toTime(0),
        leftText = step.getLabelMajor(leftTime),
        widthText = leftText.length * (this.props.majorCharWidth || 10) + 10; // upper bound estimation

    if (xFirstMajorLabel == undefined || widthText < xFirstMajorLabel) {
      this._repaintMajorText(0, leftText, orientation, className);
    }
  }

  // Cleanup leftover DOM elements from the redundant list
  util.forEach(this.dom.redundant, function (arr) {
    while (arr.length) {
      var elem = arr.pop();
      if (elem && elem.parentNode) {
        elem.parentNode.removeChild(elem);
      }
    }
  });
};

/**
 * Create a minor label for the axis at position x
 * @param {Number} x
 * @param {String} text
 * @param {String} orientation   "top" or "bottom" (default)
 * @param {String} className
 * @return {Element} Returns the HTML element of the created label
 * @private
 */
TimeAxis.prototype._repaintMinorText = function (x, text, orientation, className) {
  // reuse redundant label
  var label = this.dom.redundant.minorTexts.shift();

  if (!label) {
    // create new label
    var content = document.createTextNode('');
    label = document.createElement('div');
    label.appendChild(content);
    this.dom.foreground.appendChild(label);
  }
  this.dom.minorTexts.push(label);

  label.childNodes[0].nodeValue = text;

  label.style.top = orientation == 'top' ? this.props.majorLabelHeight + 'px' : '0';
  label.style.left = x + 'px';
  label.className = 'vis-text vis-minor ' + className;
  //label.title = title;  // TODO: this is a heavy operation

  return label;
};

/**
 * Create a Major label for the axis at position x
 * @param {Number} x
 * @param {String} text
 * @param {String} orientation   "top" or "bottom" (default)
 * @param {String} className
 * @return {Element} Returns the HTML element of the created label
 * @private
 */
TimeAxis.prototype._repaintMajorText = function (x, text, orientation, className) {
  // reuse redundant label
  var label = this.dom.redundant.majorTexts.shift();

  if (!label) {
    // create label
    var content = document.createTextNode(text);
    label = document.createElement('div');
    label.appendChild(content);
    this.dom.foreground.appendChild(label);
  }
  this.dom.majorTexts.push(label);

  label.childNodes[0].nodeValue = text;
  label.className = 'vis-text vis-major ' + className;
  //label.title = title; // TODO: this is a heavy operation

  label.style.top = orientation == 'top' ? '0' : this.props.minorLabelHeight + 'px';
  label.style.left = x + 'px';

  return label;
};

/**
 * Create a minor line for the axis at position x
 * @param {Number} x
 * @param {Number} width
 * @param {String} orientation   "top" or "bottom" (default)
 * @param {String} className
 * @return {Element} Returns the created line
 * @private
 */
TimeAxis.prototype._repaintMinorLine = function (x, width, orientation, className) {
  // reuse redundant line
  var line = this.dom.redundant.lines.shift();
  if (!line) {
    // create vertical line
    line = document.createElement('div');
    this.dom.background.appendChild(line);
  }
  this.dom.lines.push(line);

  var props = this.props;
  if (orientation == 'top') {
    line.style.top = props.majorLabelHeight + 'px';
  } else {
    line.style.top = this.body.domProps.top.height + 'px';
  }
  line.style.height = props.minorLineHeight + 'px';
  line.style.left = x - props.minorLineWidth / 2 + 'px';
  line.style.width = width + 'px';

  line.className = 'vis-grid vis-vertical vis-minor ' + className;

  return line;
};

/**
 * Create a Major line for the axis at position x
 * @param {Number} x
 * @param {Number} width
 * @param {String} orientation   "top" or "bottom" (default)
 * @param {String} className
 * @return {Element} Returns the created line
 * @private
 */
TimeAxis.prototype._repaintMajorLine = function (x, width, orientation, className) {
  // reuse redundant line
  var line = this.dom.redundant.lines.shift();
  if (!line) {
    // create vertical line
    line = document.createElement('div');
    this.dom.background.appendChild(line);
  }
  this.dom.lines.push(line);

  var props = this.props;
  if (orientation == 'top') {
    line.style.top = '0';
  } else {
    line.style.top = this.body.domProps.top.height + 'px';
  }
  line.style.left = x - props.majorLineWidth / 2 + 'px';
  line.style.height = props.majorLineHeight + 'px';
  line.style.width = width + 'px';

  line.className = 'vis-grid vis-vertical vis-major ' + className;

  return line;
};

/**
 * Determine the size of text on the axis (both major and minor axis).
 * The size is calculated only once and then cached in this.props.
 * @private
 */
TimeAxis.prototype._calculateCharSize = function () {
  // Note: We calculate char size with every redraw. Size may change, for
  // example when any of the timelines parents had display:none for example.

  // determine the char width and height on the minor axis
  if (!this.dom.measureCharMinor) {
    this.dom.measureCharMinor = document.createElement('DIV');
    this.dom.measureCharMinor.className = 'vis-text vis-minor vis-measure';
    this.dom.measureCharMinor.style.position = 'absolute';

    this.dom.measureCharMinor.appendChild(document.createTextNode('0'));
    this.dom.foreground.appendChild(this.dom.measureCharMinor);
  }
  this.props.minorCharHeight = this.dom.measureCharMinor.clientHeight;
  this.props.minorCharWidth = this.dom.measureCharMinor.clientWidth;

  // determine the char width and height on the major axis
  if (!this.dom.measureCharMajor) {
    this.dom.measureCharMajor = document.createElement('DIV');
    this.dom.measureCharMajor.className = 'vis-text vis-major vis-measure';
    this.dom.measureCharMajor.style.position = 'absolute';

    this.dom.measureCharMajor.appendChild(document.createTextNode('0'));
    this.dom.foreground.appendChild(this.dom.measureCharMajor);
  }
  this.props.majorCharHeight = this.dom.measureCharMajor.clientHeight;
  this.props.majorCharWidth = this.dom.measureCharMajor.clientWidth;
};

var warnedForOverflow = false;

module.exports = TimeAxis;

},{"../../module/moment":7,"../../util":33,"../DateUtil":14,"../TimeStep":17,"./Component":20}],26:[function(require,module,exports){
'use strict';

var Hammer = require('../../../module/hammer');
var Item = require('./Item');
var BackgroundGroup = require('../BackgroundGroup');
var RangeItem = require('./RangeItem');

/**
 * @constructor BackgroundItem
 * @extends Item
 * @param {Object} data             Object containing parameters start, end
 *                                  content, className.
 * @param {{toScreen: function, toTime: function}} conversion
 *                                  Conversion functions from time to screen and vice versa
 * @param {Object} [options]        Configuration options
 *                                  // TODO: describe options
 */
// TODO: implement support for the BackgroundItem just having a start, then being displayed as a sort of an annotation
function BackgroundItem(data, conversion, options) {
  this.props = {
    content: {
      width: 0
    }
  };
  this.overflow = false; // if contents can overflow (css styling), this flag is set to true

  // validate data
  if (data) {
    if (data.start == undefined) {
      throw new Error('Property "start" missing in item ' + data.id);
    }
    if (data.end == undefined) {
      throw new Error('Property "end" missing in item ' + data.id);
    }
  }

  Item.call(this, data, conversion, options);
}

BackgroundItem.prototype = new Item(null, null, null);

BackgroundItem.prototype.baseClassName = 'vis-item vis-background';
BackgroundItem.prototype.stack = false;

/**
 * Check whether this item is visible inside given range
 * @returns {{start: Number, end: Number}} range with a timestamp for start and end
 * @returns {boolean} True if visible
 */
BackgroundItem.prototype.isVisible = function (range) {
  // determine visibility
  return this.data.start < range.end && this.data.end > range.start;
};

/**
 * Repaint the item
 */
BackgroundItem.prototype.redraw = function () {
  var dom = this.dom;
  if (!dom) {
    // create DOM
    this.dom = {};
    dom = this.dom;

    // background box
    dom.box = document.createElement('div');
    // className is updated in redraw()

    // frame box (to prevent the item contents from overflowing
    dom.frame = document.createElement('div');
    dom.frame.className = 'vis-item-overflow';
    dom.box.appendChild(dom.frame);

    // contents box
    dom.content = document.createElement('div');
    dom.content.className = 'vis-item-content';
    dom.frame.appendChild(dom.content);

    // Note: we do NOT attach this item as attribute to the DOM,
    //       such that background items cannot be selected
    //dom.box['timeline-item'] = this;

    this.dirty = true;
  }

  // append DOM to parent DOM
  if (!this.parent) {
    throw new Error('Cannot redraw item: no parent attached');
  }
  if (!dom.box.parentNode) {
    var background = this.parent.dom.background;
    if (!background) {
      throw new Error('Cannot redraw item: parent has no background container element');
    }
    background.appendChild(dom.box);
  }
  this.displayed = true;

  // Update DOM when item is marked dirty. An item is marked dirty when:
  // - the item is not yet rendered
  // - the item's data is changed
  // - the item is selected/deselected
  if (this.dirty) {
    this._updateContents(this.dom.content);
    this._updateTitle(this.dom.content);
    this._updateDataAttributes(this.dom.content);
    this._updateStyle(this.dom.box);

    // update class
    var className = (this.data.className ? ' ' + this.data.className : '') + (this.selected ? ' vis-selected' : '');
    dom.box.className = this.baseClassName + className;

    // determine from css whether this box has overflow
    this.overflow = window.getComputedStyle(dom.content).overflow !== 'hidden';

    // recalculate size
    this.props.content.width = this.dom.content.offsetWidth;
    this.height = 0; // set height zero, so this item will be ignored when stacking items

    this.dirty = false;
  }
};

/**
 * Show the item in the DOM (when not already visible). The items DOM will
 * be created when needed.
 */
BackgroundItem.prototype.show = RangeItem.prototype.show;

/**
 * Hide the item from the DOM (when visible)
 * @return {Boolean} changed
 */
BackgroundItem.prototype.hide = RangeItem.prototype.hide;

/**
 * Reposition the item horizontally
 * @Override
 */
BackgroundItem.prototype.repositionX = RangeItem.prototype.repositionX;

/**
 * Reposition the item vertically
 * @Override
 */
BackgroundItem.prototype.repositionY = function (margin) {
  var onTop = this.options.orientation.item === 'top';
  this.dom.content.style.top = onTop ? '' : '0';
  this.dom.content.style.bottom = onTop ? '0' : '';
  var height;

  // special positioning for subgroups
  if (this.data.subgroup !== undefined) {
    // TODO: instead of calculating the top position of the subgroups here for every BackgroundItem, calculate the top of the subgroup once in Itemset

    var itemSubgroup = this.data.subgroup;
    var subgroups = this.parent.subgroups;
    var subgroupIndex = subgroups[itemSubgroup].index;
    // if the orientation is top, we need to take the difference in height into account.
    if (onTop == true) {
      // the first subgroup will have to account for the distance from the top to the first item.
      height = this.parent.subgroups[itemSubgroup].height + margin.item.vertical;
      height += subgroupIndex == 0 ? margin.axis - 0.5 * margin.item.vertical : 0;
      var newTop = this.parent.top;
      for (var subgroup in subgroups) {
        if (subgroups.hasOwnProperty(subgroup)) {
          if (subgroups[subgroup].visible == true && subgroups[subgroup].index < subgroupIndex) {
            newTop += subgroups[subgroup].height + margin.item.vertical;
          }
        }
      }

      // the others will have to be offset downwards with this same distance.
      newTop += subgroupIndex != 0 ? margin.axis - 0.5 * margin.item.vertical : 0;
      this.dom.box.style.top = newTop + 'px';
      this.dom.box.style.bottom = '';
    }
    // and when the orientation is bottom:
    else {
        var newTop = this.parent.top;
        var totalHeight = 0;
        for (var subgroup in subgroups) {
          if (subgroups.hasOwnProperty(subgroup)) {
            if (subgroups[subgroup].visible == true) {
              var newHeight = subgroups[subgroup].height + margin.item.vertical;
              totalHeight += newHeight;
              if (subgroups[subgroup].index > subgroupIndex) {
                newTop += newHeight;
              }
            }
          }
        }
        height = this.parent.subgroups[itemSubgroup].height + margin.item.vertical;
        this.dom.box.style.top = this.parent.height - totalHeight + newTop + 'px';
        this.dom.box.style.bottom = '';
      }
  }
  // and in the case of no subgroups:
  else {
      // we want backgrounds with groups to only show in groups.
      if (this.parent instanceof BackgroundGroup) {
        // if the item is not in a group:
        height = Math.max(this.parent.height, this.parent.itemSet.body.domProps.center.height, this.parent.itemSet.body.domProps.centerContainer.height);
        this.dom.box.style.top = onTop ? '0' : '';
        this.dom.box.style.bottom = onTop ? '' : '0';
      } else {
        height = this.parent.height;
        // same alignment for items when orientation is top or bottom
        this.dom.box.style.top = this.parent.top + 'px';
        this.dom.box.style.bottom = '';
      }
    }
  this.dom.box.style.height = height + 'px';
};

module.exports = BackgroundItem;

},{"../../../module/hammer":6,"../BackgroundGroup":19,"./Item":28,"./RangeItem":30}],27:[function(require,module,exports){
'use strict';

var Item = require('./Item');
var util = require('../../../util');

/**
 * @constructor BoxItem
 * @extends Item
 * @param {Object} data             Object containing parameters start
 *                                  content, className.
 * @param {{toScreen: function, toTime: function}} conversion
 *                                  Conversion functions from time to screen and vice versa
 * @param {Object} [options]        Configuration options
 *                                  // TODO: describe available options
 */
function BoxItem(data, conversion, options) {
  this.props = {
    dot: {
      width: 0,
      height: 0
    },
    line: {
      width: 0,
      height: 0
    }
  };

  // validate data
  if (data) {
    if (data.start == undefined) {
      throw new Error('Property "start" missing in item ' + data);
    }
  }

  Item.call(this, data, conversion, options);
}

BoxItem.prototype = new Item(null, null, null);

/**
 * Check whether this item is visible inside given range
 * @returns {{start: Number, end: Number}} range with a timestamp for start and end
 * @returns {boolean} True if visible
 */
BoxItem.prototype.isVisible = function (range) {
  // determine visibility
  // TODO: account for the real width of the item. Right now we just add 1/4 to the window
  var interval = (range.end - range.start) / 4;
  return this.data.start > range.start - interval && this.data.start < range.end + interval;
};

/**
 * Repaint the item
 */
BoxItem.prototype.redraw = function () {
  var dom = this.dom;
  if (!dom) {
    // create DOM
    this.dom = {};
    dom = this.dom;

    // create main box
    dom.box = document.createElement('DIV');

    // contents box (inside the background box). used for making margins
    dom.content = document.createElement('DIV');
    dom.content.className = 'vis-item-content';
    dom.box.appendChild(dom.content);

    // line to axis
    dom.line = document.createElement('DIV');
    dom.line.className = 'vis-line';

    // dot on axis
    dom.dot = document.createElement('DIV');
    dom.dot.className = 'vis-dot';

    // attach this item as attribute
    dom.box['timeline-item'] = this;

    this.dirty = true;
  }

  // append DOM to parent DOM
  if (!this.parent) {
    throw new Error('Cannot redraw item: no parent attached');
  }
  if (!dom.box.parentNode) {
    var foreground = this.parent.dom.foreground;
    if (!foreground) throw new Error('Cannot redraw item: parent has no foreground container element');
    foreground.appendChild(dom.box);
  }
  if (!dom.line.parentNode) {
    var background = this.parent.dom.background;
    if (!background) throw new Error('Cannot redraw item: parent has no background container element');
    background.appendChild(dom.line);
  }
  if (!dom.dot.parentNode) {
    var axis = this.parent.dom.axis;
    if (!background) throw new Error('Cannot redraw item: parent has no axis container element');
    axis.appendChild(dom.dot);
  }
  this.displayed = true;

  // Update DOM when item is marked dirty. An item is marked dirty when:
  // - the item is not yet rendered
  // - the item's data is changed
  // - the item is selected/deselected
  if (this.dirty) {
    this._updateContents(this.dom.content);
    this._updateTitle(this.dom.box);
    this._updateDataAttributes(this.dom.box);
    this._updateStyle(this.dom.box);

    var editable = (this.options.editable.updateTime || this.options.editable.updateGroup || this.editable === true) && this.editable !== false;

    // update class
    var className = (this.data.className ? ' ' + this.data.className : '') + (this.selected ? ' vis-selected' : '') + (editable ? ' vis-editable' : ' vis-readonly');
    dom.box.className = 'vis-item vis-box' + className;
    dom.line.className = 'vis-item vis-line' + className;
    dom.dot.className = 'vis-item vis-dot' + className;

    // recalculate size
    this.props.dot.height = dom.dot.offsetHeight;
    this.props.dot.width = dom.dot.offsetWidth;
    this.props.line.width = dom.line.offsetWidth;
    this.width = dom.box.offsetWidth;
    this.height = dom.box.offsetHeight;

    this.dirty = false;
  }

  this._repaintDeleteButton(dom.box);
};

/**
 * Show the item in the DOM (when not already displayed). The items DOM will
 * be created when needed.
 */
BoxItem.prototype.show = function () {
  if (!this.displayed) {
    this.redraw();
  }
};

/**
 * Hide the item from the DOM (when visible)
 */
BoxItem.prototype.hide = function () {
  if (this.displayed) {
    var dom = this.dom;

    if (dom.box.parentNode) dom.box.parentNode.removeChild(dom.box);
    if (dom.line.parentNode) dom.line.parentNode.removeChild(dom.line);
    if (dom.dot.parentNode) dom.dot.parentNode.removeChild(dom.dot);

    this.displayed = false;
  }
};

/**
 * Reposition the item horizontally
 * @Override
 */
BoxItem.prototype.repositionX = function () {
  var start = this.conversion.toScreen(this.data.start);
  var align = this.options.align;
  var left;

  // calculate left position of the box
  if (align == 'right') {
    this.left = start - this.width;
  } else if (align == 'left') {
    this.left = start;
  } else {
    // default or 'center'
    this.left = start - this.width / 2;
  }

  // reposition box
  this.dom.box.style.left = this.left + 'px';

  // reposition line
  this.dom.line.style.left = start - this.props.line.width / 2 + 'px';

  // reposition dot
  this.dom.dot.style.left = start - this.props.dot.width / 2 + 'px';
};

/**
 * Reposition the item vertically
 * @Override
 */
BoxItem.prototype.repositionY = function () {
  var orientation = this.options.orientation.item;
  var box = this.dom.box;
  var line = this.dom.line;
  var dot = this.dom.dot;

  if (orientation == 'top') {
    box.style.top = (this.top || 0) + 'px';

    line.style.top = '0';
    line.style.height = this.parent.top + this.top + 1 + 'px';
    line.style.bottom = '';
  } else {
    // orientation 'bottom'
    var itemSetHeight = this.parent.itemSet.props.height; // TODO: this is nasty
    var lineHeight = itemSetHeight - this.parent.top - this.parent.height + this.top;

    box.style.top = (this.parent.height - this.top - this.height || 0) + 'px';
    line.style.top = itemSetHeight - lineHeight + 'px';
    line.style.bottom = '0';
  }

  dot.style.top = -this.props.dot.height / 2 + 'px';
};

/**
 * Return the width of the item left from its start date
 * @return {number}
 */
BoxItem.prototype.getWidthLeft = function () {
  return this.width / 2;
};

/**
 * Return the width of the item right from its start date
 * @return {number}
 */
BoxItem.prototype.getWidthRight = function () {
  return this.width / 2;
};

module.exports = BoxItem;

},{"../../../util":33,"./Item":28}],28:[function(require,module,exports){
'use strict';

var Hammer = require('../../../module/hammer');
var util = require('../../../util');

/**
 * @constructor Item
 * @param {Object} data             Object containing (optional) parameters type,
 *                                  start, end, content, group, className.
 * @param {{toScreen: function, toTime: function}} conversion
 *                                  Conversion functions from time to screen and vice versa
 * @param {Object} options          Configuration options
 *                                  // TODO: describe available options
 */
function Item(data, conversion, options) {
  this.id = null;
  this.parent = null;
  this.data = data;
  this.dom = null;
  this.conversion = conversion || {};
  this.options = options || {};

  this.selected = false;
  this.displayed = false;
  this.dirty = true;

  this.top = null;
  this.left = null;
  this.width = null;
  this.height = null;

  this.editable = null;
  if (this.data && this.data.hasOwnProperty('editable') && typeof this.data.editable === 'boolean') {
    this.editable = data.editable;
  }
}

Item.prototype.stack = true;

/**
 * Select current item
 */
Item.prototype.select = function () {
  this.selected = true;
  this.dirty = true;
  if (this.displayed) this.redraw();
};

/**
 * Unselect current item
 */
Item.prototype.unselect = function () {
  this.selected = false;
  this.dirty = true;
  if (this.displayed) this.redraw();
};

/**
 * Set data for the item. Existing data will be updated. The id should not
 * be changed. When the item is displayed, it will be redrawn immediately.
 * @param {Object} data
 */
Item.prototype.setData = function (data) {
  var groupChanged = data.group != undefined && this.data.group != data.group;
  if (groupChanged) {
    this.parent.itemSet._moveToGroup(this, data.group);
  }

  if (data.hasOwnProperty('editable') && typeof data.editable === 'boolean') {
    this.editable = data.editable;
  }

  this.data = data;
  this.dirty = true;
  if (this.displayed) this.redraw();
};

/**
 * Set a parent for the item
 * @param {ItemSet | Group} parent
 */
Item.prototype.setParent = function (parent) {
  if (this.displayed) {
    this.hide();
    this.parent = parent;
    if (this.parent) {
      this.show();
    }
  } else {
    this.parent = parent;
  }
};

/**
 * Check whether this item is visible inside given range
 * @returns {{start: Number, end: Number}} range with a timestamp for start and end
 * @returns {boolean} True if visible
 */
Item.prototype.isVisible = function (range) {
  // Should be implemented by Item implementations
  return false;
};

/**
 * Show the Item in the DOM (when not already visible)
 * @return {Boolean} changed
 */
Item.prototype.show = function () {
  return false;
};

/**
 * Hide the Item from the DOM (when visible)
 * @return {Boolean} changed
 */
Item.prototype.hide = function () {
  return false;
};

/**
 * Repaint the item
 */
Item.prototype.redraw = function () {
  // should be implemented by the item
};

/**
 * Reposition the Item horizontally
 */
Item.prototype.repositionX = function () {
  // should be implemented by the item
};

/**
 * Reposition the Item vertically
 */
Item.prototype.repositionY = function () {
  // should be implemented by the item
};

/**
 * Repaint a delete button on the top right of the item when the item is selected
 * @param {HTMLElement} anchor
 * @protected
 */
Item.prototype._repaintDeleteButton = function (anchor) {
  var editable = (this.options.editable.remove || this.data.editable === true) && this.data.editable !== false;

  if (this.selected && editable && !this.dom.deleteButton) {
    // create and show button
    var me = this;

    var deleteButton = document.createElement('div');
    deleteButton.className = 'vis-delete';
    deleteButton.title = 'Delete this item';

    // TODO: be able to destroy the delete button
    new Hammer(deleteButton).on('tap', function (event) {
      event.stopPropagation();
      me.parent.removeFromDataSet(me);
    });

    anchor.appendChild(deleteButton);
    this.dom.deleteButton = deleteButton;
  } else if (!this.selected && this.dom.deleteButton) {
    // remove button
    if (this.dom.deleteButton.parentNode) {
      this.dom.deleteButton.parentNode.removeChild(this.dom.deleteButton);
    }
    this.dom.deleteButton = null;
  }
};

/**
 * Set HTML contents for the item
 * @param {Element} element   HTML element to fill with the contents
 * @private
 */
Item.prototype._updateContents = function (element) {
  var content;
  if (this.options.template) {
    var itemData = this.parent.itemSet.itemsData.get(this.id); // get a clone of the data from the dataset
    content = this.options.template(itemData);
  } else {
    content = this.data.content;
  }

  var changed = this._contentToString(this.content) !== this._contentToString(content);
  if (changed) {
    // only replace the content when changed
    if (content instanceof Element) {
      element.innerHTML = '';
      element.appendChild(content);
    } else if (content != undefined) {
      element.innerHTML = content;
    } else {
      if (!(this.data.type == 'background' && this.data.content === undefined)) {
        throw new Error('Property "content" missing in item ' + this.id);
      }
    }

    this.content = content;
  }
};

/**
 * Set HTML contents for the item
 * @param {Element} element   HTML element to fill with the contents
 * @private
 */
Item.prototype._updateTitle = function (element) {
  if (this.data.title != null) {
    element.title = this.data.title || '';
  } else {
    element.removeAttribute('vis-title');
  }
};

/**
 * Process dataAttributes timeline option and set as data- attributes on dom.content
 * @param {Element} element   HTML element to which the attributes will be attached
 * @private
 */
Item.prototype._updateDataAttributes = function (element) {
  if (this.options.dataAttributes && this.options.dataAttributes.length > 0) {
    var attributes = [];

    if (Array.isArray(this.options.dataAttributes)) {
      attributes = this.options.dataAttributes;
    } else if (this.options.dataAttributes == 'all') {
      attributes = Object.keys(this.data);
    } else {
      return;
    }

    for (var i = 0; i < attributes.length; i++) {
      var name = attributes[i];
      var value = this.data[name];

      if (value != null) {
        element.setAttribute('data-' + name, value);
      } else {
        element.removeAttribute('data-' + name);
      }
    }
  }
};

/**
 * Update custom styles of the element
 * @param element
 * @private
 */
Item.prototype._updateStyle = function (element) {
  // remove old styles
  if (this.style) {
    util.removeCssText(element, this.style);
    this.style = null;
  }

  // append new styles
  if (this.data.style) {
    util.addCssText(element, this.data.style);
    this.style = this.data.style;
  }
};

/**
 * Stringify the items contents
 * @param {string | Element | undefined} content
 * @returns {string | undefined}
 * @private
 */
Item.prototype._contentToString = function (content) {
  if (typeof content === 'string') return content;
  if (content && 'outerHTML' in content) return content.outerHTML;
  return content;
};

/**
 * Return the width of the item left from its start date
 * @return {number}
 */
Item.prototype.getWidthLeft = function () {
  return 0;
};

/**
 * Return the width of the item right from the max of its start and end date
 * @return {number}
 */
Item.prototype.getWidthRight = function () {
  return 0;
};

module.exports = Item;

},{"../../../module/hammer":6,"../../../util":33}],29:[function(require,module,exports){
'use strict';

var Item = require('./Item');

/**
 * @constructor PointItem
 * @extends Item
 * @param {Object} data             Object containing parameters start
 *                                  content, className.
 * @param {{toScreen: function, toTime: function}} conversion
 *                                  Conversion functions from time to screen and vice versa
 * @param {Object} [options]        Configuration options
 *                                  // TODO: describe available options
 */
function PointItem(data, conversion, options) {
  this.props = {
    dot: {
      top: 0,
      width: 0,
      height: 0
    },
    content: {
      height: 0,
      marginLeft: 0
    }
  };

  // validate data
  if (data) {
    if (data.start == undefined) {
      throw new Error('Property "start" missing in item ' + data);
    }
  }

  Item.call(this, data, conversion, options);
}

PointItem.prototype = new Item(null, null, null);

/**
 * Check whether this item is visible inside given range
 * @returns {{start: Number, end: Number}} range with a timestamp for start and end
 * @returns {boolean} True if visible
 */
PointItem.prototype.isVisible = function (range) {
  // determine visibility
  // TODO: account for the real width of the item. Right now we just add 1/4 to the window
  var interval = (range.end - range.start) / 4;
  return this.data.start > range.start - interval && this.data.start < range.end + interval;
};

/**
 * Repaint the item
 */
PointItem.prototype.redraw = function () {
  var dom = this.dom;
  if (!dom) {
    // create DOM
    this.dom = {};
    dom = this.dom;

    // background box
    dom.point = document.createElement('div');
    // className is updated in redraw()

    // contents box, right from the dot
    dom.content = document.createElement('div');
    dom.content.className = 'vis-item-content';
    dom.point.appendChild(dom.content);

    // dot at start
    dom.dot = document.createElement('div');
    dom.point.appendChild(dom.dot);

    // attach this item as attribute
    dom.point['timeline-item'] = this;

    this.dirty = true;
  }

  // append DOM to parent DOM
  if (!this.parent) {
    throw new Error('Cannot redraw item: no parent attached');
  }
  if (!dom.point.parentNode) {
    var foreground = this.parent.dom.foreground;
    if (!foreground) {
      throw new Error('Cannot redraw item: parent has no foreground container element');
    }
    foreground.appendChild(dom.point);
  }
  this.displayed = true;

  // Update DOM when item is marked dirty. An item is marked dirty when:
  // - the item is not yet rendered
  // - the item's data is changed
  // - the item is selected/deselected
  if (this.dirty) {
    this._updateContents(this.dom.content);
    this._updateTitle(this.dom.point);
    this._updateDataAttributes(this.dom.point);
    this._updateStyle(this.dom.point);

    var editable = (this.options.editable.updateTime || this.options.editable.updateGroup || this.editable === true) && this.editable !== false;

    // update class
    var className = (this.data.className ? ' ' + this.data.className : '') + (this.selected ? ' vis-selected' : '') + (editable ? ' vis-editable' : ' vis-readonly');
    dom.point.className = 'vis-item vis-point' + className;
    dom.dot.className = 'vis-item vis-dot' + className;

    // recalculate size of dot and contents
    this.props.dot.width = dom.dot.offsetWidth;
    this.props.dot.height = dom.dot.offsetHeight;
    this.props.content.height = dom.content.offsetHeight;

    // resize contents
    dom.content.style.marginLeft = 2 * this.props.dot.width + 'px';
    //dom.content.style.marginRight = ... + 'px'; // TODO: margin right

    // recalculate size
    this.width = dom.point.offsetWidth;
    this.height = dom.point.offsetHeight;

    // reposition the dot
    dom.dot.style.top = (this.height - this.props.dot.height) / 2 + 'px';
    dom.dot.style.left = this.props.dot.width / 2 + 'px';

    this.dirty = false;
  }

  this._repaintDeleteButton(dom.point);
};

/**
 * Show the item in the DOM (when not already visible). The items DOM will
 * be created when needed.
 */
PointItem.prototype.show = function () {
  if (!this.displayed) {
    this.redraw();
  }
};

/**
 * Hide the item from the DOM (when visible)
 */
PointItem.prototype.hide = function () {
  if (this.displayed) {
    if (this.dom.point.parentNode) {
      this.dom.point.parentNode.removeChild(this.dom.point);
    }

    this.displayed = false;
  }
};

/**
 * Reposition the item horizontally
 * @Override
 */
PointItem.prototype.repositionX = function () {
  var start = this.conversion.toScreen(this.data.start);

  this.left = start - this.props.dot.width;

  // reposition point
  this.dom.point.style.left = this.left + 'px';
};

/**
 * Reposition the item vertically
 * @Override
 */
PointItem.prototype.repositionY = function () {
  var orientation = this.options.orientation.item;
  var point = this.dom.point;

  if (orientation == 'top') {
    point.style.top = this.top + 'px';
  } else {
    point.style.top = this.parent.height - this.top - this.height + 'px';
  }
};

/**
 * Return the width of the item left from its start date
 * @return {number}
 */
PointItem.prototype.getWidthLeft = function () {
  return this.props.dot.width;
};

/**
 * Return the width of the item right from  its start date
 * @return {number}
 */
PointItem.prototype.getWidthRight = function () {
  return this.width - this.props.dot.width;
};

module.exports = PointItem;

},{"./Item":28}],30:[function(require,module,exports){
'use strict';

var Hammer = require('../../../module/hammer');
var Item = require('./Item');

/**
 * @constructor RangeItem
 * @extends Item
 * @param {Object} data             Object containing parameters start, end
 *                                  content, className.
 * @param {{toScreen: function, toTime: function}} conversion
 *                                  Conversion functions from time to screen and vice versa
 * @param {Object} [options]        Configuration options
 *                                  // TODO: describe options
 */
function RangeItem(data, conversion, options) {
  this.props = {
    content: {
      width: 0
    }
  };
  this.overflow = false; // if contents can overflow (css styling), this flag is set to true

  // validate data
  if (data) {
    if (data.start == undefined) {
      throw new Error('Property "start" missing in item ' + data.id);
    }
    if (data.end == undefined) {
      throw new Error('Property "end" missing in item ' + data.id);
    }
  }

  Item.call(this, data, conversion, options);
}

RangeItem.prototype = new Item(null, null, null);

RangeItem.prototype.baseClassName = 'vis-item vis-range';

/**
 * Check whether this item is visible inside given range
 * @returns {{start: Number, end: Number}} range with a timestamp for start and end
 * @returns {boolean} True if visible
 */
RangeItem.prototype.isVisible = function (range) {
  // determine visibility
  return this.data.start < range.end && this.data.end > range.start;
};

/**
 * Repaint the item
 */
RangeItem.prototype.redraw = function () {
  var dom = this.dom;
  if (!dom) {
    // create DOM
    this.dom = {};
    dom = this.dom;

    // background box
    dom.box = document.createElement('div');
    // className is updated in redraw()

    // frame box (to prevent the item contents from overflowing
    dom.frame = document.createElement('div');
    dom.frame.className = 'vis-item-overflow';
    dom.box.appendChild(dom.frame);

    // contents box
    dom.content = document.createElement('div');
    dom.content.className = 'vis-item-content';
    dom.frame.appendChild(dom.content);

    // attach this item as attribute
    dom.box['timeline-item'] = this;

    this.dirty = true;
  }

  // append DOM to parent DOM
  if (!this.parent) {
    throw new Error('Cannot redraw item: no parent attached');
  }
  if (!dom.box.parentNode) {
    var foreground = this.parent.dom.foreground;
    if (!foreground) {
      throw new Error('Cannot redraw item: parent has no foreground container element');
    }
    foreground.appendChild(dom.box);
  }
  this.displayed = true;

  // Update DOM when item is marked dirty. An item is marked dirty when:
  // - the item is not yet rendered
  // - the item's data is changed
  // - the item is selected/deselected
  if (this.dirty) {
    this._updateContents(this.dom.content);
    this._updateTitle(this.dom.box);
    this._updateDataAttributes(this.dom.box);
    this._updateStyle(this.dom.box);

    var editable = (this.options.editable.updateTime || this.options.editable.updateGroup || this.editable === true) && this.editable !== false;

    // update class
    var className = (this.data.className ? ' ' + this.data.className : '') + (this.selected ? ' vis-selected' : '') + (editable ? ' vis-editable' : ' vis-readonly');
    dom.box.className = this.baseClassName + className;

    // determine from css whether this box has overflow
    this.overflow = window.getComputedStyle(dom.frame).overflow !== 'hidden';

    // recalculate size
    // turn off max-width to be able to calculate the real width
    // this causes an extra browser repaint/reflow, but so be it
    this.dom.content.style.maxWidth = 'none';
    this.props.content.width = this.dom.content.offsetWidth;
    this.height = this.dom.box.offsetHeight;
    this.dom.content.style.maxWidth = '';

    this.dirty = false;
  }

  this._repaintDeleteButton(dom.box);
  this._repaintDragLeft();
  this._repaintDragRight();
};

/**
 * Show the item in the DOM (when not already visible). The items DOM will
 * be created when needed.
 */
RangeItem.prototype.show = function () {
  if (!this.displayed) {
    this.redraw();
  }
};

/**
 * Hide the item from the DOM (when visible)
 * @return {Boolean} changed
 */
RangeItem.prototype.hide = function () {
  if (this.displayed) {
    var box = this.dom.box;

    if (box.parentNode) {
      box.parentNode.removeChild(box);
    }

    this.displayed = false;
  }
};

/**
 * Reposition the item horizontally
 * @param {boolean} [limitSize=true] If true (default), the width of the range
 *                                   item will be limited, as the browser cannot
 *                                   display very wide divs. This means though
 *                                   that the applied left and width may
 *                                   not correspond to the ranges start and end
 * @Override
 */
RangeItem.prototype.repositionX = function (limitSize) {
  var parentWidth = this.parent.width;
  var start = this.conversion.toScreen(this.data.start);
  var end = this.conversion.toScreen(this.data.end);
  var contentLeft;
  var contentWidth;

  // limit the width of the range, as browsers cannot draw very wide divs
  if (limitSize === undefined || limitSize === true) {
    if (start < -parentWidth) {
      start = -parentWidth;
    }
    if (end > 2 * parentWidth) {
      end = 2 * parentWidth;
    }
  }
  var boxWidth = Math.max(end - start, 1);

  if (this.overflow) {
    this.left = start;
    this.width = boxWidth + this.props.content.width;
    contentWidth = this.props.content.width;

    // Note: The calculation of width is an optimistic calculation, giving
    //       a width which will not change when moving the Timeline
    //       So no re-stacking needed, which is nicer for the eye;
  } else {
      this.left = start;
      this.width = boxWidth;
      contentWidth = Math.min(end - start, this.props.content.width);
    }

  this.dom.box.style.left = this.left + 'px';
  this.dom.box.style.width = boxWidth + 'px';

  switch (this.options.align) {
    case 'left':
      this.dom.content.style.left = '0';
      break;

    case 'right':
      this.dom.content.style.left = Math.max(boxWidth - contentWidth, 0) + 'px';
      break;

    case 'center':
      this.dom.content.style.left = Math.max((boxWidth - contentWidth) / 2, 0) + 'px';
      break;

    default:
      // 'auto'
      // when range exceeds left of the window, position the contents at the left of the visible area
      if (this.overflow) {
        if (end > 0) {
          contentLeft = Math.max(-start, 0);
        } else {
          contentLeft = -contentWidth; // ensure it's not visible anymore
        }
      } else {
          if (start < 0) {
            contentLeft = -start;
          } else {
            contentLeft = 0;
          }
        }
      this.dom.content.style.left = contentLeft + 'px';
  }
};

/**
 * Reposition the item vertically
 * @Override
 */
RangeItem.prototype.repositionY = function () {
  var orientation = this.options.orientation.item;
  var box = this.dom.box;

  if (orientation == 'top') {
    box.style.top = this.top + 'px';
  } else {
    box.style.top = this.parent.height - this.top - this.height + 'px';
  }
};

/**
 * Repaint a drag area on the left side of the range when the range is selected
 * @protected
 */
RangeItem.prototype._repaintDragLeft = function () {
  if (this.selected && this.options.editable.updateTime && !this.dom.dragLeft) {
    // create and show drag area
    var dragLeft = document.createElement('div');
    dragLeft.className = 'vis-drag-left';
    dragLeft.dragLeftItem = this;

    this.dom.box.appendChild(dragLeft);
    this.dom.dragLeft = dragLeft;
  } else if (!this.selected && this.dom.dragLeft) {
    // delete drag area
    if (this.dom.dragLeft.parentNode) {
      this.dom.dragLeft.parentNode.removeChild(this.dom.dragLeft);
    }
    this.dom.dragLeft = null;
  }
};

/**
 * Repaint a drag area on the right side of the range when the range is selected
 * @protected
 */
RangeItem.prototype._repaintDragRight = function () {
  if (this.selected && this.options.editable.updateTime && !this.dom.dragRight) {
    // create and show drag area
    var dragRight = document.createElement('div');
    dragRight.className = 'vis-drag-right';
    dragRight.dragRightItem = this;

    this.dom.box.appendChild(dragRight);
    this.dom.dragRight = dragRight;
  } else if (!this.selected && this.dom.dragRight) {
    // delete drag area
    if (this.dom.dragRight.parentNode) {
      this.dom.dragRight.parentNode.removeChild(this.dom.dragRight);
    }
    this.dom.dragRight = null;
  }
};

module.exports = RangeItem;

},{"../../../module/hammer":6,"./Item":28}],31:[function(require,module,exports){
// English
'use strict';

exports['en'] = {
  current: 'current',
  time: 'time'
};
exports['en_EN'] = exports['en'];
exports['en_US'] = exports['en'];

// Dutch
exports['nl'] = {
  current: 'huidige',
  time: 'tijd'
};
exports['nl_NL'] = exports['nl'];
exports['nl_BE'] = exports['nl'];

},{}],32:[function(require,module,exports){
/**
 * This object contains all possible options. It will check if the types are correct, if required if the option is one
 * of the allowed values.
 *
 * __any__ means that the name of the property does not matter.
 * __type__ is a required field for all objects and contains the allowed types of all objects
 */
'use strict';

Object.defineProperty(exports, '__esModule', {
  value: true
});
var string = 'string';
var boolean = 'boolean';
var number = 'number';
var array = 'array';
var date = 'date';
var object = 'object'; // should only be in a __type__ property
var dom = 'dom';
var moment = 'moment';
var any = 'any';

var allOptions = {
  configure: {
    enabled: { boolean: boolean },
    filter: { boolean: boolean, 'function': 'function' },
    container: { dom: dom },
    __type__: { object: object, boolean: boolean, 'function': 'function' }
  },

  //globals :
  align: { string: string },
  autoResize: { boolean: boolean },
  throttleRedraw: { number: number },
  clickToUse: { boolean: boolean },
  dataAttributes: { string: string, array: array },
  editable: {
    add: { boolean: boolean, 'undefined': 'undefined' },
    remove: { boolean: boolean, 'undefined': 'undefined' },
    updateGroup: { boolean: boolean, 'undefined': 'undefined' },
    updateTime: { boolean: boolean, 'undefined': 'undefined' },
    __type__: { boolean: boolean, object: object }
  },
  end: { number: number, date: date, string: string, moment: moment },
  format: {
    minorLabels: {
      millisecond: { string: string, 'undefined': 'undefined' },
      second: { string: string, 'undefined': 'undefined' },
      minute: { string: string, 'undefined': 'undefined' },
      hour: { string: string, 'undefined': 'undefined' },
      weekday: { string: string, 'undefined': 'undefined' },
      day: { string: string, 'undefined': 'undefined' },
      month: { string: string, 'undefined': 'undefined' },
      year: { string: string, 'undefined': 'undefined' },
      __type__: { object: object }
    },
    majorLabels: {
      millisecond: { string: string, 'undefined': 'undefined' },
      second: { string: string, 'undefined': 'undefined' },
      minute: { string: string, 'undefined': 'undefined' },
      hour: { string: string, 'undefined': 'undefined' },
      weekday: { string: string, 'undefined': 'undefined' },
      day: { string: string, 'undefined': 'undefined' },
      month: { string: string, 'undefined': 'undefined' },
      year: { string: string, 'undefined': 'undefined' },
      __type__: { object: object }
    },
    __type__: { object: object }
  },
  moment: { 'function': 'function' },
  groupOrder: { string: string, 'function': 'function' },
  groupEditable: {
    add: { boolean: boolean, 'undefined': 'undefined' },
    remove: { boolean: boolean, 'undefined': 'undefined' },
    order: { boolean: boolean, 'undefined': 'undefined' },
    __type__: { boolean: boolean, object: object }
  },
  groupOrderSwap: { 'function': 'function' },
  height: { string: string, number: number },
  hiddenDates: {
    start: { date: date, number: number, string: string, moment: moment },
    end: { date: date, number: number, string: string, moment: moment },
    repeat: { string: string },
    __type__: { object: object, array: array }
  },
  itemsAlwaysDraggable: { boolean: boolean },
  locale: { string: string },
  locales: {
    __any__: { any: any },
    __type__: { object: object }
  },
  margin: {
    axis: { number: number },
    item: {
      horizontal: { number: number, 'undefined': 'undefined' },
      vertical: { number: number, 'undefined': 'undefined' },
      __type__: { object: object, number: number }
    },
    __type__: { object: object, number: number }
  },
  max: { date: date, number: number, string: string, moment: moment },
  maxHeight: { number: number, string: string },
  maxMinorChars: { number: number },
  min: { date: date, number: number, string: string, moment: moment },
  minHeight: { number: number, string: string },
  moveable: { boolean: boolean },
  multiselect: { boolean: boolean },
  multiselectPerGroup: { boolean: boolean },
  onAdd: { 'function': 'function' },
  onUpdate: { 'function': 'function' },
  onMove: { 'function': 'function' },
  onMoving: { 'function': 'function' },
  onRemove: { 'function': 'function' },
  onAddGroup: { 'function': 'function' },
  onMoveGroup: { 'function': 'function' },
  onRemoveGroup: { 'function': 'function' },
  order: { 'function': 'function' },
  orientation: {
    axis: { string: string, 'undefined': 'undefined' },
    item: { string: string, 'undefined': 'undefined' },
    __type__: { string: string, object: object }
  },
  selectable: { boolean: boolean },
  showCurrentTime: { boolean: boolean },
  showMajorLabels: { boolean: boolean },
  showMinorLabels: { boolean: boolean },
  stack: { boolean: boolean },
  snap: { 'function': 'function', 'null': 'null' },
  start: { date: date, number: number, string: string, moment: moment },
  template: { 'function': 'function' },
  groupTemplate: { 'function': 'function' },
  timeAxis: {
    scale: { string: string, 'undefined': 'undefined' },
    step: { number: number, 'undefined': 'undefined' },
    __type__: { object: object }
  },
  type: { string: string },
  width: { string: string, number: number },
  zoomable: { boolean: boolean },
  zoomKey: { string: ['ctrlKey', 'altKey', 'metaKey', ''] },
  zoomMax: { number: number },
  zoomMin: { number: number },

  __type__: { object: object }
};

var configureOptions = {
  global: {
    align: ['center', 'left', 'right'],
    autoResize: true,
    throttleRedraw: [10, 0, 1000, 10],
    clickToUse: false,
    // dataAttributes: ['all'], // FIXME: can be 'all' or string[]
    editable: {
      add: false,
      remove: false,
      updateGroup: false,
      updateTime: false
    },
    end: '',
    format: {
      minorLabels: {
        millisecond: 'SSS',
        second: 's',
        minute: 'HH:mm',
        hour: 'HH:mm',
        weekday: 'ddd D',
        day: 'D',
        month: 'MMM',
        year: 'YYYY'
      },
      majorLabels: {
        millisecond: 'HH:mm:ss',
        second: 'D MMMM HH:mm',
        minute: 'ddd D MMMM',
        hour: 'ddd D MMMM',
        weekday: 'MMMM YYYY',
        day: 'MMMM YYYY',
        month: 'YYYY',
        year: ''
      }
    },

    //groupOrder: {string, 'function': 'function'},
    groupsDraggable: false,
    height: '',
    //hiddenDates: {object, array},
    locale: '',
    margin: {
      axis: [20, 0, 100, 1],
      item: {
        horizontal: [10, 0, 100, 1],
        vertical: [10, 0, 100, 1]
      }
    },
    max: '',
    maxHeight: '',
    maxMinorChars: [7, 0, 20, 1],
    min: '',
    minHeight: '',
    moveable: false,
    multiselect: false,
    multiselectPerGroup: false,
    //onAdd: {'function': 'function'},
    //onUpdate: {'function': 'function'},
    //onMove: {'function': 'function'},
    //onMoving: {'function': 'function'},
    //onRename: {'function': 'function'},
    //order: {'function': 'function'},
    orientation: {
      axis: ['both', 'bottom', 'top'],
      item: ['bottom', 'top']
    },
    selectable: true,
    showCurrentTime: false,
    showMajorLabels: true,
    showMinorLabels: true,
    stack: true,
    //snap: {'function': 'function', nada},
    start: '',
    //template: {'function': 'function'},
    //timeAxis: {
    //  scale: ['millisecond', 'second', 'minute', 'hour', 'weekday', 'day', 'month', 'year'],
    //  step: [1, 1, 10, 1]
    //},
    type: ['box', 'point', 'range', 'background'],
    width: '100%',
    zoomable: true,
    zoomKey: ['ctrlKey', 'altKey', 'metaKey', ''],
    zoomMax: [315360000000000, 10, 315360000000000, 1],
    zoomMin: [10, 10, 315360000000000, 1]
  }
};

exports.allOptions = allOptions;
exports.configureOptions = configureOptions;

},{}],33:[function(require,module,exports){
// utility functions

// first check if moment.js is already loaded in the browser window, if so,
// use this instance. Else, load via commonjs.

'use strict';

var moment = require('./module/moment');
var uuid = require('./module/uuid');

/**
 * Test whether given object is a number
 * @param {*} object
 * @return {Boolean} isNumber
 */
exports.isNumber = function (object) {
  return object instanceof Number || typeof object == 'number';
};

/**
 * Remove everything in the DOM object
 * @param DOMobject
 */
exports.recursiveDOMDelete = function (DOMobject) {
  if (DOMobject) {
    while (DOMobject.hasChildNodes() === true) {
      exports.recursiveDOMDelete(DOMobject.firstChild);
      DOMobject.removeChild(DOMobject.firstChild);
    }
  }
};

/**
 * this function gives you a range between 0 and 1 based on the min and max values in the set, the total sum of all values and the current value.
 *
 * @param min
 * @param max
 * @param total
 * @param value
 * @returns {number}
 */
exports.giveRange = function (min, max, total, value) {
  if (max == min) {
    return 0.5;
  } else {
    var scale = 1 / (max - min);
    return Math.max(0, (value - min) * scale);
  }
};

/**
 * Test whether given object is a string
 * @param {*} object
 * @return {Boolean} isString
 */
exports.isString = function (object) {
  return object instanceof String || typeof object == 'string';
};

/**
 * Test whether given object is a Date, or a String containing a Date
 * @param {Date | String} object
 * @return {Boolean} isDate
 */
exports.isDate = function (object) {
  if (object instanceof Date) {
    return true;
  } else if (exports.isString(object)) {
    // test whether this string contains a date
    var match = ASPDateRegex.exec(object);
    if (match) {
      return true;
    } else if (!isNaN(Date.parse(object))) {
      return true;
    }
  }

  return false;
};

/**
 * Create a semi UUID
 * source: http://stackoverflow.com/a/105074/1262753
 * @return {String} uuid
 */
exports.randomUUID = function () {
  return uuid.v4();
};

/**
 * assign all keys of an object that are not nested objects to a certain value (used for color objects).
 * @param obj
 * @param value
 */
exports.assignAllKeys = function (obj, value) {
  for (var prop in obj) {
    if (obj.hasOwnProperty(prop)) {
      if (typeof obj[prop] !== 'object') {
        obj[prop] = value;
      }
    }
  }
};

/**
 * Fill an object with a possibly partially defined other object. Only copies values if the a object has an object requiring values.
 * That means an object is not created on a property if only the b object has it.
 * @param obj
 * @param value
 */
exports.fillIfDefined = function (a, b) {
  var allowDeletion = arguments.length <= 2 || arguments[2] === undefined ? false : arguments[2];

  for (var prop in a) {
    if (b[prop] !== undefined) {
      if (typeof b[prop] !== 'object') {
        if ((b[prop] === undefined || b[prop] === null) && a[prop] !== undefined && allowDeletion === true) {
          delete a[prop];
        } else {
          a[prop] = b[prop];
        }
      } else {
        if (typeof a[prop] === 'object') {
          exports.fillIfDefined(a[prop], b[prop], allowDeletion);
        }
      }
    }
  }
};

/**
 * Extend object a with the properties of object b or a series of objects
 * Only properties with defined values are copied
 * @param {Object} a
 * @param {... Object} b
 * @return {Object} a
 */
exports.protoExtend = function (a, b) {
  for (var i = 1; i < arguments.length; i++) {
    var other = arguments[i];
    for (var prop in other) {
      a[prop] = other[prop];
    }
  }
  return a;
};

/**
 * Extend object a with the properties of object b or a series of objects
 * Only properties with defined values are copied
 * @param {Object} a
 * @param {... Object} b
 * @return {Object} a
 */
exports.extend = function (a, b) {
  for (var i = 1; i < arguments.length; i++) {
    var other = arguments[i];
    for (var prop in other) {
      if (other.hasOwnProperty(prop)) {
        a[prop] = other[prop];
      }
    }
  }
  return a;
};

/**
 * Extend object a with selected properties of object b or a series of objects
 * Only properties with defined values are copied
 * @param {Array.<String>} props
 * @param {Object} a
 * @param {Object} b
 * @return {Object} a
 */
exports.selectiveExtend = function (props, a, b) {
  if (!Array.isArray(props)) {
    throw new Error('Array with property names expected as first argument');
  }

  for (var i = 2; i < arguments.length; i++) {
    var other = arguments[i];

    for (var p = 0; p < props.length; p++) {
      var prop = props[p];
      if (other.hasOwnProperty(prop)) {
        a[prop] = other[prop];
      }
    }
  }
  return a;
};

/**
 * Extend object a with selected properties of object b or a series of objects
 * Only properties with defined values are copied
 * @param {Array.<String>} props
 * @param {Object} a
 * @param {Object} b
 * @return {Object} a
 */
exports.selectiveDeepExtend = function (props, a, b) {
  var allowDeletion = arguments.length <= 3 || arguments[3] === undefined ? false : arguments[3];

  // TODO: add support for Arrays to deepExtend
  if (Array.isArray(b)) {
    throw new TypeError('Arrays are not supported by deepExtend');
  }
  for (var i = 2; i < arguments.length; i++) {
    var other = arguments[i];
    for (var p = 0; p < props.length; p++) {
      var prop = props[p];
      if (other.hasOwnProperty(prop)) {
        if (b[prop] && b[prop].constructor === Object) {
          if (a[prop] === undefined) {
            a[prop] = {};
          }
          if (a[prop].constructor === Object) {
            exports.deepExtend(a[prop], b[prop], false, allowDeletion);
          } else {
            if (b[prop] === null && a[prop] !== undefined && allowDeletion === true) {
              delete a[prop];
            } else {
              a[prop] = b[prop];
            }
          }
        } else if (Array.isArray(b[prop])) {
          throw new TypeError('Arrays are not supported by deepExtend');
        } else {
          if (b[prop] === null && a[prop] !== undefined && allowDeletion === true) {
            delete a[prop];
          } else {
            a[prop] = b[prop];
          }
        }
      }
    }
  }
  return a;
};

/**
 * Extend object a with selected properties of object b or a series of objects
 * Only properties with defined values are copied
 * @param {Array.<String>} props
 * @param {Object} a
 * @param {Object} b
 * @return {Object} a
 */
exports.selectiveNotDeepExtend = function (props, a, b) {
  var allowDeletion = arguments.length <= 3 || arguments[3] === undefined ? false : arguments[3];

  // TODO: add support for Arrays to deepExtend
  if (Array.isArray(b)) {
    throw new TypeError('Arrays are not supported by deepExtend');
  }
  for (var prop in b) {
    if (b.hasOwnProperty(prop)) {
      if (props.indexOf(prop) == -1) {
        if (b[prop] && b[prop].constructor === Object) {
          if (a[prop] === undefined) {
            a[prop] = {};
          }
          if (a[prop].constructor === Object) {
            exports.deepExtend(a[prop], b[prop]);
          } else {
            if (b[prop] === null && a[prop] !== undefined && allowDeletion === true) {
              delete a[prop];
            } else {
              a[prop] = b[prop];
            }
          }
        } else if (Array.isArray(b[prop])) {
          a[prop] = [];
          for (var i = 0; i < b[prop].length; i++) {
            a[prop].push(b[prop][i]);
          }
        } else {
          if (b[prop] === null && a[prop] !== undefined && allowDeletion === true) {
            delete a[prop];
          } else {
            a[prop] = b[prop];
          }
        }
      }
    }
  }
  return a;
};

/**
 * Deep extend an object a with the properties of object b
 * @param {Object} a
 * @param {Object} b
 * @param [Boolean] protoExtend --> optional parameter. If true, the prototype values will also be extended.
 *                                  (ie. the options objects that inherit from others will also get the inherited options)
 * @param [Boolean] global      --> optional parameter. If true, the values of fields that are null will not deleted
 * @returns {Object}
 */
exports.deepExtend = function (a, b, protoExtend, allowDeletion) {
  for (var prop in b) {
    if (b.hasOwnProperty(prop) || protoExtend === true) {
      if (b[prop] && b[prop].constructor === Object) {
        if (a[prop] === undefined) {
          a[prop] = {};
        }
        if (a[prop].constructor === Object) {
          exports.deepExtend(a[prop], b[prop], protoExtend);
        } else {
          if (b[prop] === null && a[prop] !== undefined && allowDeletion === true) {
            delete a[prop];
          } else {
            a[prop] = b[prop];
          }
        }
      } else if (Array.isArray(b[prop])) {
        a[prop] = [];
        for (var i = 0; i < b[prop].length; i++) {
          a[prop].push(b[prop][i]);
        }
      } else {
        if (b[prop] === null && a[prop] !== undefined && allowDeletion === true) {
          delete a[prop];
        } else {
          a[prop] = b[prop];
        }
      }
    }
  }
  return a;
};

/**
 * Test whether all elements in two arrays are equal.
 * @param {Array} a
 * @param {Array} b
 * @return {boolean} Returns true if both arrays have the same length and same
 *                   elements.
 */
exports.equalArray = function (a, b) {
  if (a.length != b.length) return false;

  for (var i = 0, len = a.length; i < len; i++) {
    if (a[i] != b[i]) return false;
  }

  return true;
};

/**
 * Convert an object to another type
 * @param {Boolean | Number | String | Date | Moment | Null | undefined} object
 * @param {String | undefined} type   Name of the type. Available types:
 *                                    'Boolean', 'Number', 'String',
 *                                    'Date', 'Moment', ISODate', 'ASPDate'.
 * @return {*} object
 * @throws Error
 */
exports.convert = function (object, type) {
  var match;

  if (object === undefined) {
    return undefined;
  }
  if (object === null) {
    return null;
  }

  if (!type) {
    return object;
  }
  if (!(typeof type === 'string') && !(type instanceof String)) {
    throw new Error('Type must be a string');
  }

  //noinspection FallthroughInSwitchStatementJS
  switch (type) {
    case 'boolean':
    case 'Boolean':
      return Boolean(object);

    case 'number':
    case 'Number':
      return Number(object.valueOf());

    case 'string':
    case 'String':
      return String(object);

    case 'Date':
      if (exports.isNumber(object)) {
        return new Date(object);
      }
      if (object instanceof Date) {
        return new Date(object.valueOf());
      } else if (moment.isMoment(object)) {
        return new Date(object.valueOf());
      }
      if (exports.isString(object)) {
        match = ASPDateRegex.exec(object);
        if (match) {
          // object is an ASP date
          return new Date(Number(match[1])); // parse number
        } else {
            return moment(object).toDate(); // parse string
          }
      } else {
          throw new Error('Cannot convert object of type ' + exports.getType(object) + ' to type Date');
        }

    case 'Moment':
      if (exports.isNumber(object)) {
        return moment(object);
      }
      if (object instanceof Date) {
        return moment(object.valueOf());
      } else if (moment.isMoment(object)) {
        return moment(object);
      }
      if (exports.isString(object)) {
        match = ASPDateRegex.exec(object);
        if (match) {
          // object is an ASP date
          return moment(Number(match[1])); // parse number
        } else {
            return moment(object); // parse string
          }
      } else {
          throw new Error('Cannot convert object of type ' + exports.getType(object) + ' to type Date');
        }

    case 'ISODate':
      if (exports.isNumber(object)) {
        return new Date(object);
      } else if (object instanceof Date) {
        return object.toISOString();
      } else if (moment.isMoment(object)) {
        return object.toDate().toISOString();
      } else if (exports.isString(object)) {
        match = ASPDateRegex.exec(object);
        if (match) {
          // object is an ASP date
          return new Date(Number(match[1])).toISOString(); // parse number
        } else {
            return new Date(object).toISOString(); // parse string
          }
      } else {
          throw new Error('Cannot convert object of type ' + exports.getType(object) + ' to type ISODate');
        }

    case 'ASPDate':
      if (exports.isNumber(object)) {
        return '/Date(' + object + ')/';
      } else if (object instanceof Date) {
        return '/Date(' + object.valueOf() + ')/';
      } else if (exports.isString(object)) {
        match = ASPDateRegex.exec(object);
        var value;
        if (match) {
          // object is an ASP date
          value = new Date(Number(match[1])).valueOf(); // parse number
        } else {
            value = new Date(object).valueOf(); // parse string
          }
        return '/Date(' + value + ')/';
      } else {
        throw new Error('Cannot convert object of type ' + exports.getType(object) + ' to type ASPDate');
      }

    default:
      throw new Error('Unknown type "' + type + '"');
  }
};

// parse ASP.Net Date pattern,
// for example '/Date(1198908717056)/' or '/Date(1198908717056-0700)/'
// code from http://momentjs.com/
var ASPDateRegex = /^\/?Date\((\-?\d+)/i;

/**
 * Get the type of an object, for example exports.getType([]) returns 'Array'
 * @param {*} object
 * @return {String} type
 */
exports.getType = function (object) {
  var type = typeof object;

  if (type == 'object') {
    if (object === null) {
      return 'null';
    }
    if (object instanceof Boolean) {
      return 'Boolean';
    }
    if (object instanceof Number) {
      return 'Number';
    }
    if (object instanceof String) {
      return 'String';
    }
    if (Array.isArray(object)) {
      return 'Array';
    }
    if (object instanceof Date) {
      return 'Date';
    }
    return 'Object';
  } else if (type == 'number') {
    return 'Number';
  } else if (type == 'boolean') {
    return 'Boolean';
  } else if (type == 'string') {
    return 'String';
  } else if (type === undefined) {
    return 'undefined';
  }

  return type;
};

/**
 * Used to extend an array and copy it. This is used to propagate paths recursively.
 *
 * @param arr
 * @param newValue
 * @returns {Array}
 */
exports.copyAndExtendArray = function (arr, newValue) {
  var newArr = [];
  for (var i = 0; i < arr.length; i++) {
    newArr.push(arr[i]);
  }
  newArr.push(newValue);
  return newArr;
};

/**
 * Used to extend an array and copy it. This is used to propagate paths recursively.
 *
 * @param arr
 * @param newValue
 * @returns {Array}
 */
exports.copyArray = function (arr) {
  var newArr = [];
  for (var i = 0; i < arr.length; i++) {
    newArr.push(arr[i]);
  }
  return newArr;
};

/**
 * Retrieve the absolute left value of a DOM element
 * @param {Element} elem        A dom element, for example a div
 * @return {number} left        The absolute left position of this element
 *                              in the browser page.
 */
exports.getAbsoluteLeft = function (elem) {
  return elem.getBoundingClientRect().left;
};

/**
 * Retrieve the absolute top value of a DOM element
 * @param {Element} elem        A dom element, for example a div
 * @return {number} top        The absolute top position of this element
 *                              in the browser page.
 */
exports.getAbsoluteTop = function (elem) {
  return elem.getBoundingClientRect().top;
};

/**
 * add a className to the given elements style
 * @param {Element} elem
 * @param {String} className
 */
exports.addClassName = function (elem, className) {
  var classes = elem.className.split(' ');
  if (classes.indexOf(className) == -1) {
    classes.push(className); // add the class to the array
    elem.className = classes.join(' ');
  }
};

/**
 * add a className to the given elements style
 * @param {Element} elem
 * @param {String} className
 */
exports.removeClassName = function (elem, className) {
  var classes = elem.className.split(' ');
  var index = classes.indexOf(className);
  if (index != -1) {
    classes.splice(index, 1); // remove the class from the array
    elem.className = classes.join(' ');
  }
};

/**
 * For each method for both arrays and objects.
 * In case of an array, the built-in Array.forEach() is applied.
 * In case of an Object, the method loops over all properties of the object.
 * @param {Object | Array} object   An Object or Array
 * @param {function} callback       Callback method, called for each item in
 *                                  the object or array with three parameters:
 *                                  callback(value, index, object)
 */
exports.forEach = function (object, callback) {
  var i, len;
  if (Array.isArray(object)) {
    // array
    for (i = 0, len = object.length; i < len; i++) {
      callback(object[i], i, object);
    }
  } else {
    // object
    for (i in object) {
      if (object.hasOwnProperty(i)) {
        callback(object[i], i, object);
      }
    }
  }
};

/**
 * Convert an object into an array: all objects properties are put into the
 * array. The resulting array is unordered.
 * @param {Object} object
 * @param {Array} array
 */
exports.toArray = function (object) {
  var array = [];

  for (var prop in object) {
    if (object.hasOwnProperty(prop)) array.push(object[prop]);
  }

  return array;
};

/**
 * Update a property in an object
 * @param {Object} object
 * @param {String} key
 * @param {*} value
 * @return {Boolean} changed
 */
exports.updateProperty = function (object, key, value) {
  if (object[key] !== value) {
    object[key] = value;
    return true;
  } else {
    return false;
  }
};

/**
 * Throttle the given function to be only executed once every `wait` milliseconds
 * @param {function} fn
 * @param {number} wait    Time in milliseconds
 * @returns {function} Returns the throttled function
 */
exports.throttle = function (fn, wait) {
  var timeout = null;
  var needExecution = false;

  return function throttled() {
    if (!timeout) {
      needExecution = false;
      fn();

      timeout = setTimeout(function () {
        timeout = null;
        if (needExecution) {
          throttled();
        }
      }, wait);
    } else {
      needExecution = true;
    }
  };
};

/**
 * Add and event listener. Works for all browsers
 * @param {Element}     element    An html element
 * @param {string}      action     The action, for example "click",
 *                                 without the prefix "on"
 * @param {function}    listener   The callback function to be executed
 * @param {boolean}     [useCapture]
 */
exports.addEventListener = function (element, action, listener, useCapture) {
  if (element.addEventListener) {
    if (useCapture === undefined) useCapture = false;

    if (action === "mousewheel" && navigator.userAgent.indexOf("Firefox") >= 0) {
      action = "DOMMouseScroll"; // For Firefox
    }

    element.addEventListener(action, listener, useCapture);
  } else {
    element.attachEvent("on" + action, listener); // IE browsers
  }
};

/**
 * Remove an event listener from an element
 * @param {Element}     element         An html dom element
 * @param {string}      action          The name of the event, for example "mousedown"
 * @param {function}    listener        The listener function
 * @param {boolean}     [useCapture]
 */
exports.removeEventListener = function (element, action, listener, useCapture) {
  if (element.removeEventListener) {
    // non-IE browsers
    if (useCapture === undefined) useCapture = false;

    if (action === "mousewheel" && navigator.userAgent.indexOf("Firefox") >= 0) {
      action = "DOMMouseScroll"; // For Firefox
    }

    element.removeEventListener(action, listener, useCapture);
  } else {
    // IE browsers
    element.detachEvent("on" + action, listener);
  }
};

/**
 * Cancels the event if it is cancelable, without stopping further propagation of the event.
 */
exports.preventDefault = function (event) {
  if (!event) event = window.event;

  if (event.preventDefault) {
    event.preventDefault(); // non-IE browsers
  } else {
      event.returnValue = false; // IE browsers
    }
};

/**
 * Get HTML element which is the target of the event
 * @param {Event} event
 * @return {Element} target element
 */
exports.getTarget = function (event) {
  // code from http://www.quirksmode.org/js/events_properties.html
  if (!event) {
    event = window.event;
  }

  var target;

  if (event.target) {
    target = event.target;
  } else if (event.srcElement) {
    target = event.srcElement;
  }

  if (target.nodeType != undefined && target.nodeType == 3) {
    // defeat Safari bug
    target = target.parentNode;
  }

  return target;
};

/**
 * Check if given element contains given parent somewhere in the DOM tree
 * @param {Element} element
 * @param {Element} parent
 */
exports.hasParent = function (element, parent) {
  var e = element;

  while (e) {
    if (e === parent) {
      return true;
    }
    e = e.parentNode;
  }

  return false;
};

exports.option = {};

/**
 * Convert a value into a boolean
 * @param {Boolean | function | undefined} value
 * @param {Boolean} [defaultValue]
 * @returns {Boolean} bool
 */
exports.option.asBoolean = function (value, defaultValue) {
  if (typeof value == 'function') {
    value = value();
  }

  if (value != null) {
    return value != false;
  }

  return defaultValue || null;
};

/**
 * Convert a value into a number
 * @param {Boolean | function | undefined} value
 * @param {Number} [defaultValue]
 * @returns {Number} number
 */
exports.option.asNumber = function (value, defaultValue) {
  if (typeof value == 'function') {
    value = value();
  }

  if (value != null) {
    return Number(value) || defaultValue || null;
  }

  return defaultValue || null;
};

/**
 * Convert a value into a string
 * @param {String | function | undefined} value
 * @param {String} [defaultValue]
 * @returns {String} str
 */
exports.option.asString = function (value, defaultValue) {
  if (typeof value == 'function') {
    value = value();
  }

  if (value != null) {
    return String(value);
  }

  return defaultValue || null;
};

/**
 * Convert a size or location into a string with pixels or a percentage
 * @param {String | Number | function | undefined} value
 * @param {String} [defaultValue]
 * @returns {String} size
 */
exports.option.asSize = function (value, defaultValue) {
  if (typeof value == 'function') {
    value = value();
  }

  if (exports.isString(value)) {
    return value;
  } else if (exports.isNumber(value)) {
    return value + 'px';
  } else {
    return defaultValue || null;
  }
};

/**
 * Convert a value into a DOM element
 * @param {HTMLElement | function | undefined} value
 * @param {HTMLElement} [defaultValue]
 * @returns {HTMLElement | null} dom
 */
exports.option.asElement = function (value, defaultValue) {
  if (typeof value == 'function') {
    value = value();
  }

  return value || defaultValue || null;
};

/**
 * http://stackoverflow.com/questions/5623838/rgb-to-hex-and-hex-to-rgb
 *
 * @param {String} hex
 * @returns {{r: *, g: *, b: *}} | 255 range
 */
exports.hexToRGB = function (hex) {
  // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
  var shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
  hex = hex.replace(shorthandRegex, function (m, r, g, b) {
    return r + r + g + g + b + b;
  });
  var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
};

/**
 * This function takes color in hex format or rgb() or rgba() format and overrides the opacity. Returns rgba() string.
 * @param color
 * @param opacity
 * @returns {*}
 */
exports.overrideOpacity = function (color, opacity) {
  if (color.indexOf("rgba") != -1) {
    return color;
  } else if (color.indexOf("rgb") != -1) {
    var rgb = color.substr(color.indexOf("(") + 1).replace(")", "").split(",");
    return "rgba(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + "," + opacity + ")";
  } else {
    var rgb = exports.hexToRGB(color);
    if (rgb == null) {
      return color;
    } else {
      return "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + opacity + ")";
    }
  }
};

/**
 *
 * @param red     0 -- 255
 * @param green   0 -- 255
 * @param blue    0 -- 255
 * @returns {string}
 * @constructor
 */
exports.RGBToHex = function (red, green, blue) {
  return "#" + ((1 << 24) + (red << 16) + (green << 8) + blue).toString(16).slice(1);
};

/**
 * Parse a color property into an object with border, background, and
 * highlight colors
 * @param {Object | String} color
 * @return {Object} colorObject
 */
exports.parseColor = function (color) {
  var c;
  if (exports.isString(color) === true) {
    if (exports.isValidRGB(color) === true) {
      var rgb = color.substr(4).substr(0, color.length - 5).split(',').map(function (value) {
        return parseInt(value);
      });
      color = exports.RGBToHex(rgb[0], rgb[1], rgb[2]);
    }
    if (exports.isValidHex(color) === true) {
      var hsv = exports.hexToHSV(color);
      var lighterColorHSV = { h: hsv.h, s: hsv.s * 0.8, v: Math.min(1, hsv.v * 1.02) };
      var darkerColorHSV = { h: hsv.h, s: Math.min(1, hsv.s * 1.25), v: hsv.v * 0.8 };
      var darkerColorHex = exports.HSVToHex(darkerColorHSV.h, darkerColorHSV.s, darkerColorHSV.v);
      var lighterColorHex = exports.HSVToHex(lighterColorHSV.h, lighterColorHSV.s, lighterColorHSV.v);
      c = {
        background: color,
        border: darkerColorHex,
        highlight: {
          background: lighterColorHex,
          border: darkerColorHex
        },
        hover: {
          background: lighterColorHex,
          border: darkerColorHex
        }
      };
    } else {
      c = {
        background: color,
        border: color,
        highlight: {
          background: color,
          border: color
        },
        hover: {
          background: color,
          border: color
        }
      };
    }
  } else {
    c = {};
    c.background = color.background || undefined;
    c.border = color.border || undefined;

    if (exports.isString(color.highlight)) {
      c.highlight = {
        border: color.highlight,
        background: color.highlight
      };
    } else {
      c.highlight = {};
      c.highlight.background = color.highlight && color.highlight.background || undefined;
      c.highlight.border = color.highlight && color.highlight.border || undefined;
    }

    if (exports.isString(color.hover)) {
      c.hover = {
        border: color.hover,
        background: color.hover
      };
    } else {
      c.hover = {};
      c.hover.background = color.hover && color.hover.background || undefined;
      c.hover.border = color.hover && color.hover.border || undefined;
    }
  }

  return c;
};

/**
 * http://www.javascripter.net/faq/rgb2hsv.htm
 *
 * @param red
 * @param green
 * @param blue
 * @returns {*}
 * @constructor
 */
exports.RGBToHSV = function (red, green, blue) {
  red = red / 255;green = green / 255;blue = blue / 255;
  var minRGB = Math.min(red, Math.min(green, blue));
  var maxRGB = Math.max(red, Math.max(green, blue));

  // Black-gray-white
  if (minRGB == maxRGB) {
    return { h: 0, s: 0, v: minRGB };
  }

  // Colors other than black-gray-white:
  var d = red == minRGB ? green - blue : blue == minRGB ? red - green : blue - red;
  var h = red == minRGB ? 3 : blue == minRGB ? 1 : 5;
  var hue = 60 * (h - d / (maxRGB - minRGB)) / 360;
  var saturation = (maxRGB - minRGB) / maxRGB;
  var value = maxRGB;
  return { h: hue, s: saturation, v: value };
};

var cssUtil = {
  // split a string with css styles into an object with key/values
  split: function split(cssText) {
    var styles = {};

    cssText.split(';').forEach(function (style) {
      if (style.trim() != '') {
        var parts = style.split(':');
        var key = parts[0].trim();
        var value = parts[1].trim();
        styles[key] = value;
      }
    });

    return styles;
  },

  // build a css text string from an object with key/values
  join: function join(styles) {
    return Object.keys(styles).map(function (key) {
      return key + ': ' + styles[key];
    }).join('; ');
  }
};

/**
 * Append a string with css styles to an element
 * @param {Element} element
 * @param {String} cssText
 */
exports.addCssText = function (element, cssText) {
  var currentStyles = cssUtil.split(element.style.cssText);
  var newStyles = cssUtil.split(cssText);
  var styles = exports.extend(currentStyles, newStyles);

  element.style.cssText = cssUtil.join(styles);
};

/**
 * Remove a string with css styles from an element
 * @param {Element} element
 * @param {String} cssText
 */
exports.removeCssText = function (element, cssText) {
  var styles = cssUtil.split(element.style.cssText);
  var removeStyles = cssUtil.split(cssText);

  for (var key in removeStyles) {
    if (removeStyles.hasOwnProperty(key)) {
      delete styles[key];
    }
  }

  element.style.cssText = cssUtil.join(styles);
};

/**
 * https://gist.github.com/mjijackson/5311256
 * @param h
 * @param s
 * @param v
 * @returns {{r: number, g: number, b: number}}
 * @constructor
 */
exports.HSVToRGB = function (h, s, v) {
  var r, g, b;

  var i = Math.floor(h * 6);
  var f = h * 6 - i;
  var p = v * (1 - s);
  var q = v * (1 - f * s);
  var t = v * (1 - (1 - f) * s);

  switch (i % 6) {
    case 0:
      r = v, g = t, b = p;break;
    case 1:
      r = q, g = v, b = p;break;
    case 2:
      r = p, g = v, b = t;break;
    case 3:
      r = p, g = q, b = v;break;
    case 4:
      r = t, g = p, b = v;break;
    case 5:
      r = v, g = p, b = q;break;
  }

  return { r: Math.floor(r * 255), g: Math.floor(g * 255), b: Math.floor(b * 255) };
};

exports.HSVToHex = function (h, s, v) {
  var rgb = exports.HSVToRGB(h, s, v);
  return exports.RGBToHex(rgb.r, rgb.g, rgb.b);
};

exports.hexToHSV = function (hex) {
  var rgb = exports.hexToRGB(hex);
  return exports.RGBToHSV(rgb.r, rgb.g, rgb.b);
};

exports.isValidHex = function (hex) {
  var isOk = /(^#[0-9A-F]{6}$)|(^#[0-9A-F]{3}$)/i.test(hex);
  return isOk;
};

exports.isValidRGB = function (rgb) {
  rgb = rgb.replace(" ", "");
  var isOk = /rgb\((\d{1,3}),(\d{1,3}),(\d{1,3})\)/i.test(rgb);
  return isOk;
};
exports.isValidRGBA = function (rgba) {
  rgba = rgba.replace(" ", "");
  var isOk = /rgba\((\d{1,3}),(\d{1,3}),(\d{1,3}),(.{1,3})\)/i.test(rgba);
  return isOk;
};

/**
 * This recursively redirects the prototype of JSON objects to the referenceObject
 * This is used for default options.
 *
 * @param referenceObject
 * @returns {*}
 */
exports.selectiveBridgeObject = function (fields, referenceObject) {
  if (typeof referenceObject == "object") {
    var objectTo = Object.create(referenceObject);
    for (var i = 0; i < fields.length; i++) {
      if (referenceObject.hasOwnProperty(fields[i])) {
        if (typeof referenceObject[fields[i]] == "object") {
          objectTo[fields[i]] = exports.bridgeObject(referenceObject[fields[i]]);
        }
      }
    }
    return objectTo;
  } else {
    return null;
  }
};

/**
 * This recursively redirects the prototype of JSON objects to the referenceObject
 * This is used for default options.
 *
 * @param referenceObject
 * @returns {*}
 */
exports.bridgeObject = function (referenceObject) {
  if (typeof referenceObject == "object") {
    var objectTo = Object.create(referenceObject);
    for (var i in referenceObject) {
      if (referenceObject.hasOwnProperty(i)) {
        if (typeof referenceObject[i] == "object") {
          objectTo[i] = exports.bridgeObject(referenceObject[i]);
        }
      }
    }
    return objectTo;
  } else {
    return null;
  }
};

/**
 * This method provides a stable sort implementation, very fast for presorted data
 *
 * @param a the array
 * @param a order comparator
 * @returns {the array}
 */
exports.insertSort = function (a, compare) {
  for (var i = 0; i < a.length; i++) {
    var k = a[i];
    for (var j = i; j > 0 && compare(k, a[j - 1]) < 0; j--) {
      a[j] = a[j - 1];
    }
    a[j] = k;
  }
  return a;
};

/**
 * this is used to set the options of subobjects in the options object. A requirement of these subobjects
 * is that they have an 'enabled' element which is optional for the user but mandatory for the program.
 *
 * @param [object] mergeTarget | this is either this.options or the options used for the groups.
 * @param [object] options     | options
 * @param [String] option      | this is the option key in the options argument
 * @private
 */
exports.mergeOptions = function (mergeTarget, options, option) {
  var allowDeletion = arguments.length <= 3 || arguments[3] === undefined ? false : arguments[3];
  var globalOptions = arguments.length <= 4 || arguments[4] === undefined ? {} : arguments[4];

  if (options[option] === null) {
    mergeTarget[option] = Object.create(globalOptions[option]);
  } else {
    if (options[option] !== undefined) {
      if (typeof options[option] === 'boolean') {
        mergeTarget[option].enabled = options[option];
      } else {
        if (options[option].enabled === undefined) {
          mergeTarget[option].enabled = true;
        }
        for (var prop in options[option]) {
          if (options[option].hasOwnProperty(prop)) {
            mergeTarget[option][prop] = options[option][prop];
          }
        }
      }
    }
  }
};

/**
 * This function does a binary search for a visible item in a sorted list. If we find a visible item, the code that uses
 * this function will then iterate in both directions over this sorted list to find all visible items.
 *
 * @param {Item[]} orderedItems       | Items ordered by start
 * @param {function} searchFunction   | -1 is lower, 0 is found, 1 is higher
 * @param {String} field
 * @param {String} field2
 * @returns {number}
 * @private
 */
exports.binarySearchCustom = function (orderedItems, searchFunction, field, field2) {
  var maxIterations = 10000;
  var iteration = 0;
  var low = 0;
  var high = orderedItems.length - 1;

  while (low <= high && iteration < maxIterations) {
    var middle = Math.floor((low + high) / 2);

    var item = orderedItems[middle];
    var value = field2 === undefined ? item[field] : item[field][field2];

    var searchResult = searchFunction(value);
    if (searchResult == 0) {
      // jihaa, found a visible item!
      return middle;
    } else if (searchResult == -1) {
      // it is too small --> increase low
      low = middle + 1;
    } else {
      // it is too big --> decrease high
      high = middle - 1;
    }

    iteration++;
  }

  return -1;
};

/**
 * This function does a binary search for a specific value in a sorted array. If it does not exist but is in between of
 * two values, we return either the one before or the one after, depending on user input
 * If it is found, we return the index, else -1.
 *
 * @param {Array} orderedItems
 * @param {{start: number, end: number}} target
 * @param {String} field
 * @param {String} sidePreference   'before' or 'after'
 * @returns {number}
 * @private
 */
exports.binarySearchValue = function (orderedItems, target, field, sidePreference) {
  var maxIterations = 10000;
  var iteration = 0;
  var low = 0;
  var high = orderedItems.length - 1;
  var prevValue, value, nextValue, middle;

  while (low <= high && iteration < maxIterations) {
    // get a new guess
    middle = Math.floor(0.5 * (high + low));
    prevValue = orderedItems[Math.max(0, middle - 1)][field];
    value = orderedItems[middle][field];
    nextValue = orderedItems[Math.min(orderedItems.length - 1, middle + 1)][field];

    if (value == target) {
      // we found the target
      return middle;
    } else if (prevValue < target && value > target) {
      // target is in between of the previous and the current
      return sidePreference == 'before' ? Math.max(0, middle - 1) : middle;
    } else if (value < target && nextValue > target) {
      // target is in between of the current and the next
      return sidePreference == 'before' ? middle : Math.min(orderedItems.length - 1, middle + 1);
    } else {
      // didnt find the target, we need to change our boundaries.
      if (value < target) {
        // it is too small --> increase low
        low = middle + 1;
      } else {
        // it is too big --> decrease high
        high = middle - 1;
      }
    }
    iteration++;
  }

  // didnt find anything. Return -1.
  return -1;
};

/*
 * Easing Functions - inspired from http://gizma.com/easing/
 * only considering the t value for the range [0, 1] => [0, 1]
 * https://gist.github.com/gre/1650294
 */
exports.easingFunctions = {
  // no easing, no acceleration
  linear: function linear(t) {
    return t;
  },
  // accelerating from zero velocity
  easeInQuad: function easeInQuad(t) {
    return t * t;
  },
  // decelerating to zero velocity
  easeOutQuad: function easeOutQuad(t) {
    return t * (2 - t);
  },
  // acceleration until halfway, then deceleration
  easeInOutQuad: function easeInOutQuad(t) {
    return t < .5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
  },
  // accelerating from zero velocity
  easeInCubic: function easeInCubic(t) {
    return t * t * t;
  },
  // decelerating to zero velocity
  easeOutCubic: function easeOutCubic(t) {
    return --t * t * t + 1;
  },
  // acceleration until halfway, then deceleration
  easeInOutCubic: function easeInOutCubic(t) {
    return t < .5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
  },
  // accelerating from zero velocity
  easeInQuart: function easeInQuart(t) {
    return t * t * t * t;
  },
  // decelerating to zero velocity
  easeOutQuart: function easeOutQuart(t) {
    return 1 - --t * t * t * t;
  },
  // acceleration until halfway, then deceleration
  easeInOutQuart: function easeInOutQuart(t) {
    return t < .5 ? 8 * t * t * t * t : 1 - 8 * --t * t * t * t;
  },
  // accelerating from zero velocity
  easeInQuint: function easeInQuint(t) {
    return t * t * t * t * t;
  },
  // decelerating to zero velocity
  easeOutQuint: function easeOutQuint(t) {
    return 1 + --t * t * t * t * t;
  },
  // acceleration until halfway, then deceleration
  easeInOutQuint: function easeInOutQuint(t) {
    return t < .5 ? 16 * t * t * t * t * t : 1 + 16 * --t * t * t * t * t;
  }
};

},{"./module/moment":7,"./module/uuid":8}],34:[function(require,module,exports){

/**
 * Expose `Emitter`.
 */

module.exports = Emitter;

/**
 * Initialize a new `Emitter`.
 *
 * @api public
 */

function Emitter(obj) {
  if (obj) return mixin(obj);
};

/**
 * Mixin the emitter properties.
 *
 * @param {Object} obj
 * @return {Object}
 * @api private
 */

function mixin(obj) {
  for (var key in Emitter.prototype) {
    obj[key] = Emitter.prototype[key];
  }
  return obj;
}

/**
 * Listen on the given `event` with `fn`.
 *
 * @param {String} event
 * @param {Function} fn
 * @return {Emitter}
 * @api public
 */

Emitter.prototype.on =
Emitter.prototype.addEventListener = function(event, fn){
  this._callbacks = this._callbacks || {};
  (this._callbacks[event] = this._callbacks[event] || [])
    .push(fn);
  return this;
};

/**
 * Adds an `event` listener that will be invoked a single
 * time then automatically removed.
 *
 * @param {String} event
 * @param {Function} fn
 * @return {Emitter}
 * @api public
 */

Emitter.prototype.once = function(event, fn){
  var self = this;
  this._callbacks = this._callbacks || {};

  function on() {
    self.off(event, on);
    fn.apply(this, arguments);
  }

  on.fn = fn;
  this.on(event, on);
  return this;
};

/**
 * Remove the given callback for `event` or all
 * registered callbacks.
 *
 * @param {String} event
 * @param {Function} fn
 * @return {Emitter}
 * @api public
 */

Emitter.prototype.off =
Emitter.prototype.removeListener =
Emitter.prototype.removeAllListeners =
Emitter.prototype.removeEventListener = function(event, fn){
  this._callbacks = this._callbacks || {};

  // all
  if (0 == arguments.length) {
    this._callbacks = {};
    return this;
  }

  // specific event
  var callbacks = this._callbacks[event];
  if (!callbacks) return this;

  // remove all handlers
  if (1 == arguments.length) {
    delete this._callbacks[event];
    return this;
  }

  // remove specific handler
  var cb;
  for (var i = 0; i < callbacks.length; i++) {
    cb = callbacks[i];
    if (cb === fn || cb.fn === fn) {
      callbacks.splice(i, 1);
      break;
    }
  }
  return this;
};

/**
 * Emit `event` with the given args.
 *
 * @param {String} event
 * @param {Mixed} ...
 * @return {Emitter}
 */

Emitter.prototype.emit = function(event){
  this._callbacks = this._callbacks || {};
  var args = [].slice.call(arguments, 1)
    , callbacks = this._callbacks[event];

  if (callbacks) {
    callbacks = callbacks.slice(0);
    for (var i = 0, len = callbacks.length; i < len; ++i) {
      callbacks[i].apply(this, args);
    }
  }

  return this;
};

/**
 * Return array of callbacks for `event`.
 *
 * @param {String} event
 * @return {Array}
 * @api public
 */

Emitter.prototype.listeners = function(event){
  this._callbacks = this._callbacks || {};
  return this._callbacks[event] || [];
};

/**
 * Check if this emitter has `event` handlers.
 *
 * @param {String} event
 * @return {Boolean}
 * @api public
 */

Emitter.prototype.hasListeners = function(event){
  return !! this.listeners(event).length;
};

},{}],35:[function(require,module,exports){
/*! Hammer.JS - v2.0.4 - 2014-09-28
 * http://hammerjs.github.io/
 *
 * Copyright (c) 2014 Jorik Tangelder;
 * Licensed under the MIT license */
(function(window, document, exportName, undefined) {
  'use strict';

var VENDOR_PREFIXES = ['', 'webkit', 'moz', 'MS', 'ms', 'o'];
var TEST_ELEMENT = document.createElement('div');

var TYPE_FUNCTION = 'function';

var round = Math.round;
var abs = Math.abs;
var now = Date.now;

/**
 * set a timeout with a given scope
 * @param {Function} fn
 * @param {Number} timeout
 * @param {Object} context
 * @returns {number}
 */
function setTimeoutContext(fn, timeout, context) {
    return setTimeout(bindFn(fn, context), timeout);
}

/**
 * if the argument is an array, we want to execute the fn on each entry
 * if it aint an array we don't want to do a thing.
 * this is used by all the methods that accept a single and array argument.
 * @param {*|Array} arg
 * @param {String} fn
 * @param {Object} [context]
 * @returns {Boolean}
 */
function invokeArrayArg(arg, fn, context) {
    if (Array.isArray(arg)) {
        each(arg, context[fn], context);
        return true;
    }
    return false;
}

/**
 * walk objects and arrays
 * @param {Object} obj
 * @param {Function} iterator
 * @param {Object} context
 */
function each(obj, iterator, context) {
    var i;

    if (!obj) {
        return;
    }

    if (obj.forEach) {
        obj.forEach(iterator, context);
    } else if (obj.length !== undefined) {
        i = 0;
        while (i < obj.length) {
            iterator.call(context, obj[i], i, obj);
            i++;
        }
    } else {
        for (i in obj) {
            obj.hasOwnProperty(i) && iterator.call(context, obj[i], i, obj);
        }
    }
}

/**
 * extend object.
 * means that properties in dest will be overwritten by the ones in src.
 * @param {Object} dest
 * @param {Object} src
 * @param {Boolean} [merge]
 * @returns {Object} dest
 */
function extend(dest, src, merge) {
    var keys = Object.keys(src);
    var i = 0;
    while (i < keys.length) {
        if (!merge || (merge && dest[keys[i]] === undefined)) {
            dest[keys[i]] = src[keys[i]];
        }
        i++;
    }
    return dest;
}

/**
 * merge the values from src in the dest.
 * means that properties that exist in dest will not be overwritten by src
 * @param {Object} dest
 * @param {Object} src
 * @returns {Object} dest
 */
function merge(dest, src) {
    return extend(dest, src, true);
}

/**
 * simple class inheritance
 * @param {Function} child
 * @param {Function} base
 * @param {Object} [properties]
 */
function inherit(child, base, properties) {
    var baseP = base.prototype,
        childP;

    childP = child.prototype = Object.create(baseP);
    childP.constructor = child;
    childP._super = baseP;

    if (properties) {
        extend(childP, properties);
    }
}

/**
 * simple function bind
 * @param {Function} fn
 * @param {Object} context
 * @returns {Function}
 */
function bindFn(fn, context) {
    return function boundFn() {
        return fn.apply(context, arguments);
    };
}

/**
 * let a boolean value also be a function that must return a boolean
 * this first item in args will be used as the context
 * @param {Boolean|Function} val
 * @param {Array} [args]
 * @returns {Boolean}
 */
function boolOrFn(val, args) {
    if (typeof val == TYPE_FUNCTION) {
        return val.apply(args ? args[0] || undefined : undefined, args);
    }
    return val;
}

/**
 * use the val2 when val1 is undefined
 * @param {*} val1
 * @param {*} val2
 * @returns {*}
 */
function ifUndefined(val1, val2) {
    return (val1 === undefined) ? val2 : val1;
}

/**
 * addEventListener with multiple events at once
 * @param {EventTarget} target
 * @param {String} types
 * @param {Function} handler
 */
function addEventListeners(target, types, handler) {
    each(splitStr(types), function(type) {
        target.addEventListener(type, handler, false);
    });
}

/**
 * removeEventListener with multiple events at once
 * @param {EventTarget} target
 * @param {String} types
 * @param {Function} handler
 */
function removeEventListeners(target, types, handler) {
    each(splitStr(types), function(type) {
        target.removeEventListener(type, handler, false);
    });
}

/**
 * find if a node is in the given parent
 * @method hasParent
 * @param {HTMLElement} node
 * @param {HTMLElement} parent
 * @return {Boolean} found
 */
function hasParent(node, parent) {
    while (node) {
        if (node == parent) {
            return true;
        }
        node = node.parentNode;
    }
    return false;
}

/**
 * small indexOf wrapper
 * @param {String} str
 * @param {String} find
 * @returns {Boolean} found
 */
function inStr(str, find) {
    return str.indexOf(find) > -1;
}

/**
 * split string on whitespace
 * @param {String} str
 * @returns {Array} words
 */
function splitStr(str) {
    return str.trim().split(/\s+/g);
}

/**
 * find if a array contains the object using indexOf or a simple polyFill
 * @param {Array} src
 * @param {String} find
 * @param {String} [findByKey]
 * @return {Boolean|Number} false when not found, or the index
 */
function inArray(src, find, findByKey) {
    if (src.indexOf && !findByKey) {
        return src.indexOf(find);
    } else {
        var i = 0;
        while (i < src.length) {
            if ((findByKey && src[i][findByKey] == find) || (!findByKey && src[i] === find)) {
                return i;
            }
            i++;
        }
        return -1;
    }
}

/**
 * convert array-like objects to real arrays
 * @param {Object} obj
 * @returns {Array}
 */
function toArray(obj) {
    return Array.prototype.slice.call(obj, 0);
}

/**
 * unique array with objects based on a key (like 'id') or just by the array's value
 * @param {Array} src [{id:1},{id:2},{id:1}]
 * @param {String} [key]
 * @param {Boolean} [sort=False]
 * @returns {Array} [{id:1},{id:2}]
 */
function uniqueArray(src, key, sort) {
    var results = [];
    var values = [];
    var i = 0;

    while (i < src.length) {
        var val = key ? src[i][key] : src[i];
        if (inArray(values, val) < 0) {
            results.push(src[i]);
        }
        values[i] = val;
        i++;
    }

    if (sort) {
        if (!key) {
            results = results.sort();
        } else {
            results = results.sort(function sortUniqueArray(a, b) {
                return a[key] > b[key];
            });
        }
    }

    return results;
}

/**
 * get the prefixed property
 * @param {Object} obj
 * @param {String} property
 * @returns {String|Undefined} prefixed
 */
function prefixed(obj, property) {
    var prefix, prop;
    var camelProp = property[0].toUpperCase() + property.slice(1);

    var i = 0;
    while (i < VENDOR_PREFIXES.length) {
        prefix = VENDOR_PREFIXES[i];
        prop = (prefix) ? prefix + camelProp : property;

        if (prop in obj) {
            return prop;
        }
        i++;
    }
    return undefined;
}

/**
 * get a unique id
 * @returns {number} uniqueId
 */
var _uniqueId = 1;
function uniqueId() {
    return _uniqueId++;
}

/**
 * get the window object of an element
 * @param {HTMLElement} element
 * @returns {DocumentView|Window}
 */
function getWindowForElement(element) {
    var doc = element.ownerDocument;
    return (doc.defaultView || doc.parentWindow);
}

var MOBILE_REGEX = /mobile|tablet|ip(ad|hone|od)|android/i;

var SUPPORT_TOUCH = ('ontouchstart' in window);
var SUPPORT_POINTER_EVENTS = prefixed(window, 'PointerEvent') !== undefined;
var SUPPORT_ONLY_TOUCH = SUPPORT_TOUCH && MOBILE_REGEX.test(navigator.userAgent);

var INPUT_TYPE_TOUCH = 'touch';
var INPUT_TYPE_PEN = 'pen';
var INPUT_TYPE_MOUSE = 'mouse';
var INPUT_TYPE_KINECT = 'kinect';

var COMPUTE_INTERVAL = 25;

var INPUT_START = 1;
var INPUT_MOVE = 2;
var INPUT_END = 4;
var INPUT_CANCEL = 8;

var DIRECTION_NONE = 1;
var DIRECTION_LEFT = 2;
var DIRECTION_RIGHT = 4;
var DIRECTION_UP = 8;
var DIRECTION_DOWN = 16;

var DIRECTION_HORIZONTAL = DIRECTION_LEFT | DIRECTION_RIGHT;
var DIRECTION_VERTICAL = DIRECTION_UP | DIRECTION_DOWN;
var DIRECTION_ALL = DIRECTION_HORIZONTAL | DIRECTION_VERTICAL;

var PROPS_XY = ['x', 'y'];
var PROPS_CLIENT_XY = ['clientX', 'clientY'];

/**
 * create new input type manager
 * @param {Manager} manager
 * @param {Function} callback
 * @returns {Input}
 * @constructor
 */
function Input(manager, callback) {
    var self = this;
    this.manager = manager;
    this.callback = callback;
    this.element = manager.element;
    this.target = manager.options.inputTarget;

    // smaller wrapper around the handler, for the scope and the enabled state of the manager,
    // so when disabled the input events are completely bypassed.
    this.domHandler = function(ev) {
        if (boolOrFn(manager.options.enable, [manager])) {
            self.handler(ev);
        }
    };

    this.init();

}

Input.prototype = {
    /**
     * should handle the inputEvent data and trigger the callback
     * @virtual
     */
    handler: function() { },

    /**
     * bind the events
     */
    init: function() {
        this.evEl && addEventListeners(this.element, this.evEl, this.domHandler);
        this.evTarget && addEventListeners(this.target, this.evTarget, this.domHandler);
        this.evWin && addEventListeners(getWindowForElement(this.element), this.evWin, this.domHandler);
    },

    /**
     * unbind the events
     */
    destroy: function() {
        this.evEl && removeEventListeners(this.element, this.evEl, this.domHandler);
        this.evTarget && removeEventListeners(this.target, this.evTarget, this.domHandler);
        this.evWin && removeEventListeners(getWindowForElement(this.element), this.evWin, this.domHandler);
    }
};

/**
 * create new input type manager
 * called by the Manager constructor
 * @param {Hammer} manager
 * @returns {Input}
 */
function createInputInstance(manager) {
    var Type;
    var inputClass = manager.options.inputClass;

    if (inputClass) {
        Type = inputClass;
    } else if (SUPPORT_POINTER_EVENTS) {
        Type = PointerEventInput;
    } else if (SUPPORT_ONLY_TOUCH) {
        Type = TouchInput;
    } else if (!SUPPORT_TOUCH) {
        Type = MouseInput;
    } else {
        Type = TouchMouseInput;
    }
    return new (Type)(manager, inputHandler);
}

/**
 * handle input events
 * @param {Manager} manager
 * @param {String} eventType
 * @param {Object} input
 */
function inputHandler(manager, eventType, input) {
    var pointersLen = input.pointers.length;
    var changedPointersLen = input.changedPointers.length;
    var isFirst = (eventType & INPUT_START && (pointersLen - changedPointersLen === 0));
    var isFinal = (eventType & (INPUT_END | INPUT_CANCEL) && (pointersLen - changedPointersLen === 0));

    input.isFirst = !!isFirst;
    input.isFinal = !!isFinal;

    if (isFirst) {
        manager.session = {};
    }

    // source event is the normalized value of the domEvents
    // like 'touchstart, mouseup, pointerdown'
    input.eventType = eventType;

    // compute scale, rotation etc
    computeInputData(manager, input);

    // emit secret event
    manager.emit('hammer.input', input);

    manager.recognize(input);
    manager.session.prevInput = input;
}

/**
 * extend the data with some usable properties like scale, rotate, velocity etc
 * @param {Object} manager
 * @param {Object} input
 */
function computeInputData(manager, input) {
    var session = manager.session;
    var pointers = input.pointers;
    var pointersLength = pointers.length;

    // store the first input to calculate the distance and direction
    if (!session.firstInput) {
        session.firstInput = simpleCloneInputData(input);
    }

    // to compute scale and rotation we need to store the multiple touches
    if (pointersLength > 1 && !session.firstMultiple) {
        session.firstMultiple = simpleCloneInputData(input);
    } else if (pointersLength === 1) {
        session.firstMultiple = false;
    }

    var firstInput = session.firstInput;
    var firstMultiple = session.firstMultiple;
    var offsetCenter = firstMultiple ? firstMultiple.center : firstInput.center;

    var center = input.center = getCenter(pointers);
    input.timeStamp = now();
    input.deltaTime = input.timeStamp - firstInput.timeStamp;

    input.angle = getAngle(offsetCenter, center);
    input.distance = getDistance(offsetCenter, center);

    computeDeltaXY(session, input);
    input.offsetDirection = getDirection(input.deltaX, input.deltaY);

    input.scale = firstMultiple ? getScale(firstMultiple.pointers, pointers) : 1;
    input.rotation = firstMultiple ? getRotation(firstMultiple.pointers, pointers) : 0;

    computeIntervalInputData(session, input);

    // find the correct target
    var target = manager.element;
    if (hasParent(input.srcEvent.target, target)) {
        target = input.srcEvent.target;
    }
    input.target = target;
}

function computeDeltaXY(session, input) {
    var center = input.center;
    var offset = session.offsetDelta || {};
    var prevDelta = session.prevDelta || {};
    var prevInput = session.prevInput || {};

    if (input.eventType === INPUT_START || prevInput.eventType === INPUT_END) {
        prevDelta = session.prevDelta = {
            x: prevInput.deltaX || 0,
            y: prevInput.deltaY || 0
        };

        offset = session.offsetDelta = {
            x: center.x,
            y: center.y
        };
    }

    input.deltaX = prevDelta.x + (center.x - offset.x);
    input.deltaY = prevDelta.y + (center.y - offset.y);
}

/**
 * velocity is calculated every x ms
 * @param {Object} session
 * @param {Object} input
 */
function computeIntervalInputData(session, input) {
    var last = session.lastInterval || input,
        deltaTime = input.timeStamp - last.timeStamp,
        velocity, velocityX, velocityY, direction;

    if (input.eventType != INPUT_CANCEL && (deltaTime > COMPUTE_INTERVAL || last.velocity === undefined)) {
        var deltaX = last.deltaX - input.deltaX;
        var deltaY = last.deltaY - input.deltaY;

        var v = getVelocity(deltaTime, deltaX, deltaY);
        velocityX = v.x;
        velocityY = v.y;
        velocity = (abs(v.x) > abs(v.y)) ? v.x : v.y;
        direction = getDirection(deltaX, deltaY);

        session.lastInterval = input;
    } else {
        // use latest velocity info if it doesn't overtake a minimum period
        velocity = last.velocity;
        velocityX = last.velocityX;
        velocityY = last.velocityY;
        direction = last.direction;
    }

    input.velocity = velocity;
    input.velocityX = velocityX;
    input.velocityY = velocityY;
    input.direction = direction;
}

/**
 * create a simple clone from the input used for storage of firstInput and firstMultiple
 * @param {Object} input
 * @returns {Object} clonedInputData
 */
function simpleCloneInputData(input) {
    // make a simple copy of the pointers because we will get a reference if we don't
    // we only need clientXY for the calculations
    var pointers = [];
    var i = 0;
    while (i < input.pointers.length) {
        pointers[i] = {
            clientX: round(input.pointers[i].clientX),
            clientY: round(input.pointers[i].clientY)
        };
        i++;
    }

    return {
        timeStamp: now(),
        pointers: pointers,
        center: getCenter(pointers),
        deltaX: input.deltaX,
        deltaY: input.deltaY
    };
}

/**
 * get the center of all the pointers
 * @param {Array} pointers
 * @return {Object} center contains `x` and `y` properties
 */
function getCenter(pointers) {
    var pointersLength = pointers.length;

    // no need to loop when only one touch
    if (pointersLength === 1) {
        return {
            x: round(pointers[0].clientX),
            y: round(pointers[0].clientY)
        };
    }

    var x = 0, y = 0, i = 0;
    while (i < pointersLength) {
        x += pointers[i].clientX;
        y += pointers[i].clientY;
        i++;
    }

    return {
        x: round(x / pointersLength),
        y: round(y / pointersLength)
    };
}

/**
 * calculate the velocity between two points. unit is in px per ms.
 * @param {Number} deltaTime
 * @param {Number} x
 * @param {Number} y
 * @return {Object} velocity `x` and `y`
 */
function getVelocity(deltaTime, x, y) {
    return {
        x: x / deltaTime || 0,
        y: y / deltaTime || 0
    };
}

/**
 * get the direction between two points
 * @param {Number} x
 * @param {Number} y
 * @return {Number} direction
 */
function getDirection(x, y) {
    if (x === y) {
        return DIRECTION_NONE;
    }

    if (abs(x) >= abs(y)) {
        return x > 0 ? DIRECTION_LEFT : DIRECTION_RIGHT;
    }
    return y > 0 ? DIRECTION_UP : DIRECTION_DOWN;
}

/**
 * calculate the absolute distance between two points
 * @param {Object} p1 {x, y}
 * @param {Object} p2 {x, y}
 * @param {Array} [props] containing x and y keys
 * @return {Number} distance
 */
function getDistance(p1, p2, props) {
    if (!props) {
        props = PROPS_XY;
    }
    var x = p2[props[0]] - p1[props[0]],
        y = p2[props[1]] - p1[props[1]];

    return Math.sqrt((x * x) + (y * y));
}

/**
 * calculate the angle between two coordinates
 * @param {Object} p1
 * @param {Object} p2
 * @param {Array} [props] containing x and y keys
 * @return {Number} angle
 */
function getAngle(p1, p2, props) {
    if (!props) {
        props = PROPS_XY;
    }
    var x = p2[props[0]] - p1[props[0]],
        y = p2[props[1]] - p1[props[1]];
    return Math.atan2(y, x) * 180 / Math.PI;
}

/**
 * calculate the rotation degrees between two pointersets
 * @param {Array} start array of pointers
 * @param {Array} end array of pointers
 * @return {Number} rotation
 */
function getRotation(start, end) {
    return getAngle(end[1], end[0], PROPS_CLIENT_XY) - getAngle(start[1], start[0], PROPS_CLIENT_XY);
}

/**
 * calculate the scale factor between two pointersets
 * no scale is 1, and goes down to 0 when pinched together, and bigger when pinched out
 * @param {Array} start array of pointers
 * @param {Array} end array of pointers
 * @return {Number} scale
 */
function getScale(start, end) {
    return getDistance(end[0], end[1], PROPS_CLIENT_XY) / getDistance(start[0], start[1], PROPS_CLIENT_XY);
}

var MOUSE_INPUT_MAP = {
    mousedown: INPUT_START,
    mousemove: INPUT_MOVE,
    mouseup: INPUT_END
};

var MOUSE_ELEMENT_EVENTS = 'mousedown';
var MOUSE_WINDOW_EVENTS = 'mousemove mouseup';

/**
 * Mouse events input
 * @constructor
 * @extends Input
 */
function MouseInput() {
    this.evEl = MOUSE_ELEMENT_EVENTS;
    this.evWin = MOUSE_WINDOW_EVENTS;

    this.allow = true; // used by Input.TouchMouse to disable mouse events
    this.pressed = false; // mousedown state

    Input.apply(this, arguments);
}

inherit(MouseInput, Input, {
    /**
     * handle mouse events
     * @param {Object} ev
     */
    handler: function MEhandler(ev) {
        var eventType = MOUSE_INPUT_MAP[ev.type];

        // on start we want to have the left mouse button down
        if (eventType & INPUT_START && ev.button === 0) {
            this.pressed = true;
        }

        if (eventType & INPUT_MOVE && ev.which !== 1) {
            eventType = INPUT_END;
        }

        // mouse must be down, and mouse events are allowed (see the TouchMouse input)
        if (!this.pressed || !this.allow) {
            return;
        }

        if (eventType & INPUT_END) {
            this.pressed = false;
        }

        this.callback(this.manager, eventType, {
            pointers: [ev],
            changedPointers: [ev],
            pointerType: INPUT_TYPE_MOUSE,
            srcEvent: ev
        });
    }
});

var POINTER_INPUT_MAP = {
    pointerdown: INPUT_START,
    pointermove: INPUT_MOVE,
    pointerup: INPUT_END,
    pointercancel: INPUT_CANCEL,
    pointerout: INPUT_CANCEL
};

// in IE10 the pointer types is defined as an enum
var IE10_POINTER_TYPE_ENUM = {
    2: INPUT_TYPE_TOUCH,
    3: INPUT_TYPE_PEN,
    4: INPUT_TYPE_MOUSE,
    5: INPUT_TYPE_KINECT // see https://twitter.com/jacobrossi/status/480596438489890816
};

var POINTER_ELEMENT_EVENTS = 'pointerdown';
var POINTER_WINDOW_EVENTS = 'pointermove pointerup pointercancel';

// IE10 has prefixed support, and case-sensitive
if (window.MSPointerEvent) {
    POINTER_ELEMENT_EVENTS = 'MSPointerDown';
    POINTER_WINDOW_EVENTS = 'MSPointerMove MSPointerUp MSPointerCancel';
}

/**
 * Pointer events input
 * @constructor
 * @extends Input
 */
function PointerEventInput() {
    this.evEl = POINTER_ELEMENT_EVENTS;
    this.evWin = POINTER_WINDOW_EVENTS;

    Input.apply(this, arguments);

    this.store = (this.manager.session.pointerEvents = []);
}

inherit(PointerEventInput, Input, {
    /**
     * handle mouse events
     * @param {Object} ev
     */
    handler: function PEhandler(ev) {
        var store = this.store;
        var removePointer = false;

        var eventTypeNormalized = ev.type.toLowerCase().replace('ms', '');
        var eventType = POINTER_INPUT_MAP[eventTypeNormalized];
        var pointerType = IE10_POINTER_TYPE_ENUM[ev.pointerType] || ev.pointerType;

        var isTouch = (pointerType == INPUT_TYPE_TOUCH);

        // get index of the event in the store
        var storeIndex = inArray(store, ev.pointerId, 'pointerId');

        // start and mouse must be down
        if (eventType & INPUT_START && (ev.button === 0 || isTouch)) {
            if (storeIndex < 0) {
                store.push(ev);
                storeIndex = store.length - 1;
            }
        } else if (eventType & (INPUT_END | INPUT_CANCEL)) {
            removePointer = true;
        }

        // it not found, so the pointer hasn't been down (so it's probably a hover)
        if (storeIndex < 0) {
            return;
        }

        // update the event in the store
        store[storeIndex] = ev;

        this.callback(this.manager, eventType, {
            pointers: store,
            changedPointers: [ev],
            pointerType: pointerType,
            srcEvent: ev
        });

        if (removePointer) {
            // remove from the store
            store.splice(storeIndex, 1);
        }
    }
});

var SINGLE_TOUCH_INPUT_MAP = {
    touchstart: INPUT_START,
    touchmove: INPUT_MOVE,
    touchend: INPUT_END,
    touchcancel: INPUT_CANCEL
};

var SINGLE_TOUCH_TARGET_EVENTS = 'touchstart';
var SINGLE_TOUCH_WINDOW_EVENTS = 'touchstart touchmove touchend touchcancel';

/**
 * Touch events input
 * @constructor
 * @extends Input
 */
function SingleTouchInput() {
    this.evTarget = SINGLE_TOUCH_TARGET_EVENTS;
    this.evWin = SINGLE_TOUCH_WINDOW_EVENTS;
    this.started = false;

    Input.apply(this, arguments);
}

inherit(SingleTouchInput, Input, {
    handler: function TEhandler(ev) {
        var type = SINGLE_TOUCH_INPUT_MAP[ev.type];

        // should we handle the touch events?
        if (type === INPUT_START) {
            this.started = true;
        }

        if (!this.started) {
            return;
        }

        var touches = normalizeSingleTouches.call(this, ev, type);

        // when done, reset the started state
        if (type & (INPUT_END | INPUT_CANCEL) && touches[0].length - touches[1].length === 0) {
            this.started = false;
        }

        this.callback(this.manager, type, {
            pointers: touches[0],
            changedPointers: touches[1],
            pointerType: INPUT_TYPE_TOUCH,
            srcEvent: ev
        });
    }
});

/**
 * @this {TouchInput}
 * @param {Object} ev
 * @param {Number} type flag
 * @returns {undefined|Array} [all, changed]
 */
function normalizeSingleTouches(ev, type) {
    var all = toArray(ev.touches);
    var changed = toArray(ev.changedTouches);

    if (type & (INPUT_END | INPUT_CANCEL)) {
        all = uniqueArray(all.concat(changed), 'identifier', true);
    }

    return [all, changed];
}

var TOUCH_INPUT_MAP = {
    touchstart: INPUT_START,
    touchmove: INPUT_MOVE,
    touchend: INPUT_END,
    touchcancel: INPUT_CANCEL
};

var TOUCH_TARGET_EVENTS = 'touchstart touchmove touchend touchcancel';

/**
 * Multi-user touch events input
 * @constructor
 * @extends Input
 */
function TouchInput() {
    this.evTarget = TOUCH_TARGET_EVENTS;
    this.targetIds = {};

    Input.apply(this, arguments);
}

inherit(TouchInput, Input, {
    handler: function MTEhandler(ev) {
        var type = TOUCH_INPUT_MAP[ev.type];
        var touches = getTouches.call(this, ev, type);
        if (!touches) {
            return;
        }

        this.callback(this.manager, type, {
            pointers: touches[0],
            changedPointers: touches[1],
            pointerType: INPUT_TYPE_TOUCH,
            srcEvent: ev
        });
    }
});

/**
 * @this {TouchInput}
 * @param {Object} ev
 * @param {Number} type flag
 * @returns {undefined|Array} [all, changed]
 */
function getTouches(ev, type) {
    var allTouches = toArray(ev.touches);
    var targetIds = this.targetIds;

    // when there is only one touch, the process can be simplified
    if (type & (INPUT_START | INPUT_MOVE) && allTouches.length === 1) {
        targetIds[allTouches[0].identifier] = true;
        return [allTouches, allTouches];
    }

    var i,
        targetTouches,
        changedTouches = toArray(ev.changedTouches),
        changedTargetTouches = [],
        target = this.target;

    // get target touches from touches
    targetTouches = allTouches.filter(function(touch) {
        return hasParent(touch.target, target);
    });

    // collect touches
    if (type === INPUT_START) {
        i = 0;
        while (i < targetTouches.length) {
            targetIds[targetTouches[i].identifier] = true;
            i++;
        }
    }

    // filter changed touches to only contain touches that exist in the collected target ids
    i = 0;
    while (i < changedTouches.length) {
        if (targetIds[changedTouches[i].identifier]) {
            changedTargetTouches.push(changedTouches[i]);
        }

        // cleanup removed touches
        if (type & (INPUT_END | INPUT_CANCEL)) {
            delete targetIds[changedTouches[i].identifier];
        }
        i++;
    }

    if (!changedTargetTouches.length) {
        return;
    }

    return [
        // merge targetTouches with changedTargetTouches so it contains ALL touches, including 'end' and 'cancel'
        uniqueArray(targetTouches.concat(changedTargetTouches), 'identifier', true),
        changedTargetTouches
    ];
}

/**
 * Combined touch and mouse input
 *
 * Touch has a higher priority then mouse, and while touching no mouse events are allowed.
 * This because touch devices also emit mouse events while doing a touch.
 *
 * @constructor
 * @extends Input
 */
function TouchMouseInput() {
    Input.apply(this, arguments);

    var handler = bindFn(this.handler, this);
    this.touch = new TouchInput(this.manager, handler);
    this.mouse = new MouseInput(this.manager, handler);
}

inherit(TouchMouseInput, Input, {
    /**
     * handle mouse and touch events
     * @param {Hammer} manager
     * @param {String} inputEvent
     * @param {Object} inputData
     */
    handler: function TMEhandler(manager, inputEvent, inputData) {
        var isTouch = (inputData.pointerType == INPUT_TYPE_TOUCH),
            isMouse = (inputData.pointerType == INPUT_TYPE_MOUSE);

        // when we're in a touch event, so  block all upcoming mouse events
        // most mobile browser also emit mouseevents, right after touchstart
        if (isTouch) {
            this.mouse.allow = false;
        } else if (isMouse && !this.mouse.allow) {
            return;
        }

        // reset the allowMouse when we're done
        if (inputEvent & (INPUT_END | INPUT_CANCEL)) {
            this.mouse.allow = true;
        }

        this.callback(manager, inputEvent, inputData);
    },

    /**
     * remove the event listeners
     */
    destroy: function destroy() {
        this.touch.destroy();
        this.mouse.destroy();
    }
});

var PREFIXED_TOUCH_ACTION = prefixed(TEST_ELEMENT.style, 'touchAction');
var NATIVE_TOUCH_ACTION = PREFIXED_TOUCH_ACTION !== undefined;

// magical touchAction value
var TOUCH_ACTION_COMPUTE = 'compute';
var TOUCH_ACTION_AUTO = 'auto';
var TOUCH_ACTION_MANIPULATION = 'manipulation'; // not implemented
var TOUCH_ACTION_NONE = 'none';
var TOUCH_ACTION_PAN_X = 'pan-x';
var TOUCH_ACTION_PAN_Y = 'pan-y';

/**
 * Touch Action
 * sets the touchAction property or uses the js alternative
 * @param {Manager} manager
 * @param {String} value
 * @constructor
 */
function TouchAction(manager, value) {
    this.manager = manager;
    this.set(value);
}

TouchAction.prototype = {
    /**
     * set the touchAction value on the element or enable the polyfill
     * @param {String} value
     */
    set: function(value) {
        // find out the touch-action by the event handlers
        if (value == TOUCH_ACTION_COMPUTE) {
            value = this.compute();
        }

        if (NATIVE_TOUCH_ACTION) {
            this.manager.element.style[PREFIXED_TOUCH_ACTION] = value;
        }
        this.actions = value.toLowerCase().trim();
    },

    /**
     * just re-set the touchAction value
     */
    update: function() {
        this.set(this.manager.options.touchAction);
    },

    /**
     * compute the value for the touchAction property based on the recognizer's settings
     * @returns {String} value
     */
    compute: function() {
        var actions = [];
        each(this.manager.recognizers, function(recognizer) {
            if (boolOrFn(recognizer.options.enable, [recognizer])) {
                actions = actions.concat(recognizer.getTouchAction());
            }
        });
        return cleanTouchActions(actions.join(' '));
    },

    /**
     * this method is called on each input cycle and provides the preventing of the browser behavior
     * @param {Object} input
     */
    preventDefaults: function(input) {
        // not needed with native support for the touchAction property
        if (NATIVE_TOUCH_ACTION) {
            return;
        }

        var srcEvent = input.srcEvent;
        var direction = input.offsetDirection;

        // if the touch action did prevented once this session
        if (this.manager.session.prevented) {
            srcEvent.preventDefault();
            return;
        }

        var actions = this.actions;
        var hasNone = inStr(actions, TOUCH_ACTION_NONE);
        var hasPanY = inStr(actions, TOUCH_ACTION_PAN_Y);
        var hasPanX = inStr(actions, TOUCH_ACTION_PAN_X);

        if (hasNone ||
            (hasPanY && direction & DIRECTION_HORIZONTAL) ||
            (hasPanX && direction & DIRECTION_VERTICAL)) {
            return this.preventSrc(srcEvent);
        }
    },

    /**
     * call preventDefault to prevent the browser's default behavior (scrolling in most cases)
     * @param {Object} srcEvent
     */
    preventSrc: function(srcEvent) {
        this.manager.session.prevented = true;
        srcEvent.preventDefault();
    }
};

/**
 * when the touchActions are collected they are not a valid value, so we need to clean things up. *
 * @param {String} actions
 * @returns {*}
 */
function cleanTouchActions(actions) {
    // none
    if (inStr(actions, TOUCH_ACTION_NONE)) {
        return TOUCH_ACTION_NONE;
    }

    var hasPanX = inStr(actions, TOUCH_ACTION_PAN_X);
    var hasPanY = inStr(actions, TOUCH_ACTION_PAN_Y);

    // pan-x and pan-y can be combined
    if (hasPanX && hasPanY) {
        return TOUCH_ACTION_PAN_X + ' ' + TOUCH_ACTION_PAN_Y;
    }

    // pan-x OR pan-y
    if (hasPanX || hasPanY) {
        return hasPanX ? TOUCH_ACTION_PAN_X : TOUCH_ACTION_PAN_Y;
    }

    // manipulation
    if (inStr(actions, TOUCH_ACTION_MANIPULATION)) {
        return TOUCH_ACTION_MANIPULATION;
    }

    return TOUCH_ACTION_AUTO;
}

/**
 * Recognizer flow explained; *
 * All recognizers have the initial state of POSSIBLE when a input session starts.
 * The definition of a input session is from the first input until the last input, with all it's movement in it. *
 * Example session for mouse-input: mousedown -> mousemove -> mouseup
 *
 * On each recognizing cycle (see Manager.recognize) the .recognize() method is executed
 * which determines with state it should be.
 *
 * If the recognizer has the state FAILED, CANCELLED or RECOGNIZED (equals ENDED), it is reset to
 * POSSIBLE to give it another change on the next cycle.
 *
 *               Possible
 *                  |
 *            +-----+---------------+
 *            |                     |
 *      +-----+-----+               |
 *      |           |               |
 *   Failed      Cancelled          |
 *                          +-------+------+
 *                          |              |
 *                      Recognized       Began
 *                                         |
 *                                      Changed
 *                                         |
 *                                  Ended/Recognized
 */
var STATE_POSSIBLE = 1;
var STATE_BEGAN = 2;
var STATE_CHANGED = 4;
var STATE_ENDED = 8;
var STATE_RECOGNIZED = STATE_ENDED;
var STATE_CANCELLED = 16;
var STATE_FAILED = 32;

/**
 * Recognizer
 * Every recognizer needs to extend from this class.
 * @constructor
 * @param {Object} options
 */
function Recognizer(options) {
    this.id = uniqueId();

    this.manager = null;
    this.options = merge(options || {}, this.defaults);

    // default is enable true
    this.options.enable = ifUndefined(this.options.enable, true);

    this.state = STATE_POSSIBLE;

    this.simultaneous = {};
    this.requireFail = [];
}

Recognizer.prototype = {
    /**
     * @virtual
     * @type {Object}
     */
    defaults: {},

    /**
     * set options
     * @param {Object} options
     * @return {Recognizer}
     */
    set: function(options) {
        extend(this.options, options);

        // also update the touchAction, in case something changed about the directions/enabled state
        this.manager && this.manager.touchAction.update();
        return this;
    },

    /**
     * recognize simultaneous with an other recognizer.
     * @param {Recognizer} otherRecognizer
     * @returns {Recognizer} this
     */
    recognizeWith: function(otherRecognizer) {
        if (invokeArrayArg(otherRecognizer, 'recognizeWith', this)) {
            return this;
        }

        var simultaneous = this.simultaneous;
        otherRecognizer = getRecognizerByNameIfManager(otherRecognizer, this);
        if (!simultaneous[otherRecognizer.id]) {
            simultaneous[otherRecognizer.id] = otherRecognizer;
            otherRecognizer.recognizeWith(this);
        }
        return this;
    },

    /**
     * drop the simultaneous link. it doesnt remove the link on the other recognizer.
     * @param {Recognizer} otherRecognizer
     * @returns {Recognizer} this
     */
    dropRecognizeWith: function(otherRecognizer) {
        if (invokeArrayArg(otherRecognizer, 'dropRecognizeWith', this)) {
            return this;
        }

        otherRecognizer = getRecognizerByNameIfManager(otherRecognizer, this);
        delete this.simultaneous[otherRecognizer.id];
        return this;
    },

    /**
     * recognizer can only run when an other is failing
     * @param {Recognizer} otherRecognizer
     * @returns {Recognizer} this
     */
    requireFailure: function(otherRecognizer) {
        if (invokeArrayArg(otherRecognizer, 'requireFailure', this)) {
            return this;
        }

        var requireFail = this.requireFail;
        otherRecognizer = getRecognizerByNameIfManager(otherRecognizer, this);
        if (inArray(requireFail, otherRecognizer) === -1) {
            requireFail.push(otherRecognizer);
            otherRecognizer.requireFailure(this);
        }
        return this;
    },

    /**
     * drop the requireFailure link. it does not remove the link on the other recognizer.
     * @param {Recognizer} otherRecognizer
     * @returns {Recognizer} this
     */
    dropRequireFailure: function(otherRecognizer) {
        if (invokeArrayArg(otherRecognizer, 'dropRequireFailure', this)) {
            return this;
        }

        otherRecognizer = getRecognizerByNameIfManager(otherRecognizer, this);
        var index = inArray(this.requireFail, otherRecognizer);
        if (index > -1) {
            this.requireFail.splice(index, 1);
        }
        return this;
    },

    /**
     * has require failures boolean
     * @returns {boolean}
     */
    hasRequireFailures: function() {
        return this.requireFail.length > 0;
    },

    /**
     * if the recognizer can recognize simultaneous with an other recognizer
     * @param {Recognizer} otherRecognizer
     * @returns {Boolean}
     */
    canRecognizeWith: function(otherRecognizer) {
        return !!this.simultaneous[otherRecognizer.id];
    },

    /**
     * You should use `tryEmit` instead of `emit` directly to check
     * that all the needed recognizers has failed before emitting.
     * @param {Object} input
     */
    emit: function(input) {
        var self = this;
        var state = this.state;

        function emit(withState) {
            self.manager.emit(self.options.event + (withState ? stateStr(state) : ''), input);
        }

        // 'panstart' and 'panmove'
        if (state < STATE_ENDED) {
            emit(true);
        }

        emit(); // simple 'eventName' events

        // panend and pancancel
        if (state >= STATE_ENDED) {
            emit(true);
        }
    },

    /**
     * Check that all the require failure recognizers has failed,
     * if true, it emits a gesture event,
     * otherwise, setup the state to FAILED.
     * @param {Object} input
     */
    tryEmit: function(input) {
        if (this.canEmit()) {
            return this.emit(input);
        }
        // it's failing anyway
        this.state = STATE_FAILED;
    },

    /**
     * can we emit?
     * @returns {boolean}
     */
    canEmit: function() {
        var i = 0;
        while (i < this.requireFail.length) {
            if (!(this.requireFail[i].state & (STATE_FAILED | STATE_POSSIBLE))) {
                return false;
            }
            i++;
        }
        return true;
    },

    /**
     * update the recognizer
     * @param {Object} inputData
     */
    recognize: function(inputData) {
        // make a new copy of the inputData
        // so we can change the inputData without messing up the other recognizers
        var inputDataClone = extend({}, inputData);

        // is is enabled and allow recognizing?
        if (!boolOrFn(this.options.enable, [this, inputDataClone])) {
            this.reset();
            this.state = STATE_FAILED;
            return;
        }

        // reset when we've reached the end
        if (this.state & (STATE_RECOGNIZED | STATE_CANCELLED | STATE_FAILED)) {
            this.state = STATE_POSSIBLE;
        }

        this.state = this.process(inputDataClone);

        // the recognizer has recognized a gesture
        // so trigger an event
        if (this.state & (STATE_BEGAN | STATE_CHANGED | STATE_ENDED | STATE_CANCELLED)) {
            this.tryEmit(inputDataClone);
        }
    },

    /**
     * return the state of the recognizer
     * the actual recognizing happens in this method
     * @virtual
     * @param {Object} inputData
     * @returns {Const} STATE
     */
    process: function(inputData) { }, // jshint ignore:line

    /**
     * return the preferred touch-action
     * @virtual
     * @returns {Array}
     */
    getTouchAction: function() { },

    /**
     * called when the gesture isn't allowed to recognize
     * like when another is being recognized or it is disabled
     * @virtual
     */
    reset: function() { }
};

/**
 * get a usable string, used as event postfix
 * @param {Const} state
 * @returns {String} state
 */
function stateStr(state) {
    if (state & STATE_CANCELLED) {
        return 'cancel';
    } else if (state & STATE_ENDED) {
        return 'end';
    } else if (state & STATE_CHANGED) {
        return 'move';
    } else if (state & STATE_BEGAN) {
        return 'start';
    }
    return '';
}

/**
 * direction cons to string
 * @param {Const} direction
 * @returns {String}
 */
function directionStr(direction) {
    if (direction == DIRECTION_DOWN) {
        return 'down';
    } else if (direction == DIRECTION_UP) {
        return 'up';
    } else if (direction == DIRECTION_LEFT) {
        return 'left';
    } else if (direction == DIRECTION_RIGHT) {
        return 'right';
    }
    return '';
}

/**
 * get a recognizer by name if it is bound to a manager
 * @param {Recognizer|String} otherRecognizer
 * @param {Recognizer} recognizer
 * @returns {Recognizer}
 */
function getRecognizerByNameIfManager(otherRecognizer, recognizer) {
    var manager = recognizer.manager;
    if (manager) {
        return manager.get(otherRecognizer);
    }
    return otherRecognizer;
}

/**
 * This recognizer is just used as a base for the simple attribute recognizers.
 * @constructor
 * @extends Recognizer
 */
function AttrRecognizer() {
    Recognizer.apply(this, arguments);
}

inherit(AttrRecognizer, Recognizer, {
    /**
     * @namespace
     * @memberof AttrRecognizer
     */
    defaults: {
        /**
         * @type {Number}
         * @default 1
         */
        pointers: 1
    },

    /**
     * Used to check if it the recognizer receives valid input, like input.distance > 10.
     * @memberof AttrRecognizer
     * @param {Object} input
     * @returns {Boolean} recognized
     */
    attrTest: function(input) {
        var optionPointers = this.options.pointers;
        return optionPointers === 0 || input.pointers.length === optionPointers;
    },

    /**
     * Process the input and return the state for the recognizer
     * @memberof AttrRecognizer
     * @param {Object} input
     * @returns {*} State
     */
    process: function(input) {
        var state = this.state;
        var eventType = input.eventType;

        var isRecognized = state & (STATE_BEGAN | STATE_CHANGED);
        var isValid = this.attrTest(input);

        // on cancel input and we've recognized before, return STATE_CANCELLED
        if (isRecognized && (eventType & INPUT_CANCEL || !isValid)) {
            return state | STATE_CANCELLED;
        } else if (isRecognized || isValid) {
            if (eventType & INPUT_END) {
                return state | STATE_ENDED;
            } else if (!(state & STATE_BEGAN)) {
                return STATE_BEGAN;
            }
            return state | STATE_CHANGED;
        }
        return STATE_FAILED;
    }
});

/**
 * Pan
 * Recognized when the pointer is down and moved in the allowed direction.
 * @constructor
 * @extends AttrRecognizer
 */
function PanRecognizer() {
    AttrRecognizer.apply(this, arguments);

    this.pX = null;
    this.pY = null;
}

inherit(PanRecognizer, AttrRecognizer, {
    /**
     * @namespace
     * @memberof PanRecognizer
     */
    defaults: {
        event: 'pan',
        threshold: 10,
        pointers: 1,
        direction: DIRECTION_ALL
    },

    getTouchAction: function() {
        var direction = this.options.direction;
        var actions = [];
        if (direction & DIRECTION_HORIZONTAL) {
            actions.push(TOUCH_ACTION_PAN_Y);
        }
        if (direction & DIRECTION_VERTICAL) {
            actions.push(TOUCH_ACTION_PAN_X);
        }
        return actions;
    },

    directionTest: function(input) {
        var options = this.options;
        var hasMoved = true;
        var distance = input.distance;
        var direction = input.direction;
        var x = input.deltaX;
        var y = input.deltaY;

        // lock to axis?
        if (!(direction & options.direction)) {
            if (options.direction & DIRECTION_HORIZONTAL) {
                direction = (x === 0) ? DIRECTION_NONE : (x < 0) ? DIRECTION_LEFT : DIRECTION_RIGHT;
                hasMoved = x != this.pX;
                distance = Math.abs(input.deltaX);
            } else {
                direction = (y === 0) ? DIRECTION_NONE : (y < 0) ? DIRECTION_UP : DIRECTION_DOWN;
                hasMoved = y != this.pY;
                distance = Math.abs(input.deltaY);
            }
        }
        input.direction = direction;
        return hasMoved && distance > options.threshold && direction & options.direction;
    },

    attrTest: function(input) {
        return AttrRecognizer.prototype.attrTest.call(this, input) &&
            (this.state & STATE_BEGAN || (!(this.state & STATE_BEGAN) && this.directionTest(input)));
    },

    emit: function(input) {
        this.pX = input.deltaX;
        this.pY = input.deltaY;

        var direction = directionStr(input.direction);
        if (direction) {
            this.manager.emit(this.options.event + direction, input);
        }

        this._super.emit.call(this, input);
    }
});

/**
 * Pinch
 * Recognized when two or more pointers are moving toward (zoom-in) or away from each other (zoom-out).
 * @constructor
 * @extends AttrRecognizer
 */
function PinchRecognizer() {
    AttrRecognizer.apply(this, arguments);
}

inherit(PinchRecognizer, AttrRecognizer, {
    /**
     * @namespace
     * @memberof PinchRecognizer
     */
    defaults: {
        event: 'pinch',
        threshold: 0,
        pointers: 2
    },

    getTouchAction: function() {
        return [TOUCH_ACTION_NONE];
    },

    attrTest: function(input) {
        return this._super.attrTest.call(this, input) &&
            (Math.abs(input.scale - 1) > this.options.threshold || this.state & STATE_BEGAN);
    },

    emit: function(input) {
        this._super.emit.call(this, input);
        if (input.scale !== 1) {
            var inOut = input.scale < 1 ? 'in' : 'out';
            this.manager.emit(this.options.event + inOut, input);
        }
    }
});

/**
 * Press
 * Recognized when the pointer is down for x ms without any movement.
 * @constructor
 * @extends Recognizer
 */
function PressRecognizer() {
    Recognizer.apply(this, arguments);

    this._timer = null;
    this._input = null;
}

inherit(PressRecognizer, Recognizer, {
    /**
     * @namespace
     * @memberof PressRecognizer
     */
    defaults: {
        event: 'press',
        pointers: 1,
        time: 500, // minimal time of the pointer to be pressed
        threshold: 5 // a minimal movement is ok, but keep it low
    },

    getTouchAction: function() {
        return [TOUCH_ACTION_AUTO];
    },

    process: function(input) {
        var options = this.options;
        var validPointers = input.pointers.length === options.pointers;
        var validMovement = input.distance < options.threshold;
        var validTime = input.deltaTime > options.time;

        this._input = input;

        // we only allow little movement
        // and we've reached an end event, so a tap is possible
        if (!validMovement || !validPointers || (input.eventType & (INPUT_END | INPUT_CANCEL) && !validTime)) {
            this.reset();
        } else if (input.eventType & INPUT_START) {
            this.reset();
            this._timer = setTimeoutContext(function() {
                this.state = STATE_RECOGNIZED;
                this.tryEmit();
            }, options.time, this);
        } else if (input.eventType & INPUT_END) {
            return STATE_RECOGNIZED;
        }
        return STATE_FAILED;
    },

    reset: function() {
        clearTimeout(this._timer);
    },

    emit: function(input) {
        if (this.state !== STATE_RECOGNIZED) {
            return;
        }

        if (input && (input.eventType & INPUT_END)) {
            this.manager.emit(this.options.event + 'up', input);
        } else {
            this._input.timeStamp = now();
            this.manager.emit(this.options.event, this._input);
        }
    }
});

/**
 * Rotate
 * Recognized when two or more pointer are moving in a circular motion.
 * @constructor
 * @extends AttrRecognizer
 */
function RotateRecognizer() {
    AttrRecognizer.apply(this, arguments);
}

inherit(RotateRecognizer, AttrRecognizer, {
    /**
     * @namespace
     * @memberof RotateRecognizer
     */
    defaults: {
        event: 'rotate',
        threshold: 0,
        pointers: 2
    },

    getTouchAction: function() {
        return [TOUCH_ACTION_NONE];
    },

    attrTest: function(input) {
        return this._super.attrTest.call(this, input) &&
            (Math.abs(input.rotation) > this.options.threshold || this.state & STATE_BEGAN);
    }
});

/**
 * Swipe
 * Recognized when the pointer is moving fast (velocity), with enough distance in the allowed direction.
 * @constructor
 * @extends AttrRecognizer
 */
function SwipeRecognizer() {
    AttrRecognizer.apply(this, arguments);
}

inherit(SwipeRecognizer, AttrRecognizer, {
    /**
     * @namespace
     * @memberof SwipeRecognizer
     */
    defaults: {
        event: 'swipe',
        threshold: 10,
        velocity: 0.65,
        direction: DIRECTION_HORIZONTAL | DIRECTION_VERTICAL,
        pointers: 1
    },

    getTouchAction: function() {
        return PanRecognizer.prototype.getTouchAction.call(this);
    },

    attrTest: function(input) {
        var direction = this.options.direction;
        var velocity;

        if (direction & (DIRECTION_HORIZONTAL | DIRECTION_VERTICAL)) {
            velocity = input.velocity;
        } else if (direction & DIRECTION_HORIZONTAL) {
            velocity = input.velocityX;
        } else if (direction & DIRECTION_VERTICAL) {
            velocity = input.velocityY;
        }

        return this._super.attrTest.call(this, input) &&
            direction & input.direction &&
            input.distance > this.options.threshold &&
            abs(velocity) > this.options.velocity && input.eventType & INPUT_END;
    },

    emit: function(input) {
        var direction = directionStr(input.direction);
        if (direction) {
            this.manager.emit(this.options.event + direction, input);
        }

        this.manager.emit(this.options.event, input);
    }
});

/**
 * A tap is ecognized when the pointer is doing a small tap/click. Multiple taps are recognized if they occur
 * between the given interval and position. The delay option can be used to recognize multi-taps without firing
 * a single tap.
 *
 * The eventData from the emitted event contains the property `tapCount`, which contains the amount of
 * multi-taps being recognized.
 * @constructor
 * @extends Recognizer
 */
function TapRecognizer() {
    Recognizer.apply(this, arguments);

    // previous time and center,
    // used for tap counting
    this.pTime = false;
    this.pCenter = false;

    this._timer = null;
    this._input = null;
    this.count = 0;
}

inherit(TapRecognizer, Recognizer, {
    /**
     * @namespace
     * @memberof PinchRecognizer
     */
    defaults: {
        event: 'tap',
        pointers: 1,
        taps: 1,
        interval: 300, // max time between the multi-tap taps
        time: 250, // max time of the pointer to be down (like finger on the screen)
        threshold: 2, // a minimal movement is ok, but keep it low
        posThreshold: 10 // a multi-tap can be a bit off the initial position
    },

    getTouchAction: function() {
        return [TOUCH_ACTION_MANIPULATION];
    },

    process: function(input) {
        var options = this.options;

        var validPointers = input.pointers.length === options.pointers;
        var validMovement = input.distance < options.threshold;
        var validTouchTime = input.deltaTime < options.time;

        this.reset();

        if ((input.eventType & INPUT_START) && (this.count === 0)) {
            return this.failTimeout();
        }

        // we only allow little movement
        // and we've reached an end event, so a tap is possible
        if (validMovement && validTouchTime && validPointers) {
            if (input.eventType != INPUT_END) {
                return this.failTimeout();
            }

            var validInterval = this.pTime ? (input.timeStamp - this.pTime < options.interval) : true;
            var validMultiTap = !this.pCenter || getDistance(this.pCenter, input.center) < options.posThreshold;

            this.pTime = input.timeStamp;
            this.pCenter = input.center;

            if (!validMultiTap || !validInterval) {
                this.count = 1;
            } else {
                this.count += 1;
            }

            this._input = input;

            // if tap count matches we have recognized it,
            // else it has began recognizing...
            var tapCount = this.count % options.taps;
            if (tapCount === 0) {
                // no failing requirements, immediately trigger the tap event
                // or wait as long as the multitap interval to trigger
                if (!this.hasRequireFailures()) {
                    return STATE_RECOGNIZED;
                } else {
                    this._timer = setTimeoutContext(function() {
                        this.state = STATE_RECOGNIZED;
                        this.tryEmit();
                    }, options.interval, this);
                    return STATE_BEGAN;
                }
            }
        }
        return STATE_FAILED;
    },

    failTimeout: function() {
        this._timer = setTimeoutContext(function() {
            this.state = STATE_FAILED;
        }, this.options.interval, this);
        return STATE_FAILED;
    },

    reset: function() {
        clearTimeout(this._timer);
    },

    emit: function() {
        if (this.state == STATE_RECOGNIZED ) {
            this._input.tapCount = this.count;
            this.manager.emit(this.options.event, this._input);
        }
    }
});

/**
 * Simple way to create an manager with a default set of recognizers.
 * @param {HTMLElement} element
 * @param {Object} [options]
 * @constructor
 */
function Hammer(element, options) {
    options = options || {};
    options.recognizers = ifUndefined(options.recognizers, Hammer.defaults.preset);
    return new Manager(element, options);
}

/**
 * @const {string}
 */
Hammer.VERSION = '2.0.4';

/**
 * default settings
 * @namespace
 */
Hammer.defaults = {
    /**
     * set if DOM events are being triggered.
     * But this is slower and unused by simple implementations, so disabled by default.
     * @type {Boolean}
     * @default false
     */
    domEvents: false,

    /**
     * The value for the touchAction property/fallback.
     * When set to `compute` it will magically set the correct value based on the added recognizers.
     * @type {String}
     * @default compute
     */
    touchAction: TOUCH_ACTION_COMPUTE,

    /**
     * @type {Boolean}
     * @default true
     */
    enable: true,

    /**
     * EXPERIMENTAL FEATURE -- can be removed/changed
     * Change the parent input target element.
     * If Null, then it is being set the to main element.
     * @type {Null|EventTarget}
     * @default null
     */
    inputTarget: null,

    /**
     * force an input class
     * @type {Null|Function}
     * @default null
     */
    inputClass: null,

    /**
     * Default recognizer setup when calling `Hammer()`
     * When creating a new Manager these will be skipped.
     * @type {Array}
     */
    preset: [
        // RecognizerClass, options, [recognizeWith, ...], [requireFailure, ...]
        [RotateRecognizer, { enable: false }],
        [PinchRecognizer, { enable: false }, ['rotate']],
        [SwipeRecognizer,{ direction: DIRECTION_HORIZONTAL }],
        [PanRecognizer, { direction: DIRECTION_HORIZONTAL }, ['swipe']],
        [TapRecognizer],
        [TapRecognizer, { event: 'doubletap', taps: 2 }, ['tap']],
        [PressRecognizer]
    ],

    /**
     * Some CSS properties can be used to improve the working of Hammer.
     * Add them to this method and they will be set when creating a new Manager.
     * @namespace
     */
    cssProps: {
        /**
         * Disables text selection to improve the dragging gesture. Mainly for desktop browsers.
         * @type {String}
         * @default 'none'
         */
        userSelect: 'none',

        /**
         * Disable the Windows Phone grippers when pressing an element.
         * @type {String}
         * @default 'none'
         */
        touchSelect: 'none',

        /**
         * Disables the default callout shown when you touch and hold a touch target.
         * On iOS, when you touch and hold a touch target such as a link, Safari displays
         * a callout containing information about the link. This property allows you to disable that callout.
         * @type {String}
         * @default 'none'
         */
        touchCallout: 'none',

        /**
         * Specifies whether zooming is enabled. Used by IE10>
         * @type {String}
         * @default 'none'
         */
        contentZooming: 'none',

        /**
         * Specifies that an entire element should be draggable instead of its contents. Mainly for desktop browsers.
         * @type {String}
         * @default 'none'
         */
        userDrag: 'none',

        /**
         * Overrides the highlight color shown when the user taps a link or a JavaScript
         * clickable element in iOS. This property obeys the alpha value, if specified.
         * @type {String}
         * @default 'rgba(0,0,0,0)'
         */
        tapHighlightColor: 'rgba(0,0,0,0)'
    }
};

var STOP = 1;
var FORCED_STOP = 2;

/**
 * Manager
 * @param {HTMLElement} element
 * @param {Object} [options]
 * @constructor
 */
function Manager(element, options) {
    options = options || {};

    this.options = merge(options, Hammer.defaults);
    this.options.inputTarget = this.options.inputTarget || element;

    this.handlers = {};
    this.session = {};
    this.recognizers = [];

    this.element = element;
    this.input = createInputInstance(this);
    this.touchAction = new TouchAction(this, this.options.touchAction);

    toggleCssProps(this, true);

    each(options.recognizers, function(item) {
        var recognizer = this.add(new (item[0])(item[1]));
        item[2] && recognizer.recognizeWith(item[2]);
        item[3] && recognizer.requireFailure(item[3]);
    }, this);
}

Manager.prototype = {
    /**
     * set options
     * @param {Object} options
     * @returns {Manager}
     */
    set: function(options) {
        extend(this.options, options);

        // Options that need a little more setup
        if (options.touchAction) {
            this.touchAction.update();
        }
        if (options.inputTarget) {
            // Clean up existing event listeners and reinitialize
            this.input.destroy();
            this.input.target = options.inputTarget;
            this.input.init();
        }
        return this;
    },

    /**
     * stop recognizing for this session.
     * This session will be discarded, when a new [input]start event is fired.
     * When forced, the recognizer cycle is stopped immediately.
     * @param {Boolean} [force]
     */
    stop: function(force) {
        this.session.stopped = force ? FORCED_STOP : STOP;
    },

    /**
     * run the recognizers!
     * called by the inputHandler function on every movement of the pointers (touches)
     * it walks through all the recognizers and tries to detect the gesture that is being made
     * @param {Object} inputData
     */
    recognize: function(inputData) {
        var session = this.session;
        if (session.stopped) {
            return;
        }

        // run the touch-action polyfill
        this.touchAction.preventDefaults(inputData);

        var recognizer;
        var recognizers = this.recognizers;

        // this holds the recognizer that is being recognized.
        // so the recognizer's state needs to be BEGAN, CHANGED, ENDED or RECOGNIZED
        // if no recognizer is detecting a thing, it is set to `null`
        var curRecognizer = session.curRecognizer;

        // reset when the last recognizer is recognized
        // or when we're in a new session
        if (!curRecognizer || (curRecognizer && curRecognizer.state & STATE_RECOGNIZED)) {
            curRecognizer = session.curRecognizer = null;
        }

        var i = 0;
        while (i < recognizers.length) {
            recognizer = recognizers[i];

            // find out if we are allowed try to recognize the input for this one.
            // 1.   allow if the session is NOT forced stopped (see the .stop() method)
            // 2.   allow if we still haven't recognized a gesture in this session, or the this recognizer is the one
            //      that is being recognized.
            // 3.   allow if the recognizer is allowed to run simultaneous with the current recognized recognizer.
            //      this can be setup with the `recognizeWith()` method on the recognizer.
            if (session.stopped !== FORCED_STOP && ( // 1
                    !curRecognizer || recognizer == curRecognizer || // 2
                    recognizer.canRecognizeWith(curRecognizer))) { // 3
                recognizer.recognize(inputData);
            } else {
                recognizer.reset();
            }

            // if the recognizer has been recognizing the input as a valid gesture, we want to store this one as the
            // current active recognizer. but only if we don't already have an active recognizer
            if (!curRecognizer && recognizer.state & (STATE_BEGAN | STATE_CHANGED | STATE_ENDED)) {
                curRecognizer = session.curRecognizer = recognizer;
            }
            i++;
        }
    },

    /**
     * get a recognizer by its event name.
     * @param {Recognizer|String} recognizer
     * @returns {Recognizer|Null}
     */
    get: function(recognizer) {
        if (recognizer instanceof Recognizer) {
            return recognizer;
        }

        var recognizers = this.recognizers;
        for (var i = 0; i < recognizers.length; i++) {
            if (recognizers[i].options.event == recognizer) {
                return recognizers[i];
            }
        }
        return null;
    },

    /**
     * add a recognizer to the manager
     * existing recognizers with the same event name will be removed
     * @param {Recognizer} recognizer
     * @returns {Recognizer|Manager}
     */
    add: function(recognizer) {
        if (invokeArrayArg(recognizer, 'add', this)) {
            return this;
        }

        // remove existing
        var existing = this.get(recognizer.options.event);
        if (existing) {
            this.remove(existing);
        }

        this.recognizers.push(recognizer);
        recognizer.manager = this;

        this.touchAction.update();
        return recognizer;
    },

    /**
     * remove a recognizer by name or instance
     * @param {Recognizer|String} recognizer
     * @returns {Manager}
     */
    remove: function(recognizer) {
        if (invokeArrayArg(recognizer, 'remove', this)) {
            return this;
        }

        var recognizers = this.recognizers;
        recognizer = this.get(recognizer);
        recognizers.splice(inArray(recognizers, recognizer), 1);

        this.touchAction.update();
        return this;
    },

    /**
     * bind event
     * @param {String} events
     * @param {Function} handler
     * @returns {EventEmitter} this
     */
    on: function(events, handler) {
        var handlers = this.handlers;
        each(splitStr(events), function(event) {
            handlers[event] = handlers[event] || [];
            handlers[event].push(handler);
        });
        return this;
    },

    /**
     * unbind event, leave emit blank to remove all handlers
     * @param {String} events
     * @param {Function} [handler]
     * @returns {EventEmitter} this
     */
    off: function(events, handler) {
        var handlers = this.handlers;
        each(splitStr(events), function(event) {
            if (!handler) {
                delete handlers[event];
            } else {
                handlers[event].splice(inArray(handlers[event], handler), 1);
            }
        });
        return this;
    },

    /**
     * emit event to the listeners
     * @param {String} event
     * @param {Object} data
     */
    emit: function(event, data) {
        // we also want to trigger dom events
        if (this.options.domEvents) {
            triggerDomEvent(event, data);
        }

        // no handlers, so skip it all
        var handlers = this.handlers[event] && this.handlers[event].slice();
        if (!handlers || !handlers.length) {
            return;
        }

        data.type = event;
        data.preventDefault = function() {
            data.srcEvent.preventDefault();
        };

        var i = 0;
        while (i < handlers.length) {
            handlers[i](data);
            i++;
        }
    },

    /**
     * destroy the manager and unbinds all events
     * it doesn't unbind dom events, that is the user own responsibility
     */
    destroy: function() {
        this.element && toggleCssProps(this, false);

        this.handlers = {};
        this.session = {};
        this.input.destroy();
        this.element = null;
    }
};

/**
 * add/remove the css properties as defined in manager.options.cssProps
 * @param {Manager} manager
 * @param {Boolean} add
 */
function toggleCssProps(manager, add) {
    var element = manager.element;
    each(manager.options.cssProps, function(value, name) {
        element.style[prefixed(element.style, name)] = add ? value : '';
    });
}

/**
 * trigger dom event
 * @param {String} event
 * @param {Object} data
 */
function triggerDomEvent(event, data) {
    var gestureEvent = document.createEvent('Event');
    gestureEvent.initEvent(event, true, true);
    gestureEvent.gesture = data;
    data.target.dispatchEvent(gestureEvent);
}

extend(Hammer, {
    INPUT_START: INPUT_START,
    INPUT_MOVE: INPUT_MOVE,
    INPUT_END: INPUT_END,
    INPUT_CANCEL: INPUT_CANCEL,

    STATE_POSSIBLE: STATE_POSSIBLE,
    STATE_BEGAN: STATE_BEGAN,
    STATE_CHANGED: STATE_CHANGED,
    STATE_ENDED: STATE_ENDED,
    STATE_RECOGNIZED: STATE_RECOGNIZED,
    STATE_CANCELLED: STATE_CANCELLED,
    STATE_FAILED: STATE_FAILED,

    DIRECTION_NONE: DIRECTION_NONE,
    DIRECTION_LEFT: DIRECTION_LEFT,
    DIRECTION_RIGHT: DIRECTION_RIGHT,
    DIRECTION_UP: DIRECTION_UP,
    DIRECTION_DOWN: DIRECTION_DOWN,
    DIRECTION_HORIZONTAL: DIRECTION_HORIZONTAL,
    DIRECTION_VERTICAL: DIRECTION_VERTICAL,
    DIRECTION_ALL: DIRECTION_ALL,

    Manager: Manager,
    Input: Input,
    TouchAction: TouchAction,

    TouchInput: TouchInput,
    MouseInput: MouseInput,
    PointerEventInput: PointerEventInput,
    TouchMouseInput: TouchMouseInput,
    SingleTouchInput: SingleTouchInput,

    Recognizer: Recognizer,
    AttrRecognizer: AttrRecognizer,
    Tap: TapRecognizer,
    Pan: PanRecognizer,
    Swipe: SwipeRecognizer,
    Pinch: PinchRecognizer,
    Rotate: RotateRecognizer,
    Press: PressRecognizer,

    on: addEventListeners,
    off: removeEventListeners,
    each: each,
    merge: merge,
    extend: extend,
    inherit: inherit,
    bindFn: bindFn,
    prefixed: prefixed
});

if (typeof define == TYPE_FUNCTION && define.amd) {
    define(function() {
        return Hammer;
    });
} else if (typeof module != 'undefined' && module.exports) {
    module.exports = Hammer;
} else {
    window[exportName] = Hammer;
}

})(window, document, 'Hammer');

},{}],36:[function(require,module,exports){
"use strict";
/**
 * Created by Alex on 11/6/2014.
 */

// https://github.com/umdjs/umd/blob/master/returnExports.js#L40-L60
// if the module has no dependencies, the above pattern can be simplified to
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define([], factory);
  } else if (typeof exports === 'object') {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory();
  } else {
    // Browser globals (root is window)
    root.keycharm = factory();
  }
}(this, function () {

  function keycharm(options) {
    var preventDefault = options && options.preventDefault || false;

    var container = options && options.container || window;

    var _exportFunctions = {};
    var _bound = {keydown:{}, keyup:{}};
    var _keys = {};
    var i;

    // a - z
    for (i = 97; i <= 122; i++) {_keys[String.fromCharCode(i)] = {code:65 + (i - 97), shift: false};}
    // A - Z
    for (i = 65; i <= 90; i++) {_keys[String.fromCharCode(i)] = {code:i, shift: true};}
    // 0 - 9
    for (i = 0;  i <= 9;   i++) {_keys['' + i] = {code:48 + i, shift: false};}
    // F1 - F12
    for (i = 1;  i <= 12;   i++) {_keys['F' + i] = {code:111 + i, shift: false};}
    // num0 - num9
    for (i = 0;  i <= 9;   i++) {_keys['num' + i] = {code:96 + i, shift: false};}

    // numpad misc
    _keys['num*'] = {code:106, shift: false};
    _keys['num+'] = {code:107, shift: false};
    _keys['num-'] = {code:109, shift: false};
    _keys['num/'] = {code:111, shift: false};
    _keys['num.'] = {code:110, shift: false};
    // arrows
    _keys['left']  = {code:37, shift: false};
    _keys['up']    = {code:38, shift: false};
    _keys['right'] = {code:39, shift: false};
    _keys['down']  = {code:40, shift: false};
    // extra keys
    _keys['space'] = {code:32, shift: false};
    _keys['enter'] = {code:13, shift: false};
    _keys['shift'] = {code:16, shift: undefined};
    _keys['esc']   = {code:27, shift: false};
    _keys['backspace'] = {code:8, shift: false};
    _keys['tab']       = {code:9, shift: false};
    _keys['ctrl']      = {code:17, shift: false};
    _keys['alt']       = {code:18, shift: false};
    _keys['delete']    = {code:46, shift: false};
    _keys['pageup']    = {code:33, shift: false};
    _keys['pagedown']  = {code:34, shift: false};
    // symbols
    _keys['=']     = {code:187, shift: false};
    _keys['-']     = {code:189, shift: false};
    _keys[']']     = {code:221, shift: false};
    _keys['[']     = {code:219, shift: false};



    var down = function(event) {handleEvent(event,'keydown');};
    var up = function(event) {handleEvent(event,'keyup');};

    // handle the actualy bound key with the event
    var handleEvent = function(event,type) {
      if (_bound[type][event.keyCode] !== undefined) {
        var bound = _bound[type][event.keyCode];
        for (var i = 0; i < bound.length; i++) {
          if (bound[i].shift === undefined) {
            bound[i].fn(event);
          }
          else if (bound[i].shift == true && event.shiftKey == true) {
            bound[i].fn(event);
          }
          else if (bound[i].shift == false && event.shiftKey == false) {
            bound[i].fn(event);
          }
        }

        if (preventDefault == true) {
          event.preventDefault();
        }
      }
    };

    // bind a key to a callback
    _exportFunctions.bind = function(key, callback, type) {
      if (type === undefined) {
        type = 'keydown';
      }
      if (_keys[key] === undefined) {
        throw new Error("unsupported key: " + key);
      }
      if (_bound[type][_keys[key].code] === undefined) {
        _bound[type][_keys[key].code] = [];
      }
      _bound[type][_keys[key].code].push({fn:callback, shift:_keys[key].shift});
    };


    // bind all keys to a call back (demo purposes)
    _exportFunctions.bindAll = function(callback, type) {
      if (type === undefined) {
        type = 'keydown';
      }
      for (var key in _keys) {
        if (_keys.hasOwnProperty(key)) {
          _exportFunctions.bind(key,callback,type);
        }
      }
    };

    // get the key label from an event
    _exportFunctions.getKey = function(event) {
      for (var key in _keys) {
        if (_keys.hasOwnProperty(key)) {
          if (event.shiftKey == true && _keys[key].shift == true && event.keyCode == _keys[key].code) {
            return key;
          }
          else if (event.shiftKey == false && _keys[key].shift == false && event.keyCode == _keys[key].code) {
            return key;
          }
          else if (event.keyCode == _keys[key].code && key == 'shift') {
            return key;
          }
        }
      }
      return "unknown key, currently not supported";
    };

    // unbind either a specific callback from a key or all of them (by leaving callback undefined)
    _exportFunctions.unbind = function(key, callback, type) {
      if (type === undefined) {
        type = 'keydown';
      }
      if (_keys[key] === undefined) {
        throw new Error("unsupported key: " + key);
      }
      if (callback !== undefined) {
        var newBindings = [];
        var bound = _bound[type][_keys[key].code];
        if (bound !== undefined) {
          for (var i = 0; i < bound.length; i++) {
            if (!(bound[i].fn == callback && bound[i].shift == _keys[key].shift)) {
              newBindings.push(_bound[type][_keys[key].code][i]);
            }
          }
        }
        _bound[type][_keys[key].code] = newBindings;
      }
      else {
        _bound[type][_keys[key].code] = [];
      }
    };

    // reset all bound variables.
    _exportFunctions.reset = function() {
      _bound = {keydown:{}, keyup:{}};
    };

    // unbind all listeners and reset all variables.
    _exportFunctions.destroy = function() {
      _bound = {keydown:{}, keyup:{}};
      container.removeEventListener('keydown', down, true);
      container.removeEventListener('keyup', up, true);
    };

    // create listeners.
    container.addEventListener('keydown',down,true);
    container.addEventListener('keyup',up,true);

    // return the public functions.
    return _exportFunctions;
  }

  return keycharm;
}));



},{}],37:[function(require,module,exports){
'use strict';

(function (factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define([], factory);
  } else if (typeof exports === 'object') {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory();
  } else {
    // Browser globals (root is window)
    window.propagating = factory();
  }
}(function () {
  var _firstTarget = null; // singleton, will contain the target element where the touch event started
  var _processing = false; // singleton, true when a touch event is being handled

  /**
   * Extend an Hammer.js instance with event propagation.
   *
   * Features:
   * - Events emitted by hammer will propagate in order from child to parent
   *   elements.
   * - Events are extended with a function `event.stopPropagation()` to stop
   *   propagation to parent elements.
   * - An option `preventDefault` to stop all default browser behavior.
   *
   * Usage:
   *   var hammer = propagatingHammer(new Hammer(element));
   *   var hammer = propagatingHammer(new Hammer(element), {preventDefault: true});
   *
   * @param {Hammer.Manager} hammer   An hammer instance.
   * @param {Object} [options]        Available options:
   *                                  - `preventDefault: true | 'mouse' | 'touch' | 'pen'`.
   *                                    Enforce preventing the default browser behavior.
   *                                    Cannot be set to `false`.
   * @return {Hammer.Manager} Returns the same hammer instance with extended
   *                          functionality
   */
  return function propagating(hammer, options) {
    var _options = options || {
      preventDefault: false
    };

    if (hammer.Manager) {
      // This looks like the Hammer constructor.
      // Overload the constructors with our own.
      var Hammer = hammer;

      var PropagatingHammer = function(element, options) {
        var o = Object.create(_options);
        if (options) Hammer.extend(o, options);
        return propagating(new Hammer(element, o), o);
      };
      Hammer.extend(PropagatingHammer, Hammer);

      PropagatingHammer.Manager = function (element, options) {
        var o = Object.create(_options);
        if (options) Hammer.extend(o, options);
        return propagating(new Hammer.Manager(element, o), o);
      };

      return PropagatingHammer;
    }

    // create a wrapper object which will override the functions
    // `on`, `off`, `destroy`, and `emit` of the hammer instance
    var wrapper = Object.create(hammer);

    // attach to DOM element
    var element = hammer.element;
    element.hammer = wrapper;

    // register an event to catch the start of a gesture and store the
    // target in a singleton
    hammer.on('hammer.input', function (event) {
      if (_options.preventDefault === true || (_options.preventDefault === event.pointerType)) {
        event.preventDefault();
      }
      if (event.isFirst) {
        _firstTarget = event.target;
      }
    });

    /** @type {Object.<String, Array.<function>>} */
    wrapper._handlers = {};

    /**
     * Register a handler for one or multiple events
     * @param {String} events    A space separated string with events
     * @param {function} handler A callback function, called as handler(event)
     * @returns {Hammer.Manager} Returns the hammer instance
     */
    wrapper.on = function (events, handler) {
      // register the handler
      split(events).forEach(function (event) {
        var _handlers = wrapper._handlers[event];
        if (!_handlers) {
          wrapper._handlers[event] = _handlers = [];

          // register the static, propagated handler
          hammer.on(event, propagatedHandler);
        }
        _handlers.push(handler);
      });

      return wrapper;
    };

    /**
     * Unregister a handler for one or multiple events
     * @param {String} events      A space separated string with events
     * @param {function} [handler] Optional. The registered handler. If not
     *                             provided, all handlers for given events
     *                             are removed.
     * @returns {Hammer.Manager}   Returns the hammer instance
     */
    wrapper.off = function (events, handler) {
      // unregister the handler
      split(events).forEach(function (event) {
        var _handlers = wrapper._handlers[event];
        if (_handlers) {
          _handlers = handler ? _handlers.filter(function (h) {
            return h !== handler;
          }) : [];

          if (_handlers.length > 0) {
            wrapper._handlers[event] = _handlers;
          }
          else {
            // remove static, propagated handler
            hammer.off(event, propagatedHandler);
            delete wrapper._handlers[event];
          }
        }
      });

      return wrapper;
    };

    /**
     * Emit to the event listeners
     * @param {string} eventType
     * @param {Event} event
     */
    wrapper.emit = function(eventType, event) {
      _firstTarget = event.target;
      hammer.emit(eventType, event);
    };

    wrapper.destroy = function () {
      // Detach from DOM element
      delete hammer.element.hammer;

      // clear all handlers
      wrapper._handlers = {};

      // call original hammer destroy
      hammer.destroy();
    };

    // split a string with space separated words
    function split(events) {
      return events.match(/[^ ]+/g);
    }

    /**
     * A static event handler, applying event propagation.
     * @param {Object} event
     */
    function propagatedHandler(event) {
      // let only a single hammer instance handle this event
      if (event.type !== 'hammer.input') {
        // it is possible that the same srcEvent is used with multiple hammer events,
        // we keep track on which events are handled in an object _handled
        if (!event.srcEvent._handled) {
          event.srcEvent._handled = {};
        }

        if (event.srcEvent._handled[event.type]) {
          return;
        }
        else {
          event.srcEvent._handled[event.type] = true;
        }
      }

      // attach a stopPropagation function to the event
      var stopped = false;
      event.stopPropagation = function () {
        stopped = true;
      };

      // attach firstTarget property to the event
      event.firstTarget = _firstTarget;

      // propagate over all elements (until stopped)
      var elem = _firstTarget;
      while (elem && !stopped) {
        var _handlers = elem.hammer && elem.hammer._handlers[event.type];
        if (_handlers) {
          for (var i = 0; i < _handlers.length && !stopped; i++) {
            _handlers[i](event);
          }
        }

        elem = elem.parentNode;
      }
    }

    return wrapper;
  };
}));

},{}]},{},[1])(1)
});