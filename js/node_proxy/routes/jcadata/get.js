var FS = require('fs'),
    q = require('q'),
    url = require('url'),
    jca_data = require('./jca_data');


exports.handler = function (req, res) {
    var reqUrl = req.url;
    var url_parts = url.parse(reqUrl, true);
    var query = url_parts.query;

    var server = query['server'];

    jca_data.get_jca_data(server).then(

        function(data ) {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(data, null, '  '));
        },

        function( err ) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end('');
        }
    );

};

