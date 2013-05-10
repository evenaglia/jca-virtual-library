
var FS = require('fs'),
    express = require('express');

var options = JSON.parse(FS.readFileSync('config.json').toString());

exports.options = function() {
    return options;
};

exports.do_config = function(app) {
    app.use(express.bodyParser());
    app.use(express.logger('dev'));
    app.use(express.methodOverride());
    app.use(app.router);


    //Set up routes...
    app.get('/update', require('./routes/jcadata/post').handler);
    app.get('/jcadata', require('./routes/jcadata/get').handler);

};

exports.start_app = function(app) {
    var port = options["port"] || process.env['port'];
    console.log("Express server listening on port " + port);
    app.listen(port);
};