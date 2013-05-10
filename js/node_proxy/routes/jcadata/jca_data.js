var FS = require('fs');
var jive = require('jive-sdk');

exports.get_jca_data = function(id) {
    if ( id ) {
        // return just one
        return jive.service.persistence().find( 'jcadata', {'id' : id});
    } else {
        return jive.service.persistence().find( 'jcadata' );
    }
};

exports.set_jca_data = function(json) {
    jive.logger.debug('Received', json);
    var id = json['id'];
    return jive.service.persistence().save( 'jcadata', id, json);
};