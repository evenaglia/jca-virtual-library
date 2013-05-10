var FS = require('fs'),
    q = require('q'),
    jca_data = require('./jca_data');


exports.handler = function (req, res) {

    res.writeHead(200, { 'Content-Type': 'application/json' });

    res.end(JSON.stringify(jca_data.get_jca_data(), null, '  '));

};

