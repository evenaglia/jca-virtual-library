var FS = require('fs');
var jive = require('jive-sdk');

exports.get_jca_data = function(server) {
    if ( server ) {
        // return just one
        return jive.persistence.fetch( 'jcadata', {'id' : server});
    } else {
        return jive.persistence.fetch( 'jcadata' );
    }
};

exports.set_jca_data = function(json) {
    jive.logger.debug('Received', json);
    var server = json['identifier'];
    return jive.persistence.create( 'jcadata', server, json);
};