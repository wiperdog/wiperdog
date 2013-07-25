/**
 * JavaScriptのhash, array を JavaのHashtableとArrayListに変換する
 */

function newObject(def) {
	if (def instanceof Array) {
		return newArrayList(def);
	} else if (def instanceof Object) {
		return newHashtable(def);
	} else {
		return def;
	}
}

/**
 * newHashtable
 * javaの java.util.Hashtableを作成する。
 * 引数には、JavaScriptのhashを渡す。
 */
function newHashtable(def) {
	var hashTable = new java.util.Hashtable();
	for (key in def) {
		hashTable.put(key, newObject(def[key]));
	}

	return hashTable;
}


function newArrayList(def) {
	var arrayList = new java.util.ArrayList();
	for (v in def) {
		arrayList.add(newObject(def[v]));
	}
	return arrayList;
}

/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////

/**
 * JavaScriptのhash, array を JavaのMapとArrayListに変換する
 */

function newObject2(def) {
	if (def instanceof Array) {
		return newArrayList2(def);
	} else if (def instanceof Object) {
		return newHashMap(def);
	} else {
		return def;
	}
}

/**
 * newHashtable
 * javaの java.util.Hashtableを作成する。
 * 引数には、JavaScriptのhashを渡す。
 */
function newHashMap(def) {
	var hashTable = new java.util.HashMap();
	for (key in def) {
		hashTable.put(key, newObject2(def[key]));
	}

	return hashTable;
}


function newArrayList2(def) {
	var arrayList = new java.util.ArrayList();
	for (v in def) {
		arrayList.add(newObject2(def[v]));
	}
	return arrayList;
}

/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////

function newJavaArraySize(type, size) {
	var jarr = java.lang.reflect.Array.newInstance(type, size);

	return jarr;
}

function newJavaArray(type) {
	var jarr = null;
	if (arguments.length > 2) {
		jarr = java.lang.reflect.Array.newInstance(type, arguments.length - 1);
		for (var i = 1;i < arguments.length;++i) {
			jarr[i - 1] = arguments[i];
		}
	}

	return jarr;
}

/**
 * enumJavaObject(jobj, userdata, func)
 *
 *
 * 
 * func(ctype, vtype, userdata, key, value)
 *    ctype: container type
 *    vtype: value's type
 *    userdata: user supplied data
 *    key: key of value in container
 *    value: value
 */
function enumJavaObject(jobj, userdata, func) {
	if (jobj instanceof java.util.Map) {
		var entries = jobj.entrySet();
		var it = entries.iterator();
		while (it.hasNext()) {
			var e = it.next();
			var value = e.getValue();
			var type = (value instanceof java.util.Map ? 1 : (value instanceof java.util.List ? 2 : 0));
			func(1, type, userdata, e.getKey(), value);
		}
	} else if (jobj instanceof java.util.List) {
		var count = jobj.size();
		for (var i = 0;i < count;++i) {
			var value = jobj.get(i);
			var type = (value instanceof java.util.Map ? 1 : (value instanceof java.util.List ? 2 : 0));
			func(2, type, userdata, i, value);
		}
	} else {
		var type = (jobj instanceof java.util.Map ? 1 : (jobj instanceof java.util.List ? 2 : 0));
		func(0, type, userdata, 0, jobj);
	}
}


function enumJavaObject2(jobj, userobj) {

	function callChild(parent, clsobj, key, value) {
		if (value instanceof java.util.Map) {
			return clsobj.map(parent, key, value, enumMap);
		} else if (value instanceof java.util.List) {
			return clsobj.list(parent, key, value, enumList);
		} else {
			return clsobj.obj(parent, key, value);
		}
	}

	function enumMap(jobj, clsobj) {
		var entries = jobj.entrySet();
		var it = entries.iterator();
		var parent = {};
		while (it.hasNext()) {
			var e = it.next();
			var key = e.getKey();
			var value = e.getValue();
			parent[key] = callChild(parent, clsobj, key, value);
		}
		return parent;
	}

	function enumList(jobj, clsobj) {
		var count = jobj.size();
		var parent = new Array();
		for (var i = 0;i < count;++i) {
			var value = jobj.get(i);
			parent[i] = callChild(parent, userobj, i, value);
		}
		return parent;
	}

	return callChild(null, userobj, null, jobj);

}

function enumJavaObject3(root, userobj) {

	return callChild(null, userobj, null, root);

	var enumMap = {
		isList: false,
		isMap: true,
		enumerate: function(jobj, clsobj) {
			var entries = jobj.entrySet();
			var it = entries.iterator();
			var parent = {};
			while (it.hasNext()) {
				var e = it.next();
				var key = e.getKey();
				var value = e.getValue();
				parent[key] = callChild(parent, clsobj, key, value);
			}
			return parent;
		}
	};

	var enumList = {
		isList: true,
		isMap: false,
		enumerate: function(jobj, clsobj) {
			var count = jobj.size();
			var parent = new Array();
			for (var i = 0;i < count;++i) {
				var value = jobj.get(i);
				parent[i] = callChild(parent, clsobj, i, value);
			}
			return parent;
		}
	};

	var enumObj = {
		isList: false,
		isMap: false,
		enumerate: function (jobj, clsobj) {
			return jobj;
		}
	};

	function callChild(parent, clsobj, key, value) {
		if (value instanceof java.util.Map) {
			return clsobj.callback(parent, key, value, enumMap);
		} else if (value instanceof java.util.List) {
			return clsobj.callback(parent, key, value, enumList);
		} else {
			return clsobj.callback(parent, key, value, enumObj);
		}
	}

}

function toJavaScriptObject(jobj) {
	if (jobj instanceof java.util.Map) {
		var entries = jobj.entrySet();
		var it = entries.iterator();
		var parent = {};
		while (it.hasNext()) {
			var e = it.next();
			var key = e.getKey();
			var value = e.getValue();
			parent[key] = toJavaScriptObject(value);
		}
		return parent;
	} else if (jobj instanceof java.util.List) {
		var count = jobj.size();
		var parent = new Array();
		for (var i = 0;i < count;++i) {
			var value = jobj.get(i);
			parent[i] = toJavaScriptObject(value);
		}
		return parent;
	} else {
		return jobj;
	}
}
