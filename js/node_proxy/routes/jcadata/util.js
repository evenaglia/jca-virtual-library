var options = require( '../../config');

exports.validateSecret = function(req) {
    var auth = req.headers['authorization'];
    if ( !auth ) {
        return false;
    }

    var authParts = auth.split('Basic ');
    return authParts[1] === options['secret'];
};