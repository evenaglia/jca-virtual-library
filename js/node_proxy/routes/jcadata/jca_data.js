var FS = require('fs');
var jive = require('jive-sdk');

exports.get_jca_data = function(server) {
    if ( server ) {
        // return just one
        return jive.service.persistence().find( 'jcadata', {'id' : server});
    } else {
        return jive.service.persistence().find( 'jcadata' );
    }
};

exports.set_jca_data = function(json) {
    jive.logger.debug('Received', json);
    var server = json['identifier'];
    return jive.service.persistence().save( 'jcadata', server, json);
};