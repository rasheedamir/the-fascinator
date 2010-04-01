/// <reference path="jquery-1.2.6-vsdoc.js" />
(function($) {

    $.fn.annotateImage = function(options) {
        ///    <summary>
        ///        Creates annotations on the given image.
        ///     Images are loaded from the "getUrl" propety passed into the options.
        ///    </summary>
        var opts = $.extend({}, $.fn.annotateImage.defaults, options);
        var image = this;

        this.image = this;
        this.mode = 'view';

        // Assign defaults
        this.getUrl = opts.getUrl;
        this.saveUrl = opts.saveUrl;
        this.deleteUrl = opts.deleteUrl;
        this.editable = opts.editable;
        this.portalPath = opts.portalPath;
        this.contentUri = opts.contentUri;
        this.useAjax = opts.useAjax;
        this.notes = opts.notes;

        // Add the canvas
        this.canvas = $('<div class="image-annotate-canvas"><img max-width="800px" src="'+this.attr('src')+'" style="width: '+this.width()+'; display: block; position: absolute;"/><div class="image-annotate-view"></div><div class="image-annotate-edit"><div class="image-annotate-edit-area"></div></div></div>');
        //this.canvas = $('<div class="image-annotate-canvas"><div class="image-annotate-view"></div><div class="image-annotate-edit"><div class="image-annotate-edit-area"></div></div></div>');
        this.canvas.children('.image-annotate-edit').hide();
        this.canvas.children('.image-annotate-view').hide();
        this.image.after(this.canvas);

        // Give the canvas and the container their size and background
        this.canvas.height(this.height());
        this.canvas.width(this.width());
        //this.canvas.css('background-image', 'url("' + this.attr('src') + '")');
        this.canvas.children('.image-annotate-view, .image-annotate-edit').height(this.height());
        this.canvas.children('.image-annotate-view, .image-annotate-edit').width(this.width());

        // Add the behavior: hide/show the notes when hovering the picture
        this.canvas.hover(function() {
            if ($(this).children('.image-annotate-edit').css('display') == 'none') {
                $(this).children('.image-annotate-view').show();
            }
            $('.image-annotate-area').each(function() {
                if ($(this).css('display') == 'none') {
                    $(this).show();
                }
            });
        }, function() {
            $(this).children('.image-annotate-view').hide();
            $('.image-annotate-area').each(function() {
                if ($(this).css('display') == 'block') {
                    $(this).hide();
                }
            });
        });

        this.canvas.children('.image-annotate-view').hover(function() {
            $(this).show();
        }, function() {
            $(this).hide();
        });

        // load the notes
        if (this.useAjax) {
            $.fn.annotateImage.ajaxLoad(this);
        } else {
            $.fn.annotateImage.load(this);
        }

        // Add the "Add a note" button
        if (this.editable) {
            this.button = $('<p><a class="image-annotate-add" id="image-annotate-add" href="#">Tag image</a></p>');
            this.button.click(function() {
                $.fn.annotateImage.add(image);
            });
            this.canvas.after(this.button);
        }

        this.list = $('<div class="image-annotate-list"><strong>Image Tags: </strong></div>');
        this.canvas.after(this.list);
        
        // Hide the original
        this.hide();

        return this;
    };

    /**
    * Plugin Defaults
    **/
    $.fn.annotateImage.defaults = {
        getUrl: 'your-get.rails',
        saveUrl: 'your-save.rails',
        deleteUrl: 'your-delete.rails',
        editable: true,
        useAjax: true,
        notes: new Array()
    };

    $.fn.annotateImage.clear = function(image) {
        ///    <summary>
        ///        Clears all existing annotations from the image.
        ///    </summary>    
        for (var i = 0; i < image.notes.length; i++) {
            image.notes[image.notes[i]].destroy();
        }
        image.notes = new Array();
    };

    $.fn.annotateImage.ajaxLoad = function(image) {
        ///    <summary>
        ///        Loads the annotations from the "getUrl" property passed in on the
        ///     options object.
        ///    </summary>
        $.getJSON(image.getUrl + '?ticks=' + $.fn.annotateImage.getTicks(), function(data) {
            image.notes = data;
            $.fn.annotateImage.load(image);
        });
    };

    $.fn.annotateImage.load = function(image) {
        ///    <summary>
        ///        Loads the annotations from the notes property passed in on the
        ///     options object.
        ///    </summary>
        for (var i = 0; i < image.notes.length; i++) {
            image.notes[image.notes[i]] = new $.fn.annotateView(image, image.notes[i], "");
        }
    };

    $.fn.annotateImage.getTicks = function() {
        ///    <summary>
        ///        Gets a count og the ticks for the current date.
        ///     This is used to ensure that URLs are always unique and not cached by the browser.
        ///    </summary>        
        var now = new Date();
        return now.getTime();
    };

    $.fn.annotateImage.add = function(image) {
        ///    <summary>
        ///        Adds a note to the image.
        ///    </summary>        
        if (image.mode == 'view') {
            image.mode = 'edit';
            // Create/prepare the editable note elements
            var editable = new $.fn.annotateEdit(image);

            $.fn.annotateImage.createSaveButton(editable, image);
            $.fn.annotateImage.createCancelButton(editable, image);
        }
    };

    $.fn.annotateImage.createSaveButton = function(editable, image, note) {
        ///    <summary>
        ///        Creates a Save button on the editable note.
        ///    </summary>
        var ok = $('<a class="image-annotate-edit-ok">OK</a>');

        ok.click(function() {
            var form = $('#image-annotate-edit-form form');
            var text = $('#image-annotate-text').val();
            $.fn.annotateImage.appendPosition(form, editable)
            image.mode = 'view';

            // Save via AJAX
            if (image.useAjax) {
                $.ajax({
                    url: image.saveUrl,
                    data: form.serialize(),
                    error: function(e) { alert("An error occured saving that note.") },
                    success: function(data) {
                if (data.annotation_id != undefined) {
                    editable.note.id = data.annotation_id;
                }
            },
                    dataType: "json"
                });
            }

            // Add to canvas
            if (note) {
                note.resetPosition(editable, text);
            } else {
                editable.note.editable = true;
                note = new $.fn.annotateView(image, editable.note, text)
                note.resetPosition(editable, text);
                image.notes.push(editable.note);
            }

            editable.destroy();
        });
        editable.form.append(ok);
    };

    $.fn.annotateImage.createCancelButton = function(editable, image) {
        ///    <summary>
        ///        Creates a Cancel button on the editable note.
        ///    </summary>
        var cancel = $('<a class="image-annotate-edit-close">Cancel</a>');
        cancel.click(function() {
            editable.destroy();
            image.mode = 'view';
        });
        editable.form.append(cancel);
    };

    $.fn.annotateImage.saveAsHtml = function(image, target) {
        var element = $(target);
        var html = "";
        for (var i = 0; i < image.notes.length; i++) {
            html += $.fn.annotateImage.createHiddenField("text_" + i, image.notes[i].text);
            html += $.fn.annotateImage.createHiddenField("top_" + i, image.notes[i].top);
            html += $.fn.annotateImage.createHiddenField("left_" + i, image.notes[i].left);
            html += $.fn.annotateImage.createHiddenField("height_" + i, image.notes[i].height);
            html += $.fn.annotateImage.createHiddenField("width_" + i, image.notes[i].width);
        }
        element.html(html);
    };

    $.fn.annotateImage.createHiddenField = function(name, value) {
        return '&lt;input type="hidden" name="' + name + '" value="' + value + '" /&gt;<br />';
    };

    $.fn.annotateEdit = function(image, note) {
        ///    <summary>
        ///        Defines an editable annotation area.
        ///    </summary>
        this.image = image;

        if (note) {
            this.note = note;
        } else {
            
            //Adding people search form
            
            
            var newNote = new Object();
            newNote.id = "new";
            newNote.top = 30;
            newNote.left = 30;
            newNote.width = 30;
            newNote.height = 30;
            newNote.text = "";
            this.note = newNote;
        }

        // Set area
        var area = image.canvas.children('.image-annotate-edit').children('.image-annotate-edit-area');
        this.area = area;
        this.area.css('height', this.note.height + 'px');
        this.area.css('width', this.note.width + 'px');
        this.area.css('left', this.note.left + 'px');
        this.area.css('top', this.note.top + 'px');

        // Show the edition canvas and hide the view canvas
        image.canvas.children('.image-annotate-view').hide();
        image.canvas.children('.image-annotate-edit').show();

        var peopleForm = '<div id="image_people_div" class="hidden">' +
                        '<label for="txtFirstNameImage" style="width: 6em; float: left; text-align: right; margin-right: 0.5em; display: block">First Name</label>' +
                            '<input type="text" id="txtFirstNameImage" size="35"/><br/>' +
                        '<label for="txtSurnameImage" style="width: 6em; float: left; text-align: right; margin-right: 0.5em; display: block">Surname</label>' +
                            '<input type="text" id="txtSurnameImage" size="35"/><br/>' +
                        '<button class="image-peopleTag-search" style="margin-left: 6.5em;">Search</button>' +
                        '<div class="image_search_results"></div>' +
                        '<div class="image_people_search_detail"><strong>This system searches for Australians ' + 
                            'listed in the Nominal Roll of Vietnam Veterans from <br/>The Australian Department of Veterans\' Affairs ' +
                            '(<a href="http://www.vietnamroll.gov.au" target="_blank">' + 
                            'http://www.vietnamroll.gov.au</a>)</strong></div>' +
                      '</div>'
        
        $("#peopleTag").live("click", function() {
            $('#image-annotate-text').hide();
            $('#image_people_div').show();
            $(".image-annotate-edit-ok").hide();
        });
        
        $("#freeTextTag").live("click", function() {
            $('#image-annotate-text').show();
            $('#image_people_div').hide();
            $(".image-annotate-edit-ok").show();
        });
        
        $(".image-peopleTag-search").live("click", function () {
            jQuery.post(image.portalPath + "/actions/people.ajax", { 
                func: "searchName",
                firstName: $("#txtFirstNameImage").attr("value"),
                surname: $("#txtSurnameImage").attr("value")
            }, function(data) {
                if (data.indexOf("No veteran") < 0 && data.indexOf("Could not connect") < 0) {
                    $(".image_people_search_detail").hide();
                    $(".image-annotate-edit-ok").show();
                } else {
                    $(".image_people_search_detail").show();
                    $(".image-annotate-edit-ok").hide();
                }
                $(".image_search_results").show();
                $(".image_search_results").html(data); 
            }); 
        });
        
        $("#selected_people").live("click", function() {
            $("#image-annotate-text").attr("value", $(this).attr("value"));
            $("#image-annotate-contentUri").attr("value", $(this).attr("rel"));
        });
        
        // Add the note (which we'll load with the form afterwards)
        //var form = $('<div id="image-annotate-edit-form"><form><textarea id="image-annotate-text" name="text" rows="3" cols="30">' + this.note.text + '</textarea></form></div>');
        var form = $('<div id="image-annotate-edit-form"><form>' +
                     '<input type="radio" name="image_scope" id="freeTextTag" checked="true" />Free Text Tag' +
                     '<input type="radio" name="image_scope" id="peopleTag"/>People Tag' +
                     peopleForm + 
                     '<div><textarea id="image-annotate-text" name="text" rows="3" cols="30">' + 
                     this.note.text + '</textarea></div><input type="hidden" id="image-annotate-contentUri" name="contentUri"/></form></div>');
                     
        this.form = form;

        $('body').append(this.form);
        this.form.css('left', this.area.offset().left + 'px');
        this.form.css('top', (parseInt(this.area.offset().top) + parseInt(this.area.height()) + 7) + 'px');

        // Set the area as a draggable/resizable element contained in the image canvas.
        // Would be better to use the containment option for resizable but buggy
        area.resizable({
            handles: 'all',

            stop: function(e, ui) {
                form.css('left', area.offset().left + 'px');
                form.css('top', (parseInt(area.offset().top) + parseInt(area.height()) + 2) + 'px');
            }
        })
        .draggable({
            containment: image.canvas,
            drag: function(e, ui) {
                form.css('left', area.offset().left + 'px');
                form.css('top', (parseInt(area.offset().top) + parseInt(area.height()) + 2) + 'px');
            },
            stop: function(e, ui) {
                form.css('left', area.offset().left + 'px');
                form.css('top', (parseInt(area.offset().top) + parseInt(area.height()) + 2) + 'px');
            }
        });
        return this;
    };

    $.fn.annotateEdit.prototype.destroy = function() {
        ///    <summary>
        ///        Destroys an editable annotation area.
        ///    </summary>        
        this.image.canvas.children('.image-annotate-edit').hide();
        this.area.resizable('destroy');
        this.area.draggable('destroy');
        this.area.css('height', '');
        this.area.css('width', '');
        this.area.css('left', '');
        this.area.css('top', '');
        this.form.remove();
    }

    $.fn.annotateView = function(image, note, text) {
        ///    <summary>
        ///        Defines a annotation area.
        ///    </summary>
        this.image = image;

        this.note = note;

        this.editable = (note.editable && image.editable);

        // Add the area
        this.area = $('<div class="image-annotate-area' + (this.editable ? ' image-annotate-area-editable' : '') + '"><div></div></div>');
        image.canvas.children('.image-annotate-view').prepend(this.area);

        // Add the note
        this.form = $('<div class="image-annotate-note">' + note.text + '</div>');
        this.form.hide();
        image.canvas.children('.image-annotate-view').append(this.form);
        this.form.children('span.actions').hide();
        
        // Add the note to list
        var tagText=text;
        if (note.text)
            tagText = note.text;
        if (note.contentUri)
            tagText = '<a href="' + note.contentUri + '" target="_blank">' + tagText + "</a>";
        this.imageList = $('<span class="tags-tag">' + tagText + '</span>');
        $('.image-annotate-list').append(this.imageList);
        
        // Set the position and size of the note
        this.setPosition();

        // Add the behavior: hide/display the note when hovering the area
        var annotation = this;
        this.area.hover(function() {
            annotation.show();
        }, function() {
            annotation.hide();
        });

        var imageArea = this.area;
        this.imageList.hover(function() {
            $('.image-annotate-area').each(function() {
                if ($(this).css('display') == 'block') {
                    $(this).hide();
                }
            });
            image.canvas.children('.image-annotate-view').show();
            annotation.form.show();
            annotation.area.show();
        }, function() {
            image.canvas.children('.image-annotate-view').hide();
            annotation.form.hide();
            annotation.area.hide();
        });

        // Edit a note feature
        if (this.editable) {
            var form = this;
            this.area.click(function() {
                form.edit();
            });
        }
    };

    $.fn.annotateView.prototype.setPosition = function() {
        ///    <summary>
        ///        Sets the position of an annotation.
        ///    </summary>
        this.area.children('div').height((parseInt(this.note.height) - 2) + 'px');
        this.area.children('div').width((parseInt(this.note.width) - 2) + 'px');
        this.area.css('left', (this.note.left) + 'px');
        this.area.css('top', (this.note.top) + 'px');
        this.form.css('left', (this.note.left) + 'px');
        this.form.css('top', (parseInt(this.note.top) + parseInt(this.note.height) + 7) + 'px');
    };

    $.fn.annotateView.prototype.show = function() {
        ///    <summary>
        ///        Highlights the annotation
        ///    </summary>
        this.form.fadeIn(250);
        if (!this.editable) {
            this.area.addClass('image-annotate-area-hover');
        } else {
            this.area.addClass('image-annotate-area-editable-hover');
        }
    };

    $.fn.annotateView.prototype.hide = function() {
        ///    <summary>
        ///        Removes the highlight from the annotation.
        ///    </summary>      
        this.form.fadeOut(250);
        this.area.removeClass('image-annotate-area-hover');
        this.area.removeClass('image-annotate-area-editable-hover');
    };

    $.fn.annotateView.prototype.destroy = function() {
        ///    <summary>
        ///        Destroys the annotation.
        ///    </summary>      
        this.area.remove();
        this.form.remove();
    }

    $.fn.annotateView.prototype.edit = function() {
        ///    <summary>
        ///        Edits the annotation.
        ///    </summary>      
        if (this.image.mode == 'view') {
            this.image.mode = 'edit';
            var annotation = this;

            // Create/prepare the editable note elements
            var editable = new $.fn.annotateEdit(this.image, this.note);

            $.fn.annotateImage.createSaveButton(editable, this.image, annotation);

            // Add the delete button
            var del = $('<a class="image-annotate-edit-delete">Delete</a>');
            del.click(function() {
                var form = $('#image-annotate-edit-form form');

                $.fn.annotateImage.appendPosition(form, editable)

                if (annotation.image.useAjax) {
                    $.ajax({
                        url: annotation.image.deleteUrl,
                        data: form.serialize(),
                        error: function(e) { alert("An error occured deleting that note.") }
                    });
                }

                annotation.image.mode = 'view';
                editable.destroy();
                annotation.destroy();
            });
            editable.form.append(del);

            $.fn.annotateImage.createCancelButton(editable, this.image);
        }
    };

    $.fn.annotateImage.appendPosition = function(form, editable) {
        ///    <summary>
        ///        Appends the annotations coordinates to the given form that is posted to the server.
        ///    </summary>
        var areaFields = $('<input type="hidden" value="' + editable.area.height() + '" name="height"/>' +
                           '<input type="hidden" value="' + editable.area.width() + '" name="width"/>' +
                           '<input type="hidden" value="' + editable.area.position().top + '" name="top"/>' +
                           '<input type="hidden" value="' + editable.area.position().left + '" name="left"/>' +
                           '<input type="hidden" value="' + editable.note.id + '" name="id"/>');
        form.append(areaFields);
    }

    $.fn.annotateView.prototype.resetPosition = function(editable, text) {
        ///    <summary>
        ///        Sets the position of an annotation.
        ///    </summary>
        this.form.html(text);
        this.form.hide();

        // Resize
        this.area.children('div').height(editable.area.height() + 'px');
        this.area.children('div').width((editable.area.width() - 2) + 'px');
        this.area.css('left', (editable.area.position().left) + 'px');
        this.area.css('top', (editable.area.position().top) + 'px');
        this.form.css('left', (editable.area.position().left) + 'px');
        this.form.css('top', (parseInt(editable.area.position().top) + parseInt(editable.area.height()) + 7) + 'px');

        // Save new position to note
        this.note.top = editable.area.position().top;
        this.note.left = editable.area.position().left;
        this.note.height = editable.area.height();
        this.note.width = editable.area.width();
        this.note.text = text;
        this.note.id = editable.note.id;
        this.editable = true;
    };

})(jQuery);