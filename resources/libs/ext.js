var Instascan = {};
//
Instascan.Scanner = function(opts) {};
//
Instascan.Scanner.prototype.stop = function() {};
Instascan.Scanner.prototype.start = function(camera) {};
Instascan.Scanner.prototype.stop = function(l, f) {};
Instascan.Scanner.prototype._configureVideo = function(opts) {};
Instascan.Scanner.prototype.removeAllListeners = function() {};
//
Instascan.Camera = function() {};
Instascan.Camera.prototype.getCameras = function() {};

var EventEmitter = function () {};