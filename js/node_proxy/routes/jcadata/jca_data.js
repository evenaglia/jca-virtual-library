var FS = require('fs');

var jca_data = JSON.parse(FS.readFileSync('data/sample.json').toString());

exports.get_jca_data = function() {
    return jca_data;
};

exports.set_jca_data = function(json) {
    jca_data = json;
}