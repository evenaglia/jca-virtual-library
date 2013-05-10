var q = require('q'),
    jca_data = require('./jca_data'),
    util = require('./util');

exports.handler = function (req, res) {

    if ( !util.validateSecret(req) ) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end('Invalid authorization');
        return;
    }

    var body = req.body;
    jca_data.set_jca_data(body).then(
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