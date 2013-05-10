
var express = require('express'),
    config_app = require('./config_app');

var app = express();

config_app.do_config(app);

config_app.start_app(app);



