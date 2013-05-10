var FS = require('fs'),
    q = require('q'),
    sample_data = FS.readFileSync('data/sample.json').toString();


exports.handler = function (req, res) {

    res.writeHead(200, { 'Content-Type': 'application/json' });

    res.end(sample_data);

};

