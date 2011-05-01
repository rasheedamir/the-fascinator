/* 
 * Based on http://wiki.apache.org/jakarta-velocity/JythonUberspect
 * 
 * ==========
 * 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * ==========
 * 
 * Copyright 2000-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.velocity.tools.generic.introspection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.apache.velocity.util.StringUtils;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.apache.velocity.util.introspection.VelPropertySet;
import org.python.core.PyArray;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyJavaClass;
import org.python.core.PyJavaInnerClass;
import org.python.core.PyJavaInstance;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PySingleton;
import org.python.core.PyString;

/**
 * a subclass of UberspectImpl that supports dynamic Jython objects. Methods and
 * attributes can therefore be called on a jython object within a velocity
 * template without requiring additional tools (and without resorting to direct
 * method calls against the jython methods (i.e. no __findattr__ in your
 * template)
 * 
 * OL: added Map iterator support in #foreach and javabean style properties
 * 
 * @author <a href="mailto:jasonrbriggs@gmail.com">Jason R Briggs</a>
 */
public class JythonUberspect extends UberspectImpl {
    /**
     * support the standard jython iterators (otherwise pass up to
     * UberspectImpl) in a velocity #foreach
     */
    @Override
    @SuppressWarnings("unchecked")
    public Iterator getIterator(Object obj, Info i) throws Exception {
        log.debug("getIterator: " + obj.getClass().getName());
        if (obj instanceof PySequence) {
            return new PySequenceIterator((PySequence) obj);
        } else if (obj instanceof PyDictionary) {
            return new PySequenceIterator(((PyDictionary) obj).items());
        } else if (obj instanceof PySingleton) {
            PySingleton ps = (PySingleton) obj;
            log.debug(ps);
            return super.getIterator(obj, i);
        } else if (obj instanceof PyJavaInstance) {
            PySequence seq = null;
            PyJavaInstance pji = (PyJavaInstance) obj;
            if ("java.util.HashMap.Values".equals(pji.instclass.__name__)) {
                PyJavaInnerClass pjic = (PyJavaInnerClass) pji.instclass;
                PyJavaClass parent = pjic.parent;
                log.debug("parent=" + parent);
                Object casted = parent.__tojava__(Map.class);
                log.debug("casted=" + ((PySingleton) casted).safeRepr());
                seq = new PyArray(Collection.class, new String[] {});// map.values().toArray());
            } else if ("java.util.HashMap".equals(pji.instclass.__name__)) {
                Map map = (Map) ((PyObject) obj).__tojava__(Map.class);
                seq = new PyArray(Collection.class, map.entrySet().toArray());
            } else if ("java.util.ArrayList".equals(pji.instclass.__name__)) {
                List list = (List) ((PyObject) obj).__tojava__(List.class);
                seq = new PyArray(List.class, list.toArray());
            }
            log.debug(pji.instclass.__name__);
            return new PySequenceIterator(seq);
        } else {
            return super.getIterator(obj, i);
        }
    }

    /**
     * get the method for a jython object (or pass up)
     */
    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args,
            Info i) throws Exception {
        if (obj instanceof PyObject) {
            return new PyMethod(methodName, i);
        } else {
            return super.getMethod(obj, methodName, args, i);
        }
    }

    /**
     * get a property from a jython object for data retrieval
     */
    @Override
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i)
            throws Exception {
        if (obj instanceof PyObject) {
            return new PyGetProperty(identifier, i);
        } else {
            return super.getPropertyGet(obj, identifier, i);
        }
    }

    /**
     * get a property from a jython object for data modification
     */
    @Override
    public VelPropertySet getPropertySet(Object obj, String identifier,
            Object arg, Info info) throws Exception {
        if (obj instanceof PyObject) {
            return new PySetProperty(identifier);
        } else {
            return super.getPropertySet(obj, identifier, arg, info);
        }
    }

}

/**
 * a jython velocity method
 */
class PyMethod implements VelMethod {
    private static final Logger log = Logger.getLogger(JythonUberspect.class
            .getName());
    final PyString methodname;
    final Info info;

    public PyMethod(String methodname, Info info) {
        this.methodname = new PyString(methodname);
        this.info = info;
    }

    /**
     * returns the jython method name
     */
    public String getMethodName() {
        return methodname.toString();
    }

    /**
     * the return type of the invoked method. Just being lazy here and returning
     * 'Object' for everything at the moment
     */
    @SuppressWarnings("unchecked")
    public Class getReturnType() {
        log.debug("getReturnType(" + methodname + ")");
        return Object.class;
    }

    /**
     * execute the jython method
     */
    public Object invoke(Object o, Object[] params) {
        PyObject po = (PyObject) o;
        PyObject rtn = null;
        try {
            // find the method attr on the python object
            PyObject meth = po.__findattr__(methodname);
            if (params == null || params.length < 1) {
                rtn = meth.__call__();
            } else {
                // build a python params array
                PyObject[] pparams = new PyObject[params.length];
                for (int i = 0; i < pparams.length; i++) {
                    if (params[i] instanceof String) {
                        pparams[i] = new PyString((String) params[i]);
                    } else if (params[i] instanceof PyObject) {
                        pparams[i] = (PyObject) params[i];
                    } else if (params[i] instanceof Integer) {
                        pparams[i] = new PyInteger(((Integer) params[i])
                                .intValue());
                    } else {
                        log.debug("unsupported param type : "
                                + params[i].getClass().getName());
                        return null;
                    }
                }

                rtn = meth.__call__(pparams);
                if (rtn instanceof PyNone) {
                    rtn = null;
                }
            }
            log.debug(methodname + ": rtn=" + rtn.getClass() + "=" + rtn);
            if (rtn instanceof PyInteger) {
                return rtn.asInt(0);
            }
            return rtn;
        } catch (Exception e) {
            log.debug("PyMethod.invoke: " + methodname + ", "
                    + params.toString() + ", " + info);
        }

        return null;
    }

    /**
     * is this method cacheable
     */
    public boolean isCacheable() {
        return true;
    }
}

/**
 * a jython velocity GET property
 */
class PyGetProperty implements VelPropertyGet {
    private static final Logger log = Logger.getLogger(JythonUberspect.class
            .getName());
    private PyString propname;
    private Info info;

    public PyGetProperty(String propname, Info info) {
        this.propname = new PyString(propname);
        this.info = info;
    }

    /**
     * the name of the jython property/attribute
     */
    public String getMethodName() {
        return propname.toString();
    }

    /**
     * returns the property value
     */
    public Object invoke(java.lang.Object o) {
        Object rtn = invoke(o, propname.toString());
        if (rtn == null) {
            PyMethod pm = new PyMethod("get"
                    + StringUtils.capitalizeFirstLetter(propname.toString()),
                    info);
            rtn = pm.invoke(o, new Object[] {});
        }
        return rtn;
    }

    private Object invoke(Object o, String attrName) {
        PyObject po = (PyObject) o;

        try {
            Object rtn = po.__findattr__(propname);
            if (rtn instanceof PyNone) {
                // handle python None correctly
                return null;
            } else {
                return rtn;
            }
        } catch (Exception e) {
            log.debug("PyGetProperty.invoke " + propname + "." + attrName
                    + ", " + info);
        }
        return null;
    }

    /**
     * is this property cacheable
     */
    public boolean isCacheable() {
        return true;
    }
}

/**
 * a jython velocity SET property
 */
class PySetProperty implements VelPropertySet {
    private static final Logger log = Logger.getLogger(JythonUberspect.class
            .getName());
    private PyString propname;

    public PySetProperty(String propname) {
        this.propname = new PyString(propname);
    }

    /**
     * the name of the property/attribute
     */
    public String getMethodName() {
        return propname.toString();
    }

    /**
     * set the value of a property
     */
    public Object invoke(Object o, Object arg) {
        PyObject po = (PyObject) o;
        try {
            if (arg instanceof String) {
                po.__setattr__(propname, new PyString((String) arg));
            } else if (arg instanceof PyObject) {
                po.__setattr__(propname, (PyObject) arg);
            } else {
                log.debug("unsupported argument type : "
                        + arg.getClass().getName());
            }
        } catch (Exception e) {
            log.debug("PySetProperty.invoke", e);
        }

        return null;
    }

    /**
     * is this property cacheable
     */
    public boolean isCacheable() {
        return true;
    }
}

/**
 * <p>
 * An Iterator wrapper for a PySequence.
 * </p>
 * <p>
 * WARNING : this class's operations are NOT synchronized. It is meant to be
 * used in a single thread, newly created for each use in the #foreach()
 * directive. If this is used or shared, synchronize in the next() method.
 * </p>
 * 
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @author Kent Johnson
 */
@SuppressWarnings("unchecked")
class PySequenceIterator implements Iterator {
    /**
     * The objects to iterate.
     */
    private PySequence seq;

    /**
     * The current position and size in the array.
     */
    private int pos;
    private int size;

    /**
     * Creates a new iterator instance for the specified array.
     * 
     * @param array
     *            The array for which an iterator is desired.
     */
    public PySequenceIterator(PySequence seq) {
        this.seq = seq;
        pos = 0;
        size = seq.__len__();
    }

    /**
     * Move to next element in the array.
     * 
     * @return The next object in the array.
     */
    public Object next() {
        if (pos < size) {
            return seq.__getitem__(pos++);
        }

        throw new NoSuchElementException("No more elements: " + pos + " / "
                + size);
    }

    /**
     * Check to see if there is another element in the array.
     * 
     * @return Whether there is another element.
     */
    public boolean hasNext() {
        return (pos < size);
    }

    /**
     * No op--merely added to satify the <code>Iterator</code> interface.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
