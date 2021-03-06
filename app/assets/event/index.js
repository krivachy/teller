/*
 * Happy Melly Teller
 * Copyright (C) 2013 - 2014, Happy Melly http://www.happymelly.com
 *
 * This file is part of the Happy Melly Teller.
 *
 * Happy Melly Teller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Happy Melly Teller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Happy Melly Teller.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you have questions concerning this license or the applicable additional terms, you may contact
 * by email Sergey Kotlov, sergey.kotlov@happymelly.com or
 * in writing Happy Melly One, Handelsplein 37, Rotterdam, The Netherlands, 3071 PR
 */

/**
 * Set a facilitator filter to current user (if exists)
 */
function initFacilitatorsFilter() {
    var id = $('#userId').val();
    if (id) {
        $('#facilitators').find('[value=' + id + ']').attr('selected', 'selected');
    }
}

/**
 * Initialize a brand filter with the brand which license the user helds
 */
function initBrandsFilter() {
    var personalLicense = $('#personalLicense').val();
    if (personalLicense) {
        $('#brands').find('[value=' + personalLicense + ']').attr('selected', 'selected');
    }
}

/**
 * Update a facilitators filter according to user actions
 *
 * @param brand Brand to filter
 */
function updateFacilitatorsFilter(brand) {
    var records = [];
    if (brand && brand != 'all') {
        var data = $.grep(facilitators, function (f) {
            return f.code === brand
        })[0].facilitators;
        records = data;
    } else {
        var ids = [];
        for (var i = 0; i < facilitators.length; i++) {
            var data = facilitators[i].facilitators;
            for (var j = 0; j < data.length; j++) {
                if (ids.indexOf(data[j].id) < 0) {
                    records.push(data[j]);
                    ids.push(data[j].id);
                }
            }
        }
    }
    $('#facilitators').empty().append('<option value="all">All</option>');
    records.sort(function(a, b) {
        return a.name < b.name ? -1 : (a.name > b.name ? 1 : 0);
    })
    for(var i = 0; i < records.length; i++) {
        if (records[i].name) {
            var option = $("<option></option>").attr("value", records[i].id).text(records[i].name);
            $('#facilitators').append(option);
        }
    }
}

/**
 * This function collects data from all filters and provides a requests based on their values
 * @returns {string}
 */
function makeRequestUrl() {
    var request = 'events/filtered?';
    var filter = $('#past-future').find(':selected').val();
    var counter = 0;
    if (filter != 'all') {
        request += 'future=' + ((filter == 'past') ? 'false' : 'true');
        counter += 1;
    }
    filter = $('#private').find(':selected').val();
    if (filter != 'all') {
        request += ((counter > 0) ? '&' : '') + 'public=' + ((filter == 'private') ? 'false' : 'true');
        counter += 1;
    }
    filter = $('#archived').find(':selected').val();
    if (filter != 'all') {
        request += ((counter > 0) ? '&' : '') + 'archived=' + ((filter == 'archived') ? 'true' : 'false');
        counter += 1;
    }
    filter = $('#brands').find(':selected').val();
    if (filter != 'all') {
        request += ((counter > 0) ? '&' : '') + 'brandCode=' + filter;
        counter += 1;
    }
    filter = $('#facilitators').find(':selected').val();
    if (filter != 'all') {
        request += ((counter > 0) ? '&' : '') + 'facilitator=' + filter;
    }
    return request;
}

$(document).ready( function() {
    updateFacilitatorsFilter(null);
    initBrandsFilter();
    initFacilitatorsFilter();

    var events = $('#events').dataTable({
        "sDom": '<"toolbar">rtip',
        "iDisplayLength": 25,
        "asStripeClasses":[],
        "aaSorting": [],
        "bLengthChange": false,
        "ajax": {
            "url" : makeRequestUrl(),
            "dataSrc": "",
            "deferRender": true
        },
        "order": [[ 4, "asc" ]],
        "columns": [
            { "data": "event" },
            { "data": "brand" },
            { "data": "facilitators" },
            { "data": "location" },
            { "data": "schedule" },
            { "data": "totalHours" },
            { "data": "materialsLanguage" },
            { "data": "invoice" },
            { "data": "confirmed" },
            { "data": "actions" }
        ],
        "columnDefs": [{
            "render": function(data) {
                return '<a href="' + data.url + '">' + data.title + '</a>';
            },
            "targets": 0
        }, {
            "render": function(data) {
                return '<a href="' + data.url + '">' + data.code + '</a>';
            },
            "targets": 1
        }, {
            "render": function(data) {
                var html = '';
                for (var i = 0; i < data.length; i++) {
                    html += '<div><a href="' + data[i].url + '">' + data[i].name + '</a><br/></div>';
                }
                return html;
            },
            "targets": 2
        },{
            "render": function(data) {
                return '<img align="absmiddle" width="16" src="/assets/images/flags/16/' +
                    data.country + '.png"/> ' + data.city;
            },
            "targets": 3
        }, {
            "render": function(data) {
                return data.start + ' / ' + data.end;
            },
            "targets": 4
        }, {
            "render": function(data) {
                if(data) {
                    return '<span class="label label-success">yes</span>';
                } else {
                    return '<span class="label label-danger">no</span>';
                }
            },
            "targets": 8
        },{
            "render": function(data) {
                var html = '';
                if ('edit' in data && data.edit.length > 0) {
                    html += '<a href="' + data.edit + '"><i class="glyphicon glyphicon-pencil"></i> Edit</a><br/>';
                    html += '<a href="' + data.duplicate + '"><i class="glyphicon glyphicon-edit"></i> Duplicate</a><br/>';
                }
                return html;
            },
            "targets": 9
        }]
    });
    $("body").css("cursor", "progress");
    events.on( 'xhr.dt', function () {
        $("body").css("cursor", "default");
    });

    $("div.toolbar").html($('#filter-containter').html());
    $('#filter-containter').empty();

    function updateTable() {
        $("body").css("cursor", "progress");
        events
            .api()
            .ajax
            .url(makeRequestUrl())
            .load(function(){
                $("body").css("cursor", "default");
            });
    }
    $("#brands").change(function() {
        var brand = $(this).find(':selected').val();
        updateFacilitatorsFilter(brand);
        updateTable(); });
    $('#past-future').on('change', function() { updateTable(); });
    $('#private').on('change', function() { updateTable(); });
    $('#archived').on('change', function() {  updateTable(); });
    $('#facilitators').on('change', function() { updateTable(); });

    events.fnDraw();
});
