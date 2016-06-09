/**
 * Created by felix on 6/6/16.
 */

class WebSocketEncap {

    constructor(url) {
        this.ws = new WebSocket(url);

        this.ws.onmessage = this._onmessage;
        this.ws.onclose = this._onclose;
        this.ws.onopen = this._onopen;
    }

    _onmessage (evt) {
        let json = JSON.parse(evt.data);
        if(this.handlers && this.handlers[json.type]) {
            for(let i = 0; i < this.handlers[json.type].length; i++) {
                this.handlers[json.type][i](json.data)
            }
        }
    }

    _onclose (evt) {
        if(this.handlers&& this.handlers["close"]) {
            for(let i = 0; i < this.handlers["close"].length; i++) {
                this.handlers["close"][i]();
            }
        }
    }

    _onopen (evt) {
        if(this.handlers && this.handlers["open"]) {
            for(let i = 0; i < this.handlers["open"].length; i++) {
                this.handlers["open"][i]();
            }
        }
    }

    on (type, callback) {
        if(type == "open" && this.ws.readyState == 1) {
            callback();
        }
        if(!this.ws.handlers) {
            this.ws.handlers = {};
            this.ws.handlers[type] = [];
        }
        if(!this.ws.handlers[type]) {
            this.ws.handlers[type] = [];
        }
        this.ws.handlers[type].push(callback);
    }

    send (type, data) {
        this.ws.send(JSON.stringify({type: type, data: data}));
    }
}

function ws(url) {
    return new WebSocketEncap(url);
}
