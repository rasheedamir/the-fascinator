/*
 * Anotar - Javascript Client Library v0.1
 * Copyright (C) 2010 University of Southern Queensland
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
 */
function Anotar() {

    var config = {
        "debug" : false,
        "style_prefix" : "anno-",
        "doc_root" : "#anno-root",
        "tag_list" : "p, h1, h2, h3, h4, h5, h6",
        "output_in_child" : null,
        "label" : "Comment on this:",
        "lang" : "en",

        "creator" : null,
        "creator_uri": "http://www.purl.org/anotar/ns/user/0.1#Anonymous",
        "creator_email": null,

        "client_uri" : "http://www.purl.org/anotar/client/0.1",
        "anno_type" : "http://www.w3.org/2000/10/annotationType#Comment",
        "server_address" : null,
        "server_mode" : "couch",

        "interface_label" : " &#xb6;",
        "interface_prepend" : false,
        "interface_visible" : false,

        "form_prepend" : false,
        "form_custom" : null,
        "form_cancel" : null,
        "form_submit" : null,
        "form_clear" : null,
        "form_close" : null,

        "display_custom" : null,

        "reply_label" : null,
        "reply_prepend" : true,
        "reply_visible" : true,
        "reply_uri_attr" : null,
        "reply_tag_list" : null,
        "disable_replies" : false,

        "hash_attr" : "anotar-hash",
        "hash_type" : "http://www.purl.org/anotar/locator/0.1",
        "hash_function": null,
        
        "submit_function" : null,
        "load_function" : null,

        "orphan_holder": null,
        "orphan_holder_id": null,

        "proxy_required" : true,
        "proxy_url" : null,

        "object_literal" : null,
        "page_uri" : null,
        "uri_attr" : null
    };

    // Variables accessible throughout this object
    var docRoot = null;
    var target_list = [];
    var last = null;
    var uri = null;
    var hash = null;
    var annotationComments = {};
    var annotateDiv = null;
    var commentOnThis = null;
    var textArea = null;
    var complete = false;
    var errorFlag = false;
    var errorMsg = "";
    var wait_count = 0;
    var wait_time = 500;
    var thisCallback = null;
    var isReply = false;
    var anonUri = "http://www.purl.org/anotar/ns/user/0.1#Anonymous";

    this.getConfig = function(property) {
        return config[property];
    }

    this.setConfig = function(property, value) {
        config[property] = value;
    }

    this.inheritConfig = function(newConfig) {
        // newConfig (as an onbject) has been passed by reference.
        // We need to set by value to avoid problems.
        for (var key in newConfig) {
            this.setConfig(key, newConfig[key]);
        }
    }

    this.isReplyInstance = function() {
        isReply = true;
        var opt = {style: config.style_prefix};

        config.page_uri = null;
        config.interface_visible = true;

        // How to find 'replyable' annotations
        if (config.reply_tag_list != null) {
            config.tag_list = config.reply_uri_attr;
        } else {
            config.tag_list = "." + config.style_prefix + "inline-annotation";
        }

        // How to get the annotations ID
        if (config.reply_uri_attr != null) {
            config.uri_attr = config.reply_uri_attr;
        } else {
            config.uri_attr = "id";
        }

        // What should our replay command look like
        if (config.reply_label != null) {
            config.interface_label = render_template(config.reply_label, opt);
        } else {
            config.interface_label = render_template(templates.replyButton, opt);
        }
        config.interface_prepend = config.reply_prepend;
        config.interface_visible = config.reply_visible;
    }

    this.setType = function(value){
        switch (value.toLowerCase()) {
            case "seealso":     value = "http://www.w3.org/2000/10/annotationType#SeeAlso";     break;
            case "question":    value = "http://www.w3.org/2000/10/annotationType#Question";    break;
            case "explanation": value = "http://www.w3.org/2000/10/annotationType#Explanation"; break;
            case "example":     value = "http://www.w3.org/2000/10/annotationType#Example";     break;
            case "comment":     value = "http://www.w3.org/2000/10/annotationType#Comment";     break;
            case "change":      value = "http://www.w3.org/2000/10/annotationType#Change";      break;
            case "advice":      value = "http://www.w3.org/2000/10/annotationType#Advice";      break;
            case "tag":         value = "http://www.purl.org/anotar/ns/type/0.1#Tag";           break;
            case "highlight":   value = "http://www.purl.org/anotar/ns/type/0.1#Highlight";     break;
            default: break;
        }
        this.setConfig("anno_type", value);
    }

    this.init = function(jQ) {
        // Sanity checking time
        docRoot = jQ(config.doc_root);
        if (docRoot.length == 0)
            debug_die("No document root (" + config.doc_root + ") found!");
        if (config.proxy_required &&
            config.proxy_url == null)
            debug_die("No proxy server provided!");
        if (config.server_address == null &&
           (config.submit_function == null || config.load_function == null))
            debug_die("No annotation server provided!");

        // If we haven't been given a URI or a way
        //   to find URIs then use the current URL.
        if (config.page_uri == null) {
            if (config.uri_attr == null) {
                config.page_uri = location.href;
                uri = location.href;
            }
        } else {
            uri = config.page_uri;
        }

        // Stash a reference to jQuery if we need it later
        this.setConfig("jQ", jQ);

        // Core logic
        this.findAnnotatables();
        this.addIds();
        this.addCommands();
        this.prepForm();
        this.loadAnnotations();

        // Make sure it all went smoothly
        wait_complete();
    }

    this.hasError = function() {
        return errorFlag;
    }

    this.getErrorMsg = function() {
        return errorMsg;
    }

    var wait_complete = function() {
        if (complete == false) {
            if ((wait_time * wait_count) < 30000) {
                setTimeout(function() {wait_complete();}, wait_time);
                wait_count++;
            } else {
                debug_die("Annotation server is taking too long to respond");
            }
        } else {
            if (errorFlag == true) {
                debug_die(errorMsg);
            } else {
                // If everything is good, load up replies.
                if (!isReply && !config.disable_replies) {
                    load_replies();
                }
            }
        }
    }

    var load_replies = function() {
        var replies = new Anotar();
        replies.inheritConfig(config);
        replies.isReplyInstance();
        replies.init(config.jQ);
    }

    var debug_die = function(message) {
        if (config.debug) {
            alert("DEBUG : " + message);
            return;
        } else {
            return;
        }
    }

    this.findAnnotatables = function() {
        // Get all nodes with valid selectors below the docRoot
        docRoot.find(config.tag_list).each(function() {
            if (this.innerHTML != "") {
                target_list.push(this);
            }
        });
    }

    this.addIds = function() {
        var jQ = config.jQ;
        var crcs = {}
        var me, tag, crc, c;

        // For every target
        var len = target_list.length;
        for (var i = 0; i < len; i++) {
            // jQuery init
            me = jQ(target_list[i]);
            tag = target_list[i].tagName;

            // Get the contents
            c = me.text();

            // Hash the contents
            crc = Crc32(c).toLowerCase();
            // Just in case there are identical paragraphs
            if (crcs[crc]) {crcs[crc].push(true); c = crcs[crc].length;}
            else {crcs[crc] = [true]; c = 1;}

            // Attach it to the DOM
            me.attr(config.hash_attr, "h" + crc + "p" + c);

            // If we are using per target URIs
            //  retrieve saved content here.
            if (config.page_uri == null &&
                    config.uri_attr != null) {
                uri = me.attr(config.uri_attr);
                this.loadAnnotations();
                uri = null;
            }
        }
    }

    this.addCommands = function() {
        var jQ = config.jQ;
        var opt = {style: config.style_prefix, label: config.interface_label};
        var interfaceText = render_template(templates.commandText, opt);
        var interfaceSpan = jQ(interfaceText);

        // The click even is common to both methods
        var interfaceClick = function(e) {
            // Remove the interface (if dynamic)
            if (!config.interface_visible) removeInterface();
            // Find the parent that is tagged
            var me = jQ(e.target).closest('[' + config.hash_attr + ']');
            if (me.size() > 0) annotate(me);

            return false;
        }

        var len = target_list.length;
        for (var i = 0; i < len; i++) {
            var node = jQ(target_list[i]);

            // Method 1 - Commands always visible
            if (config.interface_visible) {
                var newSpan = jQ(interfaceText);
                newSpan.click(interfaceClick);
                newSpan.mousedown(function(e) {return false;});
                newSpan.mouseup(interfaceClick); // for I.E.
                if (config.interface_prepend) {
                    node.prepend(newSpan);
                } else {
                    node.append(newSpan);
                }

            // Method 2 - Commands hide until mouseover
            } else {
                var iTimer = 0;

                var addInterface = function(jqe) {
                    if (iTimer) clearTimeout(iTimer);
                    if (config.interface_prepend) {
                        jqe.prepend(interfaceSpan);
                    } else {
                        jqe.append(interfaceSpan);
                    }
                    interfaceSpan.unbind();
                    interfaceSpan.mousedown(function(e) {return false;});
                    interfaceSpan.mouseup(interfaceClick); // for I.E.
                    interfaceSpan.click(interfaceClick);
                }

                var removeInterface = function() {
                    iTimer=setTimeout(function() {
                        iTimer=0;
                        interfaceSpan.remove();
                    }, 100);
                }

                node.mouseover(function(e) {
                    var me = jQ(e.target);

                    me.unbind();
                    me.mouseover(function(e) {addInterface(me);});
                    me.mouseout (function(e) {removeInterface();});
                    me.mousedown(function(e) {removeInterface();});
                    me.mouseover();
                });
            }
        }
    }

    this.prepForm = function() {
        var jQ = config.jQ;
        var sp = config.style_prefix;
        var opt = {style: sp, label: config.label};

        if (config.form_custom == null) {
            annotateDiv = render_template(templates.annotateForm, opt);
        } else {
            annotateDiv = render_template(config.form_custom, opt);
        }
        annotateDiv = jQ(annotateDiv);
        commentOnThis = jQ(render_template(templates.annotateTitle, opt));
        textArea = annotateDiv.find("textarea");

        if (config.form_cancel == null)
            this.setConfig("form_cancel", "button." + sp + "cancel");
        if (config.form_submit == null)
            this.setConfig("form_submit", "button." + sp + "submit");
        if (config.form_clear == null)
            this.setConfig("form_clear", "button." + sp + "clear");
        if (config.form_close == null)
            this.setConfig("form_close", "button." + sp + "close");
    }

    var annotate = function(me) {
        var jQ = config.jQ;
        var sp = config.style_prefix;
        var opt = {style: sp};

        // Hides the form
        var unWrapLast = function() {
            if (last != null) {
                annotateDiv.remove();
                commentOnThis.remove();
                last.parent().parent().replaceWith(last);
                last = null;
            }
        }

        // Cancel callback
        var closeClick = function() {
            unWrapLast();
            annotationComments[hash] = jQ.trim(textArea.val());
        }

        // Submit callback
        var submitClick = function() {
            var text, html, d, selfUrl;
            // Hide the form
            unWrapLast();
            // Wipe any saved text we have
            annotationComments[hash] = "";
            // Return if there's nothing to submit
            text = jQ.trim(textArea.val());
            if (text == "") return;
            // If this is a reply or not
            html = me.wrap("<div/>").parent().html();
            html = jQ(html).text();
            me.parent().replaceWith(me);

            hashValue = hash;
            func = config.hash_function;
            if (func != null) {
               hashValue = func(me);
            }
            data = {
                uri: uri,
                hash: hashValue,
                content: me.text(),
                body: text,
                title: null
            };
            if (config.uri_attr != null) {
                if (isReply) {
                    data.root = me.find("input[name='rootUri']").val();
                } else {
                    data.root = uri;
                }
            } else {
                data.root = uri;
            }
            data = createPayload(data);
            postNewAnnotation(data, annotateDiv);
        }

        // Retrieve comments if we have them
        var restore = function() {
            if (hash in annotationComments) {
                textArea.val(annotationComments[hash]);
            } else {
                textArea.val("");
            }
        }

        // Click logic starts.
        // Hide any other annotation forms that were visible
        if (last != null) {
            unWrapLast();
            annotateDiv.find(config.form_cancel).click();
        }

        // What are we annotating?
        hash = me.attr(config.hash_attr);
        if (typeof(hash) == "undefined") {return;}
        if (config.uri_attr != null) {
            uri = me.attr(config.uri_attr);
            if (typeof(uri) == "undefined") {
                // Our fallback is the hash
                uri = hash;
            }
            hash = null;
        }

        // Do we have any text saved?
        restore();

        // Render our form
        me.wrap(render_template(templates.annotateWrap, opt));
        if (config.form_prepend) {
            me.parent().prepend(annotateDiv);
        } else {
            me.parent().append(annotateDiv);
        }
        annotateDiv.find(config.form_clear).click(
            function() {textArea.val("");}
        );
        annotateDiv.find(config.form_cancel).click(
            function() {
                textArea.val(annotationComments[hash]);
                closeClick();
            }
        );
        annotateDiv.find(config.form_close).click(closeClick);
        annotateDiv.find(config.form_submit).click(submitClick);
        me.parent().prepend(commentOnThis);
        me.wrap(render_template(templates.annotateQuote, opt));

        last = me;
        textArea.focus();
    }

    var createPayload = function (data) {
        var schemaObject = {};

        // Version and profile
        schemaObject.clientVersionUri = config.client_uri;
        schemaObject.type = config.anno_type;

        schemaObject.title = {
            "literal": data.title,
            "uri": null
        };

        // Annotation target
        schemaObject.annotates = {
            "literal": config.object_literal,
            "uri": data.uri,
            "rootUri": data.root
        };

        // Annotation locator(s)
        if (data.hash != null && !isReply) {
            schemaObject.annotates.locators = [];
            var thisHash = {
                "originalContent": data.content,
                "type": config.hash_type,
                "value": data.hash
            };
            schemaObject.annotates.locators.push(thisHash);
        }

        // Creator
        schemaObject.creator = {
            "literal": config.creator,
            "uri": config.creator_uri,
            "email": {
                "literal": config.creator_email
            }
        };

        // Date handling
        schemaObject.dateCreated = {
            "literal": getW3cDateTimeString(),
            "uri": null
        };
        schemaObject.dateModified = {
            "literal": null,
            "uri": null
        };

        // Content
        schemaObject.content = {
            "mimeType": "text/plain",
            "literal": data.body,
            "formData": {}
        };
        schemaObject.contentUri = null;

        // Privacy
        schemaObject.isPrivate = false;

        // Language
        schemaObject.lang = config.lang;

        return schemaObject;
    }

    // Submission processing
    var postNewAnnotation = function(data, formObject){
        var thisUri = data.annotates.uri;
        // I'll do it my way?
        if (config.submit_function != null) {
            config.submit_function(data, formObject);

        // We'll do it our way?
        } else {
            var submitCallback = function (data, status) {
                var response = JSON.parse(data);
                switch (config.server_mode) {
                    case "fascinator":
                        loadAnnotation(response);
                        break;
                    default:
                        if (response.ok !== undefined && response.ok == true) {
                            var loadCallback = function (data, status) {
                                var anno = JSON.parse(data).rows[0].value;
                                if (isReply) {
                                    var parent = config.jQ("#" + uri);
                                    var child = parent.find("." + config.style_prefix + "anno-children");
                                    child.html(child.html() + renderAnnotation(anno));
                                } else {
                                    loadAnnotation(anno);
                                }
                            }
                            getAnnotation(thisUri, response.id, loadCallback);
                        } else {
                            alert("Sorry! An error occurred saving that data!");
                        }
                        break;
                }
            }

            var payload = null;
            switch (config.server_mode) {
                case "fascinator":
                    payload = {
                        action: "put",
                        rootUri: data.annotates.rootUri,
                        json: JSON.stringify(data)
                    };
                    break;
                default:
                    payload = JSON.stringify(data);
                    break;
            }

            var request = config.server_address;
            var method  = "POST";
            if (config.proxy_required) {
                getProxiedData(request, method, payload, submitCallback);
            } else {
                getDirectData(request, method, payload, submitCallback);
            }
        }
    }

    var getW3cDateTimeString = function() {
        var d = new Date();
        var zPad = function(n) {
            s = n.toString();
            if (s.length == 1)
                s = "0" + s;
            return s;
        }
        var timeZone = function() {
            tz = d.getTimezoneOffset() * 60 / -36;
            tz = tz.toString();
            if (tz[0] !== '-')
                tz = "+" + tz;
            while (tz.length < 5)
                tz = tz[0] + "0" + tz.substring(2);
            tz = tz.substring(0,3) + ":" + tz.substring(3);
            return(tz);
        }
        var response = '' +
            d.getFullYear() + "-" +
            zPad(d.getMonth() + 1) + "-" +
            zPad(d.getDate()) + "T" +
            zPad(d.getHours()) + ":" +
            zPad(d.getMinutes()) + ":" +
            zPad(d.getSeconds()) +
            timeZone()
        return response;
    }

    // Annotation retrieval
    this.loadAnnotations = function() {
        if (uri != null && !isReply) {
            // I'll do it my way?
            if (config.load_function != null) {
                config.load_function();
                complete = true;

            // We'll do it our way?
            } else {
                var loadCallback = function (data, status) {
                    var response = JSON.parse(data);
                    var len = response.length;
                    for (var i = 0; i < len; i++) {
                        loadAnnotation(response[i]);
                    }
                }
                getAnnotations(uri, loadCallback);
            }
        } else {
            complete = true;
        }
    }

    var loadAnnotation = function(annoObj) {
        // Get the object as it should be displayed
        var outputDiv = config.jQ(renderAnnotation(annoObj, "odd"));

        // Find how to attach it
        var annoUri = annoObj.annotates.uri;
        var annoHash = null;
        if (annoObj.annotates.locators !== undefined &&
            annoObj.annotates.locators.length > 0) {
            annoHash = annoObj.annotates.locators[0].value;
            if (config.hash_type != annoObj.annotates.locators[0].type) {
                return;
            }
        }

        // Find where to attach it
        var attached = false;
        var len = target_list.length;
        for (var i = 0; i < len; i++) {
            var node = config.jQ(target_list[i]);

            // Load by Hash
            if (config.uri_attr == null) {
                var thisHash = node.attr(config.hash_attr);

                if (thisHash == annoHash) {
                    attachAnnotation(node, outputDiv);
                    attached = true;
                }

            // Load by URI
            } else {
                var thisUri = node.attr(config.uri_attr);
                //console.log(thisUri, node.attr(config.uri_attr));
                if (thisUri == annoUri) {
                    attachAnnotation(node, outputDiv);
                    attached = true;
                }
            }
        }

        // Orphan handling
        if (!attached) {
            var orphanId = config.style_prefix + "orphans";
            var orphanTemplate = templates.orphanHolder;
            if (config.orphan_holder != null &&
                config.orphan_holder_id != null) {
                orphanId = config.orphan_holder_id;
                orphanTemplate = config.orphan_holder;
            }
            // Find our holder
            var holder = config.jQ("#" + orphanId);
            if (holder.size() == 0) {
                // Create the holder
                docRoot.append(render_template(orphanTemplate, {style: config.style_prefix}))
                holder = config.jQ("#" + orphanId);
            }

            //attachAnnotation(holder, outputDiv);
            holder.append(outputDiv);
        }
    }

    var getDisplayTemplate = function(type) {
        switch (type) {
            case "http://www.purl.org/anotar/ns/type/0.1#Tag":
                return templates.tagDisplay;
            case "http://www.purl.org/anotar/ns/type/0.1#Highlight":
                return templates.highlightDisplay;
            case "http://www.w3.org/2000/10/annotationType#SeeAlso":
                //return templates.seeAlsoDisplay;
            case "http://www.w3.org/2000/10/annotationType#Question":
                //return templates.questionDisplay;
            case "http://www.w3.org/2000/10/annotationType#Explanation":
                //return templates.explanationDisplay;
            case "http://www.w3.org/2000/10/annotationType#Example":
                //return templates.exampleDisplay;
            case "http://www.w3.org/2000/10/annotationType#Change":
                //return templates.changeDisplay;
            case "http://www.w3.org/2000/10/annotationType#Advice":
                //return templates.adviceDisplay;
            // Comments are also the default
            case "http://www.w3.org/2000/10/annotationType#Comment":
            default: return templates.commentDisplay;
        }
    }

    var renderAnnotation = function(annoObj, cssToggle) {
        var creator = "";
        // Use the literal first. eg. "Bob"
        if (annoObj.creator.literal != null) {
            creator = annoObj.creator.literal;
            // Can we add a URI to make a link?
            if (annoObj.creator.uri != null && annoObj.creator.uri.startsWith("http")) {
                creator = "<a href='" + annoObj.creator.uri +"'>" + creator + "</a>";
            }
        } else {
            // Or try their email address.
            if (annoObj.creator.email.literal != null) {
                creator = annoObj.creator.email.literal;
            } else {
                // Finally let's look for a URI, such as anonymous
                if (annoObj.creator.uri != null) {
                    creator = annoObj.creator.uri;
                    // An hide the anon URI since it's most common (and long)
                    if (creator == anonUri) {
                        creator = "<a href='" + anonUri +"'>Anonymous</a>";
                    }
                }
            }
        }

        if (annoObj.annotates.locators !== undefined)
            var origContent = annoObj.annotates.locators[0].originalContent;
        if (origContent == undefined) origContent = "";

        var childToggle = "";
        if (cssToggle == "odd") {
            childToggle = "even";
        } else {
            childToggle = "odd";
        }

        var replyString = "";
        if (annoObj.replies !== undefined) {
            var replies = annoObj.replies.length;
            if (replies > 0) {
                for (var i = 0; i < replies; i++) {
                    replyString += renderAnnotation(annoObj.replies[i], childToggle);
                }
            }
        }

        var opt = {
            style:     config.style_prefix,
            toggle:    cssToggle,
            id:        annoObj.uri,
            root:      annoObj.annotates.rootUri,
            original:  origContent,
            creator:   creator,
            date:      annoObj.dateCreated.literal,
            content:   annoObj.content.literal,
            children:  replyString,
            tag_count: annoObj.tagCount,
            locator:   null
        };
        if (annoObj.annotates.locators != null) {
            opt.locator = annoObj.annotates.locators[0].value;
        }
        var template = null;
        if (config.display_custom != null) {
            template = config.display_custom;
        } else {
            template = getDisplayTemplate(annoObj.type);
        }
        return render_template(template, opt);
    }

    var attachAnnotation = function(node, annotation) {
        if (config.output_in_child != null) {
            node.find(config.output_in_child).append(annotation);

        } else {
            if (isReply) {
                node.append(annotation);
            } else {
                var wrapClass = config.style_prefix + "has-annotation";

                if (!node.parent().hasClass(wrapClass)) {
                    node.wrap("<div class='" + wrapClass + "'/>");
                }

                node.parent().append(annotation);
            }
        }
    }

    var getAnnotation = function(obj, key, callback) {
        var baseQuery = "";
        var searchValue = "";
        switch (config.server_mode) {
            case "fascinator":
                debug_die("Not supported, use getAnnotations().");
                break;
            default:
                baseQuery = "_design/anotar/_view/id?key=";
                searchValue = escape('"' + key + '"');
                break;
        }

        var request = config.server_address + baseQuery + searchValue;
        var method  = "GET";

        if (config.proxy_required) {
            getProxiedData(request, method, null, callback);
        } else {
            getDirectData(request, method, null, callback);
        }
    }

    var getAnnotations = function(key, callback) {
        var baseQuery = "";
        var searchValue = "";
        switch (config.server_mode) {
            case "fascinator":
                baseQuery = "?action=getList";
                baseQuery += "&rootUri=" + escape(key);
                baseQuery += "&type=" + escape(config.anno_type);
                break;
            default:
                baseQuery = "_design/anotar/_list/nested/all?key=";
                searchValue = escape('"' + key + '"');
                break;
        }

        var request = config.server_address + baseQuery + searchValue;
        var method  = "GET";

        if (config.proxy_required) {
            getProxiedData(request, method, null, callback);
        } else {
            getDirectData(request, method, null, callback);
        }
    }

    var success = function(data, status) {
        complete = true;
        errorFlag = false;
        thisCallback(data, status);
    }
    var error = function(req, status, e) {
        errorFlag = true;
        errorMsg = "ERROR (STATUS: '" + status + "', CODE: '" + req.responseCode + "', BODY: '" + req.responseText + "')";
        complete = true;
    }

    var getDirectData = function(url, method, payload, callback) {
        thisCallback = callback;
        config.jQ.ajax({
            type: method,
            url: url,
            success: success,
            error: error,
            dataType: "text",
            data: payload
        });
    }

    var getProxiedData = function(url, method, payload, callback) {
        config.jQ.ajax({
            type: "POST",
            url: config.proxy_url,
            success: success,
            error: error,
            dataType: "text",
            data: {
                url: url,
                method: method,
                payload: payload
            }
        });
    }
}

//=======================================
// Template Rendering, Adapted from:
// http://ejohn.org/blog/javascript-micro-templating/
//=======================================
(function() {
    var cache = {};

    this.render_template = function render_template(str, data) {
        // Figure out if we're getting a template, or if we need to
        // load the template - and be sure to cache the result.
        var fn = !/\W/.test(str) ? cache[str] = cache[str] ||
                render_template(document.getElementById(str).innerHTML) :

            // Generate a reusable function that will serve as a template
            // generator (and which will be cached).
            new Function("obj",
                "var p=[];" +
                // Introduce the data as local variables using with(){}
                "with(obj) {p.push('" +

                // Convert the template into pure JavaScript
                str.replace(/[\r\t\n]/g, " ")
                    .replace(/'/g, "\r")
                    .split("<%").join("\t")
                    .replace(/((^|%>)[^\t]*)'/g, "$1\r")
                    .replace(/\t=(.*?)%>/g, "',$1,'")
                    .split("\t").join("');")
                    .split("%>").join("p.push('")
                    .split("\r").join("\\'")

                + "');} return p.join('');");
        // Provide some basic currying to the user
        return data ? fn( data ) : fn;
    };
})();

//=======================================
// Default Templates
//=======================================
var templates = {
    commandText: "<span class='<%=style%>command'><%=label%></span>",

    annotateForm:
        "<div class='<%=style%>annotate-form'>" +
          "<div class='<%=style%>annotate-form-elements'>" +
            "<textarea class='<%=style%>annotate-text'></textarea><br/>" +
            "<button class='<%=style%>cancel'>Cancel</button>&#160;" +
            "<button class='<%=style%>submit'>Submit</button> " +
          "</div>" +
          "<span class='<%=style%>info'></span>" +
        "</div>",
    annotateTitle: "<div class='<%=style%>app-label'><%=label%></div>",
    annotateWrap: "<div class='<%=style%>inline-annotation-form'/>",
    annotateQuote: "<blockquote class='<%=style%>inline-annotation-quote'/>",

    replyButton: "<button class='<%=style%>reply'>Reply</button>",

    commentDisplay:
        "<div class='<%=style%>inline-annotation <%=toggle%>' id='<%=id%>'>" +
            "<input name='rootUri' value='<%=root%>' type='hidden'/>" +
            "<div class='<%=style%>orig-content' style='display:none;'><%=original%></div>" +
            "<div class='<%=style%>anno-info'>" +
                "Comment by: <span class='<%=style%>anno-creator'><%=creator%></span>" +
                " &nbsp; <span class='<%=style%>anno-date'><%=date%></span>" +
            "</div>" +
            "<div class='<%=style%>anno-content'><%=content%></div>" +
            "<div class='<%=style%>anno-children'><%=children%></div>" +
        "</div>",

    seeAlsoDisplay: "",
    questionDisplay: "",
    explanationDisplay: "",
    exampleDisplay: "",
    changeDisplay: "",
    adviceDisplay: "",
    tagDisplay: "<span class='<%=style%>tag'><%=content%><% if(tag_count > 1){ %> (<%=tag_count%>)<% } %></span>",
    highlightDisplay: "",

    orphanHolder: "<p id='<%=style%>orphans'>Below are annotations that we can no longer attach to this document reliably because the data has changed.</p>"
}

// =======================================
// Crc32
// =======================================
function Crc32(str) {
    function Crc32Hex(str) {
        return Hex32(Crc32Str(str));
    }

    function Crc32Str(str) {
        var len = str.length;
        var crc = 0xFFFFFFFF;
        for (var n = 0; n < len; n++) {
            crc = Crc32Add(crc, str.charCodeAt(n));
        }
        return crc^0xFFFFFFFF;
    }

    function Hex32(val) {
        var n;
        var str1;
        var str2;
        n = val&0xFFFF;
        str1 = n.toString(16).toUpperCase();
        while (str1.length < 4) {
            str1 = "0" + str1;
        }
        n = (val>>>16)&0xFFFF;
        str2 = n.toString(16).toUpperCase();
        while (str2.length < 4) {
            str2 = "0" + str2;
        }
        return str2 + str1;
    }

    var Crc32Tab = [
        0x00000000,0x77073096,0xEE0E612C,0x990951BA,0x076DC419,0x706AF48F,0xE963A535,0x9E6495A3,
        0x0EDB8832,0x79DCB8A4,0xE0D5E91E,0x97D2D988,0x09B64C2B,0x7EB17CBD,0xE7B82D07,0x90BF1D91,
        0x1DB71064,0x6AB020F2,0xF3B97148,0x84BE41DE,0x1ADAD47D,0x6DDDE4EB,0xF4D4B551,0x83D385C7,
        0x136C9856,0x646BA8C0,0xFD62F97A,0x8A65C9EC,0x14015C4F,0x63066CD9,0xFA0F3D63,0x8D080DF5,
        0x3B6E20C8,0x4C69105E,0xD56041E4,0xA2677172,0x3C03E4D1,0x4B04D447,0xD20D85FD,0xA50AB56B,
        0x35B5A8FA,0x42B2986C,0xDBBBC9D6,0xACBCF940,0x32D86CE3,0x45DF5C75,0xDCD60DCF,0xABD13D59,
        0x26D930AC,0x51DE003A,0xC8D75180,0xBFD06116,0x21B4F4B5,0x56B3C423,0xCFBA9599,0xB8BDA50F,
        0x2802B89E,0x5F058808,0xC60CD9B2,0xB10BE924,0x2F6F7C87,0x58684C11,0xC1611DAB,0xB6662D3D,
        0x76DC4190,0x01DB7106,0x98D220BC,0xEFD5102A,0x71B18589,0x06B6B51F,0x9FBFE4A5,0xE8B8D433,
        0x7807C9A2,0x0F00F934,0x9609A88E,0xE10E9818,0x7F6A0DBB,0x086D3D2D,0x91646C97,0xE6635C01,
        0x6B6B51F4,0x1C6C6162,0x856530D8,0xF262004E,0x6C0695ED,0x1B01A57B,0x8208F4C1,0xF50FC457,
        0x65B0D9C6,0x12B7E950,0x8BBEB8EA,0xFCB9887C,0x62DD1DDF,0x15DA2D49,0x8CD37CF3,0xFBD44C65,
        0x4DB26158,0x3AB551CE,0xA3BC0074,0xD4BB30E2,0x4ADFA541,0x3DD895D7,0xA4D1C46D,0xD3D6F4FB,
        0x4369E96A,0x346ED9FC,0xAD678846,0xDA60B8D0,0x44042D73,0x33031DE5,0xAA0A4C5F,0xDD0D7CC9,
        0x5005713C,0x270241AA,0xBE0B1010,0xC90C2086,0x5768B525,0x206F85B3,0xB966D409,0xCE61E49F,
        0x5EDEF90E,0x29D9C998,0xB0D09822,0xC7D7A8B4,0x59B33D17,0x2EB40D81,0xB7BD5C3B,0xC0BA6CAD,
        0xEDB88320,0x9ABFB3B6,0x03B6E20C,0x74B1D29A,0xEAD54739,0x9DD277AF,0x04DB2615,0x73DC1683,
        0xE3630B12,0x94643B84,0x0D6D6A3E,0x7A6A5AA8,0xE40ECF0B,0x9309FF9D,0x0A00AE27,0x7D079EB1,
        0xF00F9344,0x8708A3D2,0x1E01F268,0x6906C2FE,0xF762575D,0x806567CB,0x196C3671,0x6E6B06E7,
        0xFED41B76,0x89D32BE0,0x10DA7A5A,0x67DD4ACC,0xF9B9DF6F,0x8EBEEFF9,0x17B7BE43,0x60B08ED5,
        0xD6D6A3E8,0xA1D1937E,0x38D8C2C4,0x4FDFF252,0xD1BB67F1,0xA6BC5767,0x3FB506DD,0x48B2364B,
        0xD80D2BDA,0xAF0A1B4C,0x36034AF6,0x41047A60,0xDF60EFC3,0xA867DF55,0x316E8EEF,0x4669BE79,
        0xCB61B38C,0xBC66831A,0x256FD2A0,0x5268E236,0xCC0C7795,0xBB0B4703,0x220216B9,0x5505262F,
        0xC5BA3BBE,0xB2BD0B28,0x2BB45A92,0x5CB36A04,0xC2D7FFA7,0xB5D0CF31,0x2CD99E8B,0x5BDEAE1D,
        0x9B64C2B0,0xEC63F226,0x756AA39C,0x026D930A,0x9C0906A9,0xEB0E363F,0x72076785,0x05005713,
        0x95BF4A82,0xE2B87A14,0x7BB12BAE,0x0CB61B38,0x92D28E9B,0xE5D5BE0D,0x7CDCEFB7,0x0BDBDF21,
        0x86D3D2D4,0xF1D4E242,0x68DDB3F8,0x1FDA836E,0x81BE16CD,0xF6B9265B,0x6FB077E1,0x18B74777,
        0x88085AE6,0xFF0F6A70,0x66063BCA,0x11010B5C,0x8F659EFF,0xF862AE69,0x616BFFD3,0x166CCF45,
        0xA00AE278,0xD70DD2EE,0x4E048354,0x3903B3C2,0xA7672661,0xD06016F7,0x4969474D,0x3E6E77DB,
        0xAED16A4A,0xD9D65ADC,0x40DF0B66,0x37D83BF0,0xA9BCAE53,0xDEBB9EC5,0x47B2CF7F,0x30B5FFE9,
        0xBDBDF21C,0xCABAC28A,0x53B39330,0x24B4A3A6,0xBAD03605,0xCDD70693,0x54DE5729,0x23D967BF,
        0xB3667A2E,0xC4614AB8,0x5D681B02,0x2A6F2B94,0xB40BBE37,0xC30C8EA1,0x5A05DF1B,0x2D02EF8D];

    function Crc32Add(crc, c) {
        return Crc32Tab[(crc^c)&0xFF]^((crc>>8)&0xFFFFFF);
    }

    return Crc32Hex(str);
}

// Basic String util
String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}