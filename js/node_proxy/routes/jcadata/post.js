var FS = require('fs'),
    q = require('q'),
    jca_data = require('./jca_data');

exports.handler = function (req, res) {

    var body = req.body;
    jca_data.set_jca_data(JSON.parse(body)).then(
        // success
        function(saved) {
            res.writeHead(204, { 'Content-Type': 'application/json' });
            res.end('');
        },
        function(err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end('');
        }
    );

};