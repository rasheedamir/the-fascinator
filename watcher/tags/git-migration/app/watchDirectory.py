#
#    Copyright (C) 2009  ADFI,
#    University of Southern Queensland
#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#

import re
import os
import glob

class WatchDirectory(object):
    # Constructor:
    #   WatchDirectory(path)
    # Properties:
    #   path
    #   watcher         # a FSWatcher (a placeholder 'tag' only. Not used internally)
    #   ignoreFileFilter
    #   ignoreDirectories
    # Methods:
    #   filter(file)
    #   addListener(listener)
    #   removeListener(listener)
    #   updateHandler(file, eventTime, eventName, isDir=False)
    #   walk(callback)-------------???
    #   __cmp__
    #   __str__
    def __init__(self, path):
        path = path.replace("\\", "/")
        if not path.endswith("/"):
            path += "/"
        self.__path = path
        self.__watcher = None
        self.__ignoreFileFilter = ""
        self.__ignoreDirectories = []
        self.__listeners = []
        self.__fileFilter = self.__createFilterFrom(self.__ignoreFileFilter)
        self.__stop = False
        self.__cxtTags = []


    @property
    def path(self):
        return self.__path

    def __getWatcher(self):
        return self.__watcher
    def __setWatcher(self, watcher):
        if self.__watcher is not None:
            self.__watcher.close()
        self.__watcher = watcher
    watcher = property(__getWatcher, __setWatcher)

    def __getIgnoreFileFilter(self):
        return self.__ignoreFileFilter
    def __setIgnoreFileFilter(self, ignoreFileFilter):
        self.__ignoreFileFilter = ignoreFileFilter
        self.__fileFilter = self.__createFilterFrom(self.__ignoreFileFilter)
    ignoreFileFilter = property(__getIgnoreFileFilter, __setIgnoreFileFilter)

    def __getIgnoreDirectories(self):
        return "|".join(self.__ignoreDirectories)
    def __setIgnoreDirectories(self, ignoreDirectories):
        #self.__ignoreDirectories = ignoreDirectories.split("|")
        splitIgnoreDirectories = ignoreDirectories.split("|")
        for dir in splitIgnoreDirectories:
                globFileList = glob.glob(os.path.join(self.__path,dir))
                for file in globFileList:
                    if file != []:
                        self.__ignoreDirectories.append(os.path.split(file)[1])
        #while self.__ignoreDirectories.count(""):
        #    self.__ignoreDirectories.remove("")
    ignoreDirectories = property(__getIgnoreDirectories, __setIgnoreDirectories)

    def __getStop(self):
        return self.__stop
    def __setStop(self, value):
        value = int(bool(value))
        self.__stop = value
    stop = property(__getStop, __setStop)

    def __getContextTags(self):
        return self.__cxtTags
    def __setContextTags(self, value):
        typeStr = str(type(value))
        if typeStr.find("dict")!=-1 or \
                typeStr.find("list")!=-1 or \
                typeStr.find("tuple")!=-1:
            value = list(value)
        else:
            value = [str(value)]
        self.__cxtTags = value
    contextTags = property(__getContextTags, __setContextTags)

    
    def filter(self, file):
        """
        returns False is we should be ignoring this file/dir
        """
        file = file[len(self.__path):]
        file = file.replace("\\", "/")
        try:
            path, filename = file.rsplit("/", 1)
            pathParts  = path.split("/")
        except:
            filename = file
            pathParts = []
        if self.__fileFilter is not None:
            filter = self.__fileFilter(filename)
            if filter:
                return False
        for part in pathParts:
            if part in self.__ignoreDirectories:
                return False
        return True

    
    def addListener(self, listener):
        self.__listeners.append(listener)


    def removeListener(self, listener):
        if self.__listeners.count(listener)>0:
            self.__listeners.remove(listener)
            return True
        return False

    
    def updateHandler(self, file, eventTime, eventName, isDir=False):
        if self.filter(file):
            for listener in self.__listeners:
                listener(file=file, eventTime=eventTime, eventName=eventName, \
                            isDir=isDir)

    
    def __createFilterFrom(self, wildCardStr):
        # note: for use with match (not search as search will match any where)
        wildCardStr = wildCardStr.strip()
        if wildCardStr=="":
            return None
        s = ""
        for c in wildCardStr:
            if c=="*":
                c = ".*"
            elif c=="?":
                c = "."
            elif c.isalnum() or c=="|":
                pass
            else:
                c = "\\" + c
            s += c
        parts = ["(^%s$)" % part for part in s.split("|")]
        while parts.count("(^$)"):
            parts.remove("(^$)")
        pattern = "|".join(parts)
        if pattern=="":
            return None
        cRegex = re.compile(pattern)
        def filter(str):
            return cRegex.match(str)!=None
        return filter


    def __cmp__(self, other):
        try:
        #self.__ignoreFileFilter = ""
        #self.__ignoreDirectories = []
            return self.name==other.name and \
                    self.__ignoreFileFilter==other.__ignoreFileFilter and \
                    self.__ignoreDirectories==other.__ignoreDirectories
        except:
            return False


    def __str__(self):
        #s = "[WatcherDirectory] path='%s' ignoreFileFilter='%s', ignoreDirectries=%s"
        #return s % (self.path, self.__ignoreFileFilter, self.__ignoreDirectories)
        s = self.path
        return s















