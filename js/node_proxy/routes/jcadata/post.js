var FS = require('fs'),
    q = require('q'),
    jca_data = require('./jca_data');

exports.handler = function (req, res) {

    var body = req.body;
    jca_data.set_jca_data(JSON.parse(body));


    res.writeHead(204, { 'Content-Type': 'application/json' });
    res.end('');

};